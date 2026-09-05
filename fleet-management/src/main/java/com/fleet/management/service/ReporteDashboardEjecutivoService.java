package com.fleet.management.service;

import com.fleet.management.dto.reporte.DashboardEjecutivoResponse;

public interface ReporteDashboardEjecutivoService {

    /**
     * Genera el dashboard ejecutivo de KPIs para el mes y anio solicitados.
     *
     * @param mes  mes del 1 al 12
     * @param anio anio en formato YYYY
     * @return objeto unico con todos los KPIs calculados
     */
    DashboardEjecutivoResponse generarDashboard(Integer mes, Integer anio);
}
