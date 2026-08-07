package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
 * Point d'attention particulier : les DEUX endpoints appliquent un contrôle
 * d'autorisation applicatif en couche service (UserService compare l'identité
 * portée par le token au compte ciblé) — ce n'est pas un rôle Spring Security
 * mais une vérification métier, qui répond 403 et non 401 : l'appelant est
 * authentifié, c'est la propriété de la ressource qui lui est refusée.
 *
 * Les tests de lecture/suppression croisée utilisent donc deux utilisateurs
 * bien distincts, chacun avec son propre email généré aléatoirement par
 * AbstractIntegrationTest#persistStandardUser(), pour ne jamais risquer de
 * comparer un utilisateur à lui-même par erreur (même piège que celui
 * identifié sur UserServiceTest#deleteById en unitaire). Symétriquement, les
 * tests du cas nominal utilisent generateTokenForUser(user) et non
 * generateStandardUserToken(), qui créerait un second utilisateur distinct.
 */
@AutoConfigureMockMvc
@Transactional
class UserControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Session persistSessionWithParticipant(User participant) {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());
        return sessionRepository.save(Session.builder()
                .name("Hatha Yoga")
                .date(new Date())
                .description("Séance découverte")
                .teacher(teacher)
                .users(new ArrayList<>(List.of(participant)))
                .build());
    }

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
        // Garde de fixture (pas le comportement testé) : userA et userB sont deux
        // comptes réellement distincts, chacun avec son propre email généré
        // aléatoirement par persistStandardUser() — condition nécessaire pour que
        // le 403 ci-dessous prouve bien un contrôle croisé, pas une comparaison
        // d'un utilisateur à lui-même.
        User userA = persistStandardUser();
        User userB = persistStandardUser();
        String tokenA = generateTokenForUser(userA);

        mockMvc.perform(get("/api/user/{id}", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                // Aucune donnée de B ne fuite dans la réponse de refus.
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @WithMockUser
    void findById_returns404_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/user/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/user/{id}", "abc"))
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

    // Cas NORMAL (tout utilisateur inscrit à au moins une session), et non cas
    // limite : avant le fix, la suppression du compte laissait la ligne de la table
    // PARTICIPATE orpheline, la table de jointure étant portée par Session et User
    // ne déclarant aucune relation inverse (donc aucun cascade).
    //
    // Vérifié : ce test est bien rouge sans le fix. Nuance sur le mode d'échec —
    // en production (transaction courte, commit réel) c'est la contrainte FK
    // participate.user_id -> users.id qui saute, d'où la DataIntegrityViolation
    // non gérée et le 500 constaté au Postman du 24/07/2026 ; ici, dans la
    // transaction longue du test, Hibernate détecte l'incohérence plus tôt et lève
    // une TransientObjectException au flush. Cause identique, symptôme différent.
    @Test
    void delete_returns200AndClearsParticipations_whenUserDeletesOwnAccountWhileEnrolledInASession() throws Exception {
        User owner = persistStandardUser();
        Session session = persistSessionWithParticipant(owner);
        String ownerToken = generateTokenForUser(owner);

        mockMvc.perform(delete("/api/user/{id}", owner.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // Force l'exécution réelle des DELETE/UPDATE en base. Sans ce flush, le
        // contexte de persistance de ce test @Transactional pourrait différer les
        // écritures jusqu'au rollback : la violation de contrainte FK (comportement
        // d'avant le fix) ne serait alors jamais déclenchée et le test serait vert
        // à tort. Le clear() force ensuite les relectures ci-dessous à repartir de
        // la base et non du cache de premier niveau.
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findById(owner.getId())).isEmpty();
        Session reloaded = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(reloaded.getUsers()).extracting(User::getId).doesNotContain(owner.getId());
    }

    // Preuve du contrôle d'autorisation applicatif : un utilisateur authentifié
    // ne peut pas supprimer le compte d'un AUTRE utilisateur. userA et userB sont
    // deux comptes distincts, chacun avec son propre email aléatoire et son propre
    // token, pour exclure toute ambiguïté sur "qui essaie de supprimer qui".
    @Test
    void delete_returns403_whenUserTriesToDeleteAnotherUsersAccount() throws Exception {
        // Garde de fixture (pas le comportement testé) : userA et userB sont deux
        // comptes réellement distincts, chacun avec son propre email généré
        // aléatoirement par persistStandardUser() — condition nécessaire pour que
        // le 403 ci-dessous prouve bien un contrôle croisé, pas une comparaison
        // d'un utilisateur à lui-même.
        User userA = persistStandardUser();
        User userB = persistStandardUser();
        String tokenA = generateTokenForUser(userA);

        // 403 et non 401 : A est bien authentifié, c'est la propriété de la
        // ressource qui lui est refusée (même sémantique que SessionService).
        mockMvc.perform(delete("/api/user/{id}", userB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        // userB n'a pas été supprimé : le contrôle a bien bloqué l'opération avant
        // toute écriture, pas seulement renvoyé un statut d'erreur en façade.
        assertThat(userRepository.findById(userB.getId())).isPresent();
    }

    @Test
    @WithMockUser
    void delete_returns404_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void delete_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/user/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns401_whenNotAuthenticated() throws Exception {
        User user = persistStandardUser();

        mockMvc.perform(delete("/api/user/{id}", user.getId()))
                .andExpect(status().isUnauthorized());
    }
}
