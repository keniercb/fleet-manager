package com.fleet.management.dto.recorrido;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecorridoRequest {

    @NotNull(message = "El vehiculo es obligatorio")
    private Long vehiculoId;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate fecha;

    @NotNull(message = "Los kilometros son obligatorios")
    @Min(value = 1, message = "Los kilometros deben ser mayor a 0")
    private Integer kilometros;
}