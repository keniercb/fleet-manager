package com.fleet.management.dto.tarjetacombustible;

import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TarjetaCombustibleResponse {

    private Long id;
    private String numero;
    // FX-13: migrado de Double a BigDecimal para precision monetaria.
    private BigDecimal saldo;
    private CurrencyResponse currency;
    private EmpresaResponse empresa;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}