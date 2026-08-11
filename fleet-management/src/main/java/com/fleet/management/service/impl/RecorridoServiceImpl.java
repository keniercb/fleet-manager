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

        Recorrido entity = Recorrido.builder()
                .vehiculo(vehiculo)
                .fecha(request.getFecha())
                .kilometros(request.getKilometros())
                .odometroInicial(request.getOdometroInicial())
                .activo(true)
                .build();

        Recorrido saved = repository.save(entity);

        // Sumar kilometros al odometro del vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
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

        // Restar los kilometros antiguos al odometro del vehiculo asociado previamente
        Vehiculo vehiculoAnterior = entity.getVehiculo();
        BigInteger kilometrosAnteriores = BigInteger.valueOf(entity.getKilometros());
        vehiculoAnterior.setOdometro(vehiculoAnterior.getOdometro().subtract(kilometrosAnteriores));
        vehiculoRepository.save(vehiculoAnterior);

        // Actualizar la entidad
        entity.setVehiculo(vehiculo);
        entity.setFecha(request.getFecha());
        entity.setKilometros(request.getKilometros());
        entity.setOdometroInicial(request.getOdometroInicial());

        Recorrido saved = repository.save(entity);

        // Sumar los nuevos kilometros al odometro del (posible nuevo) vehiculo
        vehiculo.setOdometro(vehiculo.getOdometro().add(BigInteger.valueOf(request.getKilometros())));
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
                .activo(v.getActivo())
                .fechaCreacion(v.getFechaCreacion())
                .fechaActualizacion(v.getFechaActualizacion())
                .build();
    }
}