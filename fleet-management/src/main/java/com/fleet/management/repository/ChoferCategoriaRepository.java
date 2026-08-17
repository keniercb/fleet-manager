package com.fleet.management.repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.fleet.management.model.ChoferCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChoferCategoriaRepository extends JpaRepository<ChoferCategoria, Long> {

    Page<ChoferCategoria> findByChoferId(Long choferId, Pageable pageable);

    Page<ChoferCategoria> findByCategoriaLicenciaId(Long categoriaLicenciaId, Pageable pageable);

    Optional<ChoferCategoria> findByChoferIdAndCategoriaLicenciaId(Long choferId, Long categoriaLicenciaId);

    boolean existsByChoferIdAndCategoriaLicenciaId(Long choferId, Long categoriaLicenciaId);

    @Query("SELECT cc FROM ChoferCategoria cc WHERE cc.chofer.id = :choferId AND cc.activo = true")
    Page<ChoferCategoria> findActivosByChoferId(@Param("choferId") Long choferId, Pageable pageable);

    @Query("SELECT cc FROM ChoferCategoria cc WHERE cc.categoriaLicencia.id = :categoriaId AND cc.activo = true")
    Page<ChoferCategoria> findActivosByCategoriaLicenciaId(@Param("categoriaId") Long categoriaId, Pageable pageable);

    Page<ChoferCategoria> findAllByActivoTrue(Pageable pageable);
}