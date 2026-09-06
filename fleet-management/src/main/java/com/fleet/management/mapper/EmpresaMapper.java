package com.fleet.management.mapper;

import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.model.Empresa;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = {ProvinciaMapper.class, MunicipioMapper.class})
public interface EmpresaMapper {

    EmpresaResponse toResponse(Empresa entity);
}
