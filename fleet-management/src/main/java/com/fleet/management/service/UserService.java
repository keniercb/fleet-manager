package com.fleet.management.service;

import com.fleet.management.dto.user.UserRequest;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.model.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> findAll(String filter, Pageable pageable);

    UserResponse findById(Long id);

    UserResponse findByEmail(String email);

    UserResponse create(UserRequest request);

    UserResponse createAdminUser(Empresa empresa);

    UserResponse update(Long id, UserRequest request);

    Page<UserResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable);

    void delete(Long id);
}
