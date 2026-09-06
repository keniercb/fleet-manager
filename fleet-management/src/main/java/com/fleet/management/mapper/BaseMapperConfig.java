package com.fleet.management.mapper;

import com.fleet.management.dto.user.UserAuditResponse;
import com.fleet.management.model.User;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BaseMapperConfig {

    default UserAuditResponse toAuditResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserAuditResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }
}
