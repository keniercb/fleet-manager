package com.fleet.management.integration.support;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Contenedor PostgreSQL compartido entre todos los tests de integracion.
 *
 * <p>Patron singleton: se levanta una sola vez por JVM y se reutiliza entre
 * clases de test. Cada test es responsable de limpiar sus propios datos
 * (via @Transactional + @Rollback o @Sql con AFTER_TEST_METHOD).
 *
 * <p>Con {@code withReuse(true)}, Testcontainers reutiliza el contenedor entre
 * ejecuciones si {@code ~/.testcontainers.properties} tiene
 * {@code testcontainers.reuse.enable=true}.
 */
public class PostgresTestContainer {

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fleet_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    static {
        INSTANCE.start();
    }

    public static PostgreSQLContainer<?> getInstance() {
        return INSTANCE;
    }

    public static String getJdbcUrl() {
        return INSTANCE.getJdbcUrl();
    }

    public static String getUsername() {
        return INSTANCE.getUsername();
    }

    public static String getPassword() {
        return INSTANCE.getPassword();
    }
}
