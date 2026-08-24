package com.fleet.management.config;

import com.fleet.management.model.*;
import com.fleet.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String EMPRESA_ADMIN_CODIGO = "EMP-ADMIN";
    private static final String SUPER_ADMIN_ROLE_NAME = "SUPER_ADMIN";
    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final String USER_ROLE_NAME = "USER";

    @Override
    public void run(String... args) {
        log.info("=== Iniciando datos de bootstrap ===");

        Empresa empresaAdmin = createEmpresaAdminIfNotExists();

        Set<Permission> allPermissions = createDefaultPermissions();

        Role superAdminRole = createOrUpdateSuperAdminRole(allPermissions);

        Set<Permission> adminPermissions = filterAdminPermissions(allPermissions);
        Role adminRole = createOrUpdateAdminRole(adminPermissions);

        Role userRole = createRoleIfNotExists(
                USER_ROLE_NAME,
                "Rol de usuario estandar con acceso de lectura",
                new HashSet<>()
        );

        createOrUpdateAdminUser(superAdminRole, empresaAdmin);

        createTrialPlanIfNotExists();

        log.info("=== Datos de bootstrap completados ===");
    }

    private Empresa createEmpresaAdminIfNotExists() {
        return empresaRepository.findByCodigo(EMPRESA_ADMIN_CODIGO)
                .orElseGet(() -> {
                    log.info("Creando empresa por defecto: Empresa de Administracion");
                    Empresa empresa = Empresa.builder()
                            .codigo(EMPRESA_ADMIN_CODIGO)
                            .nombre("Empresa de Administracion")
                            .activo(true)
                            .build();
                    return empresaRepository.save(empresa);
                });
    }

    private Set<Permission> createDefaultPermissions() {
        String[] modules = {
                "USER", "ROLE", "PERMISSION", "VEHICULO", "CHOFER",
                "RECORRIDO", "EMPRESA", "MARCA", "TIPO_VEHICULO",
                "TIPO_COMBUSTIBLE", "CATEGORIA_LICENCIA"
        };
        String[] actions = {"READ", "WRITE", "DELETE"};

        Set<Permission> permissions = new HashSet<>();
        for (String module : modules) {
            for (String action : actions) {
                String permissionName = module + ":" + action;
                Permission permission = permissionRepository.findByName(permissionName)
                        .orElseGet(() -> {
                            log.info("Creando permiso: {}", permissionName);
                            return permissionRepository.save(Permission.builder()
                                    .name(permissionName)
                                    .description("Permite " + action.toLowerCase() + " " + module.toLowerCase())
                                    .activo(true)
                                    .build());
                        });
                permissions.add(permission);
            }
        }
        return permissions;
    }

    private Set<Permission> filterAdminPermissions(Set<Permission> allPermissions) {
        return allPermissions.stream()
                .filter(p -> !p.getName().startsWith("ROLE:") && !p.getName().startsWith("PERMISSION:"))
                .collect(Collectors.toSet());
    }

    private Role createOrUpdateSuperAdminRole(Set<Permission> allPermissions) {
        return roleRepository.findByName(SUPER_ADMIN_ROLE_NAME)
                .map(role -> {
                    role.setPermissions(allPermissions);
                    log.info("Actualizando rol SUPER_ADMIN con todos los permisos");
                    return roleRepository.save(role);
                })
                .orElseGet(() -> {
                    log.info("Creando rol: SUPER_ADMIN");
                    return roleRepository.save(Role.builder()
                            .name(SUPER_ADMIN_ROLE_NAME)
                            .description("Rol con acceso total al sistema incluyendo roles y permisos")
                            .permissions(allPermissions)
                            .activo(true)
                            .build());
                });
    }

    private Role createOrUpdateAdminRole(Set<Permission> adminPermissions) {
        return roleRepository.findByName(ADMIN_ROLE_NAME)
                .map(role -> {
                    role.setPermissions(adminPermissions);
                    log.info("Actualizando rol ADMIN sin permisos de roles y permisos");
                    return roleRepository.save(role);
                })
                .orElseGet(() -> {
                    log.info("Creando rol: ADMIN");
                    return roleRepository.save(Role.builder()
                            .name(ADMIN_ROLE_NAME)
                            .description("Rol administrativo sin acceso a roles ni permisos")
                            .permissions(adminPermissions)
                            .activo(true)
                            .build());
                });
    }

    private Role createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creando rol: {}", name);
                    return roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .permissions(permissions)
                            .activo(true)
                            .build());
                });
    }

    private void createOrUpdateAdminUser(Role superAdminRole, Empresa empresa) {
        String adminEmail = "admin@fleet.com";
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    log.info("Usuario admin ya existe: {}", adminEmail);
                    if (user.getEmpresa() == null) {
                        user.setEmpresa(empresa);
                        log.info("Asignada empresa por defecto al usuario admin existente");
                    }
                    if (!user.getRoles().contains(superAdminRole)) {
                        user.setRoles(Set.of(superAdminRole));
                        log.info("Asignado rol SUPER_ADMIN al usuario admin existente");
                    }
                    userRepository.save(user);
                },
                () -> {
                    log.info("Creando usuario admin: {}", adminEmail);
                    User admin = User.builder()
                            .email(adminEmail)
                            .password(passwordEncoder.encode("admin123"))
                            .roles(Set.of(superAdminRole))
                            .empresa(empresa)
                            .activo(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Usuario admin creado con exito. Email: {} / Password: admin123", adminEmail);
                }
        );
    }

    private void createTrialPlanIfNotExists() {
        String trialPlanName = "Trial";
        planRepository.findByNombre(trialPlanName).ifPresentOrElse(
                plan -> log.info("Plan Trial ya existe"),
                () -> {
                    log.info("Creando plan Trial");
                    Plan trialPlan = Plan.builder()
                            .nombre(trialPlanName)
                            .precioMensual(java.math.BigDecimal.ZERO)
                            .maxUsuarios(3)
                            .maxVehiculos(3)
                            .duracion(14)
                            .features(new HashSet<>())
                            .activo(true)
                            .build();
                    planRepository.save(trialPlan);
                    log.info("Plan Trial creado con exito. MaxUsuarios: 3, MaxVehiculos: 3, Duracion: 14 dias");
                }
        );
    }
}