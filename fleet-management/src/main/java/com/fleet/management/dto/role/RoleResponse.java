package com.fleet.management.dto.role;

import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.dto.user.UserAuditResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {

    private Long id;
    private String name;
    private String description;
    private Set<PermissionResponse> permissions;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private UserAuditResponse creadoPor;
    private UserAuditResponse modificadoPor;
}
