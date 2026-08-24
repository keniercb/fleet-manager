package com.fleet.management.repository;

import com.fleet.management.model.Feature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureRepository extends JpaRepository<Feature, Long> {

    Optional<Feature> findByName(String name);

    boolean existsByName(String name);

    Page<Feature> findAllByActivoTrue(Pageable pageable);
}