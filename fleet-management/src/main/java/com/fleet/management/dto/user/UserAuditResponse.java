package com.fleet.management.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight User DTO used in audit fields (creadoPor/modificadoPor)
 * to avoid circular references and expose only non-sensitive data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAuditResponse {

    private Long id;
    private String email;
}
