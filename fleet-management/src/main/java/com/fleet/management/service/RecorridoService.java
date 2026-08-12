package com.fleet.management.service;

import com.fleet.management.dto.recorrido.RecorridoRequest;
import com.fleet.management.dto.recorrido.RecorridoResponse;

import java.time.LocalDate;
import java.util.List;

public interface RecorridoService {

    List<RecorridoResponse> findAll();

    RecorridoResponse findById(Long id);

    List<RecorridoResponse> findByVehiculoId(Long vehiculoId);

    List<RecorridoResponse> findByVehiculoIdAndFechaBetween(Long vehiculoId, LocalDate desde, LocalDate hasta);

    RecorridoResponse create(RecorridoRequest request);

    RecorridoResponse update(Long id, RecorridoRequest request);

    void delete(Long id);
}
