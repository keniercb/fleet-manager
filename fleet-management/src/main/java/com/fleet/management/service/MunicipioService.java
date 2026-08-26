package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.municipio.MunicipioRequest;
import com.fleet.management.dto.municipio.MunicipioResponse;

import java.util.List;

public interface MunicipioService {

    Page<MunicipioResponse> findAll(Pageable pageable);

    Page<MunicipioResponse> findByProvinciaId(Long provinciaId, Pageable pageable);

    List<MunicipioResponse> listByProvinciaId(Long provinciaId);

    MunicipioResponse findById(Long id);

    MunicipioResponse create(MunicipioRequest request);

    MunicipioResponse update(Long id, MunicipioRequest request);

    void delete(Long id);
}