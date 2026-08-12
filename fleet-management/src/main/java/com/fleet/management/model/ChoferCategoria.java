package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "choferes_categorias", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chofer_categoria", columnNames = {"chofer_id", "categoria_licencia_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferCategoria extends BaseEntity {

    @NotNull(message = "El chofer es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chofer_categoria_chofer"))
    private Chofer chofer;

    @NotNull(message = "La categoria de licencia es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_licencia_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chofer_categoria_categoria"))
    private CategoriaLicencia categoriaLicencia;

    @NotNull(message = "La fecha de emision es obligatoria")
    @PastOrPresent(message = "La fecha de emision no puede ser futura")
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;
}