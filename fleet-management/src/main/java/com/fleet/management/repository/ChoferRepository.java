package com.fleet.management.repository;

import com.fleet.management.model.Chofer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChoferRepository extends JpaRepository<Chofer, Long> {

    Optional<Chofer> findByCarneIdentidad(String carneIdentidad);

    Optional<Chofer> findByNumeroLicencia(String numeroLicencia);

    boolean existsByCarneIdentidad(String carneIdentidad);

    boolean existsByNumeroLicencia(String numeroLicencia);

    Page<Chofer> findAllByActivoTrue(Pageable pageable);

    @Query("SELECT c FROM Chofer c WHERE c.activo = true AND " +
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(c.carneIdentidad) LIKE LOWER(CONCAT('%', :filter, '%')))" )
    Page<Chofer> findAllByActivoTrueAndNombreOrCarneIdentidad(@Param("filter") String filter, Pageable pageable);

    Page<Chofer> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    @Query("SELECT c FROM Chofer c WHERE c.empresa.id = :empresaId AND c.activo = true AND " +
            "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(c.carneIdentidad) LIKE LOWER(CONCAT('%', :filter, '%')))")
    Page<Chofer> findByEmpresaIdAndActivoTrueAndNombreOrCarneIdentidad(@Param("empresaId") Long empresaId, @Param("filter") String filter, Pageable pageable);
}
