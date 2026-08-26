package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.municipio.MunicipioRequest;
import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Municipio;
import com.fleet.management.model.Provincia;
import com.fleet.management.repository.MunicipioRepository;
import com.fleet.management.repository.ProvinciaRepository;
import com.fleet.management.service.MunicipioService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MunicipioServiceImpl implements MunicipioService {

    private final MunicipioRepository repository;
    private final ProvinciaRepository provinciaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<MunicipioResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MunicipioResponse> findByProvinciaId(Long provinciaId, Pageable pageable) {
        return repository.findByProvinciaIdAndActivoTrue(provinciaId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipioResponse> listByProvinciaId(Long provinciaId) {
        return repository.findByProvinciaIdAndActivoTrueOrderByIdAsc(provinciaId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MunicipioResponse findById(Long id) {
        Municipio entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public MunicipioResponse create(MunicipioRequest request) {
        Provincia provincia = provinciaRepository.findById(request.getProvinciaId())
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));

        if (repository.existsByProvinciaIdAndCodigo(request.getProvinciaId(), request.getCodigo())) {
            throw new BusinessException("Ya existe un municipio con el codigo " + request.getCodigo()
                    + " en la provincia " + provincia.getNombre());
        }

        Municipio entity = Municipio.builder()
                .provincia(provincia)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public MunicipioResponse update(Long id, MunicipioRequest request) {
        Municipio entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", id));

        Provincia provincia = provinciaRepository.findById(request.getProvinciaId())
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));

        boolean codigoCambiado = !entity.getCodigo().equals(request.getCodigo())
                || !entity.getProvincia().getId().equals(request.getProvinciaId());
        if (codigoCambiado && repository.existsByProvinciaIdAndCodigo(request.getProvinciaId(), request.getCodigo())) {
            throw new BusinessException("Ya existe un municipio con el codigo " + request.getCodigo()
                    + " en la provincia " + provincia.getNombre());
        }

        entity.setProvincia(provincia);
        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Municipio entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private ProvinciaResponse toProvinciaResponse(Provincia provincia) {
        return ProvinciaResponse.builder()
                .id(provincia.getId())
                .codigo(provincia.getCodigo())
                .nombre(provincia.getNombre())
                .activo(provincia.getActivo())
                .fechaCreacion(provincia.getFechaCreacion())
                .fechaActualizacion(provincia.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(provincia.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(provincia.getModificadoPor()))
                .build();
    }

    private MunicipioResponse toResponse(Municipio entity) {
        return MunicipioResponse.builder()
                .id(entity.getId())
                .provincia(toProvinciaResponse(entity.getProvincia()))
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
