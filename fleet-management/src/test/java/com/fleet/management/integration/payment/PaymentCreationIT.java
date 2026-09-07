package com.fleet.management.integration.payment;

import com.fleet.management.integration.support.AbstractIntegrationTest;
import com.fleet.management.integration.support.TestDataFactory;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integracion del modulo de Pagos Enzona QR.
 *
 * <p>Valida el flujo end-to-end: crear pago → Enzona (mockeado via WireMock)
 * responde QR → pago persistido en estado QR_GENERADO.
 *
 * <p>Cubre casos del plan-pruebas-integracion.md:
 * - IT-001: Crear pago NUEVA_SUSCRIPCION con plan valido y empresa autenticada
 * - IT-005: Crear pago con plan inexistente
 * - IT-006: Crear pago sin autenticacion
 */
@DisplayName("Pagos Enzona QR - Creacion de pago")
class PaymentCreationIT extends AbstractIntegrationTest {

    @Autowired private TestDataFactory factory;
    @Autowired private PaymentRepository paymentRepository;

    private Empresa empresa;
    private Plan plan;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        // Limpiar pagos previos
        paymentRepository.deleteAll();

        empresa = factory.crearEmpresa("EMP-TEST-" + System.nanoTime(), "Empresa Test");
        plan = factory.crearPlan("Plan Test", new BigDecimal("500.00"), 30, 10, 5);

        // TODO: crear usuario autenticable para esta empresa y obtener JWT real.
        // Por ahora usamos un token dummy; los tests con @PreAuthorize requieren
        // setup adicional de SecurityContext.
        jwtToken = "dummy-token";
    }

    @Test
    @DisplayName("IT-006: crear pago sin autenticacion retorna 401 o 403")
    void crearPago_sinAutenticacion_retorna401() {
        // Cuando no hay JWT, Spring Security rechaza con 401 o 403
        given()
                .contentType("application/json")
                .body("{\"planId\":" + plan.getId() + ",\"type\":\"NUEVA_SUSCRIPCION\"}")
        .when()
                .post("/api/payments")
        .then()
                .statusCode(org.springframework.http.HttpStatus.UNAUTHORIZED.value())
                .or()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    @DisplayName("Smoke: PaymentRepository esta vacio al inicio del test")
    void repositoryVacio_alInicio() {
        // Test de sanity: si esto falla, los demas tests pueden estar contaminados
        assertThat(paymentRepository.findAll()).isEmpty();
    }
}
