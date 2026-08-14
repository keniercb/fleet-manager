package com.fleet.management.service.impl;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Marca;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.model.Vehiculo;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.MarcaRepository;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceImplTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private TipoVehiculoRepository tipoVehiculoRepository;

    @Mock
    private MarcaRepository marcaRepository;

    @Mock
    private TipoCombustibleRepository tipoCombustibleRepository;

    @Mock
    private ChoferRepository choferRepository;

    @InjectMocks
    private VehiculoServiceImpl vehiculoService;

    private Empresa empresa;
    private TipoVehiculo tipoVehiculo;
    private Marca marca;
    private TipoCombustible tipoCombustible;
    private Chofer chofer;
    private Vehiculo vehiculo;
    private VehiculoRequest vehiculoRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        empresa = Empresa.builder()
                .id(1L)
                .codigo("EMP-001")
                .nombre("Transportes ABC")
                .direccion("Av. Principal 123")
                .telefono("555-1234")
                .email("contacto@abc.com")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        tipoVehiculo = TipoVehiculo.builder()
                .id(1L)
                .nombre("Camion")
                .descripcion("Camion de carga")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        marca = Marca.builder()
                .id(1L)
                .nombre("Toyota")
                .descripcion("Toyota Motor Corp")
                .paisOrigen("Japon")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        tipoCombustible = TipoCombustible.builder()
                .id(1L)
                .codigo("DSL")
                .denominacion("Diesel")
                .descripcion("Combustible diesel")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        chofer = Chofer.builder()
                .id(1L)
                .empresa(empresa)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        vehiculo = Vehiculo.builder()
                .id(1L)
                .empresa(empresa)
                .tipoVehiculo(tipoVehiculo)
                .marca(marca)
                .chofer(chofer)
                .tipoCombustible(tipoCombustible)
                .matricula("MAT-001")
                .numeroMotor("MOT-001")
                .odometro(BigInteger.valueOf(50000))
                .combustible(new BigDecimal("50.00"))
                .ultimoMantenimiento(LocalDate.of(2024, 1, 1))
                .odometroUltimoMantenimiento(BigInteger.valueOf(45000))
                .indiceConsumo(new BigDecimal("8.50"))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        vehiculoRequest = VehiculoRequest.builder()
                .empresaId(1L)
                .tipoVehiculoId(1L)
                .marcaId(1L)
                .choferId(1L)
                .tipoCombustibleId(1L)
                .matricula("MAT-001")
                .numeroMotor("MOT-001")
                .odometro(BigInteger.valueOf(50000))
                .combustible(new BigDecimal("50.00"))
                .ultimoMantenimiento(LocalDate.of(2024, 1, 1))
                .odometroUltimoMantenimiento(BigInteger.valueOf(45000))
                .indiceConsumo(new BigDecimal("8.50"))
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculo));
        when(vehiculoRepository.findAll(pageable)).thenReturn(page);

        Page<VehiculoResponse> result = vehiculoService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("MAT-001", result.getContent().get(0).getMatricula());
        assertEquals("MOT-001", result.getContent().get(0).getNumeroMotor());
        verify(vehiculoRepository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));

        VehiculoResponse result = vehiculoService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("MAT-001", result.getMatricula());
        assertEquals("MOT-001", result.getNumeroMotor());
        assertEquals(BigInteger.valueOf(50000), result.getOdometro());
        assertEquals(new BigDecimal("50.00"), result.getCombustible());
        assertEquals(new BigDecimal("8.50"), result.getIndiceConsumo());
        assertNotNull(result.getChofer());
        assertEquals("Carlos", result.getChofer().getNombre());
        verify(vehiculoRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> vehiculoService.findById(99L));
        assertTrue(ex.getMessage().contains("Vehiculo"));
        assertTrue(ex.getMessage().contains("99"));
        verify(vehiculoRepository).findById(99L);
    }

    @Test
    void findByChoferIdShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculo));
        when(vehiculoRepository.findByChoferId(1L, pageable)).thenReturn(page);

        Page<VehiculoResponse> result = vehiculoService.findByChoferId(1L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getChofer().getId());
        verify(vehiculoRepository).findByChoferId(1L, pageable);
    }

    @Test
    void findByTipoVehiculoIdShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculo));
        when(vehiculoRepository.findByTipoVehiculoId(1L, pageable)).thenReturn(page);

        Page<VehiculoResponse> result = vehiculoService.findByTipoVehiculoId(1L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("Camion", result.getContent().get(0).getTipoVehiculo().getNombre());
        verify(vehiculoRepository).findByTipoVehiculoId(1L, pageable);
    }

    @Test
    void findByTipoCombustibleIdShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculo));
        when(vehiculoRepository.findByTipoCombustibleId(1L, pageable)).thenReturn(page);

        Page<VehiculoResponse> result = vehiculoService.findByTipoCombustibleId(1L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("DSL", result.getContent().get(0).getTipoCombustible().getCodigo());
        verify(vehiculoRepository).findByTipoCombustibleId(1L, pageable);
    }

    @Test
    void findSinChoferAsignadoShouldReturnPagedResponses() {
        Vehiculo vehiculoSinChofer = Vehiculo.builder()
                .id(2L)
                .empresa(empresa)
                .tipoVehiculo(tipoVehiculo)
                .marca(marca)
                .chofer(null)
                .tipoCombustible(tipoCombustible)
                .matricula("MAT-002")
                .numeroMotor("MOT-002")
                .odometro(BigInteger.valueOf(10000))
                .combustible(new BigDecimal("80.00"))
                .indiceConsumo(new BigDecimal("7.50"))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculoSinChofer));
        when(vehiculoRepository.findSinChoferAsignado(pageable)).thenReturn(page);

        Page<VehiculoResponse> result = vehiculoService.findSinChoferAsignado(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertNull(result.getContent().get(0).getChofer());
        verify(vehiculoRepository).findSinChoferAsignado(pageable);
    }

    @Test
    void createWithChoferShouldReturnResponse() {
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(vehiculoRepository.existsByNumeroMotor("MOT-001")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(tipoVehiculoRepository.findById(1L)).thenReturn(Optional.of(tipoVehiculo));
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(tipoCombustibleRepository.findById(1L)).thenReturn(Optional.of(tipoCombustible));
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        VehiculoResponse result = vehiculoService.create(vehiculoRequest);

        assertEquals("MAT-001", result.getMatricula());
        assertEquals("MOT-001", result.getNumeroMotor());
        assertNotNull(result.getChofer());
        assertEquals("Carlos", result.getChofer().getNombre());
        assertTrue(result.getActivo());
        verify(vehiculoRepository).save(any(Vehiculo.class));
    }

    @Test
    void createWithoutChoferShouldReturnResponse() {
        VehiculoRequest requestNoChofer = VehiculoRequest.builder()
                .empresaId(1L)
                .tipoVehiculoId(1L)
                .marcaId(1L)
                .choferId(null)
                .tipoCombustibleId(1L)
                .matricula("MAT-003")
                .numeroMotor("MOT-003")
                .odometro(BigInteger.valueOf(30000))
                .combustible(new BigDecimal("60.00"))
                .indiceConsumo(new BigDecimal("9.00"))
                .build();

        when(vehiculoRepository.existsByMatricula("MAT-003")).thenReturn(false);
        when(vehiculoRepository.existsByNumeroMotor("MOT-003")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(tipoVehiculoRepository.findById(1L)).thenReturn(Optional.of(tipoVehiculo));
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(tipoCombustibleRepository.findById(1L)).thenReturn(Optional.of(tipoCombustible));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> {
            Vehiculo saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        VehiculoResponse result = vehiculoService.create(requestNoChofer);

        assertEquals("MAT-003", result.getMatricula());
        assertNull(result.getChofer());
        verify(choferRepository, never()).findById(any());
        verify(vehiculoRepository).save(any(Vehiculo.class));
    }

    @Test
    void createShouldThrowResourceNotFoundExceptionWhenEmpresaNotFound() {
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(vehiculoRepository.existsByNumeroMotor("MOT-001")).thenReturn(false);
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        VehiculoRequest badRequest = VehiculoRequest.builder()
                .empresaId(99L)
                .tipoVehiculoId(1L)
                .marcaId(1L)
                .tipoCombustibleId(1L)
                .matricula("MAT-099")
                .numeroMotor("MOT-099")
                .odometro(BigInteger.ZERO)
                .combustible(BigDecimal.ZERO)
                .indiceConsumo(new BigDecimal("1.00"))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.create(badRequest));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void createShouldThrowBusinessExceptionWhenDuplicateMatricula() {
        Vehiculo existing = Vehiculo.builder()
                .id(5L)
                .matricula("MAT-001")
                .build();
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(true);
        when(vehiculoRepository.findByMatricula("MAT-001")).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> vehiculoService.create(vehiculoRequest));
        assertTrue(ex.getMessage().contains("matricula"));
        assertTrue(ex.getMessage().contains("MAT-001"));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void createShouldThrowBusinessExceptionWhenDuplicateNumeroMotor() {
        Vehiculo existing = Vehiculo.builder()
                .id(5L)
                .numeroMotor("MOT-001")
                .build();
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(vehiculoRepository.existsByNumeroMotor("MOT-001")).thenReturn(true);
        when(vehiculoRepository.findByNumeroMotor("MOT-001")).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> vehiculoService.create(vehiculoRequest));
        assertTrue(ex.getMessage().contains("numero de motor"));
        assertTrue(ex.getMessage().contains("MOT-001"));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void updateShouldReturnResponseWhenAllFieldsValid() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(false);
        when(vehiculoRepository.existsByNumeroMotor("MOT-001")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(tipoVehiculoRepository.findById(1L)).thenReturn(Optional.of(tipoVehiculo));
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(tipoCombustibleRepository.findById(1L)).thenReturn(Optional.of(tipoCombustible));
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehiculoResponse result = vehiculoService.update(1L, vehiculoRequest);

        assertEquals("MAT-001", result.getMatricula());
        assertEquals("MOT-001", result.getNumeroMotor());
        verify(vehiculoRepository).save(vehiculo);
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenVehiculoNotFound() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.update(99L, vehiculoRequest));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void updateShouldAllowSameMatriculaForSameEntity() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(true);
        when(vehiculoRepository.findByMatricula("MAT-001")).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByNumeroMotor("MOT-001")).thenReturn(true);
        when(vehiculoRepository.findByNumeroMotor("MOT-001")).thenReturn(Optional.of(vehiculo));
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(tipoVehiculoRepository.findById(1L)).thenReturn(Optional.of(tipoVehiculo));
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(tipoCombustibleRepository.findById(1L)).thenReturn(Optional.of(tipoCombustible));
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehiculoResponse result = vehiculoService.update(1L, vehiculoRequest);

        assertEquals("MAT-001", result.getMatricula());
        verify(vehiculoRepository).save(vehiculo);
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenMatriculaChangedToExisting() {
        Vehiculo another = Vehiculo.builder()
                .id(5L)
                .matricula("MAT-099")
                .build();

        VehiculoRequest changedRequest = VehiculoRequest.builder()
                .empresaId(1L)
                .tipoVehiculoId(1L)
                .marcaId(1L)
                .choferId(1L)
                .tipoCombustibleId(1L)
                .matricula("MAT-099")
                .numeroMotor("MOT-001")
                .odometro(BigInteger.valueOf(50000))
                .combustible(new BigDecimal("50.00"))
                .indiceConsumo(new BigDecimal("8.50"))
                .build();

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByMatricula("MAT-099")).thenReturn(true);
        when(vehiculoRepository.findByMatricula("MAT-099")).thenReturn(Optional.of(another));

        BusinessException ex = assertThrows(BusinessException.class, () -> vehiculoService.update(1L, changedRequest));
        assertTrue(ex.getMessage().contains("matricula"));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenNumeroMotorChangedToExisting() {
        Vehiculo another = Vehiculo.builder()
                .id(5L)
                .numeroMotor("MOT-099")
                .build();

        VehiculoRequest changedRequest = VehiculoRequest.builder()
                .empresaId(1L)
                .tipoVehiculoId(1L)
                .marcaId(1L)
                .choferId(1L)
                .tipoCombustibleId(1L)
                .matricula("MAT-001")
                .numeroMotor("MOT-099")
                .odometro(BigInteger.valueOf(50000))
                .combustible(new BigDecimal("50.00"))
                .indiceConsumo(new BigDecimal("8.50"))
                .build();

        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByMatricula("MAT-001")).thenReturn(true);
        when(vehiculoRepository.findByMatricula("MAT-001")).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByNumeroMotor("MOT-099")).thenReturn(true);
        when(vehiculoRepository.findByNumeroMotor("MOT-099")).thenReturn(Optional.of(another));

        BusinessException ex = assertThrows(BusinessException.class, () -> vehiculoService.update(1L, changedRequest));
        assertTrue(ex.getMessage().contains("numero de motor"));
        verify(vehiculoRepository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        vehiculoService.delete(1L);

        assertFalse(vehiculo.getActivo());
        verify(vehiculoRepository).findById(1L);
        verify(vehiculoRepository).save(vehiculo);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.delete(99L));
        verify(vehiculoRepository).findById(99L);
        verify(vehiculoRepository, never()).save(any());
    }
}
