package com.fleet.management.mapper;

import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.model.Chofer;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {EmpresaMapper.class, ChoferCategoriaEmbeddedMapper.class})
public interface ChoferMapper {

    ChoferResponse toResponse(Chofer entity);
}
