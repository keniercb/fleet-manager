package com.fleet.management.service;

import com.fleet.management.dto.feature.FeatureRequest;
import com.fleet.management.dto.feature.FeatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeatureService {

    Page<FeatureResponse> findAll(Pageable pageable);

    FeatureResponse findById(Long id);

    FeatureResponse create(FeatureRequest request);

    FeatureResponse update(Long id, FeatureRequest request);

    void delete(Long id);
}
