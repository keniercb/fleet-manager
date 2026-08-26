package com.fleet.management.mapper;

import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import org.mapstruct.Mapper;

@Mapper(config = BaseMapperConfig.class)
public interface SubscriptionMapper {

    SubscriptionResponse toResponse(Subscription entity);

    SubscriptionResponse.EmpresaResumidaResponse toEmpresaResumida(Empresa empresa);

    SubscriptionResponse.PlanResumidoResponse toPlanResumido(Plan plan);
}
