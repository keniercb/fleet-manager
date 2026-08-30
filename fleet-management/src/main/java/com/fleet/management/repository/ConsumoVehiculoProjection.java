package com.fleet.management.repository;

import java.math.BigDecimal;

public interface ConsumoVehiculoProjection {

    Long getVehiculoId();
    String getMatricula();
    String getModelo();
    String getMarcaNombre();
    String getTipoCombustibleCodigo();
    String getEmpresaNombre();
    BigDecimal getKmTotal();
    BigDecimal getLitrosTotal();
    BigDecimal getConsumoTeorico();
}
