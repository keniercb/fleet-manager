package com.fleet.management.repository;

import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByEmpresaIdAndActivoTrue(Long empresaId);

    Page<Subscription> findAllByActivoTrue(Pageable pageable);

    Page<Subscription> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    Page<Subscription> findByPlanIdAndActivoTrue(Long planId, Pageable pageable);

    Optional<Subscription> findFirstByEmpresaIdAndActivoTrueOrderByIdDesc(Long empresaId);

    List<Subscription> findByStatusAndEndDateAndActivoTrue(SubscriptionStatus status, LocalDate endDate);
}