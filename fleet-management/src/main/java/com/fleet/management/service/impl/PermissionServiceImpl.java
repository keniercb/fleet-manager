package com.fleet.management.service.impl;

import com.fleet.management.dto.permission.PermissionRequest;
import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.PermissionMapper;
import com.fleet.management.model.Permission;
import com.fleet.management.repository.PermissionRepository;
import com.fleet.management.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionResponse> findAll(Pageable pageable) {
        return permissionRepository.findAllByActivoTrue(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse findById(Long id) {
        Permission entity = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse findByName(String name) {
        Permission entity = permissionRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "name", name));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe un permiso con el nombre: " + request.getName());
        }

        Permission entity = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .activo(true)
                .build();
        return mapper.toResponse(permissionRepository.save(entity));
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission entity = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        permissionRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Ya existe un permiso con el nombre: " + request.getName());
            }
        });

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        return mapper.toResponse(permissionRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission entity = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        entity.setActivo(false);
        permissionRepository.save(entity);
    }
}
