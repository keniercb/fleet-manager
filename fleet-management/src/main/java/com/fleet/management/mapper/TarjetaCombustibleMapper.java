package com.fleet.management.mapper;

import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.model.TarjetaCombustible;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {CurrencyMapper.class, EmpresaMapper.class})
public interface TarjetaCombustibleMapper {

    TarjetaCombustibleResponse toResponse(TarjetaCombustible entity);
}
