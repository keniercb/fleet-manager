package com.fleet.management.dto.tipovehiculo;

import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoVehiculoResponse {

    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}