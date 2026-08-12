package com.fleet.management.dto.chofercategoria;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferCategoriaRequest {

    @NotNull(message = "El chofer es obligatorio")
    private Long choferId;

    @NotNull(message = "La categoria de licencia es obligatoria")
    private Long categoriaLicenciaId;

    @NotNull(message = "La fecha de emision es obligatoria")
    @PastOrPresent(message = "La fecha de emision no puede ser futura")
    private LocalDate fechaEmision;
}