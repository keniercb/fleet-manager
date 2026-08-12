package com.fleet.management.service;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaRequest;
import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;

import java.util.List;

public interface ChoferCategoriaService {

    List<ChoferCategoriaResponse> findAll();

    ChoferCategoriaResponse findById(Long id);

    List<ChoferCategoriaResponse> findByChoferId(Long choferId);

    List<ChoferCategoriaResponse> findByCategoriaLicenciaId(Long categoriaLicenciaId);

    ChoferCategoriaResponse create(ChoferCategoriaRequest request);

    ChoferCategoriaResponse update(Long id, ChoferCategoriaRequest request);

    void delete(Long id);
}
