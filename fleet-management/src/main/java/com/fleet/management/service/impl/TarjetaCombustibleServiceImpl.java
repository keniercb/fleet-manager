package com.fleet.management.service.impl;

import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleRequest;
import com.fleet.management.dto.tarjetacombustible.TarjetaCombustibleResponse;
import com.fleet.management.exception.BusinessError;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.exception.ResourceNotFoundException;
import com.fleet.management.mapper.TarjetaCombustibleMapper;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.TarjetaCombustible;
import com.fleet.management.repository.CurrencyRepository;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.TarjetaCombustibleRepository;
import com.fleet.management.service.TarjetaCombustibleService;
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
    private final TarjetaCombustibleMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TarjetaCombustibleResponse> findAll(String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return repository.findAllByActivoTrue(pageable).map(mapper::toResponse);
        }
        return repository.findAllByActivoTrueAndNumeroContainingIgnoreCase(filter, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TarjetaCombustibleResponse findById(Long id) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public TarjetaCombustibleResponse findByNumero(String numero) {
        TarjetaCombustible entity = repository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "numero", numero));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TarjetaCombustibleResponse> findByEmpresaId(Long empresaId, String filter, Pageable pageable) {
        if (filter == null || filter.isBlank()) {
            return repository.findByEmpresaIdAndActivoTrue(empresaId, pageable).map(mapper::toResponse);
        }
        return repository.findByEmpresaIdAndActivoTrueAndNumeroContainingIgnoreCase(empresaId, filter, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional
    public TarjetaCombustibleResponse create(TarjetaCombustibleRequest request) {
        if (repository.existsByNumero(request.getNumero())) {
            throw BusinessError.tarjetaYaExisteNumero(request.getNumero());
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
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public TarjetaCombustibleResponse update(Long id, TarjetaCombustibleRequest request) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));

        if (!entity.getNumero().equals(request.getNumero()) && repository.existsByNumero(request.getNumero())) {
            throw BusinessError.tarjetaYaExisteNumero(request.getNumero());
        }

        var currency = currencyRepository.findById(request.getCurrencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Currency", "id", request.getCurrencyId()));

        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", "id", request.getEmpresaId()));

        entity.setNumero(request.getNumero());
        entity.setSaldo(request.getSaldo());
        entity.setCurrency(currency);
        entity.setEmpresa(empresa);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TarjetaCombustible entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TarjetaCombustible", "id", id));
        entity.setActivo(false);
        repository.save(entity);
    }
}
