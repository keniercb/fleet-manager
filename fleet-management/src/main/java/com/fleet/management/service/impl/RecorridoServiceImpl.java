package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.dto.recorrido.RecorridoRequest;
import com.fleet.management.dto.recorrido.RecorridoResponse;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.*;
import com.fleet.management.repository.RecorridoRepository;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.service.RecorridoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecorridoServiceImpl implements RecorridoService {

    private final RecorridoRepository repository;
    private final VehiculoRepository vehiculoRepository;

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Override
    @Transactional(readOnly = true)
    public Page<RecorridoResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RecorridoResponse findById(Long id) {
        Recorrido entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrido", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecorridoResponse> findByVehiculoId(Long vehiculoId, Pageable pageable) {
        return repository.findByVehiculoId(vehiculoId), pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecorridoResponse> findByVehiculoIdAndFechaBetween(Long vehiculoId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        return repository.findByVehiculoIdAndFechaBetween(vehiculoId, desde, hasta), pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public RecorridoResponse create(RecorridoRequest request) {
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", request.getVehiculoId()));

        // Validar unicidad: solo un recorrido por vehiculo por fecha
        if (repository.existsByVehiculoIdAndFecha(request.getVehiculoId(), request.getFecha())) {
            throw new BusinessException("Ya existe un recorrido para el vehiculo con id "
                    + request.getVehiculoId() + " en la fecha " + request.getFecha());
        }

        // Validar que no exista un recorrido con fecha posterior para el mismo vehiculo
        if (repository.existsByVehiculoIdAndFechaAfter(request.getVehiculoId(), request.getFecha())) {
            throw new BusinessException("No se puede insertar el recorrido en la fecha "
                    + request.getFecha() + " porque ya existe un recorrido con fecha posterior para el vehiculo con id "
                    + request.getVehiculoId());
        }

        // Calcular consumo: (indiceConsumo * kilometros) / 100, redondeado a 2 decimales
        BigDecimal consumo = vehiculo.getIndiceConsumo()
                .multiply(BigDecimal.valueOf(request.getKilometros()))
                .divide(CIEN, 2, RoundingMode.HALF_UP);

        // Redondear litros abastecidos a 2 decimales
        BigDecimal litrosAbastecidos = request.getLitrosAbastecidos() != null
                ? request.getLitrosAbastecidos().setScale(2, RoundingMode.HALF_UP)
                : CERO;

        // Validar que el vehiculo tenga suficiente combustible (sumando litros abastecidos)
        BigDecimal combustibleRestante = vehiculo.getCombustible().subtract(consumo).add(litrosAbastecidos);
        if (combustibleRestante.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El recorrido no puede ser insertado porque se consume mas combustible ("
                    + consumo + ") que el disponible en el vehiculo (" + vehiculo.getCombustible() + ")");
        }

        // OdometroInicial es el odometro actual del vehiculo antes de sumar kilometros
        BigInteger odometroInicial = vehiculo.getOdometro();

        Recorrido entity = Recorrido.builder()
                .vehiculo(vehiculo)
                .fecha(request.getFecha())
                .kilometros(request.getKilometros())
                .odometroInicial(odometroInicial)
                .consumo(consumo)
                .litrosAbastecidos(litrosAbastecidos)
                .numeroChip(request.getNumeroChip())
                .lugarAbastecimiento(request.getLugarAbastecimiento())
                .activo(true)
                .build();

        Recorrido saved = repository.save(entity);

        // Sumar kilometros al odometro del vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
        // Actualizar combustible del vehiculo
        vehiculo.setCombustible(combustibleRestante);
        vehiculoRepository.save(vehiculo);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecorridoResponse update(Long id, RecorridoRequest request) {
        Recorrido entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrido", "id", id));

        // No se permite cambiar el vehiculo
        if (!entity.getVehiculo().getId().equals(request.getVehiculoId())) {
            throw new BusinessException("No se permite cambiar el vehiculo del recorrido");
        }

        // No se permite cambiar la fecha
        if (!entity.getFecha().equals(request.getFecha())) {
            throw new BusinessException("No se permite cambiar la fecha del recorrido");
        }

        Vehiculo vehiculo = entity.getVehiculo();

        // Calcular el nuevo consumo, redondeado a 2 decimales
        BigDecimal nuevoConsumo = vehiculo.getIndiceConsumo()
                .multiply(BigDecimal.valueOf(request.getKilometros()))
                .divide(CIEN, 2, RoundingMode.HALF_UP);

        // Verificar disponibilidad de combustible restaurando el consumo antiguo primero
        BigDecimal consumoAntiguo = entity.getConsumo() != null ? entity.getConsumo() : CERO;
        BigDecimal combustibleDisponible = vehiculo.getCombustible().add(consumoAntiguo).subtract(nuevoConsumo);
        if (combustibleDisponible.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El recorrido no puede ser actualizado porque se consume mas combustible ("
                    + nuevoConsumo + ") que el disponible en el vehiculo ("
                    + vehiculo.getCombustible().add(consumoAntiguo) + ")");
        }

        // Restaurar odometro y combustible del vehiculo
        BigInteger kilometrosAnteriores = BigInteger.valueOf(entity.getKilometros());
        vehiculo.setOdometro(vehiculo.getOdometro().subtract(kilometrosAnteriores));
        vehiculo.setCombustible(vehiculo.getCombustible().add(consumoAntiguo));

        // Actualizar la entidad
        entity.setKilometros(request.getKilometros());
        entity.setOdometroInicial(vehiculo.getOdometro());
        entity.setConsumo(nuevoConsumo);

        Recorrido saved = repository.save(entity);

        // Sumar kilometros al odometro y restar consumo al combustible del vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
        vehiculo.setCombustible(vehiculo.getCombustible().subtract(nuevoConsumo));
        vehiculoRepository.save(vehiculo);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Recorrido entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrido", "id", id));

        // Restar los kilometros al odometro del vehiculo
        Vehiculo vehiculo = entity.getVehiculo();
        BigInteger kilometros = BigInteger.valueOf(entity.getKilometros());
        vehiculo.setOdometro(vehiculo.getOdometro().subtract(kilometros));
        // Restaurar el combustible consumido
        BigDecimal consumo = entity.getConsumo() != null ? entity.getConsumo() : CERO;
        vehiculo.setCombustible(vehiculo.getCombustible().add(consumo));
        vehiculoRepository.save(vehiculo);

        // Baja logica
        entity.setActivo(false);
        repository.save(entity);
    }

    private RecorridoResponse toResponse(Recorrido entity) {
        return RecorridoResponse.builder()
                .id(entity.getId())
                .vehiculo(toVehiculoResumido(entity.getVehiculo()))
                .fecha(entity.getFecha())
                .kilometros(entity.getKilometros())
                .odometroInicial(entity.getOdometroInicial())
                .consumo(entity.getConsumo())
                .litrosAbastecidos(entity.getLitrosAbastecidos())
                .numeroChip(entity.getNumeroChip())
                .lugarAbastecimiento(entity.getLugarAbastecimiento())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }

    private VehiculoResponse toVehiculoResumido(Vehiculo v) {
        Empresa emp = v.getEmpresa();
        EmpresaResponse empresaResp = EmpresaResponse.builder()
                .id(emp.getId())
                .codigo(emp.getCodigo())
                .nombre(emp.getNombre())
                .activo(emp.getActivo())
                .build();

        TipoVehiculo tv = v.getTipoVehiculo();
        TipoVehiculoResponse tipoResp = TipoVehiculoResponse.builder()
                .id(tv.getId())
                .nombre(tv.getNombre())
                .activo(tv.getActivo())
                .build();

        Marca m = v.getMarca();
        MarcaResponse marcaResp = MarcaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .activo(m.getActivo())
                .build();

        TipoCombustible tc = v.getTipoCombustible();
        TipoCombustibleResponse combustibleResp = TipoCombustibleResponse.builder()
                .id(tc.getId())
                .codigo(tc.getCodigo())
                .denominacion(tc.getDenominacion())
                .activo(tc.getActivo())
                .build();

        return VehiculoResponse.builder()
                .id(v.getId())
                .empresa(empresaResp)
                .tipoVehiculo(tipoResp)
                .marca(marcaResp)
                .tipoCombustible(combustibleResp)
                .matricula(v.getMatricula())
                .numeroMotor(v.getNumeroMotor())
                .odometro(v.getOdometro())
                .combustible(v.getCombustible())
                .ultimoMantenimiento(v.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(v.getOdometroUltimoMantenimiento())
                .indiceConsumo(v.getIndiceConsumo())
                .activo(v.getActivo())
                .fechaCreacion(v.getFechaCreacion())
                .fechaActualizacion(v.getFechaActualizacion())
                .build();
    }
}
