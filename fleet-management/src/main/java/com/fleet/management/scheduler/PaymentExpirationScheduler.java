package com.fleet.management.scheduler;

import com.fleet.management.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final PaymentService paymentService;

    /**
     * Expirar QRs vencidos y hacer polling de pagos pendientes.
     * Se ejecuta cada 5 minutos.
     *
     * <p>FX-29: @SchedulerLock asegura que solo una instancia ejecute esta tarea
     * en despliegues multi-réplica. Lock por 4 minutos (menos que el intervalo
     * de 5 min para evitar solapamientos).
     */
    @Scheduled(fixedRate = 300000)
    @SchedulerLock(name = "paymentExpirationScheduler_procesarPagosPendientes",
                   lockAtLeastFor = "PT1M", lockAtMostFor = "PT4M")
    public void procesarPagosPendientes() {
        try {
            paymentService.expirarPagosVencidos();
        } catch (Exception e) {
            log.error("Error al expirar pagos vencidos", e);
        }

        try {
            paymentService.pollingPagosPendientes();
        } catch (Exception e) {
            log.error("Error en polling de pagos pendientes", e);
        }
    }
}
