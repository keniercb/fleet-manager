package com.fleet.management.dto.chofercategoria;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferCategoriaEmbeddedResponse {

    private Long id;
    private CategoriaLicenciaResponse categoriaLicencia;
    private LocalDate fechaEmision;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}