package com.fleet.management.repository;

import com.fleet.management.model.Chofer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {

    Optional<Chofer> findByCarneIdentidad(String carneIdentidad);

    Optional<Chofer> findByNumeroLicencia(String numeroLicencia);

    boolean existsByCarneIdentidad(String carneIdentidad);

    boolean existsByNumeroLicencia(String numeroLicencia);

    Page<Chofer> findAllByActivoTrue(Pageable pageable);

    Page<Chofer> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);
}
