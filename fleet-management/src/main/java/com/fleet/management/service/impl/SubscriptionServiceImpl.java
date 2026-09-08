package com.fleet.management.service.impl;

import com.fleet.management.dto.subscription.SubscriptionCreateRequest;
import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.SubscriptionMapper;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.PlanRepository;
import com.fleet.management.repository.SubscriptionRepository;
import com.fleet.management.service.SubscriptionService;
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
    private final SubscriptionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findById(Long id) {
        Subscription entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
        return mapper.toResponse(entity);
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
        return mapper.toResponse(repository.save(entity));
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
                    throw BusinessError.maxVehiculosSoloMayor(entity.getMaxVehiculos());
                }
                if (entity.getCurrentVehicleCount() != null && request.getMaxVehiculos() < entity.getCurrentVehicleCount()) {
                    throw BusinessError.maxVehiculosMenorActual(entity.getCurrentVehicleCount());
                }
                entity.setMaxVehiculos(request.getMaxVehiculos());
            }

            if (request.getMaxUsuarios() != null) {
                if (entity.getMaxUsuarios() != null && request.getMaxUsuarios() <= entity.getMaxUsuarios()) {
                    throw BusinessError.maxUsuariosSoloMayor(entity.getMaxUsuarios());
                }
                if (entity.getCurrentUserCount() != null && request.getMaxUsuarios() < entity.getCurrentUserCount()) {
                    throw BusinessError.maxUsuariosMenorActual(entity.getCurrentUserCount());
                }
                entity.setMaxUsuarios(request.getMaxUsuarios());
            }

            if (request.getPorcientoDescuentoAnual() != null) {
                entity.setPorcientoDescuentoAnual(request.getPorcientoDescuentoAnual());
            }

            return mapper.toResponse(repository.save(entity));
        } catch (OptimisticLockingFailureException ex) {
            throw BusinessError.suscripcionModificadaConcurrente();
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
        return repository.findByEmpresaIdAndActivoTrue(empresaId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> findByPlan(Long planId, Pageable pageable) {
        return repository.findByPlanIdAndActivoTrue(planId, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse findActiveByEmpresa(Long empresaId) {
        Subscription subscription = repository.findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "empresaId", empresaId));
        return mapper.toResponse(subscription);
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
            throw BusinessError.limiteVehiculosAlcanzado(max);
        }
        subscription.setCurrentVehicleCount(subscription.getCurrentVehicleCount() + 1);
        repository.save(subscription);
    }

    @Override
    @Transactional
    public void decrementVehicleCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        if (subscription.getCurrentVehicleCount() <= 0) {
            throw BusinessError.conteoVehiculosNegativo();
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
            throw BusinessError.limiteUsuariosAlcanzado(max);
        }
        subscription.setCurrentUserCount(subscription.getCurrentUserCount() + 1);
        repository.save(subscription);
    }

    @Override
    @Transactional
    public void decrementUserCount(Long subscriptionId) {
        Subscription subscription = findAndLock(subscriptionId);
        if (subscription.getCurrentUserCount() <= 0) {
            throw BusinessError.conteoUsuariosNegativo();
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
        return mapper.toResponse(repository.save(entity));
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
        // FX-25: lock pesimista real (SELECT ... FOR UPDATE) para evitar TOCTOU
        // en increment/decrementVehicleCount y increment/decrementUserCount.
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));
    }
}
