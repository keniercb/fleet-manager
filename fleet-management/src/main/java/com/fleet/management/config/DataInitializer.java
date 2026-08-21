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
        String[][] permissionNames = {
                {"user:read", "Permite leer usuarios"},
                {"user:write", "Permite crear y editar usuarios"},
                {"user:delete", "Permite eliminar usuarios"},
                {"role:read", "Permite leer roles"},
                {"role:write", "Permite crear y editar roles"},
                {"role:delete", "Permite eliminar roles"},
                {"permission:read", "Permite leer permisos"},
                {"permission:write", "Permite crear y editar permisos"},
                {"permission:delete", "Permite eliminar permisos"},
                {"vehiculo:read", "Permite leer vehiculos"},
                {"vehiculo:write", "Permite crear y editar vehiculos"},
                {"vehiculo:delete", "Permite eliminar vehiculos"},
                {"chofer:read", "Permite leer choferes"},
                {"chofer:write", "Permite crear y editar choferes"},
                {"chofer:delete", "Permite eliminar choferes"},
                {"recorrido:read", "Permite leer recorridos"},
                {"recorrido:write", "Permite crear y editar recorridos"},
                {"recorrido:delete", "Permite eliminar recorridos"},
                {"empresa:read", "Permite leer empresas"},
                {"empresa:write", "Permite crear y editar empresas"},
                {"empresa:delete", "Permite eliminar empresas"},
                {"marca:read", "Permite leer marcas"},
                {"marca:write", "Permite crear y editar marcas"},
                {"marca:delete", "Permite eliminar marcas"},
                {"tipo_vehiculo:read", "Permite leer tipos de vehiculo"},
                {"tipo_vehiculo:write", "Permite crear y editar tipos de vehiculo"},
                {"tipo_vehiculo:delete", "Permite eliminar tipos de vehiculo"},
                {"tipo_combustible:read", "Permite leer tipos de combustible"},
                {"tipo_combustible:write", "Permite crear y editar tipos de combustible"},
                {"tipo_combustible:delete", "Permite eliminar tipos de combustible"},
                {"categoria_licencia:read", "Permite leer categorias de licencia"},
                {"categoria_licencia:write", "Permite crear y editar categorias de licencia"},
                {"categoria_licencia:delete", "Permite eliminar categorias de licencia"}
        };

        Set<Permission> permissions = new HashSet<>();
        for (String[] permData : permissionNames) {
            Permission permission = permissionRepository.findByName(permData[0])
                    .orElseGet(() -> {
                        log.info("Creando permiso: {}", permData[0]);
                        return permissionRepository.save(Permission.builder()
                                .name(permData[0])
                                .description(permData[1])
                                .activo(true)
                                .build());
                    });
            permissions.add(permission);
        }
        return permissions;
    }

    private Set<Permission> filterAdminPermissions(Set<Permission> allPermissions) {
        return allPermissions.stream()
                .filter(p -> !p.getName().startsWith("role:") && !p.getName().startsWith("permission:"))
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
}