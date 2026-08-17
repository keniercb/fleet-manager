package com.fleet.management.repository;

import com.fleet.management.model.TipoCombustible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoCombustibleRepository extends JpaRepository<TipoCombustible, Long> {

    Optional<TipoCombustible> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Page<TipoCombustible> findAllByActivoTrue(Pageable pageable);
}
