package com.fleet.management.mapper;

import com.fleet.management.dto.role.RoleResponse;
import com.fleet.management.model.Role;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = PermissionMapper.class)
public interface RoleMapper {

    RoleResponse toResponse(Role entity);
}