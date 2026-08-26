package com.fleet.management.mapper;

import com.fleet.management.dto.feature.FeatureResponse;
import com.fleet.management.model.Feature;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface FeatureMapper {

    FeatureResponse toResponse(Feature entity);
}
