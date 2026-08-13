package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tipos_combustible", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipo_combustible_codigo", columnNames = "codigo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TipoCombustible extends BaseEntity {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20, message = "El codigo no puede exceder 20 caracteres")
    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 100, message = "La denominacion no puede exceder 100 caracteres")
    @Column(name = "denominacion", nullable = false, length = 100)
    private String denominacion;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}