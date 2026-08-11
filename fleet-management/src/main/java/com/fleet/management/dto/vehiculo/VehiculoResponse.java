package com.fleet.management.dto.vehiculo;

import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoResponse {

    private Long id;
    private EmpresaResponse empresa;
    private TipoVehiculoResponse tipoVehiculo;
    private MarcaResponse marca;
    private ChoferResponse chofer;
    private TipoCombustibleResponse tipoCombustible;
    private String matricula;
    private String numeroMotor;
    private BigInteger odometro;
    private Double combustible;
    private LocalDate ultimoMantenimiento;
    private BigInteger odometroUltimoMantenimiento;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
