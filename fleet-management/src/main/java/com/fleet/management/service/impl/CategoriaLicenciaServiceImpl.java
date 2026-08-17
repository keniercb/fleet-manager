package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaRequest;
import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.service.CategoriaLicenciaService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaLicenciaServiceImpl implements CategoriaLicenciaService {

    private final CategoriaLicenciaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaLicenciaResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaLicenciaResponse findById(Long id) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaLicenciaResponse findByCodigo(String codigo) {
        CategoriaLicencia entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "codigo", codigo));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public CategoriaLicenciaResponse create(CategoriaLicenciaRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una categoria de licencia con el codigo: " + request.getCodigo());
        }
        CategoriaLicencia entity = CategoriaLicencia.builder()
                .codigo(request.getCodigo().toUpperCase())
                .denominacion(request.getDenominacion())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CategoriaLicenciaResponse update(Long id, CategoriaLicenciaRequest request) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));

        String codigoUpper = request.getCodigo().toUpperCase();
        if (!entity.getCodigo().equals(codigoUpper) && repository.existsByCodigo(codigoUpper)) {
            throw new BusinessException("Ya existe una categoria de licencia con el codigo: " + codigoUpper);
        }

        entity.setCodigo(codigoUpper);
        entity.setDenominacion(request.getDenominacion());
        entity.setDescripcion(request.getDescripcion());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private CategoriaLicenciaResponse toResponse(CategoriaLicencia entity) {
        return CategoriaLicenciaResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .denominacion(entity.getDenominacion())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}