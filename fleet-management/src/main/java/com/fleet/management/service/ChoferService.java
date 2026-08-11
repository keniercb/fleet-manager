package com.fleet.management.service;

import com.fleet.management.dto.chofer.ChoferRequest;
import com.fleet.management.dto.chofer.ChoferResponse;

import java.util.List;

public interface ChoferService {

    List<ChoferResponse> findAll();

    ChoferResponse findById(Long id);

    ChoferResponse create(ChoferRequest request);

    ChoferResponse update(Long id, ChoferRequest request);

    void delete(Long id);
}
