package com.fleet.management.service;

import com.fleet.management.dto.currency.CurrencyRequest;
import com.fleet.management.dto.currency.CurrencyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CurrencyService {

    Page<CurrencyResponse> findAll(Pageable pageable);

    CurrencyResponse findById(Long id);

    CurrencyResponse findByIsoCode(String isoCode);

    CurrencyResponse create(CurrencyRequest request);

    CurrencyResponse update(Long id, CurrencyRequest request);

    void delete(Long id);
}