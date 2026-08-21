package com.fleet.management.dto.reporte;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnalisisConsumoResponse {

    private BigDecimal combustibleInicial;
    private BigDecimal combustibleRecibido;
    private BigDecimal combustibleConsumido;
    private BigDecimal existenciaFinal;
    private Integer kilometrosRecorridos;
    private BigDecimal consumidoSegunNorma;
}
