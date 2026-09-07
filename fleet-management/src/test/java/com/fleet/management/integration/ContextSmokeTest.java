package com.fleet.management.integration;

import com.fleet.management.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: valida que el contexto Spring arranca correctamente con
 * Testcontainers Postgres y WireMock para Enzona.
 *
 * <p>Si este test falla, los demas tests de integracion tampoco funcionaran.
 */
class ContextSmokeTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void beansCriticosEstanPresentes() {
        // Modulo de pagos
        assertThat(applicationContext.containsBean("paymentController")).isTrue();
        assertThat(applicationContext.containsBean("paymentServiceImpl")).isTrue();
        assertThat(applicationContext.containsBean("paymentPostPagoServiceImpl")).isTrue();
        assertThat(applicationContext.containsBean("paymentErrorRecoveryServiceImpl")).isTrue();
        assertThat(applicationContext.containsBean("enzonaQrClientImpl")).isTrue();
        assertThat(applicationContext.containsBean("enzonaRestTemplate")).isTrue();
        assertThat(applicationContext.containsBean("enzonaWebhookVerifier")).isTrue();
        // Cache (Caffeine, FX-17/19)
        assertThat(applicationContext.containsBean("cacheManager")).isTrue();
        // Jackson 3 (JsonMapper, PR #10)
        assertThat(applicationContext.containsBean("jsonMapper")).isTrue();
        // ShedLock (FX-29)
        assertThat(applicationContext.containsBean("lockProvider")).isTrue();
    }
}
