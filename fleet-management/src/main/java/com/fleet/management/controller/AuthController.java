package com.fleet.management.controller;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.model.User;
import com.fleet.management.service.AuthService;
import com.fleet.management.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

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
        return ResponseEntity.ok(userService.findById(user.getId()));
    }
}
