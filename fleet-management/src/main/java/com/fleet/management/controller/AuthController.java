package com.fleet.management.controller;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.model.User;
import com.fleet.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = authService.getCurrentUser();
        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .activo(user.getActivo())
                .fechaCreacion(user.getFechaCreacion())
                .fechaActualizacion(user.getFechaActualizacion())
                .build();
        return ResponseEntity.ok(response);
    }
}
