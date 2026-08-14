package com.fleet.management.service.impl;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.model.User;
import com.fleet.management.repository.UserRepository;
import com.fleet.management.security.CustomUserDetailsService;
import com.fleet.management.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private LoginRequestDto loginRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("admin@test.com")
                .password("encoded-pass")
                .roles(new HashSet<>())
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        loginRequest = LoginRequestDto.builder()
                .email("admin@test.com")
                .password("raw-pass")
                .build();
    }

    @Test
    void loginShouldReturnAuthResponseDtoOnSuccess() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername("admin@test.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-123");

        AuthResponseDto result = authService.login(loginRequest);

        assertEquals("jwt-token-123", result.getToken());
        assertEquals("Bearer", result.getType());
        assertEquals(1L, result.getUserId());
        assertEquals("admin@test.com", result.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("admin@test.com");
        verify(customUserDetailsService).loadUserByUsername("admin@test.com");
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void loginShouldThrowWhenAuthenticationFails() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginShouldThrowBusinessExceptionWhenUserNotFoundAfterAuth() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(loginRequest));
        assertTrue(ex.getMessage().contains("Usuario no encontrado"));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("admin@test.com");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void getCurrentUserShouldReturnUserWhenAuthenticatedWithUserDetails() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            User result = authService.getCurrentUser();

            assertEquals(1L, result.getId());
            assertEquals("admin@test.com", result.getEmail());
            verify(userRepository).findByEmail("admin@test.com");
        }
    }

    @Test
    void getCurrentUserShouldReturnUserWhenPrincipalIsString() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            User result = authService.getCurrentUser();

            assertEquals(1L, result.getId());
            assertEquals("admin@test.com", result.getEmail());
            verify(userRepository).findByEmail("admin@test.com");
        }
    }

    @Test
    void getCurrentUserShouldThrowWhenNoAuthentication() {
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUser());
            assertTrue(ex.getMessage().contains("No hay un usuario autenticado"));
        }
    }

    @Test
    void getCurrentUserShouldThrowWhenNotAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUser());
            assertTrue(ex.getMessage().contains("No hay un usuario autenticado"));
        }
    }

    @Test
    void getCurrentUserShouldThrowBusinessExceptionWhenUserNotFoundInDb() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("admin@test.com");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedContextHolder = mockStatic(SecurityContextHolder.class)) {
            mockedContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            BusinessException ex = assertThrows(BusinessException.class, () -> authService.getCurrentUser());
            assertTrue(ex.getMessage().contains("Usuario autenticado no encontrado"));
        }
    }
}