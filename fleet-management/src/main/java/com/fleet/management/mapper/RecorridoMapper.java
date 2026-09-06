package com.fleet.management.mapper;

import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.dto.recorrido.RecorridoResponse;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.Currency;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Marca;
import com.fleet.management.model.Recorrido;
import com.fleet.management.model.TarjetaCombustible;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.model.Vehiculo;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = BaseMapperConfig.class)
public interface RecorridoMapper {

    RecorridoResponse toResponse(Recorrido entity);

    // --- Resumida mappings para relaciones anidadas ---

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigo", source = "codigo")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "activo", source = "activo")
    EmpresaResponse toEmpresaResumida(Empresa empresa);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "activo", source = "activo")
    TipoVehiculoResponse toTipoVehiculoResumida(TipoVehiculo tipoVehiculo);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "activo", source = "activo")
    MarcaResponse toMarcaResumida(Marca marca);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "codigo", source = "codigo")
    @Mapping(target = "denominacion", source = "denominacion")
    @Mapping(target = "activo", source = "activo")
    TipoCombustibleResponse toTipoCombustibleResumida(TipoCombustible tipoCombustible);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "apellidos", source = "apellidos")
    @Mapping(target = "carneIdentidad", source = "carneIdentidad")
    @Mapping(target = "numeroLicencia", source = "numeroLicencia")
    @Mapping(target = "fechaNacimiento", source = "fechaNacimiento")
    @Mapping(target = "activo", source = "activo")
    ChoferResponse toChoferResumida(Chofer chofer);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "isoCode", source = "isoCode")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "activo", source = "activo")
    CurrencyResponse toCurrencyResumida(Currency currency);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "numero", source = "numero")
    @Mapping(target = "saldo", source = "saldo")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "empresa", source = "empresa")
    @Mapping(target = "activo", source = "activo")
    TarjetaCombustibleResponse toTarjetaResumida(TarjetaCombustible tarjeta);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "empresa", source = "empresa")
    @Mapping(target = "tipoVehiculo", source = "tipoVehiculo")
    @Mapping(target = "marca", source = "marca")
    @Mapping(target = "tipoCombustible", source = "tipoCombustible")
    @Mapping(target = "matricula", source = "matricula")
    @Mapping(target = "modelo", source = "modelo")
    @Mapping(target = "numeroMotor", source = "numeroMotor")
    @Mapping(target = "odometro", source = "odometro")
    @Mapping(target = "combustible", source = "combustible")
    @Mapping(target = "ultimoMantenimiento", source = "ultimoMantenimiento")
    @Mapping(target = "odometroUltimoMantenimiento", source = "odometroUltimoMantenimiento")
    @Mapping(target = "indiceConsumo", source = "indiceConsumo")
    @Mapping(target = "activo", source = "activo")
    @Mapping(target = "fechaCreacion", source = "fechaCreacion")
    @Mapping(target = "fechaActualizacion", source = "fechaActualizacion")
    @Mapping(target = "creadoPor", source = "creadoPor")
    @Mapping(target = "modificadoPor", source = "modificadoPor")
    VehiculoResponse toVehiculoResumida(Vehiculo vehiculo);
}
