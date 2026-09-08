package com.fleet.management.service.impl;

import com.fleet.management.dto.role.RoleRequest;
import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.RoleMapper;
import com.fleet.management.model.Permission;
import com.fleet.management.model.Role;
import com.fleet.management.repository.PermissionRepository;
import com.fleet.management.repository.RoleRepository;
import com.fleet.management.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> findAll(Pageable pageable) {
        return roleRepository.findAllByActivoTrue(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        Role entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findByName(String name) {
        Role entity = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> findByPermissionId(Long permissionId, Pageable pageable) {
        return roleRepository.findByPermissionId(permissionId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw BusinessError.rolYaExisteNombre(request.getName());
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());

        Role entity = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(permissions)
                .activo(true)
                .build();
        return mapper.toResponse(roleRepository.save(entity));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        roleRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw BusinessError.rolYaExisteNombre(request.getName());
            }
        });

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = resolvePermissions(request.getPermissionIds());
            entity.setPermissions(permissions);
        }

        return mapper.toResponse(roleRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        entity.setActivo(false);
        roleRepository.save(entity);
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        return permissionIds.stream()
                .map(pid -> permissionRepository.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", pid)))
                .collect(java.util.stream.Collectors.toSet());
    }
}
