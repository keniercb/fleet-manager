package com.fleet.management.dto.reporte;

import com.fleet.management.dto.chofer.ChoferResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehiculoReporteData {

    private String marca;
    private String numeroMotor;
    private String tipoCombustible;
    private BigDecimal normaConsumo;
    private String matricula;
    private ChoferResponse chofer;
}
