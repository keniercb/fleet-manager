package com.fleet.management.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuración de ShedLock para sincronizar tareas programadas en despliegues
 * multi-instancia (FX-29).
 *
 * <p>Sin ShedLock, si la app se despliega en N réplicas, todas ejecutan los
 * schedulers ({@code PaymentExpirationScheduler},
 * {@code SubscriptionExpirationScheduler}) al mismo tiempo, generando logs
 * duplicados, carga innecesaria y posibles race conditions incluso con
 * {@code @Lock(PESSIMISTIC_WRITE)}.
 *
 * <p>ShedLock usa una tabla {@code shedlock} en la BD para coordinar: la primera
 * réplica en adquirir el lock ejecuta la tarea; las demás la saltan.
 *
 * <p>La tabla debe crearse con el siguiente DDL (PostgreSQL):
 * <pre>
 * CREATE TABLE shedlock (
 *     name       VARCHAR(64)  NOT NULL PRIMARY KEY,
 *     lock_until TIMESTAMP    NOT NULL,
 *     locked_at  TIMESTAMP    NOT NULL,
 *     locked_by  VARCHAR(255) NOT NULL
 * );
 * </pre>
 *
 * <p>Si {@code spring.jpa.hibernate.ddl-auto=update} está activo, Hibernate no
 * crea esta tabla automáticamente (no es una entidad JPA). Ejecutar el DDL
 * manualmente o incluirlo en un script Flyway/Liquibase (migración V1 incluida
 * con FX-13).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")  // default 5 minutos
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        // FX-fix: ShedLock 5.16.0 usa JdbcTemplateLockProvider.Configuration.builder()
        // con withJdbcTemplate(new JdbcTemplate(dataSource)) en lugar de withDataSource.
        // usingDbTime() recomendado para usar el reloj de la BD y evitar drift de时钟 entre nodos.
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
