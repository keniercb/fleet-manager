package com.fleet.management.service.impl;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.repository.TipoVehiculoRepository;
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
class TipoVehiculoServiceImplTest {

    @Mock
    private TipoVehiculoRepository repository;

    @InjectMocks
    private TipoVehiculoServiceImpl service;

    private TipoVehiculo entity;
    private TipoVehiculoRequest request;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        entity = TipoVehiculo.builder()
                .id(1L)
                .nombre("Sedan")
                .descripcion("Vehiculo tipo sedan")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
        request = TipoVehiculoRequest.builder()
                .nombre("Sedan")
                .descripcion("Vehiculo tipo sedan")
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<TipoVehiculo> page = new PageImpl<>(List.of(entity));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<TipoVehiculoResponse> result = service.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("Sedan", result.getContent().get(0).getNombre());
        verify(repository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        TipoVehiculoResponse result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Sedan", result.getNombre());
        assertEquals("Vehiculo tipo sedan", result.getDescripcion());
        assertTrue(result.getActivo());
        verify(repository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        assertTrue(ex.getMessage().contains("TipoVehiculo"));
        assertTrue(ex.getMessage().contains("99"));
        verify(repository).findById(99L);
    }

    @Test
    void createShouldReturnResponseWhenNombreIsUnique() {
        when(repository.existsByNombre("Sedan")).thenReturn(false);
        when(repository.save(any(TipoVehiculo.class))).thenAnswer(invocation -> {
            TipoVehiculo saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TipoVehiculoResponse result = service.create(request);

        assertEquals("Sedan", result.getNombre());
        assertEquals("Vehiculo tipo sedan", result.getDescripcion());
        assertTrue(result.getActivo());
        verify(repository).existsByNombre("Sedan");
        verify(repository).save(any(TipoVehiculo.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenNombreAlreadyExists() {
        when(repository.existsByNombre("Sedan")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("Sedan"));
        verify(repository).existsByNombre("Sedan");
        verify(repository, never()).save(any());
    }

    @Test
    void updateShouldReturnResponseWhenIdExistsAndNombreIsUnique() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByNombre("SUV")).thenReturn(false);
        when(repository.save(any(TipoVehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoVehiculoRequest updateRequest = TipoVehiculoRequest.builder()
                .nombre("SUV")
                .descripcion("Vehiculo tipo SUV")
                .build();

        TipoVehiculoResponse result = service.update(1L, updateRequest);

        assertEquals("SUV", result.getNombre());
        assertEquals("Vehiculo tipo SUV", result.getDescripcion());
        verify(repository).findById(1L);
        verify(repository).existsByNombre("SUV");
        verify(repository).save(any(TipoVehiculo.class));
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
        when(repository.save(any(TipoVehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TipoVehiculoResponse result = service.update(1L, request);

        assertEquals("Sedan", result.getNombre());
        verify(repository, never()).existsByNombre(anyString());
        verify(repository).save(any(TipoVehiculo.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenNombreChangedAndAlreadyExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.existsByNombre("SUV")).thenReturn(true);

        TipoVehiculoRequest updateRequest = TipoVehiculoRequest.builder()
                .nombre("SUV")
                .descripcion("Vehiculo tipo SUV")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(1L, updateRequest));
        assertTrue(ex.getMessage().contains("SUV"));
        verify(repository).existsByNombre("SUV");
        verify(repository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(repository.save(any(TipoVehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
