package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;

import java.util.List;

public interface ChoferCategoriaService {

    Page<ChoferCategoriaResponse> findAll(Pageable pageable);

    ChoferCategoriaResponse findById(Long id);

    Page<ChoferCategoriaResponse> findByChoferId(Long choferId, Pageable pageable);

    Page<ChoferCategoriaResponse> findByCategoriaLicenciaId(Long categoriaLicenciaId, Pageable pageable);

    ChoferCategoriaResponse create(ChoferCategoriaRequest request);

    ChoferCategoriaResponse update(Long id, ChoferCategoriaRequest request);

    void delete(Long id);
}
