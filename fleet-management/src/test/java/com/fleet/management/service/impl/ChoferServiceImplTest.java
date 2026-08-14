package com.fleet.management.service.impl;

import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.ChoferCategoria;
import com.fleet.management.model.Empresa;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferCategoriaRepository;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChoferServiceImplTest {

    @Mock
    private ChoferRepository choferRepository;

    @Mock
    private CategoriaLicenciaRepository categoriaLicenciaRepository;

    @Mock
    private ChoferCategoriaRepository choferCategoriaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @InjectMocks
    private ChoferServiceImpl choferService;

    private Empresa empresa;
    private Chofer chofer;
    private CategoriaLicencia categoriaLicencia;
    private ChoferCategoria choferCategoria;
    private ChoferRequest choferRequest;
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

        categoriaLicencia = CategoriaLicencia.builder()
                .id(1L)
                .codigo("B")
                .denominacion("Licencia B")
                .descripcion("Vehiculos livianos")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        choferCategoria = ChoferCategoria.builder()
                .id(10L)
                .chofer(null)
                .categoriaLicencia(categoriaLicencia)
                .fechaEmision(LocalDate.of(2023, 1, 15))
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
                .categorias(new ArrayList<>(List.of(choferCategoria)))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
        choferCategoria.setChofer(chofer);

        choferRequest = ChoferRequest.builder()
                .empresaId(1L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Chofer> page = new PageImpl<>(List.of(chofer));
        when(choferRepository.findAll(pageable)).thenReturn(page);

        Page<ChoferResponse> result = choferService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("Carlos", result.getContent().get(0).getNombre());
        assertEquals("Perez", result.getContent().get(0).getApellidos());
        verify(choferRepository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));

        ChoferResponse result = choferService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Carlos", result.getNombre());
        assertEquals("Perez", result.getApellidos());
        assertEquals("90123456789", result.getCarneIdentidad());
        assertEquals("LIC-001", result.getNumeroLicencia());
        assertTrue(result.getActivo());
        verify(choferRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(choferRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> choferService.findById(99L));
        assertTrue(ex.getMessage().contains("Chofer"));
        assertTrue(ex.getMessage().contains("99"));
        verify(choferRepository).findById(99L);
    }

    @Test
    void createWithCategoriesShouldReturnResponse() {
        ChoferRequest.CategoriaConFechaRequest catReq = ChoferRequest.CategoriaConFechaRequest.builder()
                .categoriaLicenciaId(1L)
                .fechaEmision(LocalDate.of(2023, 1, 15))
                .build();
        ChoferRequest requestWithCats = ChoferRequest.builder()
                .empresaId(1L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .categorias(List.of(catReq))
                .build();

        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(choferRepository.save(any(Chofer.class))).thenAnswer(invocation -> {
            Chofer saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(categoriaLicenciaRepository.findById(1L)).thenReturn(Optional.of(categoriaLicencia));
        when(choferCategoriaRepository.existsByChoferIdAndCategoriaLicenciaId(2L, 1L)).thenReturn(false);

        ChoferResponse result = choferService.create(requestWithCats);

        assertEquals("Carlos", result.getNombre());
        assertEquals(1, result.getCategorias().size());
        verify(choferRepository).save(any(Chofer.class));
        verify(categoriaLicenciaRepository).findById(1L);
    }

    @Test
    void createWithoutCategoriesShouldReturnResponse() {
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(choferRepository.save(any(Chofer.class))).thenAnswer(invocation -> {
            Chofer saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        ChoferResponse result = choferService.create(choferRequest);

        assertEquals("Carlos", result.getNombre());
        assertTrue(result.getCategorias().isEmpty());
        verify(choferRepository).save(any(Chofer.class));
        verify(categoriaLicenciaRepository, never()).findById(any());
    }

    @Test
    void createShouldThrowResourceNotFoundExceptionWhenEmpresaNotFound() {
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(false);
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        ChoferRequest badRequest = ChoferRequest.builder()
                .empresaId(99L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> choferService.create(badRequest));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void createShouldThrowBusinessExceptionWhenDuplicateCarneIdentidad() {
        Chofer existingChofer = Chofer.builder()
                .id(5L)
                .carneIdentidad("90123456789")
                .build();
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(true);
        when(choferRepository.findByCarneIdentidad("90123456789")).thenReturn(Optional.of(existingChofer));

        BusinessException ex = assertThrows(BusinessException.class, () -> choferService.create(choferRequest));
        assertTrue(ex.getMessage().contains("carne de identidad"));
        assertTrue(ex.getMessage().contains("90123456789"));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void createShouldThrowBusinessExceptionWhenDuplicateNumeroLicencia() {
        Chofer existingChofer = Chofer.builder()
                .id(5L)
                .numeroLicencia("LIC-001")
                .build();
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(true);
        when(choferRepository.findByNumeroLicencia("LIC-001")).thenReturn(Optional.of(existingChofer));

        BusinessException ex = assertThrows(BusinessException.class, () -> choferService.create(choferRequest));
        assertTrue(ex.getMessage().contains("numero de licencia"));
        assertTrue(ex.getMessage().contains("LIC-001"));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void updateWithCategoriesReplacementShouldReturnResponse() {
        ChoferRequest.CategoriaConFechaRequest catReq = ChoferRequest.CategoriaConFechaRequest.builder()
                .categoriaLicenciaId(1L)
                .fechaEmision(LocalDate.of(2023, 6, 1))
                .build();
        ChoferRequest updateRequest = ChoferRequest.builder()
                .empresaId(1L)
                .nombre("Carlos Updated")
                .apellidos("Perez Updated")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .categorias(List.of(catReq))
                .build();

        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(false);
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(choferCategoriaRepository.flush()).thenReturn(null);
        when(categoriaLicenciaRepository.findById(1L)).thenReturn(Optional.of(categoriaLicencia));
        when(choferCategoriaRepository.existsByChoferIdAndCategoriaLicenciaId(1L, 1L)).thenReturn(false);
        when(choferRepository.save(any(Chofer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChoferResponse result = choferService.update(1L, updateRequest);

        assertEquals("Carlos Updated", result.getNombre());
        assertEquals("Perez Updated", result.getApellidos());
        verify(choferCategoriaRepository).flush();
        verify(choferRepository).save(chofer);
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenChoferNotFound() {
        when(choferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> choferService.update(99L, choferRequest));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenEmpresaNotFound() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(false);
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(false);
        when(empresaRepository.findById(99L)).thenReturn(Optional.empty());

        ChoferRequest badRequest = ChoferRequest.builder()
                .empresaId(99L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> choferService.update(1L, badRequest));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void updateShouldAllowSameCarneIdentidadForSameEntity() {
        ChoferRequest sameRequest = ChoferRequest.builder()
                .empresaId(1L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("90123456789")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();

        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(choferRepository.existsByCarneIdentidad("90123456789")).thenReturn(true);
        when(choferRepository.findByCarneIdentidad("90123456789")).thenReturn(Optional.of(chofer));
        when(choferRepository.existsByNumeroLicencia("LIC-001")).thenReturn(true);
        when(choferRepository.findByNumeroLicencia("LIC-001")).thenReturn(Optional.of(chofer));
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));
        when(choferRepository.save(any(Chofer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChoferResponse result = choferService.update(1L, sameRequest);

        assertEquals("Carlos", result.getNombre());
        verify(choferRepository).save(chofer);
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenCarneIdentidadChangedToExisting() {
        Chofer anotherChofer = Chofer.builder()
                .id(5L)
                .carneIdentidad("876543210")
                .build();

        ChoferRequest changedRequest = ChoferRequest.builder()
                .empresaId(1L)
                .nombre("Carlos")
                .apellidos("Perez")
                .carneIdentidad("876543210")
                .numeroLicencia("LIC-001")
                .fechaNacimiento(LocalDate.of(1985, 5, 20))
                .build();

        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(choferRepository.existsByCarneIdentidad("876543210")).thenReturn(true);
        when(choferRepository.findByCarneIdentidad("876543210")).thenReturn(Optional.of(anotherChofer));

        BusinessException ex = assertThrows(BusinessException.class, () -> choferService.update(1L, changedRequest));
        assertTrue(ex.getMessage().contains("carne de identidad"));
        verify(choferRepository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndDeactivateCategorias() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(choferRepository.save(any(Chofer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        choferService.delete(1L);

        assertFalse(chofer.getActivo());
        chofer.getCategorias().forEach(cc -> assertFalse(cc.getActivo()));
        verify(choferRepository).findById(1L);
        verify(choferRepository).save(chofer);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(choferRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> choferService.delete(99L));
        verify(choferRepository).findById(99L);
        verify(choferRepository, never()).save(any());
    }
}
