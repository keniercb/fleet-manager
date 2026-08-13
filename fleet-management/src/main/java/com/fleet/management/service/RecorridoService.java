package com.fleet.management.service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.recorrido.RecorridoRequest;
import com.fleet.management.dto.recorrido.RecorridoResponse;

import java.time.LocalDate;
import java.util.List;

public interface RecorridoService {

    Page<RecorridoResponse> findAll(Pageable pageable);

    RecorridoResponse findById(Long id);

    Page<RecorridoResponse> findByVehiculoId(Long vehiculoId, Pageable pageable);

    Page<RecorridoResponse> findByVehiculoIdAndFechaBetween(Long vehiculoId, LocalDate desde, LocalDate hasta, Pageable pageable);

    RecorridoResponse create(RecorridoRequest request);

    RecorridoResponse update(Long id, RecorridoRequest request);

    void delete(Long id);
}
