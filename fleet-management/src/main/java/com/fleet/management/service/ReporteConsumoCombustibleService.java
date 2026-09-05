package com.fleet.management.service;

import com.fleet.management.dto.reporte.ConsumoCombustibleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReporteConsumoCombustibleService {

    ConsumoCombustibleResponse generarReporte(LocalDate fechaDesde, LocalDate fechaHasta,
                                                 Long tipoVehiculoId);
}
