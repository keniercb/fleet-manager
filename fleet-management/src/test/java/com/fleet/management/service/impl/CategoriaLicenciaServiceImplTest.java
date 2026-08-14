package com.fleet.management.service.impl;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaRequest;
import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.repository.CategoriaLicenciaRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaLicenciaServiceImplTest {

    @Mock
    private CategoriaLicenciaRepository repository;

    @InjectMocks
    private CategoriaLicenciaServiceImpl service;

    private CategoriaLicencia entity;
    private CategoriaLicenciaRequest request;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        entity = CategoriaLicencia.builder()
                .id(1L)
                .codigo("A")
                .denominacion("Categoria A")
                .descripcion("Licencia tipo A - Motocicletas")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
        request = CategoriaLicenciaRequest.builder()
                .codigo("a")
                .denominacion("Categoria A")
                .descripcion("Licencia tipo A - Motocicletas")
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CategoriaLicencia> page = new PageImpl<>(List.of(entity));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<CategoriaLicenciaResponse> result = service.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("A", result.getContent().get(0).getCodigo());
        verify(repository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        CategoriaLicenciaResponse result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("A", result.getCodigo());
        assertEquals("Categoria A", result.getDenominacion());
        assertTrue(result.getActivo());
        verify(repository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        assertTrue(ex.getMessage().contains("CategoriaLicencia"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void findByCodigoShouldReturnResponseWhenFound() {
        when(repository.findByCodigo("A")).thenReturn(Optional.of(entity));

        CategoriaLicenciaResponse result = service.findByCodigo("A");

        assertEquals("A", result.getCodigo());
        assertEquals("Categoria A", result.getDenominacion());
        verify(repository).findByCodigo("A");
    }

    @Test
    void findByCodigoShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findByCodigo("Z")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findByCodigo("Z"));
        assertTrue(ex.getMessage().contains("CategoriaLicencia"));
        assertTrue(ex.getMessage().contains("Z"));
        verify(repository).findByCodigo("Z");
    }

    @Test
    void createShouldUppercaseCodigoAndReturnResponseWhenUnique() {
        when(repository.existsByCodigo("A")).thenReturn(false);
        when(repository.save(any(CategoriaLicencia.class))).thenAnswer(invocation -> {
            CategoriaLicencia saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CategoriaLicenciaResponse result = service.create(request);

        assertEquals("A", result.getCodigo());
        assertEquals("Categoria A", result.getDenominacion());
        assertTrue(result.getActivo());
        verify(repository).existsByCodigo("A");
        verify(repository).save(any(CategoriaLicencia.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenCodigoAlreadyExists() {
        when(repository.existsByCodigo("A")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("A"));
        verify(repository).existsByCodigo("A");
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldUppercaseCodigoAndReturnResponseWhenIdExistsAndCodigoIsUnique() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("B")).thenReturn(false);
        when(repository.save(any(CategoriaLicencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaLicenciaRequest updateRequest = CategoriaLicenciaRequest.builder()
                .codigo("b")
                .denominacion("Categoria B")
                .descripcion("Licencia tipo B - Automoviles")
                .build();

        CategoriaLicenciaResponse result = service.update(1L, updateRequest);

        assertEquals("B", result.getCodigo());
        assertEquals("Categoria B", result.getDenominacion());
        verify(repository).findById(1L);
        verify(repository).existsByCodigo("B");
        verify(repository).save(any(CategoriaLicencia.class));
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldNotCheckDuplicateWhenCodigoUnchanged() {
        CategoriaLicenciaRequest sameCodigoRequest = CategoriaLicenciaRequest.builder()
                .codigo("a")
                .denominacion("Categoria A Updated")
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(CategoriaLicencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoriaLicenciaResponse result = service.update(1L, sameCodigoRequest);

        assertEquals("A", result.getCodigo());
        assertEquals("Categoria A Updated", result.getDenominacion());
        verify(repository, never()).existsByCodigo(anyString());
        verify(repository).save(any(CategoriaLicencia.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenCodigoChangedAndAlreadyExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("B")).thenReturn(true);

        CategoriaLicenciaRequest updateRequest = CategoriaLicenciaRequest.builder()
                .codigo("b")
                .denominacion("Categoria B")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, updateRequest));
        assertTrue(ex.getMessage().contains("B"));
        verify(repository).existsByCodigo("B");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(CategoriaLicencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(1L);

        assertFalse(entity.getActivo());
        verify(repository).findById(1L);
        verify(repository).save(entity);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }
}
