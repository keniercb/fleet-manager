package com.fleet.management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pay_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_qr_code", columnNames = "qr_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @NotNull(message = "La empresa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_payment_empresa", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pay_empresa"))
    private Empresa empresa;

    @NotNull(message = "El plan es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_payment_plan", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pay_plan"))
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_payment_subscription",
            foreignKey = @ForeignKey(name = "fk_pay_subscription"))
    private Subscription subscription;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "La moneda es obligatoria")
    @Size(max = 10)
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @NotNull(message = "El tipo de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PaymentType type;

    @Size(max = 100)
    @Column(name = "qr_code", length = 100, unique = true)
    private String qrCode;

    @Column(name = "qr_image_base64", columnDefinition = "TEXT")
    private String qrImageBase64;

    @Size(max = 100)
    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @NotNull(message = "La fecha de expiracion es obligatoria")
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Size(max = 500)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Min(value = 0)
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Version
    @Column(name = "version")
    private Long version;
}
