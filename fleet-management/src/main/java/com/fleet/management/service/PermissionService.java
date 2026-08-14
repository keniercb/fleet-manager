package com.fleet.management.service;

import com.fleet.management.dto.permission.PermissionRequest;
import com.fleet.management.dto.permission.PermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

    Page<PermissionResponse> findAll(Pageable pageable);

    PermissionResponse findById(Long id);

    PermissionResponse findByName(String name);

    PermissionResponse create(PermissionRequest request);

    PermissionResponse update(Long id, PermissionRequest request);

    void delete(Long id);
}