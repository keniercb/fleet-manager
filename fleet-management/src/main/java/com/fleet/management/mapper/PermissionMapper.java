package com.fleet.management.mapper;

import com.fleet.management.dto.permission.PermissionResponse;
import com.fleet.management.model.Permission;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface PermissionMapper {

    PermissionResponse toResponse(Permission entity);
}