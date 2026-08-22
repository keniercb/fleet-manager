package com.fleet.management.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
