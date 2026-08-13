package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "empresas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_empresa_codigo", columnNames = "codigo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Empresa extends BaseEntity {

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 30, message = "El codigo no puede exceder 30 caracteres")
    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Size(max = 255, message = "La direccion no puede exceder 255 caracteres")
    @Column(name = "direccion", length = 255)
    private String direccion;

    @Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Email(message = "El email debe tener un formato valido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    @Column(name = "email", length = 100)
    private String email;
}