package com.openclassrooms.starterjwt.controllers;

import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.Session;
import com.openclassrooms.starterjwt.models.Teacher;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.SessionRepository;
import com.openclassrooms.starterjwt.repository.TeacherRepository;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de SessionController : vraie base MySQL (Testcontainers)
 * via AbstractIntegrationTest, requêtes passées par MockMvc.
 *
 * Authentification (voir AUDIT_AUTH_WITHMOCKUSER.md) : les tests de logique
 * métier/RBAC utilisent @WithMockUser (avec roles = "ADMIN" quand hasRole("ADMIN")
 * est requis) plutôt qu'un JWT réel — ces endpoints ne testent pas le parcours
 * JWT lui-même. Les tests "*_returns401_whenNotAuthenticated" restent sans
 * authentification pour couvrir le rejet réel par AuthTokenFilter en l'absence
 * de token. Les tests dépendant de l'identité précise du principal (self vs
 * autre utilisateur, comparée via SessionService#assertRequestingUserIsSelf)
 * restent en JWT réel généré via generateTokenForUser()/generateStandardUserToken()
 * — sauf pour "*_returns400_whenAlreadyParticipating"/"*_whenNotParticipating",
 * qui atteignent aussi ce contrôle de propriété (donc un cast interne en
 * UserDetailsImpl) sans en faire l'objet : ceux-ci utilisent
 * SecurityMockMvcRequestPostProcessors.user(...) avec un UserDetailsImpl
 * portant l'id du participant (mockPrincipalFor), pour satisfaire ce cast tout
 * en restant un mock de SecurityContext, sans JWT réel.
 *
 * Ces tests couvrent aussi le fix de sécurité de WebSecurityConfig
 * (hasRole("ADMIN") sur POST/PUT/DELETE /api/session) : les cas
 * "*_returns403_whenCalledByNonAdmin" prouvent que la restriction est
 * effective, et les cas "participate/noLongerParticipate...ByNonAdmin"
 * prouvent que ce fix ne bloque pas les routes d'inscription, qui doivent
 * rester ouvertes à tout utilisateur authentifié.
 *
 * Les cas "participate/noLongerParticipate...UserIdDoesNotMatchAuthenticatedPrincipal"
 * couvrent le fix du contrôle de propriété (problème 2) : {userId} dans le path
 * doit correspondre à l'utilisateur authentifié, sinon 403.
 */
