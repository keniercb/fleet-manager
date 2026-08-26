package com.fleet.management.config;

import com.fleet.management.model.SubscriptionStatus;
import com.fleet.management.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expirarSuscripcionesVencidas() {
        LocalDate diaAnterior = LocalDate.now().minusDays(1);

        int actualizadas = subscriptionRepository.expirarSuscripcionesVencidas(
                SubscriptionStatus.ACTIVE, diaAnterior, SubscriptionStatus.EXPIRED);

        if (actualizadas > 0) {
            log.info("Se expiraron {} suscripcion(es) con endDate={}", actualizadas, diaAnterior);
        } else {
            log.info("No hay suscripciones vencidas para expirar");
        }
    }
}
