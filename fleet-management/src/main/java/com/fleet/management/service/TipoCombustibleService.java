package com.fleet.management.service;

import com.fleet.management.dto.tipocombustible.TipoCombustibleRequest;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;

import java.util.List;

public interface TipoCombustibleService {

    List<TipoCombustibleResponse> findAll();

    TipoCombustibleResponse findById(Long id);

    TipoCombustibleResponse findByCodigo(String codigo);

    TipoCombustibleResponse create(TipoCombustibleRequest request);

    TipoCombustibleResponse update(Long id, TipoCombustibleRequest request);

    void delete(Long id);
}