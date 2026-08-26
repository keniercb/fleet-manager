package com.fleet.management.mapper;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaEmbeddedResponse;
import com.fleet.management.model.ChoferCategoria;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = CategoriaLicenciaMapper.class)
public interface ChoferCategoriaEmbeddedMapper {

    ChoferCategoriaEmbeddedResponse toResponse(ChoferCategoria entity);
}