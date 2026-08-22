package com.fleet.management.service.impl;

import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.dto.empresa.EmpresaResponse;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleRequest;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.TarjetaCombustible;
import com.fleet.management.repository.CurrencyRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.TarjetaCombustibleRepository;
import com.fleet.management.service.TarjetaCombustibleService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TarjetaCombustibleServiceImpl implements TarjetaCombustibleService {

    private final TarjetaCombustibleRepository repository;
    private final CurrencyRepository currencyRepository;
    private final EmpresaRepository empresaRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TarjetaCombustibleResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return repository.findAllByActivoTrue(pageable).map(this::toResponse);
        }
        return repository.findAllByActivoTrueAndNumeroContainingIgnoreCase(filter, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TarjetaCombustibleResponse findById(Long id) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TarjetaCombustibleResponse findByNumero(String numero) {
        TarjetaCombustible entity = repository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "numero", numero));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TarjetaCombustibleResponse> findByEmpresaId(Long empresaId, Pageable pageable) {
        return repository.findByEmpresaIdAndActivoTrue(empresaId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public TarjetaCombustibleResponse create(TarjetaCombustibleRequest request) {
        if (repository.existsByNumero(request.getNumero())) {
            throw new BusinessException("Ya existe una tarjeta de combustible con el numero: " + request.getNumero());
        }

        var currency = currencyRepository.findById(request.getCurrencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", request.getCurrencyId()));

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        TarjetaCombustible entity = TarjetaCombustible.builder()
                .numero(request.getNumero())
                .saldo(request.getSaldo())
                .currency(currency)
                .empresa(empresa)
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TarjetaCombustibleResponse update(Long id, TarjetaCombustibleRequest request) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));

        if (!entity.getNumero().equals(request.getNumero()) && repository.existsByNumero(request.getNumero())) {
            throw new BusinessException("Ya existe una tarjeta de combustible con el numero: " + request.getNumero());
        }

        var currency = currencyRepository.findById(request.getCurrencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", request.getCurrencyId()));

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        entity.setNumero(request.getNumero());
        entity.setSaldo(request.getSaldo());
        entity.setCurrency(currency);
        entity.setEmpresa(empresa);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private TarjetaCombustibleResponse toResponse(TarjetaCombustible entity) {
        return TarjetaCombustibleResponse.builder()
                .id(entity.getId())
                .numero(entity.getNumero())
                .saldo(entity.getSaldo())
                .currency(toCurrencyResponse(entity.getCurrency()))
                .empresa(toEmpresaResponse(entity.getEmpresa()))
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }

    private CurrencyResponse toCurrencyResponse(com.fleet.management.model.Currency currency) {
        return CurrencyResponse.builder()
                .id(currency.getId())
                .isoCode(currency.getIsoCode())
                .descripcion(currency.getDescripcion())
                .activo(currency.getActivo())
                .build();
    }

    private EmpresaResponse toEmpresaResponse(Empresa empresa) {
        return EmpresaResponse.builder()
                .id(empresa.getId())
                .codigo(empresa.getCodigo())
                .nombre(empresa.getNombre())
                .activo(empresa.getActivo())
                .build();
    }
}