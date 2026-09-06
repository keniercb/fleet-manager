package com.fleet.management.dto.empresa;

import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaResponse {

    private Long id;
    private String codigo;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private ProvinciaResponse provincia;
    private MunicipioResponse municipio;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}
