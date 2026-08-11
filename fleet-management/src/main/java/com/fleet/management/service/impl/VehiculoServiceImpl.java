package com.fleet.management.service.impl;

import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.Marca;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.model.Vehiculo;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.MarcaRepository;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.service.VehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final MarcaRepository marcaRepository;
    private final TipoCombustibleRepository tipoCombustibleRepository;
    private final ChoferRepository choferRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> findAll() {
        return vehiculoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse findById(Long id) {
        Vehiculo entity = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> findByChoferId(Long choferId) {
        return vehiculoRepository.findByChoferId(choferId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId) {
        return vehiculoRepository.findByTipoVehiculoId(tipoVehiculoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId) {
        return vehiculoRepository.findByTipoCombustibleId(tipoCombustibleId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> findSinChoferAsignado() {
        return vehiculoRepository.findSinChoferAsignado().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VehiculoResponse create(VehiculoRequest request) {
        validateUniqueFields(request, null);

        TipoVehiculo tipoVehiculo = tipoVehiculoRepository.findById(request.getTipoVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", request.getTipoVehiculoId()));

        TipoCombustible tipoCombustible = tipoCombustibleRepository.findById(request.getTipoCombustibleId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", request.getTipoCombustibleId()));

        Chofer chofer = null;
        if (request.getChoferId() != null) {
            chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));
        }

        Marca marca = marcaRepository.findById(request.getMarcaId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", request.getMarcaId()));

        Vehiculo entity = Vehiculo.builder()
                .tipoVehiculo(tipoVehiculo)
                .marca(marca)
                .chofer(chofer)
                .tipoCombustible(tipoCombustible)
                .matricula(request.getMatricula())
                .numeroMotor(request.getNumeroMotor())
                .odometro(request.getOdometro())
                .combustible(request.getCombustible())
                .ultimoMantenimiento(request.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(request.getOdometroUltimoMantenimiento())
                .activo(true)
                .build();
        return toResponse(vehiculoRepository.save(entity));
    }

    @Override
    @Transactional
    public VehiculoResponse update(Long id, VehiculoRequest request) {
        Vehiculo entity = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", id));

        validateUniqueFields(request, id);

        TipoVehiculo tipoVehiculo = tipoVehiculoRepository.findById(request.getTipoVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", request.getTipoVehiculoId()));

        TipoCombustible tipoCombustible = tipoCombustibleRepository.findById(request.getTipoCombustibleId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", request.getTipoCombustibleId()));

        Chofer chofer = null;
        if (request.getChoferId() != null) {
            chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));
        }

        Marca marca = marcaRepository.findById(request.getMarcaId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", request.getMarcaId()));

        entity.setTipoVehiculo(tipoVehiculo);
        entity.setMarca(marca);
        entity.setChofer(chofer);
        entity.setTipoCombustible(tipoCombustible);
        entity.setMatricula(request.getMatricula());
        entity.setNumeroMotor(request.getNumeroMotor());
        entity.setOdometro(request.getOdometro());
        entity.setCombustible(request.getCombustible());
        entity.setUltimoMantenimiento(request.getUltimoMantenimiento());
        entity.setOdometroUltimoMantenimiento(request.getOdometroUltimoMantenimiento());
        return toResponse(vehiculoRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Vehiculo entity = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", id));
        entity.setActivo(false);
        vehiculoRepository.save(entity);
    }

    private void validateUniqueFields(VehiculoRequest request, Long excludeId) {
        if (vehiculoRepository.existsByMatricula(request.getMatricula())) {
            vehiculoRepository.findByMatricula(request.getMatricula()).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw new BusinessException("Ya existe un vehiculo con la matricula: " + request.getMatricula());
                }
            });
        }
        if (vehiculoRepository.existsByNumeroMotor(request.getNumeroMotor())) {
            vehiculoRepository.findByNumeroMotor(request.getNumeroMotor()).ifPresent(existing -> {
                if (!existing.getId().equals(excludeId)) {
                    throw new BusinessException("Ya existe un vehiculo con el numero de motor: " + request.getNumeroMotor());
                }
            });
        }
    }

    private VehiculoResponse toResponse(Vehiculo entity) {
        TipoVehiculo tv = entity.getTipoVehiculo();
        TipoVehiculoResponse tipoVehiculoResp = TipoVehiculoResponse.builder()
                .id(tv.getId())
                .nombre(tv.getNombre())
                .descripcion(tv.getDescripcion())
                .activo(tv.getActivo())
                .fechaCreacion(tv.getFechaCreacion())
                .fechaActualizacion(tv.getFechaActualizacion())
                .build();

        TipoCombustible tc = entity.getTipoCombustible();
        TipoCombustibleResponse tipoCombustibleResp = TipoCombustibleResponse.builder()
                .id(tc.getId())
                .codigo(tc.getCodigo())
                .denominacion(tc.getDenominacion())
                .descripcion(tc.getDescripcion())
                .activo(tc.getActivo())
                .fechaCreacion(tc.getFechaCreacion())
                .fechaActualizacion(tc.getFechaActualizacion())
                .build();

        ChoferResponse choferResp = null;
        if (entity.getChofer() != null) {
            Chofer c = entity.getChofer();
            choferResp = ChoferResponse.builder()
                    .id(c.getId())
                    .nombre(c.getNombre())
                    .apellidos(c.getApellidos())
                    .carneIdentidad(c.getCarneIdentidad())
                    .numeroLicencia(c.getNumeroLicencia())
                    .fechaNacimiento(c.getFechaNacimiento())
                    .activo(c.getActivo())
                    .fechaCreacion(c.getFechaCreacion())
                    .fechaActualizacion(c.getFechaActualizacion())
                    .build();
        }

        Marca m = entity.getMarca();
        MarcaResponse marcaResp = MarcaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .paisOrigen(m.getPaisOrigen())
                .activo(m.getActivo())
                .fechaCreacion(m.getFechaCreacion())
                .fechaActualizacion(m.getFechaActualizacion())
                .build();

        return VehiculoResponse.builder()
                .id(entity.getId())
                .tipoVehiculo(tipoVehiculoResp)
                .marca(marcaResp)
                .chofer(choferResp)
                .tipoCombustible(tipoCombustibleResp)
                .matricula(entity.getMatricula())
                .numeroMotor(entity.getNumeroMotor())
                .odometro(entity.getOdometro())
                .combustible(entity.getCombustible())
                .ultimoMantenimiento(entity.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(entity.getOdometroUltimoMantenimiento())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .build();
    }
}