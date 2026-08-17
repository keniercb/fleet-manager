package com.fleet.management.repository;

import com.fleet.management.model.CategoriaLicencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaLicenciaRepository extends JpaRepository<CategoriaLicencia, Long> {

    Optional<CategoriaLicencia> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Page<CategoriaLicencia> findAllByActivoTrue(Pageable pageable);
}