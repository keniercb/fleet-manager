package com.fleet.management.service;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;

import java.util.List;

public interface VehiculoService {

    List<VehiculoResponse> findAll();

    VehiculoResponse findById(Long id);

    List<VehiculoResponse> findByChoferId(Long choferId);

    List<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId);

    List<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId);

    List<VehiculoResponse> findSinChoferAsignado();

    VehiculoResponse create(VehiculoRequest request);

    VehiculoResponse update(Long id, VehiculoRequest request);

    void delete(Long id);
}
