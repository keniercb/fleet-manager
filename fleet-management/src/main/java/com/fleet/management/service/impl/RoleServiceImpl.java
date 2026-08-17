package com.fleet.management.service.impl;

import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.dto.role.RoleRequest;
import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Permission;
import com.fleet.management.model.Role;
import com.fleet.management.repository.PermissionRepository;
import com.fleet.management.repository.RoleRepository;
import com.fleet.management.service.RoleService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> findAll(Pageable pageable) {
        return roleRepository.findAllByActivoTrue(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findById(Long id) {
        Role entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse findByName(String name) {
        Role entity = roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", name));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> findByPermissionId(Long permissionId, Pageable pageable) {
        return roleRepository.findByPermissionId(permissionId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe un rol con el nombre: " + request.getName());
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionIds());

        Role entity = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(permissions)
                .activo(true)
                .build();
        return toResponse(roleRepository.save(entity));
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        roleRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Ya existe un rol con el nombre: " + request.getName());
            }
        });

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());

        if (request.getPermissionIds() != null) {
            Set<Permission> permissions = resolvePermissions(request.getPermissionIds());
            entity.setPermissions(permissions);
        }

        return toResponse(roleRepository.save(entity));
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
                .collect(Collectors.toSet());
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .activo(permission.getActivo())
                .fechaCreacion(permission.getFechaCreacion())
                .fechaActualizacion(permission.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(permission.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(permission.getModificadoPor()))
                .build();
    }

    private RoleResponse toResponse(Role entity) {
        Set<PermissionResponse> permissionResponses = entity.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .permissions(permissionResponses)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}
