package com.fleet.management.service;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;

import java.util.List;

public interface EmpresaService {

    List<EmpresaResponse> findAll();

    EmpresaResponse findById(Long id);

    EmpresaResponse findByCodigo(String codigo);

    EmpresaResponse create(EmpresaRequest request);

    EmpresaResponse update(Long id, EmpresaRequest request);

    void delete(Long id);
}