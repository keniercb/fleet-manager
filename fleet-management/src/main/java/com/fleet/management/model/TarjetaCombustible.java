package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "tarjetas_combustible", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tarjeta_combustible_numero", columnNames = "numero")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TarjetaCombustible extends BaseEntity {

    @NotBlank(message = "El numero de tarjeta es obligatorio")
    @Size(max = 50, message = "El numero de tarjeta no puede exceder 50 caracteres")
    @Column(name = "numero", nullable = false, length = 50)
    private String numero;

    @NotNull(message = "El saldo es obligatorio")
    @Positive(message = "El saldo debe ser mayor a cero")
    @Column(name = "saldo", nullable = false)
    private Double saldo;

    @NotNull(message = "La moneda es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tarjeta_combustible_currency"))
    private Currency currency;
}