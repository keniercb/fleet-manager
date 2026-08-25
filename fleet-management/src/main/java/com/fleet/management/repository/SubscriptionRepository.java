package com.fleet.management.repository;

import com.fleet.management.model.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByEmpresaIdAndActivoTrue(Long empresaId);

    Page<Subscription> findAllByActivoTrue(Pageable pageable);

    Page<Subscription> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    Page<Subscription> findByPlanIdAndActivoTrue(Long planId, Pageable pageable);

    Optional<Subscription> findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(Long empresaId);
}