package com.fleet.management.util;

import com.fleet.management.exception.BusinessException;
import com.fleet.management.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long resolveEmpresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authUser)) {
            throw new BusinessException("No se pudo determinar la empresa del usuario autenticado");
        }
        var empresaRef = authUser.getUser().getEmpresa();
        if (empresaRef == null) {
            throw new BusinessException("El usuario no tiene una empresa asociada");
        }
        return empresaRef.getId();
    }
}
