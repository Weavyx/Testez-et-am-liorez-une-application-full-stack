package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
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
 * Authentification : les tests de logique métier utilisent @WithMockUser
 * (voir AUDIT_AUTH_WITHMOCKUSER.md) — ces endpoints ne testent pas le
 * parcours JWT lui-même, seulement l'accès à une ressource pour un
 * utilisateur authentifié. Le test findById_returns401_whenNotAuthenticated
 * / findAll_returns401_whenNotAuthenticated reste sans authentification pour
 * continuer à couvrir le rejet réel par AuthTokenFilter/authenticationEntryPoint
 * en l'absence de token.
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
    @WithMockUser
    void findById_returns200AndTeacher_whenTeacherExists() throws Exception {
        Teacher teacher = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());

        mockMvc.perform(get("/api/teacher/{id}", teacher.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(teacher.getId()))
                .andExpect(jsonPath("$.firstName").value("Margot"))
                .andExpect(jsonPath("$.lastName").value("Delahaye"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @WithMockUser
    void findById_returns404_whenTeacherDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/teacher/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/teacher/{id}", "abc"))
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
    @WithMockUser
    void findAll_returns200AndAllTeachers_whenAuthenticated() throws Exception {
        Teacher teacher1 = teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());
        Teacher teacher2 = teacherRepository.save(Teacher.builder()
                .firstName("Hélène")
                .lastName("Thiercelin")
                .build());

        mockMvc.perform(get("/api/teacher"))
                .andExpect(status().isOk())
                // hasSize(2) suppose la table vide en entrée de test : vrai ici grâce au
                // rollback @Transactional entre tests (aucun data.sql sous src/test/resources).
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        teacher1.getId().intValue(), teacher2.getId().intValue())))
                .andExpect(jsonPath("$[*].firstName", containsInAnyOrder("Margot", "Hélène")))
                .andExpect(jsonPath("$[*].lastName", containsInAnyOrder("Delahaye", "Thiercelin")));
    }

    @Test
    void findAll_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/teacher"))
                .andExpect(status().isUnauthorized());
    }
}
