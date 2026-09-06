package com.fleet.management.repository;

import com.fleet.management.model.Municipio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

    boolean existsByProvinciaIdAndCodigo(Long provinciaId, Integer codigo);

    Page<Municipio> findAllByActivoTrue(Pageable pageable);

    Page<Municipio> findByProvinciaIdAndActivoTrue(Long provinciaId, Pageable pageable);

    List<Municipio> findByProvinciaIdAndActivoTrueOrderByIdAsc(Long provinciaId);
}
