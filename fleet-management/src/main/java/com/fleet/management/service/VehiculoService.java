package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;

import java.util.List;

public interface VehiculoService {

    Page<VehiculoResponse> findAll(Pageable pageable);

    VehiculoResponse findById(Long id);

    Page<VehiculoResponse> findByChoferId(Long choferId, Pageable pageable);

    Page<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable);

    Page<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable);

    List<VehiculoResponse> findSinChoferAsignado();

    VehiculoResponse create(VehiculoRequest request);

    VehiculoResponse update(Long id, VehiculoRequest request);

    void delete(Long id);
}
