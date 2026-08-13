package com.fleet.management.service.impl;

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

    @Override
    @Transactional(readOnly = true)
    public List<RecorridoResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
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
    public List<RecorridoResponse> findByVehiculoId(Long vehiculoId) {
        return repository.findByVehiculoId(vehiculoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecorridoResponse> findByVehiculoIdAndFechaBetween(Long vehiculoId, LocalDate desde, LocalDate hasta) {
        return repository.findByVehiculoIdAndFechaBetween(vehiculoId, desde, hasta).stream()
                .map(this::toResponse)
                .toList();
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
        double consumo = BigDecimal.valueOf(vehiculo.getIndiceConsumo() * request.getKilometros() / 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Validar que el vehiculo tenga suficiente combustible
        double combustibleRestante = vehiculo.getCombustible() - consumo;
        if (combustibleRestante < 0) {
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
                .activo(true)
                .build();

        Recorrido saved = repository.save(entity);

        // Sumar kilometros al odometro del vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
        // Restar consumo al combustible del vehiculo
        vehiculo.setCombustible(combustibleRestante);
        vehiculoRepository.save(vehiculo);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public RecorridoResponse update(Long id, RecorridoRequest request) {
        Recorrido entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recorrido", "id", id));

        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", request.getVehiculoId()));

        // Validar unicidad (excluyendo el registro actual)
        if (repository.existsByVehiculoIdAndFecha(request.getVehiculoId(), request.getFecha())) {
            repository.findByVehiculoIdAndFecha(request.getVehiculoId(), request.getFecha()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BusinessException("Ya existe un recorrido para el vehiculo con id "
                            + request.getVehiculoId() + " en la fecha " + request.getFecha());
                }
            });
        }

        // Calcular el nuevo consumo, redondeado a 2 decimales
        double nuevoConsumo = BigDecimal.valueOf(vehiculo.getIndiceConsumo() * request.getKilometros() / 100.0)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Si es el mismo vehiculo, verificar disponibilidad de combustible
        // considerando que se restaura el consumo antiguo primero
        if (vehiculo.getId().equals(entity.getVehiculo().getId())) {
            double consumoAntiguo = entity.getConsumo() != null ? entity.getConsumo() : 0.0;
            double combustibleDisponible = vehiculo.getCombustible() + consumoAntiguo - nuevoConsumo;
            if (combustibleDisponible < 0) {
                throw new BusinessException("El recorrido no puede ser actualizado porque se consume mas combustible ("
                        + nuevoConsumo + ") que el disponible en el vehiculo ("
                        + (vehiculo.getCombustible() + consumoAntiguo) + ")");
            }
        } else {
            // Vehiculo diferente: validar contra el nuevo vehiculo
            double combustibleDisponible = vehiculo.getCombustible() - nuevoConsumo;
            if (combustibleDisponible < 0) {
                throw new BusinessException("El recorrido no puede ser actualizado porque se consume mas combustible ("
                        + nuevoConsumo + ") que el disponible en el vehiculo (" + vehiculo.getCombustible() + ")");
            }
        }

        // Restaurar odometro y combustible del vehiculo anterior
        Vehiculo vehiculoAnterior = entity.getVehiculo();
        BigInteger kilometrosAnteriores = BigInteger.valueOf(entity.getKilometros());
        vehiculoAnterior.setOdometro(vehiculoAnterior.getOdometro().subtract(kilometrosAnteriores));
        double consumoAntiguo = entity.getConsumo() != null ? entity.getConsumo() : 0.0;
        vehiculoAnterior.setCombustible(vehiculoAnterior.getCombustible() + consumoAntiguo);
        vehiculoRepository.save(vehiculoAnterior);

        // Actualizar la entidad
        entity.setVehiculo(vehiculo);
        entity.setFecha(request.getFecha());
        entity.setKilometros(request.getKilometros());
        // OdometroInicial es el odometro actual del vehiculo antes de sumar kilometros
        entity.setOdometroInicial(vehiculo.getOdometro());
        entity.setConsumo(nuevoConsumo);

        Recorrido saved = repository.save(entity);

        // Sumar kilometros al odometro y restar consumo al combustible del (posible nuevo) vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
        vehiculo.setCombustible(vehiculo.getCombustible() - nuevoConsumo);
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
        double consumo = entity.getConsumo() != null ? entity.getConsumo() : 0.0;
        vehiculo.setCombustible(vehiculo.getCombustible() + consumo);
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