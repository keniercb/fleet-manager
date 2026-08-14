package com.fleet.management.service.impl;

import com.fleet.management.dto.permission.PermissionRequest;
import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Permission;
import com.fleet.management.repository.PermissionRepository;
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
class PermissionServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionServiceImpl permissionService;

    private Permission permission;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        permission = Permission.builder()
                .id(1L)
                .name("user:read")
                .description("Read users")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Permission> page = new PageImpl<>(List.of(permission));
        when(permissionRepository.findAll(pageable)).thenReturn(page);

        Page<PermissionResponse> result = permissionService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("user:read", result.getContent().get(0).getName());
        verify(permissionRepository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));

        PermissionResponse result = permissionService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("user:read", result.getName());
        assertEquals("Read users", result.getDescription());
        assertTrue(result.getActivo());
        verify(permissionRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> permissionService.findById(99L));
        assertTrue(ex.getMessage().contains("Permission"));
        assertTrue(ex.getMessage().contains("99"));
        verify(permissionRepository).findById(99L);
    }

    @Test
    void findByNameShouldReturnResponseWhenFound() {
        when(permissionRepository.findByName("user:read")).thenReturn(Optional.of(permission));

        PermissionResponse result = permissionService.findByName("user:read");

        assertEquals("user:read", result.getName());
        verify(permissionRepository).findByName("user:read");
    }

    @Test
    void findByNameShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(permissionRepository.findByName("unknown:perm")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> permissionService.findByName("unknown:perm"));
        assertTrue(ex.getMessage().contains("Permission"));
        assertTrue(ex.getMessage().contains("unknown:perm"));
        verify(permissionRepository).findByName("unknown:perm");
    }

    @Test
    void createShouldSaveAndReturnResponse() {
        PermissionRequest request = PermissionRequest.builder()
                .name("user:write")
                .description("Write users")
                .build();

        when(permissionRepository.existsByName("user:write")).thenReturn(false);
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        PermissionResponse result = permissionService.create(request);

        assertEquals("user:write", result.getName());
        assertEquals("Write users", result.getDescription());
        assertTrue(result.getActivo());
        verify(permissionRepository).existsByName("user:write");
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenNameAlreadyExists() {
        PermissionRequest request = PermissionRequest.builder()
                .name("user:read")
                .description("Duplicate")
                .build();

        when(permissionRepository.existsByName("user:read")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> permissionService.create(request));
        assertTrue(ex.getMessage().contains("user:read"));
        verify(permissionRepository).existsByName("user:read");
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void updateShouldUpdateFieldsAndSave() {
        PermissionRequest request = PermissionRequest.builder()
                .name("user:update")
                .description("Update users")
                .build();

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.findByName("user:update")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermissionResponse result = permissionService.update(1L, request);

        assertEquals("user:update", result.getName());
        assertEquals("Update users", result.getDescription());
        verify(permissionRepository).findById(1L);
        verify(permissionRepository).findByName("user:update");
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void updateShouldAllowSameNameWithoutConflict() {
        PermissionRequest request = PermissionRequest.builder()
                .name("user:read")
                .description("Updated description")
                .build();

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.findByName("user:read")).thenReturn(Optional.of(permission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermissionResponse result = permissionService.update(1L, request);

        assertEquals("user:read", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenNameTakenByAnotherPermission() {
        Permission existingPermission = Permission.builder()
                .id(2L)
                .name("vehicle:read")
                .description("Read vehicles")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        PermissionRequest request = PermissionRequest.builder()
                .name("vehicle:read")
                .description("Trying to take vehicle:read")
                .build();

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.findByName("vehicle:read")).thenReturn(Optional.of(existingPermission));

        BusinessException ex = assertThrows(BusinessException.class, () -> permissionService.update(1L, request));
        assertTrue(ex.getMessage().contains("vehicle:read"));
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        PermissionRequest request = PermissionRequest.builder()
                .name("user:write")
                .description("Write users")
                .build();

        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> permissionService.update(99L, request));
        verify(permissionRepository).findById(99L);
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        permissionService.delete(1L);

        assertFalse(permission.getActivo());
        verify(permissionRepository).findById(1L);
        verify(permissionRepository).save(permission);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> permissionService.delete(99L));
        verify(permissionRepository).findById(99L);
        verify(permissionRepository, never()).save(any());
    }
}