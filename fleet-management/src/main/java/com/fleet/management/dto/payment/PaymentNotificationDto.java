package com.fleet.management.dto.payment;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentNotificationDto {

    private String qrCode;
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String timestamp;
}
