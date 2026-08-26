package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "municipios", uniqueConstraints = {
        @UniqueConstraint(name = "uk_municipio_provincia_codigo", columnNames = {"provincia_id", "codigo"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Municipio extends BaseEntity {

    @NotNull(message = "La provincia es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provincia_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_municipio_provincia"))
    private Provincia provincia;

    @NotNull(message = "El codigo es obligatorio")
    @Min(value = 1, message = "El codigo debe ser al menos 1")
    @Max(value = 999, message = "El codigo no puede exceder 999")
    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}