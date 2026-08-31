package com.fleet.management.service.impl;

import com.fleet.management.dto.reporte.MantenimientoReporteResponse;
import com.fleet.management.dto.reporte.MantenimientoReporteResponse.EmpresaResumidoDTO;
import com.fleet.management.dto.reporte.MantenimientoReporteResponse.VehiculoResumidoDTO;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.model.Vehiculo;
import com.fleet.management.repository.VehiculoRepository;
import com.fleet.management.security.AuthenticatedUser;
import com.fleet.management.service.ReporteMantenimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReporteMantenimientoServiceImpl implements ReporteMantenimientoService {

    private final VehiculoRepository vehiculoRepository;

    @Value("${fleet.reporte.mantenimiento.umbral-km:10000}")
    private int umbralKm;

    @Value("${fleet.reporte.mantenimiento.porcentaje-proximo:80}")
    private int porcentajeProximo;

    @Override
    @Transactional(readOnly = true)
    public Page<MantenimientoReporteResponse> reporteMantenimiento(Pageable pageable) {
        Long empresaId = resolveEmpresaId();

        // Obtener vehiculos activos de la empresa con paginacion
        Page<Vehiculo> vehiculos = vehiculoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable);

        List<MantenimientoReporteResponse> content = vehiculos.getContent().stream()
                .map(this::buildResponse)
                .toList();

        return new PageImpl<>(content, pageable, vehiculos.getTotalElements());
    }

    private MantenimientoReporteResponse buildResponse(Vehiculo v) {
        BigInteger odometroActual = v.getOdometro() != null ? v.getOdometro() : BigInteger.ZERO;
        BigInteger odometroUltMant = v.getOdometroUltimoMantenimiento();

        // kmDesdeMantenimiento: si nunca tuvo mantenimiento, usar el odometro actual
        BigInteger kmDesdeMantenimiento;
        if (odometroUltMant == null) {
            kmDesdeMantenimiento = odometroActual;
        } else {
            kmDesdeMantenimiento = odometroActual.subtract(odometroUltMant);
            if (kmDesdeMantenimiento.compareTo(BigInteger.ZERO) < 0) {
                kmDesdeMantenimiento = BigInteger.ZERO;
            }
        }

        // Dias transcurridos desde ultimo mantenimiento
        Long diasTranscurridos = null;
        if (v.getUltimoMantenimiento() != null) {
            diasTranscurridos = ChronoUnit.DAYS.between(v.getUltimoMantenimiento(), LocalDate.now());
        }

        // Clasificacion del estado
        String estado = clasificarEstado(kmDesdeMantenimiento);

        // Sub-DTOs
        VehiculoResumidoDTO vehiculoDto = VehiculoResumidoDTO.builder()
                .id(v.getId())
                .matricula(v.getMatricula())
                .modelo(v.getModelo())
                .marcaNombre(v.getMarca() != null ? v.getMarca().getNombre() : null)
                .tipoVehiculoNombre(v.getTipoVehiculo() != null ? v.getTipoVehiculo().getNombre() : null)
                .build();

        EmpresaResumidoDTO empresaDto = EmpresaResumidoDTO.builder()
                .id(v.getEmpresa().getId())
                .codigo(v.getEmpresa().getCodigo())
                .nombre(v.getEmpresa().getNombre())
                .build();

        return MantenimientoReporteResponse.builder()
                .vehiculoResumido(vehiculoDto)
                .empresaResumida(empresaDto)
                .fechaUltimoMantenimiento(v.getUltimoMantenimiento())
                .odometroUltimoMantenimiento(odometroUltMant)
                .odometroActual(odometroActual)
                .kmDesdeMantenimiento(kmDesdeMantenimiento)
                .umbralKm(umbralKm)
                .estado(estado)
                .diasTranscurridos(diasTranscurridos)
                .build();
    }

    /**
     * Clasifica el estado del mantenimiento segun el umbral configurado:
     * - AL DIA: kmDesdeMantenimiento < 80% del umbral
     * - PROXIMO: kmDesdeMantenimiento >= 80% del umbral pero no lo supera
     * - VENCIDO: kmDesdeMantenimiento >= umbral
     */
    private String clasificarEstado(BigInteger kmDesdeMantenimiento) {
        double km = kmDesdeMantenimiento.doubleValue();
        double limiteProximo = umbralKm * (porcentajeProximo / 100.0);

        if (km >= umbralKm) {
            return "VENCIDO";
        } else if (km >= limiteProximo) {
            return "PROXIMO";
        } else {
            return "AL DIA";
        }
    }

    private Long resolveEmpresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authUser)) {
            throw new BusinessException("No se pudo determinar la empresa del usuario autenticado");
        }
        var empresaRef = authUser.getUser().getEmpresa();
        if (empresaRef == null) {
            throw new BusinessException("El usuario no tiene una empresa asociada");
        }
        return empresaRef.getId();
    }
}
