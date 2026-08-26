package com.fleet.management.mapper;

import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface SubscriptionMapper {

    SubscriptionResponse toResponse(Subscription entity);

    @Mapping(target = "id", source = "empresa.id")
    @Mapping(target = "codigo", source = "empresa.codigo")
    @Mapping(target = "nombre", source = "empresa.nombre")
    @Mapping(target = "activo", source = "empresa.activo")
    SubscriptionResponse.EmpresaResumidaResponse toEmpresaResumida(Empresa empresa);

    @Mapping(target = "id", source = "plan.id")
    @Mapping(target = "nombre", source = "plan.nombre")
    @Mapping(target = "precioMensual", source = "plan.precioMensual")
    @Mapping(target = "maxUsuarios", source = "plan.maxUsuarios")
    @Mapping(target = "maxVehiculos", source = "plan.maxVehiculos")
    @Mapping(target = "duracion", source = "plan.duracion")
    @Mapping(target = "activo", source = "plan.activo")
    SubscriptionResponse.PlanResumidoResponse toPlanResumido(Plan plan);
}
