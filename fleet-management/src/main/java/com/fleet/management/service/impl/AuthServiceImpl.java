package com.fleet.management.service.impl;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.CambioPasswordRequest;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.exception.BusinessError;
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
                .orElseThrow(() -> BusinessError.usuarioNoEncontrado(request.getEmail()));

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
            throw BusinessError.noHayUsuarioAutenticado();
        }

        Object principal = authentication.getPrincipal();

        // FX-fix: detectar principal anonimo (Spring Security usa "anonymousUser"
        // cuando el JWT fue rechazado por el filtro).
        if ("anonymousUser".equals(principal)) {
            throw BusinessError.tokenInvalidoOExpirado();
        }

        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessError.usuarioAutenticadoNoEncontrado(email));
    }

    @Override
    @Transactional
    public void cambiarPassword(CambioPasswordRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

        if (!user.getActivo()) {
            throw BusinessError.usuarioInactivo(user.getEmail());
        }

        if (!passwordEncoder.matches(request.getPasswordAnterior(), user.getPassword())) {
            throw BusinessError.contrasenaAnteriorIncorrecta();
        }

        if (!request.getNuevaPassword().equals(request.getConfirmacionPassword())) {
            throw BusinessError.contrasenasNoCoinciden();
        }

        if (passwordEncoder.matches(request.getNuevaPassword(), user.getPassword())) {
            throw BusinessError.contrasenaIgualActual();
        }

        user.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        userRepository.save(user);
    }
}
