package com.fleet.management.service.impl;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.service.ChoferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChoferServiceImpl implements ChoferService {

    private final ChoferRepository choferRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChoferResponse> findAll() {
        return choferRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferResponse findById(Long id) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public ChoferResponse create(ChoferRequest request) {
        validateUniqueFields(request, null);

        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(request.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", request.getCategoriaLicenciaId()));

        Chofer entity = Chofer.builder()
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .carneIdentidad(request.getCarneIdentidad())
                .numeroLicencia(request.getNumeroLicencia())
                .fechaNacimiento(request.getFechaNacimiento())
                .categoriaLicencia(categoria)
                .activo(true)
                .build();
        return toResponse(choferRepository.save(entity));
    }

    @Override
    @Transactional
    public ChoferResponse update(Long id, ChoferRequest request) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));

        validateUniqueFields(request, id);

        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(request.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", request.getCategoriaLicenciaId()));

        entity.setNombre(request.getNombre());
        entity.setApellidos(request.getApellidos());
        entity.setCarneIdentidad(request.getCarneIdentidad());
        entity.setNumeroLicencia(request.getNumeroLicencia());
        entity.setFechaNacimiento(request.getFechaNacimiento());
        entity.setCategoriaLicencia(categoria);
        return toResponse(choferRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));
        entity.setActivo(false);
        choferRepository.save(entity);
    }

    private void validateUniqueFields(ChoferRequest request, Long excludeId) {
        if (choferRepository.existsByCarneIdentidad(request.getCarneIdentidad())) {
            choferRepository.findByCarneIdentidad(request.getCarneIdentidad()).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw new BusinessException("Ya existe un chofer con el carne de identidad: " + request.getCarneIdentidad());
                }
            });
        }
        if (choferRepository.existsByNumeroLicencia(request.getNumeroLicencia())) {
            choferRepository.findByNumeroLicencia(request.getNumeroLicencia()).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw new BusinessException("Ya existe un chofer con el numero de licencia: " + request.getNumeroLicencia());
                }
            });
        }
    }

    private ChoferResponse toResponse(Chofer entity) {
        CategoriaLicencia categoria = entity.getCategoriaLicencia();
        CategoriaLicenciaResponse categoriaResponse = (categoria != null)
                ? CategoriaLicenciaResponse.builder()
                    .id(categoria.getId())
                    .codigo(categoria.getCodigo())
                    .denominacion(categoria.getDenominacion())
                    .descripcion(categoria.getDescripcion())
                    .activo(categoria.getActivo())
                    .fechaCreacion(categoria.getFechaCreacion())
                    .fechaActualizacion(categoria.getFechaActualizacion())
                    .build()
                : null;

        return ChoferResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .apellidos(entity.getApellidos())
                .carneIdentidad(entity.getCarneIdentidad())
                .numeroLicencia(entity.getNumeroLicencia())
                .fechaNacimiento(entity.getFechaNacimiento())
                .categoriaLicencia(categoriaResponse)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}