package com.fleet.management.mapper;

import com.fleet.management.dto.chofercategoria.ChoferCategoriaResponse;
import com.fleet.management.model.ChoferCategoria;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {ChoferMapper.class, CategoriaLicenciaMapper.class})
public interface ChoferCategoriaMapper {

    ChoferCategoriaResponse toResponse(ChoferCategoria entity);
}