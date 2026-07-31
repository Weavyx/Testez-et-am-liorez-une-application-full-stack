package com.openclassrooms.starterjwt.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.starterjwt.AbstractIntegrationTest;
import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration d'AuthController : vraie base MySQL (Testcontainers)
 * via AbstractIntegrationTest, requêtes passées par MockMvc (même pattern que
 * TeacherControllerIT/SessionControllerIT).
 *
 * Particularité : /api/auth/** est entièrement public (permitAll dans
 * WebSecurityConfig), donc aucun token n'est envoyé en en-tête pour appeler
 * ces routes. Les erreurs d'authentification (mauvais mot de passe, email
 * inconnu) proviennent ici de AuthenticationManager#authenticate() appelé
 * depuis le controller : l'AuthenticationException levée remonte à travers
 * la chaîne de filtres Spring Security et est traduite en 401 par
 * l'authenticationEntryPoint configuré dans WebSecurityConfig, et non par
 * GlobalExceptionHandler.
 */
@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String signupJson(String email, String firstName, String lastName, String password) {
        return "{"
                + "\"email\":\"" + email + "\","
                + "\"firstName\":\"" + firstName + "\","
                + "\"lastName\":\"" + lastName + "\","
                + "\"password\":\"" + password + "\""
                + "}";
    }

    private String loginJson(String email, String password) {
        return "{"
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\""
                + "}";
    }

    private User persistUser(String email, String rawPassword, boolean admin) {
        return userRepository.save(User.builder()
                .email(email)
                .firstName("Jean")
                .lastName("Dupont")
                .password(passwordEncoder.encode(rawPassword))
                .admin(admin)
                .build());
    }

    // ---------- POST /api/auth/register ----------

    @Test
    void register_returns200AndSuccessMessage_whenDataIsValid() throws Exception {
        String email = uniqueEmail("new-user");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "Jean", "Dupont", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully!"));

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
        assertThat(saved.isAdmin()).isFalse();
    }

    @Test
    void register_returns400AndMessage_whenEmailIsAlreadyTaken() throws Exception {
        String email = uniqueEmail("existing-user");
        persistUser(email, "otherPassword", false);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, "Jean", "Dupont", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: Email is already taken!"));
    }

    @Test
    void register_returns400_whenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("", "Jean", "Dupont", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: email must not be blank"));
    }

    @Test
    void register_returns400_whenEmailFormatIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("not-an-email", "Jean", "Dupont", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: email must be a well-formed email address"));
    }

    @Test
    void register_returns400_whenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(uniqueEmail("short-pwd"), "Jean", "Dupont", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: password size must be between 6 and 40"));
    }

    // Preuve que handleMethodArgumentNotValidException agrège TOUTES les erreurs de
    // champ, pas seulement la première : email vide ET password trop court sont
    // toutes deux invalides ici, les deux doivent apparaître dans le message.
    @Test
    void register_returns400AndListsAllFieldErrors_whenMultipleFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("", "Jean", "Dupont", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email must not be blank")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password size must be between 6 and 40")));
    }

    // ---------- POST /api/auth/login ----------

    @Test
    void login_returns200AndJwtResponse_whenCredentialsAreValid() throws Exception {
        String email = uniqueEmail("login-user");
        User user = persistUser(email, "password123", false);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value(email))
                .andExpect(jsonPath("$.firstName").value("Jean"))
                .andExpect(jsonPath("$.lastName").value("Dupont"))
                .andExpect(jsonPath("$.admin").value(false))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        // Structure JWS (header.payload.signature) : un token arbitraire échouerait déjà ici.
        assertThat(token.split("\\.")).hasSize(3);
        // Validation cryptographique réelle (signature HS512 + expiration) via le JwtUtils du contexte,
        // pas une simple vérification "non vide" : un token malformé ou signé avec une autre clé serait rejeté ici.
        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        // Le subject encodé dans le JWT correspond bien à l'utilisateur authentifié.
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo(email);
    }

    @Test
    void login_returns401_whenPasswordIsWrong() throws Exception {
        String email = uniqueEmail("wrong-pwd");
        persistUser(email, "password123", false);

        // email et password non vides : la Bean Validation (@NotBlank sur LoginRequest) passe sans
        // erreur, donc le 401 obtenu ici ne peut provenir que de l'échec d'authentification
        // (BadCredentialsException levée par AuthenticationManager#authenticate, traduite en 401 par
        // l'authenticationEntryPoint de WebSecurityConfig) — pas d'un 400 de validation de champ.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "wrongPassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenEmailIsUnknown() throws Exception {
        // Même remarque que login_returns401_whenPasswordIsWrong : champs non vides, donc le 401
        // isole bien le cas "email inconnu" côté AuthenticationManager (UsernameNotFoundException
        // masquée en BadCredentialsException) et non une erreur de Bean Validation.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(uniqueEmail("unknown"), "password123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns400_whenPasswordIsBlank() throws Exception {
        String email = uniqueEmail("blank-pwd");
        persistUser(email, "password123", false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "")))
                .andExpect(status().isBadRequest());
    }
}
