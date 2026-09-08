package com.fleet.management.service.impl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.dto.empresa.EmpresaRequest;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.EmpresaMapper;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Municipio;
import com.fleet.management.model.Provincia;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.MunicipioRepository;
import com.fleet.management.repository.ProvinciaRepository;
import com.fleet.management.service.EmpresaService;
import com.fleet.management.service.PdfGenerationService;
import com.fleet.management.service.SubscriptionService;
import com.fleet.management.service.UserService;
import lombok.RequiredArgsConstructor;
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
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository repository;
    private final ProvinciaRepository provinciaRepository;
    private final MunicipioRepository municipioRepository;
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final EmpresaMapper mapper;
    private final PdfGenerationService pdfGenerationService;

    private static final String EMPRESA_ADMIN_CODIGO = "EMP-ADMIN";

    @Override
    @Transactional(readOnly = true)
    public Page<EmpresaResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
        }
        return repository.findAllByActivoTrueAndNombreContainingIgnoreCase(filter, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponse findById(Long id) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponse findByCodigo(String codigo) {
        Empresa entity = repository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "codigo", codigo));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public EmpresaResponse create(EmpresaRequest request) {
        if (repository.existsByCodigo(request.getCodigo())) {
            throw BusinessError.empresaYaExisteCodigo(request.getCodigo());
        }

        Provincia provincia = null;
        Municipio municipio = null;
        if (request.getProvinciaId() != null) {
            provincia = provinciaRepository.findById(request.getProvinciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));
        }
        if (request.getMunicipioId() != null) {
            municipio = municipioRepository.findById(request.getMunicipioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", request.getMunicipioId()));
            if (provincia != null && !municipio.getProvincia().getId().equals(provincia.getId())) {
                throw BusinessError.empresaMunicipioProvinciaIncompatible();
            }
        }

        Empresa entity = Empresa.builder()
                .codigo(request.getCodigo())
                .nombre(request.getNombre())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .provincia(provincia)
                .municipio(municipio)
                .activo(true)
                .build();
        entity = repository.save(entity);

        subscriptionService.createTrialSubscription(entity);
        userService.createAdminUser(entity);

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public EmpresaResponse update(Long id, EmpresaRequest request) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        validarEmpresaAdmin(entity);

        if (!entity.getCodigo().equals(request.getCodigo()) && repository.existsByCodigo(request.getCodigo())) {
            throw BusinessError.empresaYaExisteCodigo(request.getCodigo());
        }

        Provincia provincia = null;
        Municipio municipio = null;
        if (request.getProvinciaId() != null) {
            provincia = provinciaRepository.findById(request.getProvinciaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provincia", "id", request.getProvinciaId()));
        }
        if (request.getMunicipioId() != null) {
            municipio = municipioRepository.findById(request.getMunicipioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Municipio", "id", request.getMunicipioId()));
            if (provincia != null && !municipio.getProvincia().getId().equals(provincia.getId())) {
                throw BusinessError.empresaMunicipioProvinciaIncompatible();
            }
        }

        entity.setCodigo(request.getCodigo());
        entity.setNombre(request.getNombre());
        entity.setDireccion(request.getDireccion());
        entity.setTelefono(request.getTelefono());
        entity.setEmail(request.getEmail());
        entity.setProvincia(provincia);
        entity.setMunicipio(municipio);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Empresa entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", id));

        validarEmpresaAdmin(entity);

        entity.setActivo(false);
        repository.save(entity);
    }

    private void validarEmpresaAdmin(Empresa entity) {
        if (EMPRESA_ADMIN_CODIGO.equals(entity.getCodigo())) {
            throw BusinessError.empresaAdminNoModificable();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarReportePdf() {
        // 1. Obtener todas las empresas activas
        List<Empresa> empresas = repository.findAllByActivoTrueOrderByNombreAsc();

        // 2. Mapear datos para la plantilla
        List<Map<String, Object>> empresasDto = empresas.stream()
                .map(e -> {
                    Map<String, Object> emp = new HashMap<>();
                    emp.put("codigo", e.getCodigo());
                    emp.put("nombre", e.getNombre());
                    emp.put("direccion", e.getDireccion() != null ? e.getDireccion() : "-");
                    emp.put("telefono", e.getTelefono() != null ? e.getTelefono() : "-");
                    emp.put("email", e.getEmail() != null ? e.getEmail() : "-");
                    emp.put("provincia", e.getProvincia() != null ? e.getProvincia().getNombre() : "-");
                    emp.put("municipio", e.getMunicipio() != null ? e.getMunicipio().getNombre() : "-");
                    return emp;
                })
                .collect(Collectors.toList());

        // 3. Fecha de impresión
        String fechaImpresion = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // 4. Construir modelo para Thymeleaf
        Map<String, Object> model = new HashMap<>();
        model.put("empresas", empresasDto);
        model.put("fechaImpresion", fechaImpresion);
        model.put("totalEmpresas", empresasDto.size());

        // 5. Generar PDF
        return pdfGenerationService.generatePdf("reports/empresas-listado", model);
    }
}