package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.TipoCombustibleMapper;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.service.TipoCombustibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TipoCombustibleServiceImpl implements TipoCombustibleService {

    private final TipoCombustibleRepository repository;
    private final TipoCombustibleMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TipoCombustibleResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCombustibleResponse findById(Long id) {
        TipoCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCombustibleResponse findByCodigo(String codigo) {
        TipoCombustible entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "codigo", codigo));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public TipoCombustibleResponse create(TipoCombustibleRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe un tipo de combustible con el codigo: " + request.getCodigo());
        }
        TipoCombustible entity = TipoCombustible.builder()
                .codigo(request.getCodigo())
                .denominacion(request.getDenominacion())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TipoCombustibleResponse update(Long id, TipoCombustibleRequest request) {
        TipoCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", id));

        if (!entity.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe un tipo de combustible con el codigo: " + request.getCodigo());
        }

        entity.setCodigo(request.getCodigo());
        entity.setDenominacion(request.getDenominacion());
        entity.setDescripcion(request.getDescripcion());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TipoCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}
