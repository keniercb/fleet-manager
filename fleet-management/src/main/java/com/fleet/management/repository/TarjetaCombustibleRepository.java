package com.fleet.management.repository;

import com.fleet.management.model.TarjetaCombustible;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TarjetaCombustibleRepository extends JpaRepository<TarjetaCombustible, Long> {

    Optional<TarjetaCombustible> findByNumero(String numero);

    boolean existsByNumero(String numero);

    Page<TarjetaCombustible> findAllByActivoTrue(Pageable pageable);

    Page<TarjetaCombustible> findAllByActivoTrueAndNumeroContainingIgnoreCase(String numero, Pageable pageable);

    Page<TarjetaCombustible> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);
}