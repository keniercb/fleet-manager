package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaRequest;
import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.CategoriaLicenciaMapper;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.service.CategoriaLicenciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaLicenciaServiceImpl implements CategoriaLicenciaService {

    private final CategoriaLicenciaRepository repository;
    private final CategoriaLicenciaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaLicenciaResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaLicenciaResponse findById(Long id) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaLicenciaResponse findByCodigo(String codigo) {
        CategoriaLicencia entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "codigo", codigo));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public CategoriaLicenciaResponse create(CategoriaLicenciaRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw BusinessError.categoriaLicenciaYaExisteCodigo(request.getCodigo());
        }
        CategoriaLicencia entity = CategoriaLicencia.builder()
                .codigo(request.getCodigo().toUpperCase())
                .denominacion(request.getDenominacion())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CategoriaLicenciaResponse update(Long id, CategoriaLicenciaRequest request) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));

        String codigoUpper = request.getCodigo().toUpperCase();
        if (!entity.getCodigo().equals(codigoUpper) && repository.existsByCodigo(codigoUpper)) {
            throw BusinessError.categoriaLicenciaYaExisteCodigo(codigoUpper);
        }

        entity.setCodigo(codigoUpper);
        entity.setDenominacion(request.getDenominacion());
        entity.setDescripcion(request.getDescripcion());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CategoriaLicencia entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}