package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.marca.MarcaRequest;
import com.fleet.management.dto.marca.MarcaResponse;

import java.util.List;

public interface MarcaService {

    Page<MarcaResponse> findAll(String filter, Pageable pageable);

    MarcaResponse findById(Long id);

    MarcaResponse create(MarcaRequest request);

    MarcaResponse update(Long id, MarcaRequest request);

    void delete(Long id);
}