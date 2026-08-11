package com.fleet.management.service.impl;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.service.TipoCombustibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoCombustibleServiceImpl implements TipoCombustibleService {

    private final TipoCombustibleRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoCombustibleResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCombustibleResponse findById(Long id) {
        TipoCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoCombustibleResponse findByCodigo(String codigo) {
        TipoCombustible entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "codigo", codigo));
        return toResponse(entity);
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
        return toResponse(repository.save(entity));
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
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TipoCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private TipoCombustibleResponse toResponse(TipoCombustible entity) {
        return TipoCombustibleResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .denominacion(entity.getDenominacion())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}