package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;

public interface VehiculoService {

    Page<VehiculoResponse> findAll(String filter, Pageable pageable);

    VehiculoResponse findById(Long id);

    Page<VehiculoResponse> findByChoferId(Long choferId, Pageable pageable);

    Page<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable);

    Page<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable);

    Page<VehiculoResponse> findSinChoferAsignado(Pageable pageable);

    Page<VehiculoResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable);

    VehiculoResponse create(VehiculoRequest request);

    VehiculoResponse update(Long id, VehiculoRequest request);

    void delete(Long id);
}
