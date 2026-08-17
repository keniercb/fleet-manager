package com.fleet.management.repository;

import com.fleet.management.model.TipoVehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoVehiculoRepository extends JpaRepository<TipoVehiculo, Long> {

    Optional<TipoVehiculo> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    Page<TipoVehiculo> findAllByActivoTrue(Pageable pageable);
}
