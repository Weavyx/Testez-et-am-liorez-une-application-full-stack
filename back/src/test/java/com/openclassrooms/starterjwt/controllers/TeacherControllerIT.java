package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de TeacherController : vraie base MySQL (Testcontainers)
 * via AbstractIntegrationTest, requêtes passées par MockMvc.
 *
 * Authentification : un vrai token JWT est généré via
 * AbstractIntegrationTest.generateStandardUserToken() pour un utilisateur
 * réellement inséré en base, et envoyé en en-tête Authorization. Ce choix
 * (plutôt que @WithMockUser) fait passer chaque requête par le vrai
 * AuthTokenFilter (parsing + validation du JWT, puis chargement de
 * l'utilisateur via UserDetailsServiceImpl), ce qui valide la chaîne de
 * sécurité de bout en bout et non un SecurityContext injecté artificiellement.
 * Cette approche est conservée pour les prochains controllers d'intégration
 * (notamment AuthController, où un JWT réel est incontournable) afin de
 * rester cohérent.
 *
 * Isolation : @Transactional fait rollback la transaction ouverte par chaque
 * test à la fin de celui-ci, donc les données insérées (teacher, user) ne
 * fuient jamais vers le test suivant, sans nécessiter de nettoyage manuel.
 */
@AutoConfigureMockMvc
@Transactional
class TeacherControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    void findById_returns200AndTeacher_whenTeacherExists() throws Exception {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/teacher/{id}", teacher.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teacher.getId()))
                .andExpect(jsonPath("$.firstName").value("Margot"))
                .andExpect(jsonPath("$.lastName").value("Delahaye"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void findById_returns404_whenTeacherDoesNotExist() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/teacher/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/teacher/{id}", "abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returns401_whenNotAuthenticated() throws Exception {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());

        mockMvc.perform(get("/api/teacher/{id}", teacher.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findAll_returns200AndAllTeachers_whenAuthenticated() throws Exception {
        Teacher teacher1 = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());
        Teacher teacher2 = teacherRepository.save(Teacher.builder()
                .firstName("Hélène")
                .lastName("Thiercelin")
                .build());
        String token = generateStandardUserToken();

        mockMvc.perform(get("/api/teacher")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        teacher1.getId().intValue(), teacher2.getId().intValue())));
    }

    @Test
    void findAll_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/teacher"))
                .andExpect(status().isUnauthorized());
    }
}
