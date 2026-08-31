package com.fleet.management.service;

import com.fleet.management.dto.reporte.MantenimientoReporteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReporteMantenimientoService {

    Page<MantenimientoReporteResponse> reporteMantenimiento(Pageable pageable);
}
