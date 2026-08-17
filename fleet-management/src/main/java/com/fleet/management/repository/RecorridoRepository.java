package com.fleet.management.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.model.Recorrido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecorridoRepository extends JpaRepository<Recorrido, Long> {

    Page<Recorrido> findByVehiculoId(Long vehiculoId, Pageable pageable);

    Optional<Recorrido> findByVehiculoIdAndFecha(Long vehiculoId, LocalDate fecha);

    boolean existsByVehiculoIdAndFecha(Long vehiculoId, LocalDate fecha);

    @Query("SELECT r FROM Recorrido r WHERE r.vehiculo.id = :vehiculoId " +
           "AND r.fecha BETWEEN :desde AND :hasta ORDER BY r.fecha ASC")
    Page<Recorrido> findByVehiculoIdAndFechaBetween(@Param("vehiculoId") Long vehiculoId,
                                                    @Param("desde") LocalDate desde,
                                                    @Param("hasta") LocalDate hasta,
                                                    Pageable pageable);

    boolean existsByVehiculoIdAndFechaAfter(Long vehiculoId, LocalDate fecha);

    Page<Recorrido> findAllByActivoTrue(Pageable pageable);
}
