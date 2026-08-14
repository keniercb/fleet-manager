package com.fleet.management.service.impl;

import com.fleet.management.dto.user.UserRequest;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Permission;
import com.fleet.management.model.Role;
import com.fleet.management.model.User;
import com.fleet.management.repository.RoleRepository;
import com.fleet.management.repository.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
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
        user = User.builder()
                .id(1L)
                .email("admin@test.com")
                .password("encoded-pass")
                .roles(new HashSet<>(Set.of(role)))
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();
    }

    @Test
    void findAllShouldReturnPagedResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserResponse> result = userService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals("admin@test.com", result.getContent().get(0).getEmail());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void findByIdShouldReturnResponseWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse result = userService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("admin@test.com", result.getEmail());
        assertTrue(result.getActivo());
        assertEquals(1, result.getRoles().size());
        verify(userRepository).findById(1L);
    }

    @Test
    void findByIdShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
        assertTrue(ex.getMessage().contains("User"));
        assertTrue(ex.getMessage().contains("99"));
        verify(userRepository).findById(99L);
    }

    @Test
    void findByEmailShouldReturnResponseWhenFound() {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserResponse result = userService.findByEmail("admin@test.com");

        assertEquals("admin@test.com", result.getEmail());
        verify(userRepository).findByEmail("admin@test.com");
    }

    @Test
    void findByEmailShouldThrowResourceNotFoundExceptionWhenNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> userService.findByEmail("unknown@test.com"));
        assertTrue(ex.getMessage().contains("User"));
        assertTrue(ex.getMessage().contains("unknown@test.com"));
        verify(userRepository).findByEmail("unknown@test.com");
    }

    @Test
    void createShouldEncodePasswordAndSave() {
        UserRequest request = UserRequest.builder()
                .email("new@test.com")
                .password("plain-pass")
                .build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        UserResponse result = userService.create(request);

        assertEquals("new@test.com", result.getEmail());
        assertTrue(result.getActivo());
        verify(userRepository).existsByEmail("new@test.com");
        verify(passwordEncoder).encode("plain-pass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createShouldThrowBusinessExceptionWhenEmailAlreadyExists() {
        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("plain-pass")
                .build();

        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.create(request));
        assertTrue(ex.getMessage().contains("admin@test.com"));
        verify(userRepository).existsByEmail("admin@test.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createWithRoleIdsShouldResolveRoles() {
        UserRequest request = UserRequest.builder()
                .email("new@test.com")
                .password("plain-pass")
                .roleIds(Set.of(1L))
                .build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        UserResponse result = userService.create(request);

        assertEquals("new@test.com", result.getEmail());
        assertEquals(1, result.getRoles().size());
        verify(roleRepository).findById(1L);
    }

    @Test
    void createWithRoleIdsShouldThrowWhenRoleIdNotFound() {
        UserRequest request = UserRequest.builder()
                .email("new@test.com")
                .password("plain-pass")
                .roleIds(Set.of(99L))
                .build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> userService.create(request));
        assertTrue(ex.getMessage().contains("Role"));
        assertTrue(ex.getMessage().contains("99"));
        verify(roleRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createWithEmptyRoleIdsShouldCreateUserWithNoRoles() {
        UserRequest request = UserRequest.builder()
                .email("new@test.com")
                .password("plain-pass")
                .roleIds(new HashSet<>())
                .build();

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setFechaCreacion(now);
            saved.setFechaActualizacion(now);
            return saved;
        });

        UserResponse result = userService.create(request);

        assertEquals("new@test.com", result.getEmail());
        assertTrue(result.getRoles().isEmpty());
        verify(roleRepository, never()).findById(anyLong());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateShouldEncodePasswordAndSave() {
        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("new-pass")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.update(1L, request);

        assertEquals("admin@test.com", result.getEmail());
        verify(passwordEncoder).encode("new-pass");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateShouldNotChangeEmail() {
        UserRequest request = UserRequest.builder()
                .email("different@test.com")
                .password("new-pass")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.update(1L, request);

        assertEquals("admin@test.com", result.getEmail());
    }

    @Test
    void updateWithRoleIdsShouldUpdateRoles() {
        Role newRole = Role.builder()
                .id(2L)
                .name("USER")
                .description("Regular user")
                .permissions(new HashSet<>())
                .activo(true)
                .fechaCreacion(now)
                .fechaActualizacion(now)
                .build();

        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("new-pass")
                .roleIds(Set.of(2L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded-pass");
        when(roleRepository.findById(2L)).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse result = userService.update(1L, request);

        assertEquals(1, result.getRoles().size());
        assertTrue(result.getRoles().stream().anyMatch(r -> "USER".equals(r.getName())));
        verify(roleRepository).findById(2L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateWithoutRoleIdsShouldNotChangeRoles() {
        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("new-pass")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-pass")).thenReturn("new-encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.update(1L, request);

        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName())));
        verify(roleRepository, never()).findById(anyLong());
    }

    @Test
    void updateShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("new-pass")
                .build();

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.update(99L, request));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateWithRoleIdsShouldThrowWhenRoleIdNotFound() {
        UserRequest request = UserRequest.builder()
                .email("admin@test.com")
                .password("new-pass")
                .roleIds(Set.of(99L))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> userService.update(1L, request));
        assertTrue(ex.getMessage().contains("Role"));
        verify(roleRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteShouldSetActivoFalseAndSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.delete(1L);

        assertFalse(user.getActivo());
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void deleteShouldThrowResourceNotFoundExceptionWhenIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(99L));
        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any());
    }
}