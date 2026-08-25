package com.fleet.management.service;

import com.fleet.management.dto.plan.PlanRequest;
import com.fleet.management.dto.plan.PlanResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanService {

    Page<PlanResponse> findAll(Pageable pageable);

    PlanResponse findById(Long id);

    PlanResponse create(PlanRequest request);

    PlanResponse update(Long id, PlanRequest request);

    void delete(Long id);
}