package com.fleet.management.service;

import com.fleet.management.dto.user.UserRequest;
import com.fleet.management.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> findAll(String filter, Pageable pageable);

    UserResponse findById(Long id);

    UserResponse findByEmail(String email);

    UserResponse create(UserRequest request);

    UserResponse update(Long id, UserRequest request);

    Page<UserResponse> findByEmpresaId(Long empresaId, Pageable pageable);

    void delete(Long id);
}
