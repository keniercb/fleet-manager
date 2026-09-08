package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.TipoVehiculoMapper;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.service.TipoVehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TipoVehiculoServiceImpl implements TipoVehiculoService {

    private final TipoVehiculoRepository repository;
    private final TipoVehiculoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TipoVehiculoResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoVehiculoResponse findById(Long id) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public TipoVehiculoResponse create(TipoVehiculoRequest request) {
        if (repository.existsByNombre(request.getNombre())) {
            throw BusinessError.tipoVehiculoYaExisteNombre(request.getNombre());
        }
        TipoVehiculo entity = TipoVehiculo.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TipoVehiculoResponse update(Long id, TipoVehiculoRequest request) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));

        if (!entity.getNombre().equals(request.getNombre()) && repository.existsByNombre(request.getNombre())) {
            throw BusinessError.tipoVehiculoYaExisteNombre(request.getNombre());
        }

        entity.setNombre(request.getNombre());
        entity.setDescripcion(request.getDescripcion());
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TipoVehiculo entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}
