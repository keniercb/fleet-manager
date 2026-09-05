package com.fleet.management.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.model.Vehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByMatricula(String matricula);

    Optional<Vehiculo> findByNumeroMotor(String numeroMotor);

    boolean existsByMatricula(String matricula);

    boolean existsByNumeroMotor(String numeroMotor);

    Page<Vehiculo> findByChoferId(Long choferId, Pageable pageable);

    Page<Vehiculo> findByTipoVehiculoId(Long tipoVehiculoId, Pageable pageable);

    Page<Vehiculo> findByTipoCombustibleId(Long tipoCombustibleId, Pageable pageable);

    @Query("SELECT v FROM Vehiculo v WHERE v.chofer IS NULL AND v.activo = true")
    Page<Vehiculo> findSinChoferAsignado(Pageable pageable);

    @Query("SELECT v FROM Vehiculo v WHERE v.chofer.id = :choferId AND v.activo = true")
    Page<Vehiculo> findActivosByChoferId(@Param("choferId") Long choferId, Pageable pageable);

    Page<Vehiculo> findAllByActivoTrue(Pageable pageable);

    @Query("SELECT v FROM Vehiculo v WHERE v.activo = true AND " +
            "(LOWER(v.matricula) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(v.numeroMotor) LIKE LOWER(CONCAT('%', :filter, '%')))" )
    Page<Vehiculo> findAllByActivoTrueAndMatriculaOrNumeroMotor(@Param("filter") String filter, Pageable pageable);

    Page<Vehiculo> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    @Query("SELECT v FROM Vehiculo v WHERE v.empresa.id = :empresaId AND v.activo = true AND " +
            "(LOWER(v.matricula) LIKE LOWER(CONCAT('%', :filter, '%')) OR " +
            "LOWER(v.numeroMotor) LIKE LOWER(CONCAT('%', :filter, '%')))")
    Page<Vehiculo> findByEmpresaIdAndActivoTrueAndMatriculaOrNumeroMotor(@Param("empresaId") Long empresaId, @Param("filter") String filter, Pageable pageable);

    /**
     * Retorna todos los vehiculos activos de una empresa sin paginacion.
     * Usado para generar reportes PDF.
     */
    List<Vehiculo> findByEmpresaIdAndActivoTrueOrderByMatriculaAsc(Long empresaId);

    /**
     * FX-34: variante con @EntityGraph que carga marca, tipoVehiculo,
     * tipoCombustible, empresa y chofer en una sola query, evitando N+1
     * en la generación del reporte PDF.
     */
    @EntityGraph(attributePaths = {"marca", "tipoVehiculo", "tipoCombustible", "empresa", "empresa.provincia", "empresa.municipio", "chofer"})
    @Query("SELECT v FROM Vehiculo v WHERE v.empresa.id = :empresaId AND v.activo = true ORDER BY v.matricula ASC")
    List<Vehiculo> findForReporteByEmpresaId(@Param("empresaId") Long empresaId);

    /**
     * FX-12: carga un vehiculo con SELECT ... FOR UPDATE para evitar
     * race conditions en read-modify-write del odometro y combustible.
     * Usar solo dentro de una transaccion activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehiculo v WHERE v.id = :id")
    Optional<Vehiculo> findByIdForUpdate(@Param("id") Long id);

    // --- Dashboard Ejecutivo ---

    @Query("SELECT v FROM Vehiculo v " +
           "WHERE v.empresa.id = :empresaId AND v.activo = true " +
           "AND v.id IN (SELECT DISTINCT r.vehiculo.id FROM Recorrido r " +
           "WHERE r.activo = true AND r.fecha BETWEEN :desde AND :hasta)")
    List<Vehiculo> findVehiculosConRecorridoEnPeriodo(@Param("empresaId") Long empresaId,
                                                       @Param("desde") LocalDate desde,
                                                       @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(v) FROM Vehiculo v " +
           "WHERE v.empresa.id = :empresaId AND v.activo = true " +
           "AND (v.odometro - COALESCE(v.odometroUltimoMantenimiento, 0)) > :umbralKm")
    long countVehiculosAlertaMantenimiento(@Param("empresaId") Long empresaId,
                                            @Param("umbralKm") BigInteger umbralKm);

    @Query("SELECT COUNT(v) FROM Vehiculo v " +
           "WHERE v.empresa.id = :empresaId AND v.activo = true")
    long countActivosByEmpresaId(@Param("empresaId") Long empresaId);
}
