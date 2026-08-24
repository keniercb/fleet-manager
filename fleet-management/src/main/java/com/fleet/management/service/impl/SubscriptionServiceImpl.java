package com.fleet.management.service.impl;

import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.PlanRepository;
import com.fleet.management.repository.SubscriptionRepository;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository repository;
    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long id) {
        Subscription entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public SubscriptionResponse create(SubscriptionRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getPlanId()));

        Subscription entity = Subscription.builder()
                .empresa(empresa)
                .plan(plan)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(request.getStatus())
                .currentVehicleCount(request.getCurrentVehicleCount())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.getPlanId()));

        try {
            Subscription entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

            entity.setEmpresa(empresa);
            entity.setPlan(plan);
            entity.setStartDate(request.getStartDate());
            entity.setEndDate(request.getEndDate());
            entity.setStatus(request.getStatus());
            entity.setCurrentVehicleCount(request.getCurrentVehicleCount());
            return toResponse(repository.save(entity));
        } catch (OptimisticLockingFailureException ex) {
            throw new BusinessException("La suscripcion fue modificada por otro usuario. Intente nuevamente.");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Subscription entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> findByEmpresa(Long empresaId, Pageable pageable) {
        return repository.findByEmpresaIdAndActivoTrue(empresaId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> findByPlan(Long planId, Pageable pageable) {
        return repository.findByPlanIdAndActivoTrue(planId, pageable).map(this::toResponse);
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }

    private SubscriptionResponse toResponse(Subscription entity) {
        Empresa empresa = entity.getEmpresa();
        SubscriptionResponse.EmpresaResumidaResponse empresaResumida = SubscriptionResponse.EmpresaResumidaResponse.builder()
                .id(empresa.getId())
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .activo(empresa.getActivo())
                .build();

        Plan plan = entity.getPlan();
        SubscriptionResponse.PlanResumidoResponse planResumido = SubscriptionResponse.PlanResumidoResponse.builder()
                .id(plan.getId())
                .nombre(plan.getNombre())
                .precioMensual(plan.getPrecioMensual())
                .maxUsuarios(plan.getMaxUsuarios())
                .maxVehiculos(plan.getMaxVehiculos())
                .duracion(plan.getDuracion())
                .activo(plan.getActivo())
                .build();

        return SubscriptionResponse.builder()
                .id(entity.getId())
                .empresa(empresaResumida)
                .plan(planResumido)
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .currentVehicleCount(entity.getCurrentVehicleCount())
                .version(entity.getVersion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}