@AutoConfigureMockMvc
@Transactional
class SessionControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Même secret que JwtUtils (profil test, voir application-test.yml), pour
    // fabriquer un token expiré signé "correctement" mais avec une expiration
    // passée — voir findAll_returns401_whenTokenIsExpired.
    @Value("${oc.app.jwtSecret}")
    private String jwtSecret;

    private Teacher persistTeacher() {
        return teacherRepository.save(Teacher.builder()
                .firstName("Margot")
                .lastName("Delahaye")
                .build());
    }

    private Session persistSession(Teacher teacher) {
        return persistSession(teacher, "Hatha Yoga");
    }

    private Session persistSession(Teacher teacher, String name) {
        return sessionRepository.save(Session.builder()
                .name(name)
                .date(new Date())
                .description("Séance découverte")
                .teacher(teacher)
                .users(new ArrayList<>())
                .build());
    }

    private User persistParticipant() {
        return userRepository.save(User.builder()
                .email("participant-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .firstName("Jean")
                .lastName("Dupont")
                .password("encoded-password")
                .admin(false)
                .build());
    }

    private String validSessionJson(Long teacherId) {
        return "{"
                + "\"name\":\"Hatha Yoga\","
                + "\"date\":\"2026-08-01\","
                + "\"teacher_id\":" + teacherId + ","
                + "\"description\":\"Séance découverte\","
                + "\"users\":[]"
                + "}";
    }

    private String sessionJsonWithoutName(Long teacherId) {
        return "{"
                + "\"date\":\"2026-08-01\","
                + "\"teacher_id\":" + teacherId + ","
                + "\"description\":\"Séance découverte\""
                + "}";
    }

    // ---------- GET /api/session/{id} ----------

    @Test
    @WithMockUser
    void findById_returns200AndSession_whenSessionExists() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(get("/api/session/{id}", session.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.name").value("Hatha Yoga"))
                .andExpect(jsonPath("$.description").value("Séance découverte"))
                .andExpect(jsonPath("$.teacher_id").value(teacher.getId()))
                .andExpect(jsonPath("$.date").isNotEmpty())
                .andExpect(jsonPath("$.users", hasSize(0)))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @WithMockUser
    void findById_returns404_whenSessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/session/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void findById_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(get("/api/session/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returns401_whenNotAuthenticated() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(get("/api/session/{id}", session.getId()))
                .andExpect(status().isUnauthorized());
    }

    // ---------- GET /api/session ----------

    @Test
    @WithMockUser
    void findAll_returns200AndAllSessions_whenAuthenticated() throws Exception {
        Session session1 = persistSession(persistTeacher(), "Hatha Yoga");
        Session session2 = persistSession(persistTeacher(), "Vinyasa Flow");

        mockMvc.perform(get("/api/session"))
                .andExpect(status().isOk())
                // hasSize(2) suppose la table vide en entrée de test : vrai ici grâce au
                // rollback @Transactional entre tests (aucun data.sql sous src/test/resources).
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        session1.getId().intValue(), session2.getId().intValue())))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Hatha Yoga", "Vinyasa Flow")));
    }

    @Test
    void findAll_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/session"))
                .andExpect(status().isUnauthorized());
    }

    // Trou de couverture identifié dans AUDIT_AUTH_WITHMOCKUSER.md : aucun test
    // d'intégration n'exerçait un token présent mais invalide contre un vrai
    // endpoint protégé (seulement en unitaire, JwtUtilsTest/AuthTokenFilterTest).
    // Ici, AuthTokenFilter.validateJwtToken(...) échoue réellement (MalformedJwtException),
    // aucune authentication n'est posée dans le SecurityContext, et la requête est
    // rejetée par le vrai authenticationEntryPoint, comme pour findAll_returns401_whenNotAuthenticated.
    @Test
    void findAll_returns401_whenTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/session")
                        .header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // Même trou de couverture, cas du token expiré : signé avec le même secret que
    // celui utilisé par JwtUtils en profil test (application-test.yml) mais avec une
    // expiration déjà passée, pour que la vraie validation cryptographique
    // (JwtUtils.validateJwtToken) échoue sur ExpiredJwtException, pas sur une
    // signature invalide.
    @Test
    void findAll_returns401_whenTokenIsExpired() throws Exception {
        String expiredToken = Jwts.builder()
                .setSubject("expired-user@test.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20_000))
                .setExpiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();

        mockMvc.perform(get("/api/session")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    // ---------- POST /api/session ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns200_whenCalledByAdmin() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hatha Yoga"))
                .andExpect(jsonPath("$.description").value("Séance découverte"))
                .andExpect(jsonPath("$.teacher_id").value(teacher.getId()))
                .andExpect(jsonPath("$.date", containsString("2026-08-01")))
                .andExpect(jsonPath("$.users", hasSize(0)));
    }

    // Preuve du fix : un utilisateur authentifié mais non-admin ne peut pas créer de session.
    @Test
    @WithMockUser
    void create_returns403_whenCalledByNonAdmin() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns400_whenNameIsMissing() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionJsonWithoutName(teacher.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns401_whenNotAuthenticated() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(post("/api/session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ---------- PUT /api/session/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200_whenCalledByAdmin() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(put("/api/session/{id}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(session.getId()))
                .andExpect(jsonPath("$.name").value("Hatha Yoga"))
                .andExpect(jsonPath("$.description").value("Séance découverte"))
                .andExpect(jsonPath("$.teacher_id").value(teacher.getId()))
                .andExpect(jsonPath("$.date", containsString("2026-08-01")))
                .andExpect(jsonPath("$.users", hasSize(0)));
    }

    // Preuve du fix : un utilisateur authentifié mais non-admin ne peut pas modifier de session.
    @Test
    @WithMockUser
    void update_returns403_whenCalledByNonAdmin() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(put("/api/session/{id}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns400_whenIdIsNotNumeric() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(put("/api/session/{id}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isBadRequest());
    }

    // Test de contrat : hasRole("ADMIN") est évalué par Spring Security avant
    // que le controller n'atteigne Long.parseLong(id). Un id invalide combiné
    // à un appelant non-admin doit donc rester bloqué en 403, jamais retomber
    // en 400 (cas déjà couvert séparément par update_returns400_whenIdIsNotNumeric
    // avec un token admin, pour isoler la validation du format de la question de rôle).
    // Verrouille ce comportement contre une régression (ex. réordonnancement
    // accidentel des matchers de sécurité).
    @Test
    @WithMockUser
    void update_returns403NotBadRequest_whenCalledByNonAdminWithInvalidId() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(put("/api/session/{id}", "abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isForbidden());
    }

    // Preuve du fix (problème 1) : PUT sur un id inexistant retourne 404, pas 500.
    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns404_whenSessionDoesNotExist() throws Exception {
        Teacher teacher = persistTeacher();

        mockMvc.perform(put("/api/session/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns400_whenValidationFails() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(put("/api/session/{id}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionJsonWithoutName(teacher.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returns401_whenNotAuthenticated() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(put("/api/session/{id}", session.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSessionJson(teacher.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ---------- DELETE /api/session/{id} ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns200_whenCalledByAdmin() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(delete("/api/session/{id}", session.getId()))
                .andExpect(status().isOk());

        assertThat(sessionRepository.existsById(session.getId())).isFalse();
    }

    // Preuve du fix : un utilisateur authentifié mais non-admin ne peut pas supprimer de session.
    @Test
    @WithMockUser
    void delete_returns403_whenCalledByNonAdmin() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(delete("/api/session/{id}", session.getId()))
                .andExpect(status().isForbidden());
    }

    // P1-04 : Long.parseLong(id) est appelé directement dans le controller (pas
    // via un @PathVariable Long), donc c'est bien NumberFormatException qui est
    // levée, interceptée par GlobalExceptionHandler → 400 (même mécanisme que
    // findById_returns400_whenIdIsNotNumeric et update_returns400_whenIdIsNotNumeric).
    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns400_whenIdIsNotNumeric() throws Exception {
        mockMvc.perform(delete("/api/session/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns404_whenSessionDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/session/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns401_whenNotAuthenticated() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(delete("/api/session/{id}", session.getId()))
                .andExpect(status().isUnauthorized());
    }

    // Vérifié manuellement (Postman) : la suppression d'une session à laquelle des
    // utilisateurs sont inscrits réussit et la table de jointure PARTICIPATE est
    // nettoyée automatiquement par Hibernate (association @ManyToMany propriétaire
    // côté Session), sans CascadeType explicite ni code applicatif dédié — voir
    // AUDIT_PHASE4_POINTS_RESTANTS.md. Ce comportement n'était couvert par aucun
    // test avant ce cas ; il verrouille le pendant "session" du cas symétrique
    // côté compte utilisateur (delete_returns200AndClearsParticipations_when...
    // dans UserControllerIT).
    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns200AndClearsParticipations_whenSessionHasParticipants() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        session.getUsers().add(participant);
        sessionRepository.save(session);

        Long participateCountBefore = countParticipateRowsForSession(session.getId());
        assertThat(participateCountBefore).isEqualTo(1L);

        mockMvc.perform(delete("/api/session/{id}", session.getId()))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        assertThat(sessionRepository.existsById(session.getId())).isFalse();
        assertThat(countParticipateRowsForSession(session.getId())).isZero();
    }

    private Long countParticipateRowsForSession(Long sessionId) {
        Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM participate WHERE session_id = :sessionId");
        query.setParameter("sessionId", sessionId);
        return ((Number) query.getSingleResult()).longValue();
    }

    // ---------- POST /api/session/{id}/participate/{userId} ----------

    // Preuve que le fix ne casse pas participate : un non-admin authentifié peut
    // s'inscrire lui-même (le token est généré pour le même utilisateur que
    // {userId}, cohérent avec le fix de contrôle de propriété du problème 2).
    @Test
    void participate_returns200_whenNonAdminParticipatesForThemselves() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        String token = generateTokenForUser(participant);

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // Preuve du fix (problème 2) : un utilisateur authentifié ne peut pas inscrire
    // un autre utilisateur que lui-même.
    @Test
    void participate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        String token = generateStandardUserToken();

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // P1-04 : id non-numérique → Long.parseLong(id) est évalué avant tout accès
    // au service (donc avant même la vérification de propriété), NumberFormatException
    // interceptée par GlobalExceptionHandler → 400.
    @Test
    @WithMockUser
    void participate_returns400_whenIdIsNotNumeric() throws Exception {
        User participant = persistParticipant();

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", "abc", participant.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void participate_returns404_whenSessionDoesNotExist() throws Exception {
        User participant = persistParticipant();

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", 999999L, participant.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void participate_returns404_whenUserDoesNotExist() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", session.getId(), 999999L))
                .andExpect(status().isNotFound());
    }

    // Note : ce test atteint assertRequestingUserIsSelf() (le participant existe
    // déjà en session) avant l'assertion "déjà inscrit" — cette méthode caste le
    // principal en UserDetailsImpl (voir SessionService), ce qu'un simple
    // @WithMockUser ne fournit pas (principal Spring standard, pas UserDetailsImpl).
    // On injecte donc directement un UserDetailsImpl portant l'id du participant
    // via SecurityMockMvcRequestPostProcessors.user(...), qui reste un mock de
    // SecurityContext (pas de JWT réel) tout en satisfaisant ce cast.
    @Test
    void participate_returns400_whenAlreadyParticipating() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        session.getUsers().add(participant);
        sessionRepository.save(session);

        mockMvc.perform(post("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .with(user(mockPrincipalFor(participant))))
                .andExpect(status().isBadRequest());
    }

    // ---------- DELETE /api/session/{id}/participate/{userId} ----------

    // Preuve que le fix ne casse pas noLongerParticipate : un non-admin authentifié
    // peut se désinscrire lui-même (même remarque que participate ci-dessus).
    @Test
    void noLongerParticipate_returns200_whenNonAdminParticipatesForThemselves() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        session.getUsers().add(participant);
        sessionRepository.save(session);
        String token = generateTokenForUser(participant);

        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // Preuve du fix (problème 2) : un utilisateur authentifié ne peut pas
    // désinscrire un autre utilisateur que lui-même.
    @Test
    void noLongerParticipate_returns403_whenUserIdDoesNotMatchAuthenticatedPrincipal() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();
        session.getUsers().add(participant);
        sessionRepository.save(session);
        String token = generateStandardUserToken();

        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // P1-04 : même mécanisme que participate_returns400_whenIdIsNotNumeric.
    @Test
    @WithMockUser
    void noLongerParticipate_returns400_whenIdIsNotNumeric() throws Exception {
        User participant = persistParticipant();

        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", "abc", participant.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void noLongerParticipate_returns404_whenSessionDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", 999999L, 1L))
                .andExpect(status().isNotFound());
    }

    // Problème 3 : noLongerParticipate() vérifie désormais l'existence de
    // l'utilisateur, alignée sur participate() (même code de retour, 404).
    @Test
    @WithMockUser
    void noLongerParticipate_returns404_whenUserDoesNotExist() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);

        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", session.getId(), 999999L))
                .andExpect(status().isNotFound());
    }

    // Note : même remarque que participate_returns400_whenAlreadyParticipating —
    // ce test atteint assertRequestingUserIsSelf() (session et utilisateur
    // existent) avant l'assertion "pas inscrit", d'où l'injection directe d'un
    // UserDetailsImpl portant l'id du participant plutôt qu'un simple @WithMockUser.
    @Test
    void noLongerParticipate_returns400_whenNotParticipating() throws Exception {
        Teacher teacher = persistTeacher();
        Session session = persistSession(teacher);
        User participant = persistParticipant();

        mockMvc.perform(delete("/api/session/{id}/participate/{userId}", session.getId(), participant.getId())
                        .with(user(mockPrincipalFor(participant))))
                .andExpect(status().isBadRequest());
    }

    private UserDetailsImpl mockPrincipalFor(User participant) {
        return UserDetailsImpl.builder()
                .id(participant.getId())
                .username(participant.getEmail())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .admin(participant.isAdmin())
                .password(participant.getPassword())
                .build();
    }
}
