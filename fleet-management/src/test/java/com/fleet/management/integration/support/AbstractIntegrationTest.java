package com.fleet.management.integration.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Clase base abstracta para todos los tests de integracion.
 *
 * <p>Levanta el contexto Spring completo con perfil {@code test}, conecta a
 * un contenedor Postgres real (via Testcontainers) y expone las propiedades
 * dinamicas del contenedor a Spring.
 *
 * <p>Los tests concretos heredan y usan {@link #autenticadoComo(String, String)}
 * para obtener un JWT valido y configurar RestAssured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTestContainer::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTestContainer::getUsername);
        registry.add("spring.datasource.password", PostgresTestContainer::getPassword);
        // WireMock URL para Enzona
        registry.add("enzona.base-url", EnzonaWireMockConfig::getBaseUrl);
        registry.add("enzona.token-url", () -> EnzonaWireMockConfig.getBaseUrl() + "/token");
        // Credenciales dummy de Enzona para tests
        registry.add("enzona.client-id", () -> "test-client-id");
        registry.add("enzona.client-secret", () -> "test-client-secret");
        registry.add("enzona.merchant-uuid", () -> "test-merchant-uuid");
        // En tests, no requerir firma HMAC ni IP whitelist
        registry.add("enzona.allowed-ips", () -> "");
        registry.add("enzona.webhook-secret", () -> "");
        // JWT secret de test
        registry.add("jwt.secret", () -> "test-secret-DO-NOT-USE-IN-PROD-5A7B3C9D2E1F4A6B8C0D3E5F7A9B1C3D");
        registry.add("jwt.audience", () -> "fleet-management-web");
        // Rate limit desactivado en tests funcionales
        registry.add("fleet.security.rate-limit.max-attempts", () -> "1000");
    }
}
