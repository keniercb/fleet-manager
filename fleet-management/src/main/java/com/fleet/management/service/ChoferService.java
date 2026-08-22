package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;

import java.util.List;

public interface ChoferService {

    Page<ChoferResponse> findAll(String filter, Pageable pageable);

    ChoferResponse findById(Long id);

    Page<ChoferResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable);

    ChoferResponse create(ChoferRequest request);

    ChoferResponse update(Long id, ChoferRequest request);

    void delete(Long id);
}
