package com.fleet.management.service;

import com.fleet.management.dto.role.RoleRequest;
import com.fleet.management.dto.role.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    Page<RoleResponse> findAll(Pageable pageable);

    RoleResponse findById(Long id);

    RoleResponse findByName(String name);

    Page<RoleResponse> findByPermissionId(Long permissionId, Pageable pageable);

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long id, RoleRequest request);

    void delete(Long id);
}