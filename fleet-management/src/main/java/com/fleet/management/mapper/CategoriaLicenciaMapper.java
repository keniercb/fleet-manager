package com.fleet.management.mapper;

import com.fleet.management.dto.categorialicencia.CategoriaLicenciaResponse;
import com.fleet.management.model.CategoriaLicencia;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface CategoriaLicenciaMapper {

    CategoriaLicenciaResponse toResponse(CategoriaLicencia entity);
}