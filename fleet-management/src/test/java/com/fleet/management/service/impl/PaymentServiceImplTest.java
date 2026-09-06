package com.fleet.management.service.impl;

import com.fleet.management.client.enzona.EnzonaQrClient;
import com.fleet.management.client.enzona.dto.EnzonaQrMerchantResponse;
import com.fleet.management.config.PaymentConfig;
import com.fleet.management.dto.payment.PaymentNotificationDto;
import com.fleet.management.dto.payment.PaymentResponse;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.model.*;
import com.fleet.management.repository.*;
import com.fleet.management.service.PaymentPostPagoService;
import com.fleet.management.service.PaymentErrorRecoveryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de PaymentServiceImpl.
 *
 * <p>Cubre los flujos afectados por los fixes FX:
 * <ul>
 *   <li>FX-21: reintentar con error → paymentErrorRecoveryService.markAsFailed
 *       en lugar de persistir directamente.</li>
 *   <li>FX-05: post-pago delegado a paymentPostPagoService.</li>
 *   <li>FX-04: idempotencia en webhook.</li>
 *   <li>FX-02: scoping por empresa (validateOwnership).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl - tests unitarios")
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private PlanRepository planRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private EnzonaQrClient enzonaQrClient;
    @Mock private PaymentConfig paymentConfig;
    @Mock private com.fleet.management.service.SubscriptionService subscriptionService;
    @Mock private PaymentPostPagoService paymentPostPagoService;
    @Mock private PaymentErrorRecoveryService paymentErrorRecoveryService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Empresa empresa;
    private Plan plan;
    private Payment pago;

    @BeforeEach
    void setUp() {
        empresa = new Empresa();
        empresa.setId(1L);
        empresa.setCodigo("EMP-001");
        empresa.setNombre("Empresa Test");
        empresa.setActivo(true);

        plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Plan Basico");
        plan.setPrecioMensual(new BigDecimal("500.00"));
        plan.setDuracion(30);
        plan.setMaxVehiculos(10);
        plan.setMaxUsuarios(5);
        plan.setActivo(true);

        pago = new Payment();
        pago.setId(1L);
        pago.setEmpresa(empresa);
        pago.setPlan(plan);
        pago.setAmount(new BigDecimal("500.00"));
        pago.setCurrency("CUP");
        pago.setStatus(PaymentStatus.QR_GENERADO);
        pago.setType(PaymentType.NUEVA_SUSCRIPCION);
        pago.setRetryCount(0);
        pago.setActivo(true);

        // Simular usuario autenticado como SUPER_ADMIN (para validar bypass de ownership)
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin@fleet.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FX-21: reintentar con error de Enzona invoca paymentErrorRecoveryService.markAsFailed")
    void reintentar_conErrorEnzona_invocaMarkAsFailed() {
        // Given: pago en estado FALLIDO
        pago.setStatus(PaymentStatus.FALLIDO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(enzonaQrClient.crearQrMerchant(any(), any()))
                .thenThrow(new BusinessException("Enzona caido"));

        // When: se reintenta
        assertThatThrownBy(() -> paymentService.reintentar(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Error al reintentar");

        // Then: el servicio de recuperacion fue invocado (NO se persistio directamente en PaymentServiceImpl)
        verify(paymentErrorRecoveryService).markAsFailed(eq(1L), anyString());
    }

    @Test
    @DisplayName("FX-21: reintentar exitoso no invoca markAsFailed")
    void reintentar_exitoso_noInvocaMarkAsFailed() {
        // Given: pago FALLIDO
        pago.setStatus(PaymentStatus.FALLIDO);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pago));

        EnzonaQrMerchantResponse qrResponse = new EnzonaQrMerchantResponse();
        qrResponse.setVendorIdentityCode("test-qr-code");
        qrResponse.setImage("base64-image");
        when(enzonaQrClient.crearQrMerchant(any(), any())).thenReturn(qrResponse);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        PaymentResponse response = paymentService.reintentar(1L);

        // Then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.QR_GENERADO);
        verify(paymentErrorRecoveryService, never()).markAsFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("FX-02: findById de pago propio retorna response para SUPER_ADMIN")
    void findById_pagoPropio_retornaResponse() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(pago));

        PaymentResponse response = paymentService.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.QR_GENERADO);
    }

    @Test
    @DisplayName("FX-04: procesarNotificacion con pago ya PAGADO retorna idempotente")
    void procesarNotificacion_pagoYaPagado_retornaIdempotente() {
        // Given: pago ya PAGADO
        pago.setStatus(PaymentStatus.PAGADO);
        pago.setExternalTransactionId("txn-001");
        when(paymentRepository.findByQrCode("qr-001")).thenReturn(Optional.of(pago));

        // When: llega notificacion duplicada
        PaymentNotificationDto notification = PaymentNotificationDto.builder()
                .qrCode("qr-001")
                .transactionId("txn-001")
                .amount(new BigDecimal("500.00"))
                .currency("CUP")
                .build();
        paymentService.procesarNotificacion(notification);

        // Then: no se invoca post-pago ni se vuelve a guardar
        verify(paymentPostPagoService, never()).ejecutar(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}
