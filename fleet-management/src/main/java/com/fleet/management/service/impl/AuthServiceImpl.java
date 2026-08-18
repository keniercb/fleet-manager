package com.fleet.management.service.impl;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.CambioPasswordRequest;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.User;
import com.fleet.management.repository.UserRepository;
import com.fleet.management.security.CustomUserDetailsService;
import com.fleet.management.security.JwtService;
import com.fleet.management.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        String token = jwtService.generateToken(
                customUserDetailsService.loadUserByUsername(request.getEmail())
        );

        return AuthResponseDto.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("No hay un usuario autenticado");
        }

        Object principal = authentication.getPrincipal();
        String email;

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no encontrado en la base de datos"));
    }

    @Override
    @Transactional
    public void cambiarPassword(CambioPasswordRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        if (!user.getActivo()) {
            throw new BusinessException("El usuario esta inactivo");
        }

        if (!passwordEncoder.matches(request.getPasswordAnterior(), user.getPassword())) {
            throw new BusinessException("La contrasena anterior es incorrecta");
        }

        if (!request.getNuevaPassword().equals(request.getConfirmacionPassword())) {
            throw new BusinessException("La nueva contrasena y la confirmacion no coinciden");
        }

        if (passwordEncoder.matches(request.getNuevaPassword(), user.getPassword())) {
            throw new BusinessException("La nueva contrasena debe ser diferente a la actual");
        }

        user.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        userRepository.save(user);
    }
}
