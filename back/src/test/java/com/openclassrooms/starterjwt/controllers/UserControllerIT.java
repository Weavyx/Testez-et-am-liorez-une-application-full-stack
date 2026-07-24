package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration d'UserController : vraie base MySQL (Testcontainers)
 * via AbstractIntegrationTest, requêtes passées par MockMvc, JWT réel (même
 * pattern que TeacherControllerIT/SessionControllerIT/AuthControllerIT).
 *
 * Point d'attention particulier : DELETE /api/user/{id} applique un contrôle
 * d'autorisation applicatif (UserService#deleteById compare le username porté
 * par le token à l'email du compte ciblé) — ce n'est pas un rôle Spring
 * Security mais une vérification métier. Les tests "delete...ByAnotherUser"
 * utilisent donc deux utilisateurs bien distincts, chacun avec son propre
 * email généré aléatoirement par AbstractIntegrationTest#persistStandardUser(),
 * pour ne jamais risquer de comparer un utilisateur à lui-même par erreur
 * (même piège que celui identifié sur UserServiceTest#deleteById en unitaire).
 */
@AutoConfigureMockMvc
@Transactional
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // ---------- GET /api/user/{id} ----------

    @Test
    void findById_returns200AndUserDto_whenUserReadsOwnAccount() throws Exception {
        User user = persistStandardUser();
        // Le token porte l'identité de `user` lui-même : c'est une lecture de son
        // PROPRE compte. Utiliser generateStandardUserToken() créerait un second
        // utilisateur distinct et testerait donc une lecture croisée (désormais 403).
        String ownToken = generateTokenForUser(user);

        mockMvc.perform(get("/api/user/{id}", user.getId())
                        .header("Authorization", "Bearer " + ownToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.firstName").value(user.getFirstName()))
                .andExpect(jsonPath("$.lastName").value(user.getLastName()))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                // Point de vigilance sécurité : le mot de passe (encodé ou non) ne doit
                // jamais apparaître dans la réponse, malgré @JsonIgnore sur UserDto#password.
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // Contrôle de propriété en lecture : un utilisateur authentifié ne peut pas
    // consulter la fiche d'un AUTRE compte (email, nom, statut admin). Même
    // construction que delete_returns403_whenUserTriesToDeleteAnotherUsersAccount :
    // deux comptes réellement distincts, chacun son email aléatoire.
    @Test
    void findById_returns403_whenUserReadsAnotherUsersAccount() throws Exception {
        User userA = persistStandardUser();
        User userB = persistStandardUser();
        assertThat(userA.getEmail()).isNotEqualTo(userB.getEmail());
        String tokenA = generateTokenForUser(userA);

        mockMvc.perform(get("/api/user/{id}", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                // Aucune donnée de B ne fuite dans la réponse de refus.
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void findById_returns404_whenUserDoesNotExist() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/user/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/user/{id}", "abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returns401_whenNotAuthenticated() throws Exception {
        User user = persistStandardUser();

        mockMvc.perform(get("/api/user/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---------- DELETE /api/user/{id} ----------

    @Test
    void delete_returns200_whenUserDeletesOwnAccount() throws Exception {
        User owner = persistStandardUser();
        String ownerToken = generateTokenForUser(owner);

        mockMvc.perform(delete("/api/user/{id}", owner.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(owner.getId())).isEmpty();
    }

    // Preuve du contrôle d'autorisation applicatif : un utilisateur authentifié
    // ne peut pas supprimer le compte d'un AUTRE utilisateur. userA et userB sont
    // deux comptes distincts, chacun avec son propre email aléatoire et son propre
    // token, pour exclure toute ambiguïté sur "qui essaie de supprimer qui".
    @Test
    void delete_returns401_whenUserTriesToDeleteAnotherUsersAccount() throws Exception {
        User userA = persistStandardUser();
        User userB = persistStandardUser();
        assertThat(userA.getEmail()).isNotEqualTo(userB.getEmail());
        String tokenA = generateTokenForUser(userA);

        mockMvc.perform(delete("/api/user/{id}", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnauthorized());

        // userB n'a pas été supprimé : le contrôle a bien bloqué l'opération avant
        // toute écriture, pas seulement renvoyé un statut d'erreur en façade.
        assertThat(userRepository.findById(userB.getId())).isPresent();
    }

    @Test
    void delete_returns404_whenUserDoesNotExist() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(delete("/api/user/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns400_whenIdIsNotNumeric() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(delete("/api/user/{id}", "abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns401_whenNotAuthenticated() throws Exception {
        User user = persistStandardUser();

        mockMvc.perform(delete("/api/user/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }
}
