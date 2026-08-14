package com.fleet.management.service.impl;

import com.fleet.management.dto.recorrido.RecorridoRequest;
import com.fleet.management.dto.recorrido.RecorridoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.*;
import com.fleet.management.repository.RecorridoRepository;
import com.fleet.management.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecorridoServiceImplTest {

    @Mock
    private RecorridoRepository repository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private RecorridoServiceImpl service;

    private static final LocalDate FECHA = LocalDate.of(2024, 6, 15);
    private static final LocalDateTime NOW = LocalDateTime.now();

    // --- Shared test fixtures ---
    private Empresa empresa;
    private TipoVehiculo tipoVehiculo;
    private Marca marca;
    private TipoCombustible tipoCombustible;
    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        empresa = Empresa.builder()
                .id(1L).codigo("EMP-001").nombre("Transportes ABC")
                .activo(true).fechaCreacion(NOW).fechaActualizacion(NOW).build();

        tipoVehiculo = TipoVehiculo.builder()
                .id(1L).nombre("Camion").descripcion("Camion de carga")
                .activo(true).fechaCreacion(NOW).fechaActualizacion(NOW).build();

        marca = Marca.builder()
                .id(1L).nombre("Toyota").descripcion("Toyota Motor Corp")
                .activo(true).fechaCreacion(NOW).fechaActualizacion(NOW).build();

        tipoCombustible = TipoCombustible.builder()
                .id(1L).codigo("DSL").denominacion("Diesel")
                .activo(true).fechaCreacion(NOW).fechaActualizacion(NOW).build();

        vehiculo = Vehiculo.builder()
                .id(10L)
                .empresa(empresa)
                .tipoVehiculo(tipoVehiculo)
                .marca(marca)
                .tipoCombustible(tipoCombustible)
                .matricula("MAT-001")
                .numeroMotor("MOT-001")
                .odometro(BigInteger.valueOf(50000))
                .combustible(new BigDecimal("50.00"))
                .indiceConsumo(new BigDecimal("8.50"))
                .activo(true)
                .fechaCreacion(NOW)
                .fechaActualizacion(NOW)
                .build();
    }

    private RecorridoRequest buildRequest(Long vehiculoId, LocalDate fecha, Integer kilometros,
                                          BigDecimal litrosAbastecidos, String lugar) {
        return RecorridoRequest.builder()
                .vehiculoId(vehiculoId)
                .fecha(fecha)
                .kilometros(kilometros)
                .litrosAbastecidos(litrosAbastecidos)
                .lugarAbastecimiento(lugar)
                .build();
    }

    private Recorrido buildRecorrido(Long id, Vehiculo v, LocalDate fecha, Integer kilometros,
                                      BigInteger odometroInicial, BigDecimal consumo,
                                      BigDecimal litrosAbastecidos) {
        return Recorrido.builder()
                .id(id)
                .vehiculo(v)
                .fecha(fecha)
                .kilometros(kilometros)
                .odometroInicial(odometroInicial)
                .consumo(consumo)
                .litrosAbastecidos(litrosAbastecidos)
                .lugarAbastecimiento("Estacion Central")
                .activo(true)
                .fechaCreacion(NOW)
                .fechaActualizacion(NOW)
                .build();
    }

    // =========================================================================
    // CREATE TESTS
    // =========================================================================
    @Nested
    class Create {

        @Test
        void createShouldReturnResponseWithCorrectConsumptionAndFuelUpdate() {
            // vehiculo: odometro=50000, combustible=50.00, indiceConsumo=8.50
            // kilometros=100 => consumo = 8.50 * 100 / 100 = 8.50
            // combustibleRestante = 50.00 - 8.50 + 0 = 41.50
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            RecorridoResponse response = service.create(request);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(new BigDecimal("8.50"), response.getConsumo());
            assertEquals(new BigDecimal("0.00"), response.getLitrosAbastecidos());
            assertEquals(BigInteger.valueOf(50000), response.getOdometroInicial());
            assertEquals(100, response.getKilometros());
            assertTrue(response.getActivo());

            // Vehicle updated: odometro += 100, combustible = 41.50
            assertEquals(BigInteger.valueOf(50100), vehiculo.getOdometro());
            assertEquals(new BigDecimal("41.50"), vehiculo.getCombustible());

            verify(repository).save(any(Recorrido.class));
            verify(vehiculoRepository).save(vehiculo);
        }

        @Test
        void createShouldCalculateConsumoWithHalfUpRounding() {
            // vehiculo: indiceConsumo=8.33, kilometros=150
            // consumo = 8.33 * 150 / 100 = 12.495 => HALF_UP => 12.50
            vehiculo.setIndiceConsumo(new BigDecimal("8.33"));
            vehiculo.setCombustible(new BigDecimal("100.00"));

            RecorridoRequest request = buildRequest(10L, FECHA, 150, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(2L);
                return r;
            });

            RecorridoResponse response = service.create(request);

            assertEquals(new BigDecimal("12.50"), response.getConsumo());
            // combustible = 100.00 - 12.50 = 87.50
            assertEquals(new BigDecimal("87.50"), vehiculo.getCombustible());
        }

        @Test
        void createShouldIncludeLitrosAbastecidosInFuelCalculation() {
            // vehiculo: combustible=5.00, indiceConsumo=8.50
            // kilometros=100 => consumo = 8.50
            // litrosAbastecidos=10.00
            // combustibleRestante = 5.00 - 8.50 + 10.00 = 6.50
            vehiculo.setCombustible(new BigDecimal("5.00"));
            RecorridoRequest request = buildRequest(10L, FECHA, 100, new BigDecimal("10.00"), "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(3L);
                return r;
            });

            RecorridoResponse response = service.create(request);

            assertEquals(new BigDecimal("10.00"), response.getLitrosAbastecidos());
            assertEquals(new BigDecimal("8.50"), response.getConsumo());
            assertEquals(new BigDecimal("6.50"), vehiculo.getCombustible());
        }

        @Test
        void createShouldSetOdometroInicialFromVehiculoOdometro() {
            vehiculo.setOdometro(BigInteger.valueOf(123456));
            RecorridoRequest request = buildRequest(10L, FECHA, 50, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(4L);
                return r;
            });

            service.create(request);

            verify(repository).save(argThat(r ->
                r.getOdometroInicial().equals(BigInteger.valueOf(123456))
            ));
        }

        @Test
        void createShouldAddKilometrosToVehiculoOdometro() {
            BigInteger originalOdometro = vehiculo.getOdometro();
            RecorridoRequest request = buildRequest(10L, FECHA, 250, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(5L);
                return r;
            });

            service.create(request);

            assertEquals(originalOdometro.add(BigInteger.valueOf(250)), vehiculo.getOdometro());
        }

        @Test
        void createShouldTreatNullLitrosAbastecidosAsZero() {
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(6L);
                return r;
            });

            RecorridoResponse response = service.create(request);

            assertEquals(new BigDecimal("0.00"), response.getLitrosAbastecidos());
        }

        @Test
        void createShouldThrowResourceNotFoundExceptionWhenVehiculoNotFound() {
            RecorridoRequest request = buildRequest(99L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.create(request));
            assertTrue(ex.getMessage().contains("Vehiculo"));
            assertTrue(ex.getMessage().contains("99"));
            verify(repository, never()).save(any());
        }

        @Test
        void createShouldThrowBusinessExceptionWhenDuplicateVehiculoFecha() {
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(request));
            assertTrue(ex.getMessage().contains("Ya existe un recorrido"));
            assertTrue(ex.getMessage().contains("10"));
            assertTrue(ex.getMessage().contains(FECHA.toString()));
            verify(repository, never()).save(any());
        }

        @Test
        void createShouldThrowBusinessExceptionWhenFutureDateRecorridoExists() {
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(request));
            assertTrue(ex.getMessage().contains("fecha posterior"));
            assertTrue(ex.getMessage().contains("10"));
            verify(repository, never()).save(any());
        }

        @Test
        void createShouldThrowBusinessExceptionWhenInsufficientFuel() {
            // vehiculo: combustible=5.00, indiceConsumo=8.50
            // kilometros=100 => consumo = 8.50
            // combustibleRestante = 5.00 - 8.50 = -3.50 < 0
            vehiculo.setCombustible(new BigDecimal("5.00"));
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.create(request));
            assertTrue(ex.getMessage().contains("mas combustible"));
            verify(repository, never()).save(any());
            verify(vehiculoRepository, never()).save(any());
        }

        @Test
        void createShouldAllowExactZeroFuelRemaining() {
            // vehiculo: combustible=8.50, indiceConsumo=8.50, kilometros=100
            // consumo = 8.50, restante = 8.50 - 8.50 = 0.00 (exactly zero, valid)
            vehiculo.setCombustible(new BigDecimal("8.50"));
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(vehiculoRepository.findById(10L)).thenReturn(Optional.of(vehiculo));
            when(repository.existsByVehiculoIdAndFecha(10L, FECHA)).thenReturn(false);
            when(repository.existsByVehiculoIdAndFechaAfter(10L, FECHA)).thenReturn(false);
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> {
                Recorrido r = inv.getArgument(0);
                r.setId(7L);
                return r;
            });

            RecorridoResponse response = service.create(request);

            assertNotNull(response);
            assertEquals(new BigDecimal("0.00"), vehiculo.getCombustible());
        }
    }

    // =========================================================================
    // UPDATE TESTS
    // =========================================================================
    @Nested
    class Update {

        @Test
        void updateShouldReturnResponseWithRecalculatedValues() {
            // Existing: kilometros=100, consumo=8.50 (8.50*100/100)
            // Current vehiculo state: odometro=50100, combustible=41.50 (after original create)
            vehiculo.setOdometro(BigInteger.valueOf(50100));
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            // New: kilometros=200 => nuevoConsumo = 8.50 * 200 / 100 = 17.00
            RecorridoRequest request = buildRequest(10L, FECHA, 200, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            RecorridoResponse response = service.update(1L, request);

            assertEquals(new BigDecimal("17.00"), response.getConsumo());
            assertEquals(200, response.getKilometros());
            // Odometro: 50100 - 100 (old) + 200 (new) = 50200
            assertEquals(BigInteger.valueOf(50200), vehiculo.getOdometro());
            // Combustible: 41.50 + 8.50 (restore old) - 17.00 (new consumo) = 33.00
            assertEquals(new BigDecimal("33.00"), vehiculo.getCombustible());
            // odometroInicial should be the restored odometro (50100 - 100 = 50000)
            assertEquals(BigInteger.valueOf(50000), response.getOdometroInicial());

            verify(repository).save(existing);
            verify(vehiculoRepository).save(vehiculo);
        }

        @Test
        void updateShouldThrowBusinessExceptionWhenChangingVehiculo() {
            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            RecorridoRequest request = buildRequest(99L, FECHA, 100, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(1L, request));
            assertTrue(ex.getMessage().contains("vehiculo"));
            verify(repository, never()).save(any());
        }

        @Test
        void updateShouldThrowBusinessExceptionWhenChangingFecha() {
            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            LocalDate differentDate = LocalDate.of(2024, 6, 20);
            RecorridoRequest request = buildRequest(10L, differentDate, 100, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(1L, request));
            assertTrue(ex.getMessage().contains("fecha"));
            verify(repository, never()).save(any());
        }

        @Test
        void updateShouldThrowResourceNotFoundExceptionWhenRecorridoNotFound() {
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(99L, request));
            assertTrue(ex.getMessage().contains("Recorrido"));
            assertTrue(ex.getMessage().contains("99"));
        }

        @Test
        void updateShouldRestoreOldConsumoBeforeValidatingFuel() {
            // Existing: kilometros=200, consumo=17.00
            // Current vehiculo: combustible=33.00, odometro=50200
            vehiculo.setOdometro(BigInteger.valueOf(50200));
            vehiculo.setCombustible(new BigDecimal("33.00"));

            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 200,
                    BigInteger.valueOf(50000), new BigDecimal("17.00"), new BigDecimal("0.00"));

            // New: kilometros=400 => nuevoConsumo = 8.50 * 400 / 100 = 34.00
            // Restored fuel: 33.00 + 17.00 = 50.00
            // After new: 50.00 - 34.00 = 16.00 >= 0, OK
            RecorridoRequest request = buildRequest(10L, FECHA, 400, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            RecorridoResponse response = service.update(1L, request);

            assertNotNull(response);
            assertEquals(new BigDecimal("34.00"), response.getConsumo());
            // Odometro: 50200 - 200 + 400 = 50400
            assertEquals(BigInteger.valueOf(50400), vehiculo.getOdometro());
            // Combustible: (33.00 + 17.00) - 34.00 = 16.00
            assertEquals(new BigDecimal("16.00"), vehiculo.getCombustible());
        }

        @Test
        void updateShouldThrowBusinessExceptionWhenInsufficientFuelAfterRestore() {
            // Existing: kilometros=100, consumo=8.50
            // Current vehiculo: combustible=41.50
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            // New: kilometros=1000 => nuevoConsumo = 8.50 * 1000 / 100 = 85.00
            // Restored fuel: 41.50 + 8.50 = 50.00
            // After new: 50.00 - 85.00 = -35.00 < 0 => FAIL
            RecorridoRequest request = buildRequest(10L, FECHA, 1000, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.update(1L, request));
            assertTrue(ex.getMessage().contains("mas combustible"));
            verify(repository, never()).save(any());
        }

        @Test
        void updateShouldHandleNullOldConsumoAsZero() {
            // Existing: consumo=null (edge case from data)
            vehiculo.setOdometro(BigInteger.valueOf(50000));
            vehiculo.setCombustible(new BigDecimal("50.00"));

            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(49900), null, new BigDecimal("0.00"));

            // New: kilometros=100 => nuevoConsumo = 8.50
            // Restored fuel: 50.00 + 0.00 = 50.00
            // After new: 50.00 - 8.50 = 41.50
            RecorridoRequest request = buildRequest(10L, FECHA, 100, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            RecorridoResponse response = service.update(1L, request);

            assertNotNull(response);
            assertEquals(new BigDecimal("8.50"), response.getConsumo());
            assertEquals(new BigDecimal("41.50"), vehiculo.getCombustible());
        }

        @Test
        void updateShouldCorrectlyUpdateOdometroInicial() {
            vehiculo.setOdometro(BigInteger.valueOf(50100));
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido existing = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            RecorridoRequest request = buildRequest(10L, FECHA, 200, null, "Estacion Central");

            when(repository.findById(1L)).thenReturn(Optional.of(existing));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.update(1L, request);

            // After restoring old odometro: 50100 - 100 = 50000
            // This becomes the new odometroInicial
            verify(repository).save(argThat(r ->
                r.getOdometroInicial().equals(BigInteger.valueOf(50000))
            ));
        }
    }

    // =========================================================================
    // DELETE TESTS
    // =========================================================================
    @Nested
    class Delete {

        @Test
        void deleteShouldSetActivoFalse() {
            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1L);

            assertFalse(recorrido.getActivo());
            verify(repository).save(recorrido);
        }

        @Test
        void deleteShouldRestoreVehiculoOdometro() {
            vehiculo.setOdometro(BigInteger.valueOf(50100));
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1L);

            // odometro: 50100 - 100 = 50000
            assertEquals(BigInteger.valueOf(50000), vehiculo.getOdometro());
            verify(vehiculoRepository).save(vehiculo);
        }

        @Test
        void deleteShouldRestoreVehiculoCombustible() {
            vehiculo.setOdometro(BigInteger.valueOf(50100));
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1L);

            // combustible: 41.50 + 8.50 = 50.00
            assertEquals(new BigDecimal("50.00"), vehiculo.getCombustible());
            verify(vehiculoRepository).save(vehiculo);
        }

        @Test
        void deleteShouldHandleNullConsumoAsZero() {
            vehiculo.setCombustible(new BigDecimal("41.50"));

            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), null, new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1L);

            // combustible: 41.50 + 0.00 = 41.50 (unchanged)
            assertEquals(new BigDecimal("41.50"), vehiculo.getCombustible());
        }

        @Test
        void deleteShouldThrowResourceNotFoundExceptionWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.delete(99L));
            assertTrue(ex.getMessage().contains("Recorrido"));
            assertTrue(ex.getMessage().contains("99"));
            verify(repository, never()).save(any());
            verify(vehiculoRepository, never()).save(any());
        }

        @Test
        void deleteShouldRestoreBothOdometroAndCombustibleCorrectly() {
            // Comprehensive: verify both restores happen together
            vehiculo.setOdometro(BigInteger.valueOf(50300));
            vehiculo.setCombustible(new BigDecimal("25.50"));

            // recorrido: 300km, consumo = 8.50 * 300 / 100 = 25.50
            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 300,
                    BigInteger.valueOf(50000), new BigDecimal("25.50"), new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));
            when(repository.save(any(Recorrido.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1L);

            assertEquals(BigInteger.valueOf(50000), vehiculo.getOdometro());
            assertEquals(new BigDecimal("51.00"), vehiculo.getCombustible());
            assertFalse(recorrido.getActivo());
        }
    }

    // =========================================================================
    // FIND TESTS
    // =========================================================================
    @Nested
    class Find {

        @Test
        void findByIdShouldReturnResponseWhenFound() {
            Recorrido recorrido = buildRecorrido(1L, vehiculo, FECHA, 100,
                    BigInteger.valueOf(50000), new BigDecimal("8.50"), new BigDecimal("0.00"));

            when(repository.findById(1L)).thenReturn(Optional.of(recorrido));

            RecorridoResponse response = service.findById(1L);

            assertEquals(1L, response.getId());
            assertEquals(FECHA, response.getFecha());
            assertEquals(100, response.getKilometros());
            assertEquals(new BigDecimal("8.50"), response.getConsumo());
            assertNotNull(response.getVehiculo());
            assertEquals("MAT-001", response.getVehiculo().getMatricula());
            verify(repository).findById(1L);
        }

        @Test
        void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.findById(99L));
            assertTrue(ex.getMessage().contains("Recorrido"));
            assertTrue(ex.getMessage().contains("99"));
        }
    }
}
