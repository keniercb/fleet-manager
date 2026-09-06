package com.fleet.management.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AbastecimientoVehiculoProjection {

    Long getVehiculoId();
    String getMatricula();
    String getModelo();
    String getMarcaNombre();
    String getTipoCombustibleCodigo();
    Long getTotalAbastecimientos();
    BigDecimal getTotalLitros();
    LocalDate getFechaPrimera();
    LocalDate getFechaUltima();
}