package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.marca.MarcaRequest;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Marca;
import com.fleet.management.repository.MarcaRepository;
import com.fleet.management.service.MarcaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarcaServiceImpl implements MarcaService {

    private final MarcaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<MarcaResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MarcaResponse findById(Long id) {
        Marca entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public MarcaResponse create(MarcaRequest request) {
        if (repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una marca con el nombre: " + request.getNombre());
        }
        Marca entity = Marca.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .paisOrigen(request.getPaisOrigen())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public MarcaResponse update(Long id, MarcaRequest request) {
        Marca entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", id));

        if (!entity.getNombre().equals(request.getNombre()) && repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe una marca con el nombre: " + request.getNombre());
        }

        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
        entity.setPaisOrigen(request.getPaisOrigen());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Marca entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private MarcaResponse toResponse(Marca entity) {
        return MarcaResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .paisOrigen(entity.getPaisOrigen())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}