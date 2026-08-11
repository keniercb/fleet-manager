package com.fleet.management.service;

import com.fleet.management.dto.marca.MarcaRequest;
import com.fleet.management.dto.marca.MarcaResponse;

import java.util.List;

public interface MarcaService {

    List<MarcaResponse> findAll();

    MarcaResponse findById(Long id);

    MarcaResponse create(MarcaRequest request);

    MarcaResponse update(Long id, MarcaRequest request);

    void delete(Long id);
}