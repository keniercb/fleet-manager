package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;

import java.util.List;

public interface TipoCombustibleService {

    Page<TipoCombustibleResponse> findAll(Pageable pageable);

    TipoCombustibleResponse findById(Long id);

    TipoCombustibleResponse findByCodigo(String codigo);

    TipoCombustibleResponse create(TipoCombustibleRequest request);

    TipoCombustibleResponse update(Long id, TipoCombustibleRequest request);

    void delete(Long id);
}