package com.fleet.management.repository;

import com.fleet.management.model.Plan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    Page<Plan> findAllByActivoTrue(Pageable pageable);

    @Query("SELECT p FROM Plan p WHERE p.activo = true AND p.nombre <> 'Trial'")
    Page<Plan> findAllByActivoTrueAndNombreNotTrial(Pageable pageable);
}