package com.fleet.management.dto.vehiculo;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoRequest {

    @NotNull(message = "La empresa es obligatoria")
    private Long empresaId;

    @NotNull(message = "El tipo de vehiculo es obligatorio")
    private Long tipoVehiculoId;

    @NotNull(message = "La marca es obligatoria")
    private Long marcaId;

    private Long choferId;

    @NotNull(message = "El tipo de combustible es obligatorio")
    private Long tipoCombustibleId;

    @NotBlank(message = "La matricula es obligatoria")
    @Size(max = 20, message = "La matricula no puede exceder 20 caracteres")
    private String matricula;

    @NotBlank(message = "El numero de motor es obligatorio")
    @Size(max = 50, message = "El numero de motor no puede exceder 50 caracteres")
    private String numeroMotor;

    @NotNull(message = "El odometro es obligatorio")
    @Min(value = 0, message = "El odometro no puede ser negativo")
    private BigInteger odometro;

    @NotNull(message = "El nivel de combustible es obligatorio")
    @DecimalMin(value = "0.0", message = "El nivel de combustible no puede ser negativo")
    private BigDecimal combustible;

    @PastOrPresent(message = "La fecha del ultimo mantenimiento no puede ser futura")
    private LocalDate ultimoMantenimiento;

    @Min(value = 0, message = "El odometro del ultimo mantenimiento no puede ser negativo")
    private BigInteger odometroUltimoMantenimiento;

    @DecimalMin(value = "0.0", inclusive = false, message = "El indice de consumo debe ser mayor a 0")
    private BigDecimal indiceConsumo;
}
