package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO con los datos de la empresa para el encabezado de reportes.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmpresaReporteDto {

    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String provincia;
    private String municipio;
}