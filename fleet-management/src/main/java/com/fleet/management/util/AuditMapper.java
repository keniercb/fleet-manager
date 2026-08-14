package com.fleet.management.util;

import com.fleet.management.dto.user.UserAuditResponse;
import com.fleet.management.model.User;

/**
 * Utility class to map User entity to UserAuditResponse for audit fields.
 */
public final class AuditMapper {

    private AuditMapper() {}

    /**
     * Maps a User entity to a lightweight UserAuditResponse.
     * Returns null if the user is null (e.g. bootstrap data without authenticated user).
     */
    public static UserAuditResponse toAuditResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserAuditResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }
}
