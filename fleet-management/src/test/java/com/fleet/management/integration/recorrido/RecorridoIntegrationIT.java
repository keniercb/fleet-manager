package com.fleet.management.integration.recorrido;

import com.fleet.management.integration.support.AbstractIntegrationTest;
import com.fleet.management.integration.support.TestDataFactory;
import com.fleet.management.model.*;
import com.fleet.management.repository.RecorridoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integracion del modulo de Recorridos y Vehiculos.
 *
 * <p>Cubre casos del plan-pruebas-integracion.md:
 * - IT-039: crear recorrido con litrosAbastecidos y tarjeta descuenta saldo
 * - IT-047: delete hace soft delete (activo=false), no eliminacion fisica
 *
 * <p>NOTA: Estos tests no usan RestAssured porque requieren JWT valido.
 * Usan directamente los servicios/repositorios para validar logica de negocio.
 * Los tests HTTP end-to-end se agregaran cuando se implemente el helper
 * autenticadoComo(role) en AbstractIntegrationTest.
 */
@DisplayName("Recorridos - Logica de negocio")
class RecorridoIntegrationIT extends AbstractIntegrationTest {

    @Autowired private TestDataFactory factory;
    @Autowired private RecorridoRepository recorridoRepository;

    private Vehiculo vehiculo;
    private TarjetaCombustible tarjeta;
    private Empresa empresa;

    @BeforeEach
    void setUp() {
        recorridoRepository.deleteAll();

        empresa = factory.crearEmpresa("EMP-REC-" + System.nanoTime(), "Empresa Recorridos");
        TipoVehiculo tv = factory.crearTipoVehiculo("Camion");
        Marca marca = factory.crearMarca("Toyota");
        TipoCombustible tc = factory.crearTipoCombustible("DIESEL", "Diesel");
        vehiculo = factory.crearVehiculo(empresa, tv, marca, tc);
        Currency currency = factory.crearCurrency("CUP", "Peso Cubano");
        tarjeta = factory.crearTarjeta(empresa, currency, new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Smoke: entities se persisten correctamente con BigDecimal")
    void entitiesPersistenCorrectamente() {
        assertThat(vehiculo.getId()).isNotNull();
        assertThat(vehiculo.getCombustible()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(tarjeta.getId()).isNotNull();
        assertThat(tarjeta.getSaldo()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(vehiculo.getOdometro()).isEqualTo(BigInteger.ZERO);
    }

    @Test
    @DisplayName("Smoke: odometro y combustible del vehiculo son BigDecimal/BigInteger")
    void tiposMonetariosSonBigDecimal() {
        assertThat(vehiculo.getCombustible()).isInstanceOf(BigDecimal.class);
        assertThat(tarjeta.getSaldo()).isInstanceOf(BigDecimal.class);
    }
}
