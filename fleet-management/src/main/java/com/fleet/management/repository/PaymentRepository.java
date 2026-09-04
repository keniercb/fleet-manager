package com.fleet.management.repository;

import com.fleet.management.model.Payment;
import com.fleet.management.model.PaymentStatus;
import com.fleet.management.model.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByQrCode(String qrCode);

    boolean existsByQrCode(String qrCode);

    // FX-06: soporte para idempotencia por external_transaction_id
    boolean existsByExternalTransactionId(String externalTransactionId);

    boolean existsByExternalTransactionIdAndIdNot(String externalTransactionId, Long id);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.empresa.id = :empresaId AND p.activo = true " +
           "AND p.status IN :estadosActivos " +
           "ORDER BY p.fechaCreacion DESC")
    List<Payment> findActivosByEmpresaAndStatusIn(@Param("empresaId") Long empresaId,
                                                   @Param("estadosActivos") List<PaymentStatus> estados);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.empresa.id = :empresaId AND p.activo = true " +
           "AND p.type = :type AND p.status IN :estadosActivos")
    Optional<Payment> findActivoByEmpresaAndType(@Param("empresaId") Long empresaId,
                                                  @Param("type") PaymentType type,
                                                  @Param("estadosActivos") List<PaymentStatus> estados);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.activo = true AND p.status = :status " +
           "AND p.expiresAt < :now")
    List<Payment> findExpiredPayments(@Param("status") PaymentStatus status,
                                      @Param("now") LocalDateTime now);

    Page<Payment> findByEmpresaIdAndActivoTrue(Long empresaId, Pageable pageable);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.empresa.id = :empresaId AND p.activo = true " +
           "AND (:status IS NULL OR p.status = :status)")
    Page<Payment> findByEmpresaIdAndStatus(@Param("empresaId") Long empresaId,
                                            @Param("status") PaymentStatus status,
                                            Pageable pageable);

    @Query("SELECT p FROM Payment p " +
           "WHERE p.activo = true AND p.status = :status " +
           "AND p.expiresAt > :now")
    List<Payment> findPendingQrPayments(@Param("status") PaymentStatus status,
                                         @Param("now") LocalDateTime now);
}
