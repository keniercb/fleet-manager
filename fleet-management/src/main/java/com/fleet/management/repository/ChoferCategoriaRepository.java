package com.fleet.management.repository;

import com.fleet.management.model.ChoferCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChoferCategoriaRepository extends JpaRepository<ChoferCategoria, Long> {

    List<ChoferCategoria> findByChoferId(Long choferId);

    List<ChoferCategoria> findByCategoriaLicenciaId(Long categoriaLicenciaId);

    Optional<ChoferCategoria> findByChoferIdAndCategoriaLicenciaId(Long choferId, Long categoriaLicenciaId);

    boolean existsByChoferIdAndCategoriaLicenciaId(Long choferId, Long categoriaLicenciaId);

    @Query("SELECT cc FROM ChoferCategoria cc WHERE cc.chofer.id = :choferId AND cc.activo = true")
    List<ChoferCategoria> findActivosByChoferId(@Param("choferId") Long choferId);

    @Query("SELECT cc FROM ChoferCategoria cc WHERE cc.categoriaLicencia.id = :categoriaId AND cc.activo = true")
    List<ChoferCategoria> findActivosByCategoriaLicenciaId(@Param("categoriaId") Long categoriaId);
}