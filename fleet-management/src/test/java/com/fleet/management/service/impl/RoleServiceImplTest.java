package com.fleet.management.service.impl;

import com.fleet.management.dto.role.RoleRequest;
import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Permission;
import com.fleet.management.model.Role;
import com.fleet.management.repository.PermissionRepository;
import com.fleet.management.repository.RoleRepository;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role role;
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
        role = Role.builder()
                .id(1L)
                .name("ADMIN")
                .description("Administrator")
                .permissions(new HashSet<>(Set.of(permission)))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Role> page = new PageImpl<>(List.of(role));
        when(roleRepository.findAll(pageable)).thenReturn(page);

        Page<RoleResponse> result = roleService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("ADMIN", result.getContent().get(0).getName());
        verify(roleRepository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

        RoleResponse result = roleService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("ADMIN", result.getName());
        assertEquals("Administrator", result.getDescription());
        assertTrue(result.getActivo());
        assertEquals(1, result.getPermissions().size());
        verify(roleRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> roleService.findById(99L));
        assertTrue(ex.getMessage().contains("Role"));
        assertTrue(ex.getMessage().contains("99"));
        verify(roleRepository).findById(99L);
    }

    @Test
    void findByNameShouldReturnResponseWhenFound() {
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        RoleResponse result = roleService.findByName("ADMIN");

        assertEquals("ADMIN", result.getName());
        verify(roleRepository).findByName("ADMIN");
    }

    @Test
    void findByNameShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> roleService.findByName("UNKNOWN"));
        assertTrue(ex.getMessage().contains("Role"));
        assertTrue(ex.getMessage().contains("UNKNOWN"));
        verify(roleRepository).findByName("UNKNOWN");
    }

    @Test
    void findByPermissionIdShouldReturnPagedRoles() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Role> page = new PageImpl<>(List.of(role));
        when(roleRepository.findByPermissionId(1L, pageable)).thenReturn(page);

        Page<RoleResponse> result = roleService.findByPermissionId(1L, pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("ADMIN", result.getContent().get(0).getName());
        verify(roleRepository).findByPermissionId(1L, pageable);
    }

    @Test
    void createShouldSaveAndReturnResponse() {
        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Regular user")
                .build();

        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        RoleResponse result = roleService.create(request);

        assertEquals("USER", result.getName());
        assertEquals("Regular user", result.getDescription());
        assertTrue(result.getActivo());
        verify(roleRepository).existsByName("USER");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenNameAlreadyExists() {
        RoleRequest request = RoleRequest.builder()
                .name("ADMIN")
                .description("Duplicate")
                .build();

        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> roleService.create(request));
        assertTrue(ex.getMessage().contains("ADMIN"));
        verify(roleRepository).existsByName("ADMIN");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createWithPermissionIdsShouldResolvePermissions() {
        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Regular user")
                .permissionIds(Set.of(1L))
                .build();

        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(permissionRepository.findById(1L)).thenReturn(Optional.of(permission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        RoleResponse result = roleService.create(request);

        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().stream().anyMatch(p -> "user:read".equals(p.getName())));
        verify(permissionRepository).findById(1L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createShouldThrowResourceNotFoundExceptionWhenPermissionIdNotFound() {
        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Regular user")
                .permissionIds(Set.of(99L))
                .build();

        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> roleService.create(request));
        assertTrue(ex.getMessage().contains("Permission"));
        assertTrue(ex.getMessage().contains("99"));
        verify(permissionRepository).findById(99L);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void createWithEmptyPermissionIdsShouldCreateRoleWithNoPermissions() {
        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Regular user")
                .permissionIds(new HashSet<>())
                .build();

        when(roleRepository.existsByName("USER")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        RoleResponse result = roleService.create(request);

        assertTrue(result.getPermissions().isEmpty());
        verify(permissionRepository, never()).findById(anyLong());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateShouldUpdateFieldsAndSave() {
        RoleRequest request = RoleRequest.builder()
                .name("SUPER_ADMIN")
                .description("Super administrator")
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.update(1L, request);

        assertEquals("SUPER_ADMIN", result.getName());
        assertEquals("Super administrator", result.getDescription());
        verify(roleRepository).findById(1L);
        verify(roleRepository).findByName("SUPER_ADMIN");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateShouldAllowSameNameWithoutConflict() {
        RoleRequest request = RoleRequest.builder()
                .name("ADMIN")
                .description("Updated description")
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.update(1L, request);

        assertEquals("ADMIN", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateShouldThrowBusinessExceptionWhenNameTakenByAnotherRole() {
        Role anotherRole = Role.builder()
                .id(2L)
                .name("USER")
                .description("Regular user")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Trying to take USER name")
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(anotherRole));

        BusinessException ex = assertThrows(BusinessException.class, () -> roleService.update(1L, request));
        assertTrue(ex.getMessage().contains("USER"));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateWithPermissionIdsShouldUpdatePermissions() {
        Permission newPermission = Permission.builder()
                .id(2L)
                .name("user:write")
                .description("Write users")
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        RoleRequest request = RoleRequest.builder()
                .name("ADMIN")
                .description("Administrator")
                .permissionIds(Set.of(2L))
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findById(2L)).thenReturn(Optional.of(newPermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.update(1L, request);

        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().stream().anyMatch(p -> "user:write".equals(p.getName())));
        verify(permissionRepository).findById(2L);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateWithoutPermissionIdsShouldNotChangePermissions() {
        RoleRequest request = RoleRequest.builder()
                .name("ADMIN")
                .description("Updated description")
                .build();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleResponse result = roleService.update(1L, request);

        assertEquals(1, result.getPermissions().size());
        assertTrue(result.getPermissions().stream().anyMatch(p -> "user:read".equals(p.getName())));
        verify(permissionRepository, never()).findById(anyLong());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        RoleRequest request = RoleRequest.builder()
                .name("USER")
                .description("Regular user")
                .build();

        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.update(99L, request));
        verify(roleRepository).findById(99L);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        roleService.delete(1L);

        assertFalse(role.getActivo());
        verify(roleRepository).findById(1L);
        verify(roleRepository).save(role);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.delete(99L));
        verify(roleRepository).findById(99L);
        verify(roleRepository, never()).save(any());
    }
}