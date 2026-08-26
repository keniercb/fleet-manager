package com.fleet.management.service.impl;

import com.fleet.management.dto.subscription.SubscriptionCreateRequest;
import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
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
import java.util.Optional;

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
    public SubscriptionResponse create(SubscriptionCreateRequest request) {
        Empresa empresa = resolveEmpresa(request.getEmpresaId());
        Plan plan = resolvePlan(request.getPlanId());

        LocalDate now = LocalDate.now();
        LocalDate endDate;

        Optional<Subscription> activeSubscription = repository.findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(empresa.getId());
        if (activeSubscription.isPresent()) {
            Subscription current = activeSubscription.get();
            current.setStatus(SubscriptionStatus.EXPIRED);
            current.setActivo(false);
            repository.save(current);
            endDate = current.getEndDate().plusDays(plan.getDuracion());
        } else {
            endDate = now.plusDays(plan.getDuracion());
        }

        Subscription entity = Subscription.builder()
                .empresa(empresa)
                .plan(plan)
                .startDate(now)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .maxVehiculos(plan.getMaxVehiculos())
                .maxUsuarios(plan.getMaxUsuarios())
                .currentVehicleCount(0)
                .currentUserCount(0)
                .porcientoDescuentoAnual(request.getPorcientoDescuentoAnual())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubscriptionResponse createTrialSubscription(Empresa empresa) {
        Plan trialPlan = planRepository.findByNombre("Trial")
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "nombre", "Trial"));
        return buildAndSave(empresa, trialPlan, SubscriptionStatus.ACTIVE);
    }

    @Override
    @Transactional
    public SubscriptionResponse update(Long id, SubscriptionRequest request) {
        try {
            Subscription entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

            if (request.getStatus() != null) {
                entity.setStatus(request.getStatus());
            }

            if (request.getMaxVehiculos() != null) {
                if (entity.getMaxVehiculos() != null && request.getMaxVehiculos() <= entity.getMaxVehiculos()) {
                    throw new BusinessException("La cantidad maxima de vehiculos solo puede ser mayor que la actual: "
                            + entity.getMaxVehiculos());
                }
                if (entity.getCurrentVehicleCount() != null && request.getMaxVehiculos() < entity.getCurrentVehicleCount()) {
                    throw new BusinessException("La cantidad maxima de vehiculos no puede ser menor que los vehiculos actuales: "
                            + entity.getCurrentVehicleCount());
                }
                entity.setMaxVehiculos(request.getMaxVehiculos());
            }

            if (request.getMaxUsuarios() != null) {
                if (entity.getMaxUsuarios() != null && request.getMaxUsuarios() <= entity.getMaxUsuarios()) {
                    throw new BusinessException("La cantidad maxima de usuarios solo puede ser mayor que la actual: "
                            + entity.getMaxUsuarios());
                }
                if (entity.getCurrentUserCount() != null && request.getMaxUsuarios() < entity.getCurrentUserCount()) {
                    throw new BusinessException("La cantidad maxima de usuarios no puede ser menor que los usuarios actuales: "
                            + entity.getCurrentUserCount());
                }
                entity.setMaxUsuarios(request.getMaxUsuarios());
            }

            if (request.getPorcientoDescuentoAnual() != null) {
                entity.setPorcientoDescuentoAnual(request.getPorcientoDescuentoAnual());
            }

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

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findActiveByEmpresa(Long empresaId) {
        Subscription subscription = repository.findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "empresaId", empresaId));
        return toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Subscription> getActiveSubscriptionEntity(Long empresaId) {
        return repository.findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(empresaId);
    }

    @Override
    @Transactional
    public void incrementVehicleCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        Integer max = subscription.getMaxVehiculos();
        if (max != null && subscription.getCurrentVehicleCount() + 1 > max) {
            throw new BusinessException("No se puede agregar el vehiculo. Se ha alcanzado el limite de "
                    + max + " vehiculos de la suscripcion");
        }
        subscription.setCurrentVehicleCount(subscription.getCurrentVehicleCount() + 1);
        repository.save(subscription);
    }

    @Override
    @Transactional
    public void decrementVehicleCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        if (subscription.getCurrentVehicleCount() <= 0) {
            throw new BusinessException("El conteo de vehiculos no puede ser negativo");
        }
        subscription.setCurrentVehicleCount(subscription.getCurrentVehicleCount() - 1);
        repository.save(subscription);
    }

    @Override
    @Transactional
    public void incrementUserCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        Integer max = subscription.getMaxUsuarios();
        if (max != null && subscription.getCurrentUserCount() + 1 > max) {
            throw new BusinessException("No se puede agregar el usuario. Se ha alcanzado el limite de "
                    + max + " usuarios de la suscripcion");
        }
        subscription.setCurrentUserCount(subscription.getCurrentUserCount() + 1);
        repository.save(subscription);
    }

    @Override
    @Transactional
    public void decrementUserCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        if (subscription.getCurrentUserCount() <= 0) {
            throw new BusinessException("El conteo de usuarios no puede ser negativo");
        }
        subscription.setCurrentUserCount(subscription.getCurrentUserCount() - 1);
        repository.save(subscription);
    }

    // ---- private helpers ----

    private SubscriptionResponse buildAndSave(Empresa empresa, Plan plan, SubscriptionStatus status) {
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(plan.getDuracion());

        Subscription entity = Subscription.builder()
                .empresa(empresa)
                .plan(plan)
                .startDate(now)
                .endDate(endDate)
                .status(status)
                .maxVehiculos(plan.getMaxVehiculos())
                .maxUsuarios(plan.getMaxUsuarios())
                .currentVehicleCount(0)
                .currentUserCount(0)
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    private Empresa resolveEmpresa(Long empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaId));
    }

    private Plan resolvePlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));
    }

    private Subscription findAndLock(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
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
                .maxVehiculos(entity.getMaxVehiculos())
                .maxUsuarios(entity.getMaxUsuarios())
                .currentVehicleCount(entity.getCurrentVehicleCount())
                .currentUserCount(entity.getCurrentUserCount())
                .porcientoDescuentoAnual(entity.getPorcientoDescuentoAnual())
                .version(entity.getVersion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}
