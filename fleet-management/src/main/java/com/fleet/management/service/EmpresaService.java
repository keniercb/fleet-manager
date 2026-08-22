package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;

import java.util.List;

public interface EmpresaService {

    Page<EmpresaResponse> findAll(String filter, Pageable pageable);

    EmpresaResponse findById(Long id);

    EmpresaResponse findByCodigo(String codigo);

    EmpresaResponse create(EmpresaRequest request);

    EmpresaResponse update(Long id, EmpresaRequest request);

    void delete(Long id);
}