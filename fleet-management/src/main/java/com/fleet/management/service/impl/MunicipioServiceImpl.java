package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.municipio.MunicipioRequest;
import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.MunicipioMapper;
import com.fleet.management.model.Municipio;
import com.fleet.management.model.Provincia;
import com.fleet.management.repository.MunicipioRepository;
import com.fleet.management.repository.ProvinciaRepository;
import com.fleet.management.service.MunicipioService;
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
    private final MunicipioMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<MunicipioResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MunicipioResponse> findByProvinciaId(Long provinciaId, Pageable pageable) {
        return repository.findByProvinciaIdAndActivoTrue(provinciaId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MunicipioResponse> listByProvinciaId(Long provinciaId) {
        return repository.findByProvinciaIdAndActivoTrueOrderByIdAsc(provinciaId).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MunicipioResponse findById(Long id) {
        Municipio entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public MunicipioResponse create(MunicipioRequest request) {
        Provincia provincia = provinciaRepository.findById(request.getProvinciaId())
                .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));

        if (repository.existsByProvinciaIdAndCodigo(request.getProvinciaId(), request.getCodigo())) {
            throw BusinessError.municipioYaExisteCodigo(request.getCodigo());
        }

        Municipio entity = Municipio.builder()
                .provincia(provincia)
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .activo(true)
                .build();
        return mapper.toResponse(repository.save(entity));
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
            throw BusinessError.municipioYaExisteCodigo(request.getCodigo());
        }

        entity.setProvincia(provincia);
        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Municipio entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}