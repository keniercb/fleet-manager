package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "sub_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Subscription extends BaseEntity {

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_sub_subscription_empresa", nullable = false)
    private Empresa empresa;

    @NotNull(message = "El plan es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_sub_subscription_plan", nullable = false)
    private Plan plan;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Min(value = 0, message = "La cantidad maxima de vehiculos no puede ser negativa")
    @Column(name = "max_vehiculos")
    private Integer maxVehiculos;

    @Min(value = 0, message = "La cantidad maxima de usuarios no puede ser negativa")
    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Min(value = 0, message = "La cantidad de vehiculos no puede ser negativa")
    @Column(name = "current_vehicle_count")
    private Integer currentVehicleCount;

    @Min(value = 0, message = "La cantidad de usuarios no puede ser negativa")
    @Column(name = "current_user_count")
    private Integer currentUserCount;

    @Version
    @Column(name = "version")
    private Long version;
}