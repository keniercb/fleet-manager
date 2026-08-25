package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sub_features", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sub_feature_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Feature extends BaseEntity {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Size(max = 255, message = "La descripcion no puede exceder 255 caracteres")
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}