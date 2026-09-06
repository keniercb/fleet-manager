package com.fleet.management.integration.support;

import com.fleet.management.model.*;
import com.fleet.management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

/**
 * Factory centralizada para crear entidades de test con defaults sensatos.
 *
 * <p>Evita dispersion de datos de prueba y permite reutilizar builders
 * consistentes entre tests.
 */
@Component
public class TestDataFactory {

    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private TipoVehiculoRepository tipoVehiculoRepository;
    @Autowired private MarcaRepository marcaRepository;
    @Autowired private TipoCombustibleRepository tipoCombustibleRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PlanRepository planRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private TarjetaCombustibleRepository tarjetaRepository;

    public Empresa crearEmpresa(String codigo, String nombre) {
        Empresa e = new Empresa();
        e.setCodigo(codigo);
        e.setNombre(nombre);
        e.setActivo(true);
        return empresaRepository.save(e);
    }

    public TipoVehiculo crearTipoVehiculo(String nombre) {
        TipoVehiculo t = new TipoVehiculo();
        t.setNombre(nombre);
        t.setActivo(true);
        return tipoVehiculoRepository.save(t);
    }

    public Marca crearMarca(String nombre) {
        Marca m = new Marca();
        m.setNombre(nombre);
        m.setActivo(true);
        return marcaRepository.save(m);
    }

    public TipoCombustible crearTipoCombustible(String codigo, String denominacion) {
        TipoCombustible t = new TipoCombustible();
        t.setCodigo(codigo);
        t.setDenominacion(denominacion);
        t.setActivo(true);
        return tipoCombustibleRepository.save(t);
    }

    public Vehiculo crearVehiculo(Empresa empresa, TipoVehiculo tv, Marca marca, TipoCombustible tc) {
        Vehiculo v = new Vehiculo();
        v.setEmpresa(empresa);
        v.setTipoVehiculo(tv);
        v.setMarca(marca);
        v.setTipoCombustible(tc);
        v.setMatricula("MAT-" + System.nanoTime());
        v.setNumeroMotor("MOT-" + System.nanoTime());
        v.setOdometro(BigInteger.ZERO);
        v.setCombustible(new BigDecimal("100.00"));
        v.setIndiceConsumo(new BigDecimal("8.50"));
        v.setActivo(true);
        return vehiculoRepository.save(v);
    }

    public Currency crearCurrency(String iso, String desc) {
        Currency c = new Currency();
        c.setIsoCode(iso);
        c.setDescripcion(desc);
        c.setActivo(true);
        return currencyRepository.save(c);
    }

    public TarjetaCombustible crearTarjeta(Empresa empresa, Currency currency, BigDecimal saldoInicial) {
        TarjetaCombustible t = new TarjetaCombustible();
        t.setNumero("TARJ-" + System.nanoTime());
        t.setSaldo(saldoInicial);
        t.setCurrency(currency);
        t.setEmpresa(empresa);
        t.setActivo(true);
        return tarjetaRepository.save(t);
    }

    public Plan crearPlan(String nombre, BigDecimal precio, int duracion, int maxVehiculos, int maxUsuarios) {
        Plan p = new Plan();
        p.setNombre(nombre);
        p.setPrecioMensual(precio);
        p.setDuracion(duracion);
        p.setMaxVehiculos(maxVehiculos);
        p.setMaxUsuarios(maxUsuarios);
        p.setActivo(true);
        return planRepository.save(p);
    }

    public Subscription crearSubscriptionActiva(Empresa empresa, Plan plan) {
        Subscription s = new Subscription();
        s.setEmpresa(empresa);
        s.setPlan(plan);
        s.setStartDate(LocalDate.now());
        s.setEndDate(LocalDate.now().plusDays(plan.getDuracion()));
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setMaxVehiculos(plan.getMaxVehiculos());
        s.setMaxUsuarios(plan.getMaxUsuarios());
        s.setCurrentVehicleCount(0);
        s.setCurrentUserCount(0);
        s.setActivo(true);
        return subscriptionRepository.save(s);
    }
}
