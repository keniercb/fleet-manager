package com.fleet.management.service.impl;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.repository.TipoCombustibleRepository;
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
class TipoCombustibleServiceImplTest {

    @Mock
    private TipoCombustibleRepository repository;

    @InjectMocks
    private TipoCombustibleServiceImpl service;

    private TipoCombustible entity;
    private TipoCombustibleRequest request;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        entity = TipoCombustible.builder()
                .id(1L)
                .codigo("GASOLINA")
                .denominacion("Gasolina 95 Octanos")
                .descripcion("Combustible gasolina regular")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
        request = TipoCombustibleRequest.builder()
                .codigo("GASOLINA")
                .denominacion("Gasolina 95 Octanos")
                .descripcion("Combustible gasolina regular")
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TipoCombustible> page = new PageImpl<>(List.of(entity));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<TipoCombustibleResponse> result = service.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("GASOLINA", result.getContent().get(0).getCodigo());
        verify(repository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        TipoCombustibleResponse result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("GASOLINA", result.getCodigo());
        assertEquals("Gasolina 95 Octanos", result.getDenominacion());
        assertTrue(result.getActivo());
        verify(repository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        assertTrue(ex.getMessage().contains("TipoCombustible"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void findByCodigoShouldReturnResponseWhenFound() {
        when(repository.findByCodigo("GASOLINA")).thenReturn(Optional.of(entity));

        TipoCombustibleResponse result = service.findByCodigo("GASOLINA");

        assertEquals("GASOLINA", result.getCodigo());
        assertEquals("Gasolina 95 Octanos", result.getDenominacion());
        verify(repository).findByCodigo("GASOLINA");
    }

    @Test
    void findByCodigoShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findByCodigo("UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findByCodigo("UNKNOWN"));
        assertTrue(ex.getMessage().contains("TipoCombustible"));
        assertTrue(ex.getMessage().contains("UNKNOWN"));
        verify(repository).findByCodigo("UNKNOWN");
    }

    @Test
    void createShouldReturnResponseWhenCodigoIsUnique() {
        when(repository.existsByCodigo("GASOLINA")).thenReturn(false);
        when(repository.save(any(TipoCombustible.class))).thenAnswer(invocation -> {
            TipoCombustible saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TipoCombustibleResponse result = service.create(request);

        assertEquals("GASOLINA", result.getCodigo());
        assertEquals("Gasolina 95 Octanos", result.getDenominacion());
        assertTrue(result.getActivo());
        verify(repository).existsByCodigo("GASOLINA");
        verify(repository).save(any(TipoCombustible.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenCodigoAlreadyExists() {
        when(repository.existsByCodigo("GASOLINA")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("GASOLINA"));
        verify(repository).existsByCodigo("GASOLINA");
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldReturnResponseWhenIdExistsAndCodigoIsUnique() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("DIESEL")).thenReturn(false);
        when(repository.save(any(TipoCombustible.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoCombustibleRequest updateRequest = TipoCombustibleRequest.builder()
                .codigo("DIESEL")
                .denominacion("Diesel Premium")
                .descripcion("Combustible diesel premium")
                .build();

        TipoCombustibleResponse result = service.update(1L, updateRequest);

        assertEquals("DIESEL", result.getCodigo());
        assertEquals("Diesel Premium", result.getDenominacion());
        verify(repository).findById(1L);
        verify(repository).existsByCodigo("DIESEL");
        verify(repository).save(any(TipoCombustible.class));
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
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(TipoCombustible.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoCombustibleResponse result = service.update(1L, request);

        assertEquals("GASOLINA", result.getCodigo());
        verify(repository, never()).existsByCodigo(anyString());
        verify(repository).save(any(TipoCombustible.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenCodigoChangedAndAlreadyExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("DIESEL")).thenReturn(true);

        TipoCombustibleRequest updateRequest = TipoCombustibleRequest.builder()
                .codigo("DIESEL")
                .denominacion("Diesel Premium")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, updateRequest));
        assertTrue(ex.getMessage().contains("DIESEL"));
        verify(repository).existsByCodigo("DIESEL");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(TipoCombustible.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
