package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "currencies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_currency_iso_code", columnNames = "iso_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Currency extends BaseEntity {

    @NotBlank(message = "El codigo ISO es obligatorio")
    @Size(max = 10, message = "El codigo ISO no puede exceder 10 caracteres")
    @Column(name = "iso_code", nullable = false, length = 10)
    private String isoCode;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 100, message = "La descripcion no puede exceder 100 caracteres")
    @Column(name = "descripcion", nullable = false, length = 100)
    private String descripcion;
}
