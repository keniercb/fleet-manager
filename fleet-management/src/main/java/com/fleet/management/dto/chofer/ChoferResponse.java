package com.fleet.management.dto.chofer;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaEmbeddedResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChoferResponse {

    private Long id;
    private EmpresaResponse empresa;
    private String nombre;
    private String apellidos;
    private String carneIdentidad;
    private String numeroLicencia;
    private LocalDate fechaNacimiento;
    private List<ChoferCategoriaEmbeddedResponse> categorias;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}