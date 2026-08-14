package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaEmbeddedResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.ChoferCategoria;
import com.fleet.management.model.Empresa;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferCategoriaRepository;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.service.ChoferService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChoferServiceImpl implements ChoferService {

    private final ChoferRepository choferRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;
    private final ChoferCategoriaRepository choferCategoriaRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferResponse> findAll(Pageable pageable) {
        return choferRepository.findAll(pageable)
                .map(this::toResponse);
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

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        Chofer entity = Chofer.builder()
                .empresa(empresa)
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .carneIdentidad(request.getCarneIdentidad())
                .numeroLicencia(request.getNumeroLicencia())
                .fechaNacimiento(request.getFechaNacimiento())
                .categorias(new ArrayList<>())
                .activo(true)
                .build();

        choferRepository.save(entity);

        if (request.getCategorias() != null) {
            for (ChoferRequest.CategoriaConFechaRequest catReq : request.getCategorias()) {
                addCategoriaToChofer(entity, catReq);
            }
        }

        return toResponse(choferRepository.save(entity));
    }

    @Override
    @Transactional
    public ChoferResponse update(Long id, ChoferRequest request) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));

        validateUniqueFields(request, id);

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        entity.setEmpresa(empresa);
        entity.setNombre(request.getNombre());
        entity.setApellidos(request.getApellidos());
        entity.setCarneIdentidad(request.getCarneIdentidad());
        entity.setNumeroLicencia(request.getNumeroLicencia());
        entity.setFechaNacimiento(request.getFechaNacimiento());

        // Reemplazar categorias si se envian
        if (request.getCategorias() != null) {
            entity.getCategorias().clear();
            choferCategoriaRepository.flush();
            for (ChoferRequest.CategoriaConFechaRequest catReq : request.getCategorias()) {
                addCategoriaToChofer(entity, catReq);
            }
        }

        return toResponse(choferRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));
        entity.setActivo(false);
        // Desactivar tambien las categorias del chofer
        entity.getCategorias().forEach(cc -> cc.setActivo(false));
        choferRepository.save(entity);
    }

    private void addCategoriaToChofer(Chofer chofer, ChoferRequest.CategoriaConFechaRequest catReq) {
        CategoriaLicencia categoria = categoriaLicenciaRepository.findById(catReq.getCategoriaLicenciaId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaLicencia", "id", catReq.getCategoriaLicenciaId()));

        if (choferCategoriaRepository.existsByChoferIdAndCategoriaLicenciaId(chofer.getId(), categoria.getId())) {
            throw new BusinessException("El chofer ya tiene asignada la categoria de licencia: "
                    + categoria.getCodigo() + " - " + categoria.getDenominacion());
        }

        ChoferCategoria cc = ChoferCategoria.builder()
                .chofer(chofer)
                .categoriaLicencia(categoria)
                .fechaEmision(catReq.getFechaEmision())
                .activo(true)
                .build();
        chofer.getCategorias().add(cc);
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

    private EmpresaResponse toEmpresaResponse(Empresa empresa) {
        return EmpresaResponse.builder()
                .id(empresa.getId())
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .email(empresa.getEmail())
                .activo(empresa.getActivo())
                .fechaCreacion(empresa.getFechaCreacion())
                .fechaActualizacion(empresa.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(empresa.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(empresa.getModificadoPor()))
                .build();
    }

    private ChoferResponse toResponse(Chofer entity) {
        List<ChoferCategoriaEmbeddedResponse> categoriasResponse = entity.getCategorias().stream()
                .map(cc -> {
                    CategoriaLicencia cat = cc.getCategoriaLicencia();
                    CategoriaLicenciaResponse catResp = CategoriaLicenciaResponse.builder()
                            .id(cat.getId())
                            .codigo(cat.getCodigo())
                            .denominacion(cat.getDenominacion())
                            .descripcion(cat.getDescripcion())
                            .activo(cat.getActivo())
                            .fechaCreacion(cat.getFechaCreacion())
                            .fechaActualizacion(cat.getFechaActualizacion())
                            .creadoPor(AuditMapper.toAuditResponse(cat.getCreadoPor()))
                            .modificadoPor(AuditMapper.toAuditResponse(cat.getModificadoPor()))
                            .build();
                    return ChoferCategoriaEmbeddedResponse.builder()
                            .id(cc.getId())
                            .categoriaLicencia(catResp)
                            .fechaEmision(cc.getFechaEmision())
                            .activo(cc.getActivo())
                            .fechaCreacion(cc.getFechaCreacion())
                            .fechaActualizacion(cc.getFechaActualizacion())
                            .creadoPor(AuditMapper.toAuditResponse(cc.getCreadoPor()))
                            .modificadoPor(AuditMapper.toAuditResponse(cc.getModificadoPor()))
                            .build();
                })
                .toList();

        return ChoferResponse.builder()
                .id(entity.getId())
                .empresa(toEmpresaResponse(entity.getEmpresa()))
                .nombre(entity.getNombre())
                .apellidos(entity.getApellidos())
                .carneIdentidad(entity.getCarneIdentidad())
                .numeroLicencia(entity.getNumeroLicencia())
                .fechaNacimiento(entity.getFechaNacimiento())
                .categorias(categoriasResponse)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}