package com.fleet.management.repository;

import com.fleet.management.model.TarjetaCombustible;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaCombustibleRepository extends JpaRepository<TarjetaCombustible, Long> {

    Optional<TarjetaCombustible> findByNumero(String numero);

    boolean existsByNumero(String numero);

    Page<TarjetaCombustible> findAllByActivoTrue(Pageable pageable);

    Page<TarjetaCombustible> findAllByActivoTrueAndNumeroContainingIgnoreCase(String numero, Pageable pageable);

    Page<TarjetaCombustible> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    Page<TarjetaCombustible> findByEmpresaIdAndActivoTrueAndNumeroContainingIgnoreCase(Long empresaId, String numero, Pageable pageable);

    /**
     * Tarjetas activas de una empresa ordenadas por ID (para reporte PDF).
     * Carga currency y empresa con EntityGraph para evitar N+1.
     */
    @EntityGraph(attributePaths = {"currency", "empresa"})
    List<TarjetaCombustible> findByEmpresaIdAndActivoTrueOrderByIdAsc(Long empresaId);

    /**
     * FX-12: carga una tarjeta con SELECT ... FOR UPDATE para evitar
     * race conditions en read-modify-write del saldo.
     * Usar solo dentro de una transaccion activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TarjetaCombustible t WHERE t.id = :id")
    Optional<TarjetaCombustible> findByIdForUpdate(@Param("id") Long id);
}