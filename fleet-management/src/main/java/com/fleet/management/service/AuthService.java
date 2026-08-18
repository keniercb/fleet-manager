package com.fleet.management.service;

import com.fleet.management.dto.auth.AuthResponseDto;
import com.fleet.management.dto.auth.CambioPasswordRequest;
import com.fleet.management.dto.auth.LoginRequestDto;
import com.fleet.management.model.User;

public interface AuthService {

    AuthResponseDto login(LoginRequestDto request);

    User getCurrentUser();

    void cambiarPassword(CambioPasswordRequest request);
}