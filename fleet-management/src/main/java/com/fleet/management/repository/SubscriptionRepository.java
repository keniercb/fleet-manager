package com.fleet.management.repository;

import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query("UPDATE Subscription s SET s.status = :nuevoEstado, s.activo = false, s.version = s.version + 1 " +
            "WHERE s.status = :estadoActual AND s.endDate = :endDate AND s.activo = true")
    int expirarSuscripcionesVencidas(@Param("estadoActual") SubscriptionStatus estadoActual,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("nuevoEstado") SubscriptionStatus nuevoEstado);

    /**
     * FX-25: carga una suscripcion con SELECT ... FOR UPDATE para evitar
     * race conditions en increment/decrementVehicleCount (TOCTOU).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);
}