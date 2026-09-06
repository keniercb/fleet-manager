package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sub_plans", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sub_plan_nombre", columnNames = "nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Plan extends BaseEntity {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100, unique = true)
    private String nombre;

    @DecimalMin(value = "0.0", message = "El precio mensual no puede ser negativo")
    @Column(name = "precio_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Min(value = 1, message = "El maximo de usuarios debe ser al menos 1")
    @Column(name = "max_usuarios", nullable = false)
    private Integer maxUsuarios;

    @Min(value = 1, message = "El maximo de vehiculos debe ser al menos 1")
    @Column(name = "max_vehiculos", nullable = false)
    private Integer maxVehiculos;

    @Min(value = 1, message = "La duracion debe ser al menos 1 dia")
    @Column(name = "duracion", nullable = false)
    private Integer duracion;

    @DecimalMin(value = "0.00", message = "El porciento de descuento anual no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El porciento de descuento anual no puede superar 100")
    @Column(name = "porciento_descuento_anual", precision = 5, scale = 2)
    private BigDecimal porcientoDescuentoAnual;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sub_plan_features",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<Feature> features = new HashSet<>();
}