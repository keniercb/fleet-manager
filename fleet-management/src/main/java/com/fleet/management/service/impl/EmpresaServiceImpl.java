package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Municipio;
import com.fleet.management.model.Provincia;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.MunicipioRepository;
import com.fleet.management.repository.ProvinciaRepository;
import com.fleet.management.service.EmpresaService;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.service.UserService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository repository;
    private final ProvinciaRepository provinciaRepository;
    private final MunicipioRepository municipioRepository;
    private final SubscriptionService subscriptionService;
    private final UserService userService;

    private static final String EMPRESA_ADMIN_CODIGO = "EMP-ADMIN";

    @Override
    @Transactional(readOnly = true)
    public Page<EmpresaResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return repository.findAllByActivoTrue(pageable).map(this::toResponse);
        }
        return repository.findAllByActivoTrueAndNombreContainingIgnoreCase(filter, pageable).map(this::toResponse);
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

        Provincia provincia = null;
        Municipio municipio = null;
        if (request.getProvinciaId() != null) {
            provincia = provinciaRepository.findById(request.getProvinciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));
        }
        if (request.getMunicipioId() != null) {
            municipio = municipioRepository.findById(request.getMunicipioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", request.getMunicipioId()));
            if (provincia != null && !municipio.getProvincia().getId().equals(provincia.getId())) {
                throw new BusinessException("El municipio no pertenece a la provincia seleccionada");
            }
        }

        Empresa entity = Empresa.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .provincia(provincia)
                .municipio(municipio)
                .activo(true)
                .build();
        entity = repository.save(entity);

        subscriptionService.createTrialSubscription(entity);
        userService.createAdminUser(entity);

        return toResponse(entity);
    }

    @Override
    @Transactional
    public EmpresaResponse update(Long id, EmpresaRequest request) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        validarEmpresaAdmin(entity);

        if (!entity.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw new BusinessException("Ya existe una empresa con el codigo: " + request.getCodigo());
        }

        Provincia provincia = null;
        Municipio municipio = null;
        if (request.getProvinciaId() != null) {
            provincia = provinciaRepository.findById(request.getProvinciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));
        }
        if (request.getMunicipioId() != null) {
            municipio = municipioRepository.findById(request.getMunicipioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", request.getMunicipioId()));
            if (provincia != null && !municipio.getProvincia().getId().equals(provincia.getId())) {
                throw new BusinessException("El municipio no pertenece a la provincia seleccionada");
            }
        }

        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        entity.setDireccion(request.getDireccion());
        entity.setTelefono(request.getTelefono());
        entity.setEmail(request.getEmail());
        entity.setProvincia(provincia);
        entity.setMunicipio(municipio);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        validarEmpresaAdmin(entity);

        entity.setActivo(false);
        repository.save(entity);
    }

    private void validarEmpresaAdmin(Empresa entity) {
        if (EMPRESA_ADMIN_CODIGO.equals(entity.getCodigo())) {
            throw new BusinessException("La empresa de administracion no puede ser modificada ni eliminada");
        }
    }

    private EmpresaResponse toResponse(Empresa entity) {
        ProvinciaResponse provinciaResp = null;
        if (entity.getProvincia() != null) {
            Provincia p = entity.getProvincia();
            provinciaResp = ProvinciaResponse.builder()
                    .id(p.getId())
                    .codigo(p.getCodigo())
                    .nombre(p.getNombre())
                    .build();
        }

        MunicipioResponse municipioResp = null;
        if (entity.getMunicipio() != null) {
            Municipio m = entity.getMunicipio();
            municipioResp = MunicipioResponse.builder()
                    .id(m.getId())
                    .codigo(m.getCodigo())
                    .nombre(m.getNombre())
                    .provincia(provinciaResp)
                    .build();
        }

        return EmpresaResponse.builder()
                .id(entity.getId())
                .codigo(entity.getCodigo())
                .nombre(entity.getNombre())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .email(entity.getEmail())
                .provincia(provinciaResp)
                .municipio(municipioResp)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}