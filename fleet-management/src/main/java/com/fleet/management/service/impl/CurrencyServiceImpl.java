package com.fleet.management.service.impl;

import com.fleet.management.dto.currency.CurrencyRequest;
import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.Currency;
import com.fleet.management.repository.CurrencyRepository;
import com.fleet.management.service.CurrencyService;
import com.fleet.management.util.AuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<CurrencyResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse findById(Long id) {
        Currency entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", id));
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse findByIsoCode(String isoCode) {
        Currency entity = repository.findByIsoCode(isoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "isoCode", isoCode));
        return toResponse(entity);
    }

    @Override
    @Transactional
    public CurrencyResponse create(CurrencyRequest request) {
        if (repository.existsByIsoCode(request.getIsoCode())) {
            throw new BusinessException("Ya existe una moneda con el codigo ISO: " + request.getIsoCode());
        }
        Currency entity = Currency.builder()
                .isoCode(request.getIsoCode())
                .descripcion(request.getDescripcion())
                .activo(true)
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CurrencyResponse update(Long id, CurrencyRequest request) {
        Currency entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", id));

        if (!entity.getIsoCode().equals(request.getIsoCode()) && repository.existsByIsoCode(request.getIsoCode())) {
            throw new BusinessException("Ya existe una moneda con el codigo ISO: " + request.getIsoCode());
        }

        entity.setIsoCode(request.getIsoCode());
        entity.setDescripcion(request.getDescripcion());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Currency entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }

    private CurrencyResponse toResponse(Currency entity) {
        return CurrencyResponse.builder()
                .id(entity.getId())
                .isoCode(entity.getIsoCode())
                .descripcion(entity.getDescripcion())
                .activo(entity.getActivo())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaActualizacion(entity.getFechaActualizacion())
                .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
                .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
                .build();
    }
}