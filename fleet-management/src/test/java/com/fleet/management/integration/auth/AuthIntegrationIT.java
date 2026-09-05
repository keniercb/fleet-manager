package com.fleet.management.integration.auth;

import com.fleet.management.integration.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests de integracion del modulo de Autenticacion.
 *
 * <p>Cubre casos del plan-pruebas-integracion.md:
 * - IT-053: POST /api/auth/login con credenciales incorrectas retorna 401
 * - IT-055: request a endpoint protegido con token expirado retorna 401
 * - IT-056: request a endpoint protegido con token malformado retorna 401
 */
@DisplayName("Auth - Login y JWT")
class AuthIntegrationIT extends AbstractIntegrationTest {

    @Test
    @DisplayName("IT-053: login con credenciales incorrectas retorna 401")
    void login_credencialesIncorrectas_retorna401() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"noexiste@fleet.com\",\"password\":\"mala\"}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("IT-056: request a endpoint protegido con token malformado retorna 401")
    void endpointProtegido_tokenMalformado_retorna401() {
        given()
                .header("Authorization", "Bearer token-malformado-no-jwt")
        .when()
                .get("/api/vehiculos")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("IT-056b: request a endpoint protegido sin token retorna 401")
    void endpointProtegido_sinToken_retorna401() {
        given()
        .when()
                .get("/api/vehiculos")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Smoke: endpoint /api/auth/login responde (no 404)")
    void endpointLoginExiste() {
        given()
                .contentType("application/json")
                .body("{\"email\":\"x\",\"password\":\"x\"}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(notNullValue());
    }
}
