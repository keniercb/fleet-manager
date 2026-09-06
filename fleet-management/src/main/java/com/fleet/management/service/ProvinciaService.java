package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.provincia.ProvinciaRequest;
import com.fleet.management.dto.provincia.ProvinciaResponse;

public interface ProvinciaService {

    Page<ProvinciaResponse> findAll(Pageable pageable);

    ProvinciaResponse findById(Long id);

    ProvinciaResponse create(ProvinciaRequest request);

    ProvinciaResponse update(Long id, ProvinciaRequest request);

    void delete(Long id);
}