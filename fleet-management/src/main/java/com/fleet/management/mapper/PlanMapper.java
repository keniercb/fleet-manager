package com.fleet.management.mapper;

import com.fleet.management.dto.plan.PlanResponse;
import com.fleet.management.model.Plan;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class, uses = FeatureMapper.class)
public interface PlanMapper {

    PlanResponse toResponse(Plan entity);
}
