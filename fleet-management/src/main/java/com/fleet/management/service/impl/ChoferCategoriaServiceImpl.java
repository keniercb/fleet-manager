package com.fleet.management.service.impl;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChoferCategoriaServiceImpl implements ChoferCategoriaService {

    private final ChoferCategoriaRepository repository;
    private final ChoferRepository choferRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChoferCategoriaResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferCategoriaResponse findById(Long id) {
        ChoferCategoria entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChoferCategoria", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoferCategoriaResponse> findByChoferId(Long choferId) {
        return repository.findByChoferId(choferId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoferCategoriaResponse> findByCategoriaLicenciaId(Long categoriaLicenciaId) {
        return repository.findByCategoriaLicenciaId(categoriaLicenciaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChoferCategoriaResponse create(ChoferCategoriaRequest request) {
        Chofer chofer = choferRepository.findById(request.getChoferId())
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));

        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(request.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", request.getCategoriaLicenciaId()));

        if (repository.existsByChoferIdAndCategoriaLicenciaId(request.getChoferId(), request.getCategoriaLicenciaId())) {
            throw new BusinessException("El chofer ya tiene asignada la categoria de licencia con id: "
                    + request.getCategoriaLicenciaId());
        }

        ChoferCategoria entity = ChoferCategoria.builder()
                .chofer(chofer)
                .categoriaLicencia(categoria)
                .fechaEmision(request.getFechaEmision())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
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
                throw new BusinessException("El chofer ya tiene asignada la categoria de licencia con id: "
                        + request.getCategoriaLicenciaId());
            }
        }

        entity.setChofer(chofer);
        entity.setCategoriaLicencia(categoria);
        entity.setFechaEmision(request.getFechaEmision());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ChoferCategoria entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ChoferCategoria", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private ChoferCategoriaResponse toResponse(ChoferCategoria entity) {
        Chofer chofer = entity.getChofer();
        ChoferResponse choferResponse = ChoferResponse.builder()
                .id(chofer.getId())
                .nombre(chofer.getNombre())
                .apellidos(chofer.getApellidos())
                .carneIdentidad(chofer.getCarneIdentidad())
                .numeroLicencia(chofer.getNumeroLicencia())
                .fechaNacimiento(chofer.getFechaNacimiento())
                .activo(chofer.getActivo())
                .fechaCreacion(chofer.getFechaCreacion())
                .fechaActualizacion(chofer.getFechaActualizacion())
                .build();

        CategoriaLicencia cat = entity.getCategoriaLicencia();
        CategoriaLicenciaResponse categoriaResponse = CategoriaLicenciaResponse.builder()
                .id(cat.getId())
                .codigo(cat.getCodigo())
                .denominacion(cat.getDenominacion())
                .descripcion(cat.getDescripcion())
                .activo(cat.getActivo())
                .fechaCreacion(cat.getFechaCreacion())
                .fechaActualizacion(cat.getFechaActualizacion())
                .build();

        return ChoferCategoriaResponse.builder()
                .id(entity.getId())
                .chofer(choferResponse)
                .categoriaLicencia(categoriaResponse)
                .fechaEmision(entity.getFechaEmision())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}