package com.fleet.management.service;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;

import java.util.List;

public interface TipoVehiculoService {

    List<TipoVehiculoResponse> findAll();

    TipoVehiculoResponse findById(Long id);

    TipoVehiculoResponse create(TipoVehiculoRequest request);

    TipoVehiculoResponse update(Long id, TipoVehiculoRequest request);

    void delete(Long id);
}