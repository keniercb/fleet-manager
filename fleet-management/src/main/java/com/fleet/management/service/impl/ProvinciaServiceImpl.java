package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.provincia.ProvinciaRequest;
import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Provincia;
import com.fleet.management.repository.ProvinciaRepository;
import com.fleet.management.service.ProvinciaService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProvinciaServiceImpl implements ProvinciaService {

    private final ProvinciaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProvinciaResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProvinciaResponse findById(Long id) {
        Provincia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public ProvinciaResponse create(ProvinciaRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una provincia con el codigo: " + request.getCodigo());
        }
        Provincia entity = Provincia.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ProvinciaResponse update(Long id, ProvinciaRequest request) {
        Provincia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", id));

        if (!entity.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una provincia con el codigo: " + request.getCodigo());
        }

        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Provincia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private ProvinciaResponse toResponse(Provincia entity) {
        return ProvinciaResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nombre(entity.getNombre())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}