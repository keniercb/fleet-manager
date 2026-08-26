package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.vehiculo.VehiculoRequest;
import com.fleet.management.dto.vehiculo.VehiculoResponse;
import com.fleet.management.dto.reporte.EmpresaReporteDto;
import com.fleet.management.dto.reporte.VehiculoFilaReporteDto;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Chofer;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Marca;
import com.fleet.management.model.TipoCombustible;
import com.fleet.management.model.TipoVehiculo;
import com.fleet.management.model.Vehiculo;
import com.fleet.management.model.Subscription;
import com.fleet.management.repository.ChoferRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.MarcaRepository;
import com.fleet.management.repository.TipoCombustibleRepository;
import com.fleet.management.repository.TipoVehiculoRepository;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.security.AuthenticatedUser;
import com.fleet.management.service.PdfGenerationService;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.service.VehiculoService;
import com.fleet.management.mapper.VehiculoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class VehiculoServiceImpl implements VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final EmpresaRepository empresaRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final MarcaRepository marcaRepository;
    private final TipoCombustibleRepository tipoCombustibleRepository;
    private final ChoferRepository choferRepository;
    private final SubscriptionService subscriptionService;
    private final PdfGenerationService pdfGenerationService;
    private final VehiculoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return vehiculoRepository.findAllByActivoTrue(pageable)
                    .map(mapper::toResponse);
        }
        return vehiculoRepository.findAllByActivoTrueAndMatriculaOrNumeroMotor(filter, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse findById(Long id) {
        Vehiculo entity = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehiculo", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByChoferId(Long choferId, Pageable pageable) {
        return vehiculoRepository.findByChoferId(choferId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable) {
        return vehiculoRepository.findByTipoVehiculoId(tipoVehiculoId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable) {
        return vehiculoRepository.findByTipoCombustibleId(tipoCombustibleId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findSinChoferAsignado(Pageable pageable) {
        return vehiculoRepository.findSinChoferAsignado(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculoResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return vehiculoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                    .map(mapper::toResponse);
        }
        return vehiculoRepository.findByEmpresaIdAndActivoTrueAndMatriculaOrNumeroMotor(empresaId, filter, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public VehiculoResponse create(VehiculoRequest request) {
        Subscription activeSubscription = subscriptionService.getActiveSubscriptionEntity(request.getEmpresaId())
                .orElseThrow(() -> new BusinessException("La empresa no tiene una suscripcion activa"));

        Integer maxVehiculos = activeSubscription.getMaxVehiculos();
        if (maxVehiculos != null && activeSubscription.getCurrentVehicleCount() >= maxVehiculos) {
            throw new BusinessException("No se puede crear el vehiculo. Se ha alcanzado el limite de "
                    + maxVehiculos + " vehiculos");
        }

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
        Vehiculo saved = vehiculoRepository.save(entity);
        subscriptionService.incrementVehicleCount(activeSubscription.getId());
        return mapper.toResponse(saved);
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
        return mapper.toResponse(vehiculoRepository.save(entity));
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

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePdf() {
        // 1. Obtener la empresa del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authUser)) {
            throw new BusinessException("No se pudo determinar la empresa del usuario autenticado");
        }
        Empresa empresaRef = authUser.getUser().getEmpresa();
        if (empresaRef == null) {
            throw new BusinessException("El usuario no tiene una empresa asociada");
        }

        // Fetch empresa dentro de la sesion actual para evitar LazyInitializationException
        // (el proxy del AuthenticatedUser pertenece a la sesion del filtro de autenticacion)
        Empresa empresa = empresaRepository.findById(empresaRef.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", empresaRef.getId()));

        // 2. Obtener vehiculos activos de la empresa
        List<Vehiculo> vehiculos = vehiculoRepository.findByEmpresaIdAndActivoTrueOrderByMatriculaAsc(empresa.getId());

        // 3. Mapear datos del encabezado
        EmpresaReporteDto empresaDto = EmpresaReporteDto.builder()
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .direccion(empresa.getDireccion())
                .telefono(empresa.getTelefono())
                .email(empresa.getEmail())
                .provincia(empresa.getProvincia() != null ? empresa.getProvincia().getNombre() : null)
                .municipio(empresa.getMunicipio() != null ? empresa.getMunicipio().getNombre() : null)
                .build();

        // 4. Mapear datos de vehiculos
        List<VehiculoFilaReporteDto> vehiculosDto = vehiculos.stream()
                .map(v -> VehiculoFilaReporteDto.builder()
                        .tipoVehiculo(v.getTipoVehiculo().getNombre())
                        .matricula(v.getMatricula())
                        .marca(v.getMarca().getNombre())
                        .modelo(v.getModelo())
                        .numeroMotor(v.getNumeroMotor())
                        .odometro(v.getOdometro() != null ? v.getOdometro().toString() : "0")
                        .combustibleLitros(v.getCombustible() != null ? v.getCombustible().toPlainString() : "0.00")
                        .build())
                .collect(Collectors.toList());

        // 5. Fecha de impresion
        String fechaImpresion = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // 6. Construir modelo para Thymeleaf
        Map<String, Object> model = new HashMap<>();
        model.put("empresa", empresaDto);
        model.put("vehiculos", vehiculosDto);
        model.put("fechaImpresion", fechaImpresion);

        // 7. Generar PDF
        return pdfGenerationService.generatePdf("reports/vehiculos-listado", model);
    }
}