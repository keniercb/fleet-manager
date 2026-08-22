package com.fleet.management.repository;

import com.fleet.management.model.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Page<Empresa> findAllByActivoTrue(Pageable pageable);

    Page<Empresa> findAllByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
}