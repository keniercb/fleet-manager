package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

@Entity
@Table(name = "recorridos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recorrido_vehiculo_fecha", columnNames = {"vehiculo_id", "fecha"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Recorrido extends BaseEntity {

    @NotNull(message = "El vehiculo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recorrido_vehiculo"))
    private Vehiculo vehiculo;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotNull(message = "Los kilometros son obligatorios")
    @Min(value = 1, message = "Los kilometros deben ser mayor a 0")
    @Column(name = "kilometros", nullable = false)
    private Integer kilometros;

    @NotNull(message = "El odometro inicial es obligatorio")
    @Min(value = 0, message = "El odometro inicial no puede ser negativo")
    @Column(name = "odometro_inicial", nullable = false)
    private BigInteger odometroInicial;

    @DecimalMin(value = "0.0", message = "El combustible inicial no puede ser negativo")
    @Column(name = "combustible_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal combustibleInicial;

    @DecimalMin(value = "0.0", message = "El consumo no puede ser negativo")
    @Column(name = "consumo", nullable = false, precision = 10, scale = 2)
    private BigDecimal consumo;

    @DecimalMin(value = "0.0", message = "Los litros abastecidos no pueden ser negativos")
    @Column(name = "litros_abastecidos", nullable = false, precision = 10, scale = 2)
    private BigDecimal litrosAbastecidos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id",
            foreignKey = @ForeignKey(name = "fk_recorrido_chofer"))
    private Chofer chofer;

    @Size(max = 50, message = "El numero de chip no puede exceder 50 caracteres")
    @Column(name = "numero_chip", length = 50)
    private String numeroChip;

    @Size(max = 100, message = "El lugar de abastecimiento no puede exceder 100 caracteres")
    @Column(name = "lugar_abastecimiento", length = 100)
    private String lugarAbastecimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarjeta_combustible_id",
            foreignKey = @ForeignKey(name = "fk_recorrido_tarjeta_combustible"))
    private TarjetaCombustible tarjetaCombustible;

    @Column(name = "importe_abastecido")
    private Double importeAbastecido;
}
