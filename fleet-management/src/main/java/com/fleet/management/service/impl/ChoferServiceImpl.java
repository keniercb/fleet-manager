package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.ChoferMapper;
import com.fleet.management.model.CategoriaLicencia;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.ChoferCategoria;
import com.fleet.management.model.Empresa;
import com.fleet.management.repository.CategoriaLicenciaRepository;
import com.fleet.management.repository.ChoferCategoriaRepository;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.service.ChoferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ChoferServiceImpl implements ChoferService {

    private final ChoferRepository choferRepository;
    private final CategoriaLicenciaRepository categoriaLicenciaRepository;
    private final ChoferCategoriaRepository choferCategoriaRepository;
    private final EmpresaRepository empresaRepository;
    private final ChoferMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return choferRepository.findAllByActivoTrue(pageable)
                    .map(mapper::toResponse);
        }
        return choferRepository.findAllByActivoTrueAndNombreOrCarneIdentidad(filter, pageable)
                    .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferResponse findById(Long id) {
        Chofer entity = choferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChoferResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return choferRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                    .map(mapper::toResponse);
        }
        return choferRepository.findByEmpresaIdAndActivoTrueAndNombreOrCarneIdentidad(empresaId, filter, pageable)
                    .map(mapper::toResponse);
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

        return mapper.toResponse(choferRepository.save(entity));
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

        return mapper.toResponse(choferRepository.save(entity));
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
            throw BusinessError.choferCategoriaYaAsignada(request.getCategorias().get(0).getCategoriaLicenciaId()) + " - " + categoria.getDenominacion());
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
                    throw BusinessError.choferYaExisteCarne(request.getCarneIdentidad());
                }
            });
        }
        if (choferRepository.existsByNumeroLicencia(request.getNumeroLicencia())) {
            choferRepository.findByNumeroLicencia(request.getNumeroLicencia()).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw BusinessError.choferYaExisteLicencia(request.getNumeroLicencia());
                }
            });
        }
    }
}