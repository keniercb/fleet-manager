package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "tipos_vehiculo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tipo_vehiculo_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoVehiculo extends BaseEntity {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}