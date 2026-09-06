package com.fleet.management.service.impl;

import com.fleet.management.dto.subscription.SubscriptionRequest;
import com.fleet.management.exception.BusinessException;
import com.fleet.management.model.Empresa;
import com.fleet.management.model.Plan;
import com.fleet.management.model.Subscription;
import com.fleet.management.model.SubscriptionStatus;
import com.fleet.management.repository.EmpresaRepository;
import com.fleet.management.repository.PlanRepository;
import com.fleet.management.repository.SubscriptionRepository;
import com.fleet.management.mapper.SubscriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de SubscriptionServiceImpl.
 *
 * <p>Cubre el flujo afectado por FX-25: findAndLock ahora usa
 * repository.findByIdForUpdate (PESSIMISTIC_WRITE) en lugar de findById.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl - tests unitarios")
class SubscriptionServiceImplTest {

    @Mock private SubscriptionRepository repository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private PlanRepository planRepository;
    @Mock private SubscriptionMapper mapper;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private Subscription subscription;
    private Plan plan;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Plan Basico");
        plan.setDuracion(30);
        plan.setMaxVehiculos(10);
        plan.setMaxUsuarios(5);
        plan.setActivo(true);

        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setCodigo("EMP-001");
        empresa.setNombre("Empresa Test");
        empresa.setActivo(true);

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setEmpresa(empresa);
        subscription.setPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setEndDate(LocalDate.now().plusDays(30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setMaxVehiculos(10);
        subscription.setMaxUsuarios(5);
        subscription.setCurrentVehicleCount(5);
        subscription.setCurrentUserCount(2);
        subscription.setActivo(true);
    }

    @Test
    @DisplayName("FX-25: incrementVehicleCount usa findByIdForUpdate (PESSIMISTIC_WRITE)")
    void incrementVehicleCount_usaFindByIdForUpdate() {
        // Given
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        subscriptionService.incrementVehicleCount(1L);

        // Then: verifica que se uso findByIdForUpdate (no findById)
        verify(repository).findByIdForUpdate(1L);
        assertThat(subscription.getCurrentVehicleCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("FX-25: incrementVehicleCount lanza BusinessException si excede maxVehiculos")
    void incrementVehicleCount_excedeMax_lanzaBusinessException() {
        subscription.setMaxVehiculos(5);
        subscription.setCurrentVehicleCount(5);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.incrementVehicleCount(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limite");
    }

    @Test
    @DisplayName("FX-25: decrementVehicleCount usa findByIdForUpdate")
    void decrementVehicleCount_usaFindByIdForUpdate() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.decrementVehicleCount(1L);

        verify(repository).findByIdForUpdate(1L);
        assertThat(subscription.getCurrentVehicleCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("FX-25: incrementUserCount usa findByIdForUpdate")
    void incrementUserCount_usaFindByIdForUpdate() {
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(subscription));
        when(repository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.incrementUserCount(1L);

        verify(repository).findByIdForUpdate(1L);
        assertThat(subscription.getCurrentUserCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("FX-25: incrementUserCount lanza BusinessException si excede maxUsuarios")
    void incrementUserCount_excedeMax_lanzaBusinessException() {
        subscription.setMaxUsuarios(2);
        subscription.setCurrentUserCount(2);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.incrementUserCount(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("limite");
    }
}
