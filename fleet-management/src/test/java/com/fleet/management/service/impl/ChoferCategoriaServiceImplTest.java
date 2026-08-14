package com.fleet.management.service.impl;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.ChoferCategoria;
import com.fleet.management.model.Empresa;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferCategoriaRepository;
import com.fleet.management.repository.ChoferRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChoferCategoriaServiceImplTest {

    @Mock
    private ChoferCategoriaRepository repository;

    @Mock
    private ChoferRepository choferRepository;

    @Mock
    private CategoriaLicenciaRepository categoriaLicenciaRepository;

    @InjectMocks
    private ChoferCategoriaServiceImpl choferCategoriaService;

    private Empresa empresa;
    private Chofer chofer;
    private CategoriaLicencia categoriaLicencia;
    private ChoferCategoria choferCategoria;
    private ChoferCategoriaRequest request;
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

        categoriaLicencia = CategoriaLicencia.builder()
                .id(2L)
                .codigo("B")
                .denominacion("Licencia B")
                .descripcion("Vehiculos livianos")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        choferCategoria = ChoferCategoria.builder()
                .id(10L)
                .chofer(chofer)
                .categoriaLicencia(categoriaLicencia)
                .fechaEmision(LocalDate.of(2023, 1, 15))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        request = ChoferCategoriaRequest.builder()
                .choferId(1L)
                .categoriaLicenciaId(2L)
                .fechaEmision(LocalDate.of(2023, 1, 15))
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChoferCategoria> page = new PageImpl<>(List.of(choferCategoria));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<ChoferCategoriaResponse> result = choferCategoriaService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(LocalDate.of(2023, 1, 15), result.getContent().get(0).getFechaEmision());
        verify(repository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(10L)).thenReturn(Optional.of(choferCategoria));

        ChoferCategoriaResponse result = choferCategoriaService.findById(10L);

        assertEquals(10L, result.getId());
        assertEquals(1L, result.getChofer().getId());
        assertEquals("Carlos", result.getChofer().getNombre());
        assertEquals(2L, result.getCategoriaLicencia().getId());
        assertEquals("B", result.getCategoriaLicencia().getCodigo());
        verify(repository).findById(10L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> choferCategoriaService.findById(99L));
        assertTrue(ex.getMessage().contains("ChoferCategoria"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void findByChoferIdShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChoferCategoria> page = new PageImpl<>(List.of(choferCategoria));
        when(repository.findByChoferId(1L, pageable)).thenReturn(page);

        Page<ChoferCategoriaResponse> result = choferCategoriaService.findByChoferId(1L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getChofer().getId());
        verify(repository).findByChoferId(1L, pageable);
    }

    @Test
    void findByCategoriaLicenciaIdShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ChoferCategoria> page = new PageImpl<>(List.of(choferCategoria));
        when(repository.findByCategoriaLicenciaId(2L, pageable)).thenReturn(page);

        Page<ChoferCategoriaResponse> result = choferCategoriaService.findByCategoriaLicenciaId(2L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).getCategoriaLicencia().getId());
        verify(repository).findByCategoriaLicenciaId(2L, pageable);
    }

    @Test
    void createShouldReturnResponseWhenChoferAndCategoriaValid() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(categoriaLicenciaRepository.findById(2L)).thenReturn(Optional.of(categoriaLicencia));
        when(repository.existsByChoferIdAndCategoriaLicenciaId(1L, 2L)).thenReturn(false);
        when(repository.save(any(ChoferCategoria.class))).thenAnswer(invocation -> {
            ChoferCategoria saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        ChoferCategoriaResponse result = choferCategoriaService.create(request);

        assertEquals(20L, result.getId());
        assertEquals(1L, result.getChofer().getId());
        assertEquals(2L, result.getCategoriaLicencia().getId());
        assertTrue(result.getActivo());
        verify(repository).save(any(ChoferCategoria.class));
    }

    @Test
    void createShouldThrowResourceNotFoundExceptionWhenChoferNotFound() {
        when(choferRepository.findById(99L)).thenReturn(Optional.empty());

        ChoferCategoriaRequest badRequest = ChoferCategoriaRequest.builder()
                .choferId(99L)
                .categoriaLicenciaId(2L)
                .fechaEmision(LocalDate.of(2023, 1, 15))
                .build();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> choferCategoriaService.create(badRequest));
        assertTrue(ex.getMessage().contains("Chofer"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldThrowResourceNotFoundExceptionWhenCategoriaNotFound() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(categoriaLicenciaRepository.findById(99L)).thenReturn(Optional.empty());

        ChoferCategoriaRequest badRequest = ChoferCategoriaRequest.builder()
                .choferId(1L)
                .categoriaLicenciaId(99L)
                .fechaEmision(LocalDate.of(2023, 1, 15))
                .build();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> choferCategoriaService.create(badRequest));
        assertTrue(ex.getMessage().contains("CategoriaLicencia"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldThrowBusinessExceptionWhenDuplicateChoferAndCategoria() {
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(categoriaLicenciaRepository.findById(2L)).thenReturn(Optional.of(categoriaLicencia));
        when(repository.existsByChoferIdAndCategoriaLicenciaId(1L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> choferCategoriaService.create(request));
        assertTrue(ex.getMessage().contains("2"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateWithSameChoferAndCategoriaShouldReturnResponse() {
        ChoferCategoriaRequest sameRequest = ChoferCategoriaRequest.builder()
                .choferId(1L)
                .categoriaLicenciaId(2L)
                .fechaEmision(LocalDate.of(2024, 6, 1))
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(choferCategoria));
        when(choferRepository.findById(1L)).thenReturn(Optional.of(chofer));
        when(categoriaLicenciaRepository.findById(2L)).thenReturn(Optional.of(categoriaLicencia));
        when(repository.save(any(ChoferCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChoferCategoriaResponse result = choferCategoriaService.update(10L, sameRequest);

        assertEquals(LocalDate.of(2024, 6, 1), result.getFechaEmision());
        assertEquals(1L, result.getChofer().getId());
        assertEquals(2L, result.getCategoriaLicencia().getId());
        verify(repository).existsByChoferIdAndCategoriaLicenciaId(anyLong(), anyLong());
        verify(repository).save(choferCategoria);
    }

    @Test
    void updateWithChangedChoferAndCategoriaShouldReturnResponse() {
        CategoriaLicencia newCategoria = CategoriaLicencia.builder()
                .id(3L)
                .codigo("C")
                .denominacion("Licencia C")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        Chofer newChofer = Chofer.builder()
                .id(5L)
                .empresa(empresa)
                .nombre("Maria")
                .apellidos("Lopez")
                .carneIdentidad("876543210")
                .numeroLicencia("LIC-002")
                .fechaNacimiento(LocalDate.of(1990, 3, 10))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        ChoferCategoriaRequest changedRequest = ChoferCategoriaRequest.builder()
                .choferId(5L)
                .categoriaLicenciaId(3L)
                .fechaEmision(LocalDate.of(2024, 6, 1))
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(choferCategoria));
        when(choferRepository.findById(5L)).thenReturn(Optional.of(newChofer));
        when(categoriaLicenciaRepository.findById(3L)).thenReturn(Optional.of(newCategoria));
        when(repository.existsByChoferIdAndCategoriaLicenciaId(5L, 3L)).thenReturn(false);
        when(repository.save(any(ChoferCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChoferCategoriaResponse result = choferCategoriaService.update(10L, changedRequest);

        assertEquals(5L, result.getChofer().getId());
        assertEquals("Maria", result.getChofer().getNombre());
        assertEquals(3L, result.getCategoriaLicencia().getId());
        assertEquals("C", result.getCategoriaLicencia().getCodigo());
        verify(repository).existsByChoferIdAndCategoriaLicenciaId(5L, 3L);
        verify(repository).save(choferCategoria);
    }

    @Test
    void updateWithChangedChoferAndCategoriaShouldThrowWhenDuplicate() {
        Chofer newChofer = Chofer.builder()
                .id(5L)
                .empresa(empresa)
                .nombre("Maria")
                .apellidos("Lopez")
                .carneIdentidad("876543210")
                .numeroLicencia("LIC-002")
                .fechaNacimiento(LocalDate.of(1990, 3, 10))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        ChoferCategoriaRequest changedRequest = ChoferCategoriaRequest.builder()
                .choferId(5L)
                .categoriaLicenciaId(2L)
                .fechaEmision(LocalDate.of(2024, 6, 1))
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(choferCategoria));
        when(choferRepository.findById(5L)).thenReturn(Optional.of(newChofer));
        when(categoriaLicenciaRepository.findById(2L)).thenReturn(Optional.of(categoriaLicencia));
        when(repository.existsByChoferIdAndCategoriaLicenciaId(5L, 2L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> choferCategoriaService.update(10L, changedRequest));
        assertTrue(ex.getMessage().contains("2"));
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenEntityNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> choferCategoriaService.update(99L, request));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(10L)).thenReturn(Optional.of(choferCategoria));
        when(repository.save(any(ChoferCategoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        choferCategoriaService.delete(10L);

        assertFalse(choferCategoria.getActivo());
        verify(repository).findById(10L);
        verify(repository).save(choferCategoria);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> choferCategoriaService.delete(99L));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }
}
