package com.fleet.management.repository;

import com.fleet.management.model.Provincia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProvinciaRepository extends JpaRepository<Provincia, Long> {

    Optional<Provincia> findByCodigo(Integer codigo);

    boolean existsByCodigo(Integer codigo);

    Page<Provincia> findAllByActivoTrue(Pageable pageable);
}
