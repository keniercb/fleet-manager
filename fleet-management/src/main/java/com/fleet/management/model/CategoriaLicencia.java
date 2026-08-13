package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categorias_licencia", uniqueConstraints = {
        @UniqueConstraint(name = "uk_categoria_licencia_codigo", columnNames = "codigo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CategoriaLicencia extends BaseEntity {

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "^[A-Za-z]$", message = "El codigo debe ser una unica letra")
    @Column(name = "codigo", nullable = false, length = 1)
    private String codigo;

    @NotBlank(message = "La denominacion es obligatoria")
    @Size(max = 100, message = "La denominacion no puede exceder 100 caracteres")
    @Column(name = "denominacion", nullable = false, length = 100)
    private String denominacion;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @OneToMany(mappedBy = "categoriaLicencia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChoferCategoria> choferes = new ArrayList<>();
}