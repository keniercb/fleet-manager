package com.fleet.management.service;

import com.fleet.management.dto.reporte.VehiculoConsumoReporteDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ReporteTransporteService {

    Page<VehiculoConsumoReporteDTO> consumoPorVehiculo(LocalDate fechaDesde, LocalDate fechaHasta,
                                                          Long tipoVehiculoId, Long marcaId,
                                                          Long tipoCombustibleId,
                                                          Pageable pageable);
}
