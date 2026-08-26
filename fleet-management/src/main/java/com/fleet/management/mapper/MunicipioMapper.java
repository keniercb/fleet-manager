package com.fleet.management.mapper;

import com.fleet.management.dto.municipio.MunicipioResponse;
import com.fleet.management.model.Municipio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class, uses = ProvinciaMapper.class)
public interface MunicipioMapper {

    MunicipioResponse toResponse(Municipio entity);
}