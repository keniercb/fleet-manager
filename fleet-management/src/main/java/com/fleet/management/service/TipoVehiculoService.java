package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoRequest;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;

import java.util.List;

public interface TipoVehiculoService {

    Page<TipoVehiculoResponse> findAll(Pageable pageable);

    TipoVehiculoResponse findById(Long id);

    TipoVehiculoResponse create(TipoVehiculoRequest request);

    TipoVehiculoResponse update(Long id, TipoVehiculoRequest request);

    void delete(Long id);
}