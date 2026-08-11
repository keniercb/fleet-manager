package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@Table(name = "vehiculos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehiculo_matricula", columnNames = "matricula"),
        @UniqueConstraint(name = "uk_vehiculo_numero_motor", columnNames = "numero_motor")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo extends BaseEntity {

    @NotNull(message = "El tipo de vehiculo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_vehiculo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehiculo_tipo_vehiculo"))
    private TipoVehiculo tipoVehiculo;

    @NotNull(message = "La marca es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marca_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehiculo_marca"))
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id",
            foreignKey = @ForeignKey(name = "fk_vehiculo_chofer"))
    private Chofer chofer;

    @NotNull(message = "El tipo de combustible es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_combustible_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_vehiculo_tipo_combustible"))
    private TipoCombustible tipoCombustible;

    @NotBlank(message = "La matricula es obligatoria")
    @Size(max = 20, message = "La matricula no puede exceder 20 caracteres")
    @Column(name = "matricula", nullable = false, length = 20, unique = true)
    private String matricula;

    @NotBlank(message = "El numero de motor es obligatorio")
    @Size(max = 50, message = "El numero de motor no puede exceder 50 caracteres")
    @Column(name = "numero_motor", nullable = false, length = 50, unique = true)
    private String numeroMotor;

    @NotNull(message = "El odometro es obligatorio")
    @Min(value = 0, message = "El odometro no puede ser negativo")
    @Column(name = "odometro", nullable = false)
    private BigInteger odometro;

    @NotNull(message = "El nivel de combustible es obligatorio")
    @DecimalMin(value = "0.0", message = "El nivel de combustible no puede ser negativo")
    @Column(name = "combustible", nullable = false)
    private Double combustible;

    @PastOrPresent(message = "La fecha del ultimo mantenimiento no puede ser futura")
    @Column(name = "ultimo_mantenimiento")
    private LocalDate ultimoMantenimiento;

    @Min(value = 0, message = "El odometro del ultimo mantenimiento no puede ser negativo")
    @Column(name = "odometro_ultimo_mantenimiento")
    private BigInteger odometroUltimoMantenimiento;
}
