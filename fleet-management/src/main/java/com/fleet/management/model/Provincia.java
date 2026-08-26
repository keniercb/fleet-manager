package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "provincias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_provincia_codigo", columnNames = "codigo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Provincia extends BaseEntity {

    @NotNull(message = "El codigo es obligatorio")
    @Min(value = 1, message = "El codigo debe ser al menos 1")
    @Max(value = 99, message = "El codigo no puede exceder 99")
    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
}