package com.fleet.management.service;

import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.model.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {

    Page<SubscriptionResponse> findAll(Pageable pageable);

    SubscriptionResponse findById(Long id);

    SubscriptionResponse create(SubscriptionRequest request);

    SubscriptionResponse createTrialSubscription(Empresa empresa);

    SubscriptionResponse update(Long id, SubscriptionRequest request);

    void delete(Long id);

    Page<SubscriptionResponse> findByEmpresa(Long empresaId, Pageable pageable);

    Page<SubscriptionResponse> findByPlan(Long planId, Pageable pageable);

    SubscriptionResponse findActiveByEmpresa(Long empresaId);

    void incrementVehicleCount(Long subscriptionId);

    void decrementVehicleCount(Long subscriptionId);

    void incrementUserCount(Long subscriptionId);

    void decrementUserCount(Long subscriptionId);
}