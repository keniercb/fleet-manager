package com.fleet.management.service.impl;

import com.fleet.management.dto.feature.FeatureResponse;
import com.fleet.management.dto.plan.PlanRequest;
import com.fleet.management.dto.plan.PlanResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Feature;
import com.fleet.management.model.Plan;
import com.fleet.management.repository.FeatureRepository;
import com.fleet.management.repository.PlanRepository;
import com.fleet.management.service.PlanService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository repository;
    private final FeatureRepository featureRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PlanResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse findById(Long id) {
        Plan entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public PlanResponse create(PlanRequest request) {
        if (repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un plan con el nombre: " + request.getNombre());
        }

        Set<Feature> features = resolveFeatures(request.getFeatureIds());

        Plan entity = Plan.builder()
                .nombre(request.getNombre())
                .precioMensual(request.getPrecioMensual())
                .maxUsuarios(request.getMaxUsuarios())
                .maxVehiculos(request.getMaxVehiculos())
                .duracion(request.getDuracion())
                .porcientoDescuentoAnual(request.getPorcientoDescuentoAnual())
                .features(features)
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public PlanResponse update(Long id, PlanRequest request) {
        Plan entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));

        if (!entity.getNombre().equals(request.getNombre()) && repository.existsByNombre(request.getNombre())) {
            throw new BusinessException("Ya existe un plan con el nombre: " + request.getNombre());
        }

        Set<Feature> features = resolveFeatures(request.getFeatureIds());

        entity.setNombre(request.getNombre());
        entity.setPrecioMensual(request.getPrecioMensual());
        entity.setMaxUsuarios(request.getMaxUsuarios());
        entity.setMaxVehiculos(request.getMaxVehiculos());
        entity.setDuracion(request.getDuracion());
        entity.setPorcientoDescuentoAnual(request.getPorcientoDescuentoAnual());
        entity.setFeatures(features);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Plan entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calcularImporteFacturacion(Long planId, boolean facturarAnual) {
        Plan plan = repository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        BigDecimal precio = plan.getPrecioMensual();
        BigDecimal importe;

        if (facturarAnual) {
            BigDecimal descuento = plan.getPorcientoDescuentoAnual() != null
                    ? plan.getPorcientoDescuentoAnual() : BigDecimal.ZERO;
            // Formula: ((precio * porcientoDescuentoAnual / 100) / 30) * 360
            importe = precio.multiply(descuento)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(360))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            // Formula: (precio / 30) * duracion
            importe = precio.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(plan.getDuracion()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return importe;
    }

    private Set<Feature> resolveFeatures(List<Long> featureIds) {
        Set<Feature> features = new HashSet<>();
        if (featureIds != null) {
            for (Long featureId : featureIds) {
                Feature feature = featureRepository.findById(featureId)
                        .orElseThrow(() -> new ResourceNotFoundException("Feature", "id", featureId));
                features.add(feature);
            }
        }
        return features;
    }

    private PlanResponse toResponse(Plan entity) {
        List<FeatureResponse> featureResponses = entity.getFeatures().stream()
                .map(this::toFeatureResponse)
                .collect(Collectors.toList());

        return PlanResponse.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .precioMensual(entity.getPrecioMensual())
                .maxUsuarios(entity.getMaxUsuarios())
                .maxVehiculos(entity.getMaxVehiculos())
                .duracion(entity.getDuracion())
                .porcientoDescuentoAnual(entity.getPorcientoDescuentoAnual())
                .features(featureResponses)
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }

    private FeatureResponse toFeatureResponse(Feature entity) {
        return FeatureResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}