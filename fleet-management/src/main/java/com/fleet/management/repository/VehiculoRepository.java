package com.fleet.management.repository;

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

    List<Vehiculo> findByChoferId(Long choferId);

    List<Vehiculo> findByTipoVehiculoId(Long tipoVehiculoId);

    List<Vehiculo> findByTipoCombustibleId(Long tipoCombustibleId);

    @Query("SELECT v FROM Vehiculo v WHERE v.chofer IS NULL AND v.activo = true")
    List<Vehiculo> findSinChoferAsignado();

    @Query("SELECT v FROM Vehiculo v WHERE v.chofer.id = :choferId AND v.activo = true")
    List<Vehiculo> findActivosByChoferId(@Param("choferId") Long choferId);
}
