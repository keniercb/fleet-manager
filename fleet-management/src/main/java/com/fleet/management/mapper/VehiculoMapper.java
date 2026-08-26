package com.fleet.management.mapper;

import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.model.Vehiculo;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {EmpresaMapper.class, TipoVehiculoMapper.class, MarcaMapper.class, TipoCombustibleMapper.class, ChoferMapper.class})
public interface VehiculoMapper {

    VehiculoResponse toResponse(Vehiculo entity);
}
