package com.fleet.management.service.impl;

import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.dto.user.UserRequest;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Role;
import com.fleet.management.model.User;
import com.fleet.management.repository.RoleRepository;
import com.fleet.management.repository.UserRepository;
import com.fleet.management.service.UserService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return userRepository.findAllByActivoTrue(pageable)
                    .map(this::toResponse);
        }
        return userRepository.findAllByActivoTrueAndEmailContainingIgnoreCase(filter, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        User entity = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe un usuario con el email: " + request.getEmail());
        }

        Set<Role> roles = resolveRoles(request.getRoleIds());

        User entity = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .activo(true)
                .build();
        return toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        entity.setPassword(passwordEncoder.encode(request.getPassword()));

        if (request.getRoleIds() != null) {
            Set<Role> roles = resolveRoles(request.getRoleIds());
            entity.setRoles(roles);
        }

        return toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findByEmpresaId(Long empresaId, Pageable pageable) {
        return userRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        entity.setActivo(false);
        userRepository.save(entity);
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        return roleIds.stream()
                .map(roleId -> roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId)))
                .collect(Collectors.toSet());
    }

    private PermissionResponse toPermissionResponse(com.fleet.management.model.Permission permission) {
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

    private RoleResponse toRoleResponse(Role role) {
        Set<PermissionResponse> permissionResponses = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(permissionResponses)
                .activo(role.getActivo())
                .fechaCreacion(role.getFechaCreacion())
                .fechaActualizacion(role.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(role.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(role.getModificadoPor()))
                .build();
    }

    private UserResponse toResponse(User entity) {
        Set<RoleResponse> roleResponses = entity.getRoles().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .empresa(entity.getEmpresa() != null ? toEmpresaResponse(entity.getEmpresa()) : null)
                .roles(roleResponses)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }

    private EmpresaResponse toEmpresaResponse(com.fleet.management.model.Empresa empresa) {
        return EmpresaResponse.builder()
                .id(empresa.getId())
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .email(empresa.getEmail())
                .activo(empresa.getActivo())
                .build();
    }
}
