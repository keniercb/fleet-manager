package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LecturaDiariaResponse {

    private Integer dia;
    private BigInteger odometro;
    private BigDecimal combustibleEnDeposito;
    private BigDecimal combustibleConsumido;
    private BigDecimal combustibleAbastecido;
    private BigDecimal saldoCombustible;
    private Integer kilometrosRecorridos;
}
