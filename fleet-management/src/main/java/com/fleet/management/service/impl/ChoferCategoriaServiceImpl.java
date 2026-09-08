package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.ChoferCategoriaMapper;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.ChoferCategoria;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferCategoriaRepository;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.service.ChoferCategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChoferCategoriaServiceImpl implements ChoferCategoriaService {

    private final ChoferCategoriaRepository repository;
    private final ChoferRepository choferRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;
    private final ChoferCategoriaMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferCategoriaResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferCategoriaResponse findById(Long id) {
        ChoferCategoria entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChoferCategoria", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferCategoriaResponse> findByChoferId(Long choferId, Pageable pageable) {
        return repository.findByChoferId(choferId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferCategoriaResponse> findByCategoriaLicenciaId(Long categoriaLicenciaId, Pageable pageable) {
        return repository.findByCategoriaLicenciaId(categoriaLicenciaId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public ChoferCategoriaResponse create(ChoferCategoriaRequest request) {
        Chofer chofer = choferRepository.findById(request.getChoferId())
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));

        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(request.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", request.getCategoriaLicenciaId()));

        if (repository.existsByChoferIdAndCategoriaLicenciaId(request.getChoferId(), request.getCategoriaLicenciaId())) {
            throw BusinessError.choferCategoriaYaAsignada(request.getCategoriaLicenciaId());
        }

        ChoferCategoria entity = ChoferCategoria.builder()
                .chofer(chofer)
                .categoriaLicencia(categoria)
                .fechaEmision(request.getFechaEmision())
                .activo(true)
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ChoferCategoriaResponse update(Long id, ChoferCategoriaRequest request) {
        ChoferCategoria entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChoferCategoria", "id", id));

        Chofer chofer = choferRepository.findById(request.getChoferId())
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));

        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(request.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", request.getCategoriaLicenciaId()));

        // Validar unicidad solo si cambian el chofer o la categoria
        if (!entity.getChofer().getId().equals(request.getChoferId())
                || !entity.getCategoriaLicencia().getId().equals(request.getCategoriaLicenciaId())) {
            if (repository.existsByChoferIdAndCategoriaLicenciaId(request.getChoferId(), request.getCategoriaLicenciaId())) {
                throw BusinessError.choferCategoriaYaAsignada(request.getCategoriaLicenciaId());
            }
        }

        entity.setChofer(chofer);
        entity.setCategoriaLicencia(categoria);
        entity.setFechaEmision(request.getFechaEmision());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ChoferCategoria entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChoferCategoria", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}