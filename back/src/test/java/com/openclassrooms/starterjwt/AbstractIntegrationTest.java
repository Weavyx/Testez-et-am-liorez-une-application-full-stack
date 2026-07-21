package com.openclassrooms.starterjwt;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle commun des tests d'intégration : démarre le contexte Spring complet
 * sur un port aléatoire, avec une vraie base MySQL fournie par Testcontainers.
 * Les tests de controllers doivent étendre cette classe plutôt que de
 * redéfinir leur propre configuration Testcontainers/profil.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static final MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("yoga_test")
            .withUsername("yoga_test")
            .withPassword("yoga_test");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
    }
}
