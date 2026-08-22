package com.fleet.management.service.impl;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaServiceImplTest {

    @Mock
    private EmpresaRepository repository;

    @InjectMocks
    private EmpresaServiceImpl service;

    private Empresa entity;
    private EmpresaRequest request;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        entity = Empresa.builder()
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
        request = EmpresaRequest.builder()
                .codigo("EMP-001")
                .nombre("Transportes ABC")
                .direccion("Av. Principal 123")
                .telefono("555-1234")
                .email("contacto@abc.com")
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Empresa> page = new PageImpl<>(List.of(entity));
        when(repository.findAllByActivoTrue(pageable)).thenReturn(page);

        Page<EmpresaResponse> result = service.findAll(null, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("EMP-001", result.getContent().get(0).getCodigo());
        assertEquals("Transportes ABC", result.getContent().get(0).getNombre());
        verify(repository).findAllByActivoTrue(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        EmpresaResponse result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("EMP-001", result.getCodigo());
        assertEquals("Transportes ABC", result.getNombre());
        assertEquals("Av. Principal 123", result.getDireccion());
        assertEquals("555-1234", result.getTelefono());
        assertEquals("contacto@abc.com", result.getEmail());
        assertTrue(result.getActivo());
        verify(repository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        assertTrue(ex.getMessage().contains("Empresa"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void findByCodigoShouldReturnResponseWhenFound() {
        when(repository.findByCodigo("EMP-001")).thenReturn(Optional.of(entity));

        EmpresaResponse result = service.findByCodigo("EMP-001");

        assertEquals("EMP-001", result.getCodigo());
        assertEquals("Transportes ABC", result.getNombre());
        verify(repository).findByCodigo("EMP-001");
    }

    @Test
    void findByCodigoShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findByCodigo("UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findByCodigo("UNKNOWN"));
        assertTrue(ex.getMessage().contains("Empresa"));
        assertTrue(ex.getMessage().contains("UNKNOWN"));
        verify(repository).findByCodigo("UNKNOWN");
    }

    @Test
    void createShouldReturnResponseWhenCodigoIsUnique() {
        when(repository.existsByCodigo("EMP-001")).thenReturn(false);
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> {
            Empresa saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EmpresaResponse result = service.create(request);

        assertEquals("EMP-001", result.getCodigo());
        assertEquals("Transportes ABC", result.getNombre());
        assertEquals("Av. Principal 123", result.getDireccion());
        assertEquals("555-1234", result.getTelefono());
        assertEquals("contacto@abc.com", result.getEmail());
        assertTrue(result.getActivo());
        verify(repository).existsByCodigo("EMP-001");
        verify(repository).save(any(Empresa.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenCodigoAlreadyExists() {
        when(repository.existsByCodigo("EMP-001")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("EMP-001"));
        verify(repository).existsByCodigo("EMP-001");
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldReturnResponseWhenIdExistsAndCodigoIsUnique() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("EMP-002")).thenReturn(false);
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmpresaRequest updateRequest = EmpresaRequest.builder()
                .codigo("EMP-002")
                .nombre("Transportes XYZ")
                .direccion("Av. Secundaria 456")
                .telefono("555-9876")
                .email("info@xyz.com")
                .build();

        EmpresaResponse result = service.update(1L, updateRequest);

        assertEquals("EMP-002", result.getCodigo());
        assertEquals("Transportes XYZ", result.getNombre());
        assertEquals("Av. Secundaria 456", result.getDireccion());
        assertEquals("555-9876", result.getTelefono());
        assertEquals("info@xyz.com", result.getEmail());
        verify(repository).findById(1L);
        verify(repository).existsByCodigo("EMP-002");
        verify(repository).save(any(Empresa.class));
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
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmpresaResponse result = service.update(1L, request);

        assertEquals("EMP-001", result.getCodigo());
        verify(repository, never()).existsByCodigo(anyString());
        verify(repository).save(any(Empresa.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenCodigoChangedAndAlreadyExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByCodigo("EMP-002")).thenReturn(true);

        EmpresaRequest updateRequest = EmpresaRequest.builder()
                .codigo("EMP-002")
                .nombre("Transportes XYZ")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, updateRequest));
        assertTrue(ex.getMessage().contains("EMP-002"));
        verify(repository).existsByCodigo("EMP-002");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

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