package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "choferes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chofer_carne_identidad", columnNames = "carne_identidad"),
        @UniqueConstraint(name = "uk_chofer_numero_licencia", columnNames = "numero_licencia")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chofer extends BaseEntity {

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chofer_empresa"))
    private Empresa empresa;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, message = "El nombre no puede exceder 50 caracteres")
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @NotBlank(message = "El carne de identidad es obligatorio")
    @Size(max = 20, message = "El carne de identidad no puede exceder 20 caracteres")
    @Column(name = "carne_identidad", nullable = false, length = 20, unique = true)
    private String carneIdentidad;

    @NotBlank(message = "El numero de licencia es obligatorio")
    @Size(max = 30, message = "El numero de licencia no puede exceder 30 caracteres")
    @Column(name = "numero_licencia", nullable = false, length = 30, unique = true)
    private String numeroLicencia;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @OneToMany(mappedBy = "chofer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChoferCategoria> categorias = new ArrayList<>();
}