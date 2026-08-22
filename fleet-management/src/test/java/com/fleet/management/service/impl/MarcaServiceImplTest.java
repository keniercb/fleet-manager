package com.fleet.management.service.impl;

import com.fleet.management.dto.marca.MarcaRequest;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Marca;
import com.fleet.management.repository.MarcaRepository;
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
class MarcaServiceImplTest {

    @Mock
    private MarcaRepository repository;

    @InjectMocks
    private MarcaServiceImpl service;

    private Marca entity;
    private MarcaRequest request;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        entity = Marca.builder()
                .id(1L)
                .nombre("Toyota")
                .descripcion("Marca japonesa de vehiculos")
                .paisOrigen("Japon")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
        request = MarcaRequest.builder()
                .nombre("Toyota")
                .descripcion("Marca japonesa de vehiculos")
                .paisOrigen("Japon")
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Marca> page = new PageImpl<>(List.of(entity));
        when(repository.findAllByActivoTrue(pageable)).thenReturn(page);

        Page<MarcaResponse> result = service.findAll(null, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("Toyota", result.getContent().get(0).getNombre());
        verify(repository).findAllByActivoTrue(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        MarcaResponse result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Toyota", result.getNombre());
        assertEquals("Marca japonesa de vehiculos", result.getDescripcion());
        assertEquals("Japon", result.getPaisOrigen());
        assertTrue(result.getActivo());
        verify(repository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        assertTrue(ex.getMessage().contains("Marca"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void createShouldReturnResponseWhenNombreIsUnique() {
        when(repository.existsByNombre("Toyota")).thenReturn(false);
        when(repository.save(any(Marca.class))).thenAnswer(invocation -> {
            Marca saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MarcaResponse result = service.create(request);

        assertEquals("Toyota", result.getNombre());
        assertEquals("Marca japonesa de vehiculos", result.getDescripcion());
        assertEquals("Japon", result.getPaisOrigen());
        assertTrue(result.getActivo());
        verify(repository).existsByNombre("Toyota");
        verify(repository).save(any(Marca.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenNombreAlreadyExists() {
        when(repository.existsByNombre("Toyota")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Toyota"));
        verify(repository).existsByNombre("Toyota");
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldReturnResponseWhenIdExistsAndNombreIsUnique() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByNombre("Honda")).thenReturn(false);
        when(repository.save(any(Marca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarcaRequest updateRequest = MarcaRequest.builder()
                .nombre("Honda")
                .descripcion("Marca japonesa de motos y autos")
                .paisOrigen("Japon")
                .build();

        MarcaResponse result = service.update(1L, updateRequest);

        assertEquals("Honda", result.getNombre());
        assertEquals("Marca japonesa de motos y autos", result.getDescripcion());
        verify(repository).findById(1L);
        verify(repository).existsByNombre("Honda");
        verify(repository).save(any(Marca.class));
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L, request));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldNotCheckDuplicateWhenNombreUnchanged() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(Marca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MarcaResponse result = service.update(1L, request);

        assertEquals("Toyota", result.getNombre());
        verify(repository, never()).existsByNombre(anyString());
        verify(repository).save(any(Marca.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenNombreChangedAndAlreadyExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByNombre("Honda")).thenReturn(true);

        MarcaRequest updateRequest = MarcaRequest.builder()
                .nombre("Honda")
                .descripcion("Marca japonesa")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, updateRequest));
        assertTrue(ex.getMessage().contains("Honda"));
        verify(repository).existsByNombre("Honda");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(Marca.class))).thenAnswer(invocation -> invocation.getArgument(0));

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