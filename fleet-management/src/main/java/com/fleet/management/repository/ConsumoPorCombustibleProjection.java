package com.fleet.management.repository;

import java.math.BigDecimal;

public interface ConsumoPorCombustibleProjection {

    String getTipoCombustible();
    BigDecimal getVolumenConsumido();
    BigDecimal getVolumenAbastecido();
    BigDecimal getCostoEstimado();
    Long getCantidadRecorridos();
}
