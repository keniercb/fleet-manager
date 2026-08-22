package com.fleet.management.service;

import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleRequest;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TarjetaCombustibleService {

    Page<TarjetaCombustibleResponse> findAll(String filter, Pageable pageable);

    TarjetaCombustibleResponse findById(Long id);

    TarjetaCombustibleResponse findByNumero(String numero);

    TarjetaCombustibleResponse create(TarjetaCombustibleRequest request);

    TarjetaCombustibleResponse update(Long id, TarjetaCombustibleRequest request);

    void delete(Long id);

    Page<TarjetaCombustibleResponse> findByEmpresaId(Long empresaId, Pageable pageable);
}