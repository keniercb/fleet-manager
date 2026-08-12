package com.fleet.management.service.impl;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.service.TipoVehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoVehiculoServiceImpl implements TipoVehiculoService {

    private final TipoVehiculoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<TipoVehiculoResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TipoVehiculoResponse findById(Long id) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public TipoVehiculoResponse create(TipoVehiculoRequest request) {
        if (repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un tipo de vehiculo con el nombre: " + request.getNombre());
        }
        TipoVehiculo entity = TipoVehiculo.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TipoVehiculoResponse update(Long id, TipoVehiculoRequest request) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));

        if (!entity.getNombre().equals(request.getNombre()) && repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un tipo de vehiculo con el nombre: " + request.getNombre());
        }

        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private TipoVehiculoResponse toResponse(TipoVehiculo entity) {
        return TipoVehiculoResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}
