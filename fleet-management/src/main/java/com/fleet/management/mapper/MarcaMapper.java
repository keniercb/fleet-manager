package com.fleet.management.mapper;

import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.model.Marca;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface MarcaMapper {

    MarcaResponse toResponse(Marca entity);
}
