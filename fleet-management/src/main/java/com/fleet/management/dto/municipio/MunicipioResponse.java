package com.fleet.management.dto.municipio;

import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MunicipioResponse {

    private Long id;
    private ProvinciaResponse provincia;
    private Integer codigo;
    private String nombre;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}