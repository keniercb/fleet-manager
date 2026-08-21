package com.fleet.management.service.impl;

import com.fleet.management.dto.currency.CurrencyResponse;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleRequest;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.model.TarjetaCombustible;
import com.fleet.management.repository.CurrencyRepository;
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

    @Override
    @Transactional(readOnly = true)
    public Page<TarjetaCombustibleResponse> findAll(Pageable pageable) {
        return repository.findAllByActivoTrue(pageable).map(this::toResponse);
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
    @Transactional
    public TarjetaCombustibleResponse create(TarjetaCombustibleRequest request) {
        if (repository.existsByNumero(request.getNumero())) {
            throw new BusinessException("Ya existe una tarjeta de combustible con el numero: " + request.getNumero());
        }

        var currency = currencyRepository.findById(request.getCurrencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", request.getCurrencyId()));

        TarjetaCombustible entity = TarjetaCombustible.builder()
                .numero(request.getNumero())
                .saldo(request.getSaldo())
                .currency(currency)
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

        entity.setNumero(request.getNumero());
        entity.setSaldo(request.getSaldo());
        entity.setCurrency(currency);
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
}