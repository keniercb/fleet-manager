package com.fleet.management.service.impl;

import com.fleet.management.dto.user.UserRequest;
import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.User;
import com.fleet.management.repository.UserRepository;
import com.fleet.management.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
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
        validateUniqueEmail(request.getEmail(), null);

        User entity = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .activo(true)
                .build();
        return toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        validateUniqueEmail(request.getEmail(), id);

        entity.setEmail(request.getEmail());
        entity.setPassword(request.getPassword());
        return toResponse(userRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        entity.setActivo(false);
        userRepository.save(entity);
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        if (userRepository.existsByEmail(email)) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw new BusinessException("Ya existe un usuario con el email: " + email);
                }
            });
        }
    }

    private UserResponse toResponse(User entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}
