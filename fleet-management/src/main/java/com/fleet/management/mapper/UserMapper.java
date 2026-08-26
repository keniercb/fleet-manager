package com.fleet.management.mapper;

import com.fleet.management.dto.user.UserResponse;
import com.fleet.management.model.User;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {EmpresaMapper.class, RoleMapper.class})
public interface UserMapper {

    UserResponse toResponse(User entity);
}