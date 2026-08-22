package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.chofer.ChoferResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.marca.MarcaResponse;
import com.fleet.management.dto.tipocombustible.TipoCombustibleResponse;
import com.fleet.management.dto.tipovehiculo.TipoVehiculoResponse;
import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Marca;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.model.Vehiculo;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.MarcaRepository;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.service.VehiculoService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final EmpresaRepository empresaRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final MarcaRepository marcaRepository;
    private final TipoCombustibleRepository tipoCombustibleRepository;
    private final ChoferRepository choferRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return vehiculoRepository.findAllByActivoTrue(pageable)
                    .map(this::toResponse);
        }
        return vehiculoRepository.findAllByActivoTrueAndMatriculaOrNumeroMotor(filter, pageable)
                .map(this::toResponse);
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
    public Page<VehiculoResponse> findByChoferId(Long choferId, Pageable pageable) {
        return vehiculoRepository.findByChoferId(choferId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable) {
        return vehiculoRepository.findByTipoVehiculoId(tipoVehiculoId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable) {
        return vehiculoRepository.findByTipoCombustibleId(tipoCombustibleId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findSinChoferAsignado(Pageable pageable) {
        return vehiculoRepository.findSinChoferAsignado(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByEmpresaId(Long empresaId, Pageable pageable) {
        return vehiculoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public VehiculoResponse create(VehiculoRequest request) {
        validateUniqueFields(request, null);

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        TipoVehiculo tipoVehiculo = tipoVehiculoRepository.findById(request.getTipoVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", request.getTipoVehiculoId()));

        Marca marca = marcaRepository.findById(request.getMarcaId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", request.getMarcaId()));

        TipoCombustible tipoCombustible = tipoCombustibleRepository.findById(request.getTipoCombustibleId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", request.getTipoCombustibleId()));

        Chofer chofer = null;
        if (request.getChoferId() != null) {
            chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));
        }

        Vehiculo entity = Vehiculo.builder()
                .empresa(empresa)
                .tipoVehiculo(tipoVehiculo)
                .marca(marca)
                .chofer(chofer)
                .tipoCombustible(tipoCombustible)
                .matricula(request.getMatricula())
                .modelo(request.getModelo())
                .numeroMotor(request.getNumeroMotor())
                .odometro(request.getOdometro())
                .combustible(request.getCombustible())
                .ultimoMantenimiento(request.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(request.getOdometroUltimoMantenimiento())
                .indiceConsumo(request.getIndiceConsumo())
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

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        TipoVehiculo tipoVehiculo = tipoVehiculoRepository.findById(request.getTipoVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoVehiculo", "id", request.getTipoVehiculoId()));

        Marca marca = marcaRepository.findById(request.getMarcaId())
                .orElseThrow(() -> new ResourceNotFoundException("Marca", "id", request.getMarcaId()));

        TipoCombustible tipoCombustible = tipoCombustibleRepository.findById(request.getTipoCombustibleId())
                .orElseThrow(() -> new ResourceNotFoundException("TipoCombustible", "id", request.getTipoCombustibleId()));

        Chofer chofer = null;
        if (request.getChoferId() != null) {
            chofer = choferRepository.findById(request.getChoferId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chofer", "id", request.getChoferId()));
        }

        entity.setEmpresa(empresa);
        entity.setTipoVehiculo(tipoVehiculo);
        entity.setMarca(marca);
        entity.setChofer(chofer);
        entity.setTipoCombustible(tipoCombustible);
        entity.setMatricula(request.getMatricula());
        entity.setModelo(request.getModelo());
        entity.setNumeroMotor(request.getNumeroMotor());
        entity.setOdometro(request.getOdometro());
        entity.setCombustible(request.getCombustible());
        entity.setUltimoMantenimiento(request.getUltimoMantenimiento());
        entity.setOdometroUltimoMantenimiento(request.getOdometroUltimoMantenimiento());
        entity.setIndiceConsumo(request.getIndiceConsumo());
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

    private EmpresaResponse toEmpresaResponse(Empresa empresa) {
        return EmpresaResponse.builder()
                .id(empresa.getId())
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .email(empresa.getEmail())
                .activo(empresa.getActivo())
                .fechaCreacion(empresa.getFechaCreacion())
                .fechaActualizacion(empresa.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(empresa.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(empresa.getModificadoPor()))
                .build();
    }

    private VehiculoResponse toResponse(Vehiculo entity) {
        EmpresaResponse empresaResp = toEmpresaResponse(entity.getEmpresa());

        TipoVehiculo tv = entity.getTipoVehiculo();
        TipoVehiculoResponse tipoVehiculoResp = TipoVehiculoResponse.builder()
                .id(tv.getId())
                .nombre(tv.getNombre())
                .descripcion(tv.getDescripcion())
                .activo(tv.getActivo())
                .fechaCreacion(tv.getFechaCreacion())
                .fechaActualizacion(tv.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(tv.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(tv.getModificadoPor()))
                .build();

        Marca m = entity.getMarca();
        MarcaResponse marcaResp = MarcaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .paisOrigen(m.getPaisOrigen())
                .activo(m.getActivo())
                .fechaCreacion(m.getFechaCreacion())
                .fechaActualizacion(m.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(m.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(m.getModificadoPor()))
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
                .creadoPor(AuditMapper.toAuditResponse(tc.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(tc.getModificadoPor()))
                .build();

        ChoferResponse choferResp = null;
        if (entity.getChofer() != null) {
            Chofer c = entity.getChofer();
            choferResp = ChoferResponse.builder()
                    .id(c.getId())
                    .empresa(toEmpresaResponse(c.getEmpresa()))
                    .nombre(c.getNombre())
                    .apellidos(c.getApellidos())
                    .carneIdentidad(c.getCarneIdentidad())
                    .numeroLicencia(c.getNumeroLicencia())
                    .fechaNacimiento(c.getFechaNacimiento())
                    .activo(c.getActivo())
                    .fechaCreacion(c.getFechaCreacion())
                    .fechaActualizacion(c.getFechaActualizacion())
                    .creadoPor(AuditMapper.toAuditResponse(c.getCreadoPor()))
                    .modificadoPor(AuditMapper.toAuditResponse(c.getModificadoPor()))
                    .build();
        }

        return VehiculoResponse.builder()
                .id(entity.getId())
                .empresa(empresaResp)
                .tipoVehiculo(tipoVehiculoResp)
                .marca(marcaResp)
                .chofer(choferResp)
                .tipoCombustible(tipoCombustibleResp)
                .matricula(entity.getMatricula())
                .modelo(entity.getModelo())
                .numeroMotor(entity.getNumeroMotor())
                .odometro(entity.getOdometro())
                .combustible(entity.getCombustible())
                .ultimoMantenimiento(entity.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(entity.getOdometroUltimoMantenimiento())
                .indiceConsumo(entity.getIndiceConsumo())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}
