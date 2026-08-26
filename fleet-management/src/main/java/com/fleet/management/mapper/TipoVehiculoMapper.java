package com.fleet.management.mapper;

import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.model.TipoVehiculo;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface TipoVehiculoMapper {

    TipoVehiculoResponse toResponse(TipoVehiculo entity);
}
