package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaRequest;
import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;

import java.util.List;

public interface CategoriaLicenciaService {

    Page<CategoriaLicenciaResponse> findAll(Pageable pageable);

    CategoriaLicenciaResponse findById(Long id);

    CategoriaLicenciaResponse findByCodigo(String codigo);

    CategoriaLicenciaResponse create(CategoriaLicenciaRequest request);

    CategoriaLicenciaResponse update(Long id, CategoriaLicenciaRequest request);

    void delete(Long id);
}