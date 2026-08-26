package com.fleet.management.config;

import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import com.fleet.management.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void expirarSuscripcionesVencidas() {
        LocalDate diaAnterior = LocalDate.now().minusDays(1);
        List<Subscription> vencidas = subscriptionRepository
                .findByStatusAndEndDateAndActivoTrue(SubscriptionStatus.ACTIVE, diaAnterior);

        if (vencidas.isEmpty()) {
            log.info("No hay suscripciones vencidas para expirar");
            return;
        }

        for (Subscription subscription : vencidas) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            subscription.setActivo(false);
            subscriptionRepository.save(subscription);
            log.info("Suscripcion {} de la empresa {} expirada (endDate={})",
                    subscription.getId(),
                    subscription.getEmpresa().getNombre(),
                    subscription.getEndDate());
        }

        log.info("Se expiraron {} suscripcion(es)", vencidas.size());
    }
}
