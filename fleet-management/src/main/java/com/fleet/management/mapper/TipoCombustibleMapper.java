package com.fleet.management.mapper;

import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.model.TipoCombustible;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface TipoCombustibleMapper {

    TipoCombustibleResponse toResponse(TipoCombustible entity);
}
