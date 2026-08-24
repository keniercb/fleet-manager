package com.fleet.management.service.impl;

import com.fleet.management.dto.feature.FeatureRequest;
import com.fleet.management.dto.feature.FeatureResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Feature;
import com.fleet.management.repository.FeatureRepository;
import com.fleet.management.service.FeatureService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeatureServiceImpl implements FeatureService {

    private final FeatureRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<FeatureResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureResponse findById(Long id) {
        Feature entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public FeatureResponse create(FeatureRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe un feature con el nombre: " + request.getName());
        }
        Feature entity = Feature.builder()
                .name(request.getName())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public FeatureResponse update(Long id, FeatureRequest request) {
        Feature entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", "id", id));

        if (!entity.getName().equals(request.getName()) && repository.existsByName(request.getName())) {
            throw new BusinessException("Ya existe un feature con el nombre: " + request.getName());
        }

        entity.setName(request.getName());
        entity.setDescripcion(request.getDescripcion());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Feature entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private FeatureResponse toResponse(Feature entity) {
        return FeatureResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}
