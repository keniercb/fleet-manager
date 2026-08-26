package com.fleet.management.mapper;

import com.fleet.management.dto.provincia.ProvinciaResponse;
import com.fleet.management.model.Provincia;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface ProvinciaMapper {

    ProvinciaResponse toResponse(Provincia entity);
}
