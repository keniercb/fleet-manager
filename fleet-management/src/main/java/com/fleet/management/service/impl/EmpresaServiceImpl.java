package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.service.EmpresaService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<EmpresaResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponse findById(Long id) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponse findByCodigo(String codigo) {
        Empresa entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "codigo", codigo));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public EmpresaResponse create(EmpresaRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una empresa con el codigo: " + request.getCodigo());
        }
        Empresa entity = Empresa.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public EmpresaResponse update(Long id, EmpresaRequest request) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        if (!entity.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una empresa con el codigo: " + request.getCodigo());
        }

        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        entity.setDireccion(request.getDireccion());
        entity.setTelefono(request.getTelefono());
        entity.setEmail(request.getEmail());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private EmpresaResponse toResponse(Empresa entity) {
        return EmpresaResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nombre(entity.getNombre())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .email(entity.getEmail())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}