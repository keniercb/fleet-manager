package com.fleet.management.mapper;

import com.fleet.management.dto.subscription.SubscriptionResponse;
import com.fleet.management.model.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface SubscriptionMapper {

    @Mapping(target = "empresa", expression = "java(mapEmpresaResumida(entity))")
    @Mapping(target = "plan", expression = "java(mapPlanResumido(entity))")
    SubscriptionResponse toResponse(Subscription entity);

    /**
     * Helper method invoked by MapStruct to build EmpresaResumidaResponse.
     * Using a default method avoids inner-class return type issues in the
     * annotation processor.
     */
    default SubscriptionResponse.EmpresaResumidaResponse mapEmpresaResumida(Subscription entity) {
        if (entity.getEmpresa() == null) {
            return null;
        }
        return SubscriptionResponse.EmpresaResumidaResponse.builder()
                .id(entity.getEmpresa().getId())
                .codigo(entity.getEmpresa().getCodigo())
                .nombre(entity.getEmpresa().getNombre())
                .activo(entity.getEmpresa().getActivo())
                .build();
    }

    /**
     * Helper method invoked by MapStruct to build PlanResumidoResponse.
     */
    default SubscriptionResponse.PlanResumidoResponse mapPlanResumido(Subscription entity) {
        if (entity.getPlan() == null) {
            return null;
        }
        return SubscriptionResponse.PlanResumidoResponse.builder()
                .id(entity.getPlan().getId())
                .nombre(entity.getPlan().getNombre())
                .precioMensual(entity.getPlan().getPrecioMensual())
                .maxUsuarios(entity.getPlan().getMaxUsuarios())
                .maxVehiculos(entity.getPlan().getMaxVehiculos())
                .duracion(entity.getPlan().getDuracion())
                .activo(entity.getPlan().getActivo())
                .build();
    }
}
