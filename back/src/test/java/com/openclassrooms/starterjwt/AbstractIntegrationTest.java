package com.openclassrooms.starterjwt;

import com.openclassrooms.starterjwt.models.User;
import com.openclassrooms.starterjwt.repository.UserRepository;
import com.openclassrooms.starterjwt.security.jwt.JwtUtils;
import com.openclassrooms.starterjwt.security.services.UserDetailsImpl;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import java.util.UUID;

/**
 * Socle commun des tests d'intégration : démarre le contexte Spring complet
 * sur un port aléatoire, avec une vraie base MySQL fournie par Testcontainers.
 * Les tests de controllers doivent étendre cette classe plutôt que de
 * redéfinir leur propre configuration Testcontainers/profil.
 *
 * Le conteneur MySQL est démarré une seule fois pour toute la JVM de test via
 * un bloc statique (pattern "singleton container"), et non via @Container/
 * @Testcontainers. Avec plusieurs classes IT partageant une configuration
 * Spring identique, Spring réutilise un même ApplicationContext mis en cache
 * entre les classes ; @Container sur un champ static relance un nouveau
 * conteneur (donc un nouveau port) à chaque beforeAll de classe, ce qui laisse
 * le contexte Spring en cache pointer vers un port désormais mort dès qu'une
 * deuxième classe IT à configuration identique s'exécute. Démarrer le
 * conteneur une seule fois pour toute la JVM (Ryuk le nettoie en fin de run)
 * élimine ce problème.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("yoga_test")
            .withUsername("yoga_test")
            .withPassword("yoga_test");

    static {
        mysqlContainer.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Insère un utilisateur standard (admin=false) et retourne son JWT réel,
     * généré via JwtUtils pour passer par le vrai AuthTokenFilter côté requête.
     */
    protected String generateStandardUserToken() {
        return generateUserToken(false);
    }

    /**
     * Insère un utilisateur administrateur (admin=true) et retourne son JWT réel.
     */
    protected String generateAdminUserToken() {
        return generateUserToken(true);
    }

    private String generateUserToken(boolean admin) {
        User user = userRepository.save(User.builder()
                .email("it-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .firstName("Test")
                .lastName("User")
                .password("encoded-password")
                .admin(admin)
                .build());

        UserDetailsImpl userDetails = UserDetailsImpl.builder()
                .id(user.getId())
                .username(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .admin(user.isAdmin())
                .password(user.getPassword())
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        return jwtUtils.generateJwtToken(authentication);
    }
}
