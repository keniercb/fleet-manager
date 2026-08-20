package com.fleet.management.dto.recorrido;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecorridoRequest {

    @NotNull(message = "El vehiculo es obligatorio")
    private Long vehiculoId;

    private Long choferId;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate fecha;

    @NotNull(message = "Los kilometros son obligatorios")
    @Min(value = 1, message = "Los kilometros deben ser mayor a 0")
    private Integer kilometros;

    @DecimalMin(value = "0.0", message = "Los litros abastecidos no pueden ser negativos")
    private BigDecimal litrosAbastecidos;

    @Size(max = 50, message = "El numero de chip no puede exceder 50 caracteres")
    private String numeroChip;

    @Size(max = 100, message = "El lugar de abastecimiento no puede exceder 100 caracteres")
    private String lugarAbastecimiento;
}