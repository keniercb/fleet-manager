# Documento Técnico — Sistema de Gestión del Parque de Vehículos (Fleet Management)

**Versión:** 2.0  
**Fecha:** 2026-08-26  
**Estado:** Desarrollo activo  
**Rama:** `developFT`

---

## Tabla de Contenido

1. [Introducción](#1-introducción)
2. [Stack Tecnológico](#2-stack-tecnológico)
3. [Arquitectura del Sistema](#3-arquitectura-del-sistema)
4. [Estructura del Proyecto](#4-estructura-del-proyecto)
5. [Modelo de Datos](#5-modelo-de-datos)
6. [API REST — Endpoints](#6-api-rest--endpoints)
7. [Seguridad y Autenticación](#7-seguridad-y-autenticación)
8. [Capa de Servicios — Lógica de Negocio](#8-capa-de-servicios--lógica-de-negocio)
9. [Reportes](#9-reportes)
10. [Manejo de Excepciones](#10-manejo-de-excepciones)
11. [Utilidades y Patrones Comunes](#11-utilidades-y-patrones-comunes)
12. [Inicialización de Datos](#12-inicialización-de-datos)
13. [Decisiones de Diseño](#13-decisiones-de-diseño)
14. [Pruebas Unitarias](#14-pruebas-unitarias)
15. [Configuración de Entornos](#15-configuración-de-entornos)
16. [Problemas Conocidos y Mejoras Pendientes](#16-problemas-conocidos-y-mejoras-pendientes)

---

## 1. Introducción

El **Sistema de Gestión del Parque de Vehículos** (Fleet Management) es una aplicación backend construida con Spring Boot que proporciona una API REST para la gestión integral de vehículos, choferes, empresas, recorridos, tarjetas de combustible y los catálogos maestros asociados (marcas, tipos de vehículos, tipos de combustible, categorías de licencia, monedas). El sistema implementa control de acceso basado en roles (RBAC) con tres niveles (SUPER_ADMIN, ADMIN, USER), auditoría automática de entidades, lógica de negocio transaccional para la gestión de recorridos y consumo de combustible, y un módulo de reportes de movimiento mensual.

El sistema está diseñado como un microservicio backend que puede ser consumido por cualquier frontend (Angular, React, Vue) a través de su API REST documentada con OpenAPI/Swagger.

### Alcance Funcional

- Gestión de **Empresas** propietarias de vehículos
- Gestión de **Vehículos** con control de odómetro y combustible
- Gestión de **Choferes** con categorías de licencia
- Registro y control de **Recorridos** con cálculo automático de consumo y validación de saldo de tarjetas de combustible
- Gestión de **Tarjetas de Combustible** con control de saldo y relación a monedas
- Gestión de **Monedas (Currency)** como catálogo maestro
- **Reporte de movimiento mensual** por vehículo con análisis de consumo comparado con la norma
- Gestión de catálogos maestros: **Marcas**, **Tipos de Vehículo**, **Tipos de Combustible**, **Categorías de Licencia**
- Sistema de **Autenticación JWT** con tres roles y permisos granulares
- **Auditoría automática** en todas las entidades (creador, modificador, timestamps)
- **Baja lógica** (soft delete) en todas las entidades
- **Filtrado** en endpoints de listado por texto libre
- Cambio de **contraseña** autenticado
- Asociación de **usuarios a empresas** con protección de empresa del administrador

---

## 2. Stack Tecnológico

| Componente | Tecnología | Versión | Observaciones |
|---|---|---|---|
| Lenguaje | Java | 25 | LTS |
| Framework | Spring Boot | 3.5.5 | |
| Build Tool | Maven | — | `pom.xml` |
| ORM | Spring Data JPA / Hibernate | (managed by Spring Boot) | `ddl-auto=update` |
| Base de datos (desarrollo) | H2 | (in-memory) | Consola habilitada en `/h2-console` |
| Base de datos (producción) | PostgreSQL | — | Requiere instalación externa |
| Seguridad | Spring Security + JWT (jjwt) | 0.12.6 | Sesiones stateless |
| Validación | Jakarta Bean Validation | (Jakarta EE 10) | Anotaciones en entidades y DTOs |
| Reducción de boilerplate | Lombok | (managed by Spring Boot) | `@Getter`, `@Setter`, `@SuperBuilder`, `@Builder` |
| Documentación API | OpenAPI / Swagger v3 | (springdoc) | Tags en controladores |
| Testing | JUnit 5 + Mockito | (Spring Boot Starter Test) | Solo pruebas unitarias |
| Logs | SLF4J + Logback | (Spring Boot default) | DEBUG en desarrollo |

**Dependencias NO utilizadas (decisiones conscientes):**

- **MapStruct:** Mapeo entidad-DTO realizado manualmente
- **QueryDSL:** Consultas construidas con métodos Spring Data JPA derivados y `@Query`
- **Flyway / Liquibase:** Sin migraciones versionadas de base de datos
- **Spring Cache:** Sin mecanismo de caché
- **Thymeleaf / Flying Saucer:** Sin generación de PDF (pendiente)

---

## 3. Arquitectura del Sistema

### 3.1 Patrón Arquitectónico

El sistema sigue una **arquitectura en capas (Layered Architecture)** clásica con separación estricta de responsabilidades:

```
┌──────────────────────────────────────────────┐
│              Cliente (Frontend)              │
│         Angular / React / Vue / cURL         │
└────────────────────┬─────────────────────────┘
                     │ HTTP/REST (JSON)
┌────────────────────▼─────────────────────────┐
│           Controladores (Controllers)         │
│      Validación de entrada, paginación,       │
│      filtrado, mapeo request → servicio      │
├──────────────────────────────────────────────┤
│             Servicios (Services)              │
│      Lógica de negocio, reglas,              │
│      mapeo entidad ↔ DTO                      │
├──────────────────────────────────────────────┤
│            Repositorios (Repositories)        │
│      Spring Data JPA, consultas,             │
│      abstracción de acceso a datos            │
├──────────────────────────────────────────────┤
│              Modelo de Dominio                 │
│      Entidades JPA, BaseEntity,              │
│      relaciones, auditoría                    │
├──────────────────────────────────────────────┤
│               Base de Datos                   │
│        H2 (dev) / PostgreSQL (prod)           │
└──────────────────────────────────────────────┘
```

### 3.2 Capas Detalladas

#### Controladores (`controller/`)

- Exponen endpoints REST bajo el prefijo `/api/`
- Reciben `XRequest` DTOs con anotaciones de validación Jakarta
- Delegan al servicio correspondiente
- Retornan `ResponseEntity<XResponse>` con códigos HTTP apropiados
- Todos los endpoints de listado soportan **paginación** y algunos soportan **filtrado** por texto libre
- 15 controladores REST en total

#### Servicios (`service/` + `service/impl/`)

- Cada dominio tiene una interfaz `XService` y su implementación `XServiceImpl`
- 16 servicios en total (incluyendo `AuthService`)
- Contienen toda la lógica de negocio y reglas de validación
- Realizan el mapeo manual entre entidades y DTOs mediante métodos `toResponse()` privados
- Anotados con `@Transactional` para garantizar atomicidad

#### Repositorios (`repository/`)

- 14 interfaces Spring Data JPA que extienden `JpaRepository<Entity, Long>`
- Proveen métodos de búsqueda por claves naturales (`findByCodigo`, `findByEmail`, `findByMatricula`, `findByNumero`, `findByIsoCode`, etc.)
- Todos incluyen `findAllByActivoTrue(Pageable)` para soportar baja lógica con paginación
- Algunos incluyen métodos de búsqueda con filtro: `findAllByActivoTrueAndNombreContainingIgnoreCase(String filter, Pageable)`
- Algunos incluyen consultas personalizadas con `@Query`

#### Modelo (`model/`)

- 16 entidades JPA que heredan de `BaseEntity`
- Relaciones mapeadas con `FetchType.LAZY` (excepto `User.roles` y `Role.permissions` que son `EAGER`)
- Validaciones con anotaciones Jakarta (`@NotNull`, `@NotBlank`, `@Size`, `@Past`, `@PastOrPresent`, `@DecimalMin`, `@Positive`, `@Email`)

---

## 4. Estructura del Proyecto

```
fleet-management/
├── pom.xml                                          # Configuración Maven y dependencias
├── src/
│   ├── main/
│   │   ├── java/com/fleet/management/
│   │   │   ├── FleetManagementApplication.java       # Punto de entrada Spring Boot
│   │   │   ├── config/                               # Configuración de la aplicación
│   │   │   │   ├── DataInitializer.java              # Bootstrap de datos iniciales
│   │   │   │   ├── JpaAuditingConfig.java            # Habilita @EnableJpaAuditing
│   │   │   │   └── SecurityConfig.java               # Cadena de filtros JWT, CORS, RBAC
│   │   │   ├── security/                             # Componentes de seguridad
│   │   │   │   ├── AuthenticatedUser.java            # UserDetails personalizado (envuelve User)
│   │   │   │   ├── CustomUserDetailsService.java     # Carga de usuario + autoridades
│   │   │   │   ├── JwtAuthenticationFilter.java      # Filtro OncePerRequest para JWT
│   │   │   │   └── JwtService.java                   # Generación y validación de tokens JWT
│   │   │   ├── model/                                # 16 entidades JPA
│   │   │   │   ├── BaseEntity.java                   # Superclase abstracta (id, audit, soft-delete)
│   │   │   │   ├── User.java                         # Usuario del sistema (con empresa)
│   │   │   │   ├── Role.java                         # Rol (RBAC)
│   │   │   │   ├── Permission.java                   # Permiso granular
│   │   │   │   ├── Empresa.java                      # Empresa propietaria
│   │   │   │   ├── Vehiculo.java                     # Vehículo
│   │   │   │   ├── Chofer.java                       # Chofer / Conductor
│   │   │   │   ├── ChoferCategoria.java              # Relación chofer-categoría licencia
│   │   │   │   ├── CategoriaLicencia.java            # Categoría de licencia (A, B, C...)
│   │   │   │   ├── TipoVehiculo.java                 # Tipo de vehículo (camión, auto...)
│   │   │   │   ├── Marca.java                        # Marca de vehículo
│   │   │   │   ├── TipoCombustible.java              # Tipo de combustible
│   │   │   │   ├── Recorrido.java                    # Recorrido / Viaje
│   │   │   │   ├── TarjetaCombustible.java           # Tarjeta de combustible
│   │   │   │   └── Currency.java                     # Moneda (código ISO)
│   │   │   ├── dto/                                  # DTOs de request/response por dominio
│   │   │   │   ├── auth/                             # LoginRequestDto, AuthResponseDto, CambioPasswordRequest
│   │   │   │   ├── user/                             # UserRequest, UserResponse, UserAuditResponse
│   │   │   │   ├── role/                             # RoleRequest, RoleResponse
│   │   │   │   ├── permission/                       # PermissionRequest, PermissionResponse
│   │   │   │   ├── empresa/                          # EmpresaRequest, EmpresaResponse
│   │   │   │   ├── vehiculo/                         # VehiculoRequest, VehiculoResponse
│   │   │   │   ├── chofer/                           # ChoferRequest, ChoferResponse
│   │   │   │   ├── chofercategoria/                  # ChoferCategoriaRequest, ChoferCategoriaResponse, ChoferCategoriaEmbeddedResponse
│   │   │   │   ├── recorriddo/                      # RecorridoRequest, RecorridoResponse
│   │   │   │   ├── tipovehiculo/                     # TipoVehiculoRequest, TipoVehiculoResponse
│   │   │   │   ├── marca/                            # MarcaRequest, MarcaResponse
│   │   │   │   ├── tipocombustible/                  # TipoCombustibleRequest, TipoCombustibleResponse
│   │   │   │   ├── categorialicencia/               # CategoriaLicenciaRequest, CategoriaLicenciaResponse
│   │   │   │   ├── tarjetacombustible/               # TarjetaCombustibleRequest, TarjetaCombustibleResponse
│   │   │   │   ├── currency/                         # CurrencyRequest, CurrencyResponse
│   │   │   │   └── reporte/                          # VehiculoReporteData, LecturaDiariaResponse, AnalisisConsumoResponse, ReporteMovimientoMensualResponse
│   │   │   ├── repository/                           # 14 repositorios Spring Data JPA
│   │   │   ├── service/                              # 16 interfaces de servicio
│   │   │   ├── service/impl/                         # 16 implementaciones de servicio
│   │   │   ├── controller/                           # 15 controladores REST
│   │   │   ├── exception/                            # Manejo global de excepciones
│   │   │   │   ├── GlobalExceptionHandler.java       # @RestControllerAdvice
│   │   │   │   ├── ResourceNotFoundException.java     # Excepción 404
│   │   │   │   └── BusinessException.java             # Excepción 400
│   │   │   └── util/                                 # Utilidades compartidas
│   │   │       ├── PaginationUtils.java              # Conversión de paginación
│   │   │       ├── PaginationParams.java             # POJO de parámetros de paginación
│   │   │       └── AuditMapper.java                  # Mapeo User → UserAuditResponse
│   │   └── resources/
│   │       ├── application.properties                # Configuración común
│   │       ├── application-dev.properties            # Perfil desarrollo (H2)
│   │       └── application-prod.properties           # Perfil producción (PostgreSQL)
│   └── test/
│       └── java/com/fleet/management/service/impl/   # 13 clases de prueba unitaria
└── scripts/                                           # Scripts Python de refactorización
    ├── replace_to_pagination_utils.py
    ├── fix_controllers.py
    ├── fix_tolist.py
    ├── add_pagination.py
    └── replace_pageable.py
```

---

## 5. Modelo de Datos

### 5.1 BaseEntity — Superclase Abstracta

Todas las entidades heredan de `BaseEntity`, la cual proporciona campos comunes de identificación, auditoría y eliminación lógica:

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `Long` | PK, auto-increment `IDENTITY` | Identificador único |
| `activo` | `Boolean` | `NOT NULL`, default `true` | Flag de baja lógica (soft delete) |
| `fechaCreacion` | `LocalDateTime` | `NOT NULL`, `updatable=false` | Fecha de creación (auto vía `@PrePersist`) |
| `fechaActualizacion` | `LocalDateTime` | nullable | Fecha de última modificación (auto vía `@PreUpdate`) |
| `creadoPor` | `User` (lazy) | FK `creado_por_id` | Usuario que creó la entidad |
| `modificadoPor` | `User` (lazy) | FK `modificado_por_id` | Usuario que modificó la entidad por última vez |

**Mecanismo de auditoría:** Los campos `creadoPor` y `modificadoPor` se pueblan automáticamente en los callbacks `@PrePersist` y `@PreUpdate` de `BaseEntity`, leyendo el `AuthenticatedUser` del `SecurityContextHolder`. Esto permite capturar la entidad `User` completa (no solo el nombre) como referencia de auditoría.

### 5.2 Diagrama de Relaciones Entidad (ER)

```
                          ┌─────────────┐
                          │   Permission │
                          ├─────────────┤
                          │ id          │
                          │ name (UQ)   │
                          │ description │
                          │ [audit...]  │
                          └──────┬──────┘
                                 │ M:N
                          ┌──────▼──────┐     ┌──────────────────┐
                          │    Role     │────▶│      User        │
                          ├─────────────┤ M:N ├──────────────────┤
                          │ id          │     │ id               │
                          │ name (UQ)   │     │ email (UQ)      │
                          │ description │     │ password         │
                          │ [audit...]  │     │ roles (M:N)      │
                          └─────────────┘     │ empresa (FK)     │
                                              │ [audit...]       │
                                              └──────┬───────────┘
                                                     │ N:1

┌───────────────────┐        ┌───────────────────┐
│     Empresa       │        │  TipoVehiculo     │
├───────────────────┤        ├───────────────────┤
│ id                │────┐   │ id                │
│ codigo (UQ)       │    │   │ nombre (UQ)       │
│ nombre            │    │   │ descripcion       │
│ direccion         │    │   │ [audit...]        │
│ telefono          │    │   └────────┬──────────┘
│ email             │    │            │
│ [audit...]        │    │            │
└───────┬───────────┘    │            │
        │ 1:N            │            │
        │                │            │
┌───────▼───────┐  ┌────▼────┐  ┌─────▼──────┐   ┌──────────────────┐
│   Vehiculo    │  │ Chofer  │  │   Marca    │   │ TipoCombustible  │
├───────────────┤  ├─────────┤  ├────────────┤   ├──────────────────┤
│ id            │  │ id      │  │ id         │   │ id               │
│ matricula(UQ) │  │ empresa │  │ nombre(UQ) │   │ codigo (UQ)      │
│ numero_motor  │  │ nombre  │  │ descripcion│   │ denominacion     │
│   (UQ)        │  │ apellidos│ │ paisOrigen │   │ descripcion      │
│ modelo        │  │ carne_  │  │ [audit...] │   │ [audit...]       │
│ odometro      │  │ identidad│ └─────┬──────┘   └────────┬─────────┘
│ combustible   │  │ (UQ)   │        │                   │
│ indiceConsumo │  │ numero_ │        └──────┐    ┌──────┘
│ [FKs...]      │  │ licencia│               │    │
│ [audit...]    │  │ (UQ)   │               │    │
└───────┬───────┘  │ fecha_  │               │    │
        │          │ nacimiento│              │    │
        │ 1:N      │ [audit...]│              │    │
┌───────▼───────┐  └────┬─────┘              │    │
│   Recorrido   │       │ 1:N                │    │
├───────────────┤  ┌────▼──────────────┐     │    │
│ id            │  │  ChoferCategoria  │     │    │
│ vehiculo (FK) │  ├───────────────────┤     │    │
│ fecha         │  │ chofer_id +       │     │    │
│ kilometros    │  │ categoria_lic_id  │     │    │
│ odometroInic. │  │ (UQ compuesto)    │     │    │
│ combustibleIn.│  │ fechaEmision      │     │    │
│ consumo       │  └────────┬──────────┘     │    │
│ litrosAbast.  │           │ N:1             │    │
│ chofer (FK)   │  ┌────────▼──────────┐     │    │
│ tarjeta (FK)  │  │CategoriaLicencia  │     │    │
│ numeroChip    │  ├───────────────────┤     │    │
│ lugarAbast.   │  │ id                │     │    │
│ importeAbast. │  │ codigo (UQ, 1ch)  │     │    │
│ [audit...]    │  │ denominacion      │     │    │
└───────────────┘  │ descripcion       │     │    │
                   │ [audit...]        │     │    │
                   └──────────────────┘     │    │
                                             │    │
       Vehiculo ──┬── empresa (FK, NOT NULL)  │
                  ├── tipoVehiculo (FK, NOT NULL)  │
                  ├── marca (FK, NOT NULL) ──────┘    │
                  ├── chofer (FK, nullable)             │
                  └── tipoCombustible (FK, NOT NULL) ───┘

┌───────────────────┐     ┌───────────────┐
│ TarjetaCombustible│     │   Currency     │
├───────────────────┤     ├───────────────┤
│ id                │     │ id            │
│ numero (UQ)       │────▶│ isoCode (UQ)  │
│ saldo             │ N:1 │ descripcion   │
│ empresa (FK)      │     │ [audit...]    │
│ [audit...]        │     └───────────────┘
└───────────────────┘

Unicidad compuesta en Recorrido: (vehiculo_id, fecha)
Unicidad compuesta en ChoferCategoria: (chofer_id, categoria_licencia_id)
```

### 5.3 Detalle de Entidades

#### 5.3.1 User

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `email` | `String(100)` | `NOT NULL`, `UNIQUE` | Correo electrónico (login) |
| `password` | `String(255)` | `NOT NULL` | Contraseña encriptada con BCrypt |
| `roles` | `Set<Role>` | M:N via `user_roles` | Roles asignados al usuario |
| `empresa` | `Empresa` | N:1, LAZY, nullable | Empresa a la que pertenece el usuario |

#### 5.3.2 Role

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `name` | `String(50)` | `NOT NULL`, `UNIQUE` | Nombre del rol (ej: SUPER_ADMIN, ADMIN, USER) |
| `description` | `String(255)` | nullable | Descripción del rol |
| `permissions` | `Set<Permission>` | M:N via `role_permissions` | Permisos asignados al rol |

#### 5.3.3 Permission

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `name` | `String` | `UNIQUE` | Nombre del permiso (ej: `vehiculo:read`) |
| `description` | `String` | nullable | Descripción del permiso |

#### 5.3.4 Empresa

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `codigo` | `String(30)` | `NOT NULL`, `UNIQUE` | Código identificador de la empresa |
| `nombre` | `String(150)` | `NOT NULL` | Nombre de la empresa |
| `direccion` | `String(255)` | nullable | Dirección física |
| `telefono` | `String(20)` | nullable | Teléfono de contacto |
| `email` | `String(100)` | nullable, `@Email` | Correo electrónico |

#### 5.3.5 Vehiculo

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `empresa` | `Empresa` | `NOT NULL`, FK | Empresa propietaria |
| `tipoVehiculo` | `TipoVehiculo` | `NOT NULL`, FK | Tipo de vehículo |
| `marca` | `Marca` | `NOT NULL`, FK | Marca del vehículo |
| `chofer` | `Chofer` | nullable, FK | Chofer asignado (puede ser null) |
| `tipoCombustible` | `TipoCombustible` | `NOT NULL`, FK | Tipo de combustible |
| `matricula` | `String(20)` | `NOT NULL`, `UNIQUE` | Matrícula / placa |
| `modelo` | `String(100)` | nullable | Modelo del vehículo |
| `numeroMotor` | `String(50)` | `NOT NULL`, `UNIQUE` | Número de motor |
| `odometro` | `BigInteger` | `NOT NULL`, `>= 0` | Kilometraje actual del odómetro |
| `combustible` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Nivel actual de combustible (litros) |
| `ultimoMantenimiento` | `LocalDate` | `@PastOrPresent` | Fecha del último mantenimiento |
| `odometroUltimoMantenimiento` | `BigInteger` | `>= 0` | Odómetro en el último mantenimiento |
| `indiceConsumo` | `BigDecimal(10,2)` | `NOT NULL`, `> 0` | Consumo por cada 100 km (L/100km) |

#### 5.3.6 Chofer

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `empresa` | `Empresa` | `NOT NULL`, FK | Empresa a la que pertenece |
| `nombre` | `String(50)` | `NOT NULL` | Nombre del chofer |
| `apellidos` | `String(100)` | `NOT NULL` | Apellidos del chofer |
| `carneIdentidad` | `String(20)` | `NOT NULL`, `UNIQUE` | Carné de identidad |
| `numeroLicencia` | `String(30)` | `NOT NULL`, `UNIQUE` | Número de licencia de conducir |
| `fechaNacimiento` | `LocalDate` | `NOT NULL`, `@Past` | Fecha de nacimiento |
| `categorias` | `List<ChoferCategoria>` | 1:N, cascade ALL | Categorías de licencia con fecha de emisión |

#### 5.3.7 ChoferCategoria

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `chofer` | `Chofer` | FK (parte de UK compuesto) | Chofer |
| `categoriaLicencia` | `CategoriaLicencia` | FK (parte de UK compuesto) | Categoría de licencia |
| `fechaEmision` | `LocalDate` | `NOT NULL` | Fecha de emisión de la categoría |

**Unicidad compuesta:** `(chofer_id, categoria_licencia_id)`

#### 5.3.8 CategoriaLicencia

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `codigo` | `String` | `UNIQUE` (1 carácter) | Código de categoría (A, B, C, D, E...) |
| `denominacion` | `String` | `NOT NULL` | Denominación de la categoría |
| `descripcion` | `String` | nullable | Descripción detallada |

#### 5.3.9 TipoVehiculo

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `nombre` | `String` | `UNIQUE`, `NOT NULL` | Nombre del tipo (Camión, Auto, Ómnibus...) |
| `descripcion` | `String` | nullable | Descripción del tipo |

#### 5.3.10 Marca

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `nombre` | `String` | `UNIQUE`, `NOT NULL` | Nombre de la marca |
| `descripcion` | `String` | nullable | Descripción de la marca |
| `paisOrigen` | `String` | nullable | País de origen de la marca |

#### 5.3.11 TipoCombustible

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `codigo` | `String` | `UNIQUE` | Código del tipo (ej: diesel, gasolina) |
| `denominacion` | `String` | `NOT NULL` | Denominación completa |
| `descripcion` | `String` | nullable | Descripción del tipo de combustible |

#### 5.3.12 Recorrido

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `vehiculo` | `Vehiculo` | FK (parte de UK compuesto) | Vehículo que realiza el recorrido |
| `fecha` | `LocalDate` | FK (parte de UK compuesto), `@PastOrPresent` | Fecha del recorrido |
| `kilometros` | `Integer` | `NOT NULL`, `>= 1` | Kilómetros recorridos |
| `odometroInicial` | `BigInteger` | `NOT NULL`, `>= 0` | Odómetro del vehículo antes del recorrido |
| `combustibleInicial` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Combustible en depósito antes del recorrido |
| `consumo` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Combustible consumido = (índiceConsumo × km) / 100 |
| `litrosAbastecidos` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Litros repostados durante el recorrido |
| `chofer` | `Chofer` | nullable, FK | Chofer que realizó el recorrido |
| `numeroChip` | `String(50)` | nullable | Número de chip de la tarjeta de combustible |
| `lugarAbastecimiento` | `String(100)` | nullable | Lugar donde se repostó |
| `tarjetaCombustible` | `TarjetaCombustible` | nullable, FK | Tarjeta de combustible utilizada |
| `importeAbastecido` | `Double` | nullable | Importe monetario del abastecimiento |

**Unicidad compuesta:** `(vehiculo_id, fecha)`

#### 5.3.13 TarjetaCombustible

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `numero` | `String(50)` | `NOT NULL`, `UNIQUE` | Número de tarjeta |
| `saldo` | `Double` | `NOT NULL`, `> 0` | Saldo disponible en la tarjeta |
| `currency` | `Currency` | `NOT NULL`, FK | Moneda asociada a la tarjeta |
| `empresa` | `Empresa` | `NOT NULL`, FK | Empresa propietaria de la tarjeta |

#### 5.3.14 Currency

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `isoCode` | `String(10)` | `NOT NULL`, `UNIQUE` | Código ISO de la moneda (ej: USD, CUP, EUR) |
| `descripcion` | `String(100)` | `NOT NULL` | Descripción de la moneda |

---

## 6. API REST — Endpoints

### 6.1 Autenticación

| Método | Ruta | Descripción | Auth | Rol requerido |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión, retorna JWT | No | — |
| `POST` | `/api/auth/logout` | Cerrar sesión (no-op, stateless) | No | — |
| `GET` | `/api/auth/me` | Obtener usuario actual (con empresa) | Sí | Cualquiera |
| `PUT` | `/api/auth/cambiar-password` | Cambiar contraseña del usuario | Sí | Cualquiera |

**`CambioPasswordRequest`:** `userId`, `passwordAnterior`, `nuevaPassword`, `confirmacionPassword` — valida que la contraseña anterior coincida y que la nueva contraseña y su confirmación sean iguales.

### 6.2 Gestión de Usuarios

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/users` | Listar usuarios (paginado, filtrable) | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/{id}` | Obtener usuario por ID | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/email/{email}` | Obtener usuario por email | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/empresa/{empresaId}` | Usuarios por empresa (paginado, filtrable) | SUPER_ADMIN, ADMIN |
| `POST` | `/api/users` | Crear usuario | SUPER_ADMIN, ADMIN |
| `PUT` | `/api/users/{id}` | Actualizar usuario | SUPER_ADMIN, ADMIN |
| `DELETE` | `/api/users/{id}` | Eliminar usuario (lógico) | SUPER_ADMIN, ADMIN |

### 6.3 Gestión de Roles

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/roles/**` | Listar/obtener roles (solo lectura) | SUPER_ADMIN, ADMIN |
| `POST/PUT/DELETE` | `/api/roles/**` | Crear, actualizar, eliminar roles | SUPER_ADMIN |

### 6.4 Gestión de Permisos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| Todos | `/api/permissions/**` | CRUD completo de permisos | SUPER_ADMIN |

### 6.5 Gestión de Empresas

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/empresas` | Listar empresas (paginado, filtrable) | Autenticado |
| `GET` | `/api/empresas/{id}` | Obtener empresa por ID | Autenticado |
| `GET` | `/api/empresas/codigo/{codigo}` | Buscar empresa por código | Autenticado |
| `POST` | `/api/empresas` | Crear empresa | Autenticado |
| `PUT` | `/api/empresas/{id}` | Actualizar empresa | Autenticado |
| `DELETE` | `/api/empresas/{id}` | Eliminar empresa (lógico) | Autenticado |

### 6.6 Gestión de Vehículos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/vehiculos` | Listar vehículos (paginado, filtrable) | Autenticado |
| `GET` | `/api/vehiculos/{id}` | Obtener vehículo por ID | Autenticado |
| `GET` | `/api/vehiculos/sin-chofer` | Vehículos sin chofer asignado | Autenticado |
| `GET` | `/api/vehiculos/chofer/{choferId}` | Vehículos por chofer | Autenticado |
| `GET` | `/api/vehiculos/tipo-vehiculo/{tipoVehiculoId}` | Vehículos por tipo de vehículo | Autenticado |
| `GET` | `/api/vehiculos/tipo-combustible/{tipoCombustibleId}` | Vehículos por tipo de combustible | Autenticado |
| `GET` | `/api/vehiculos/empresa/{empresaId}` | Vehículos por empresa (paginado, filtrable) | Autenticado |
| `GET` | `/api/vehiculos/reporte-movimiento-mensual/{vehiculoId}?mes=&anio=` | Reporte mensual de movimiento | Autenticado |
| `POST` | `/api/vehiculos` | Crear vehículo | Autenticado |
| `PUT` | `/api/vehiculos/{id}` | Actualizar vehículo | Autenticado |
| `DELETE` | `/api/vehiculos/{id}` | Eliminar vehículo (lógico) | Autenticado |

### 6.7 Gestión de Choferes

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/choferes` | Listar choferes (paginado, filtrable) | Autenticado |
| `GET` | `/api/choferes/{id}` | Obtener chofer por ID | Autenticado |
| `GET` | `/api/choferes/empresa/{empresaId}` | Choferes por empresa (paginado, filtrable) | Autenticado |
| `POST` | `/api/choferes` | Crear chofer (con categorías) | Autenticado |
| `PUT` | `/api/choferes/{id}` | Actualizar chofer (reemplaza categorías) | Autenticado |
| `DELETE` | `/api/choferes/{id}` | Eliminar chofer (lógico + categorías) | Autenticado |

### 6.8 Gestión de Recorridos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/recorridos` | Listar recorridos (paginado) | Autenticado |
| `GET` | `/api/recorridos/{id}` | Obtener recorrido por ID | Autenticado |
| `GET` | `/api/recorridos/vehiculo/{vehiculoId}?from=&to=` | Recorridos por vehículo (rango de fechas opcional) | Autenticado |
| `POST` | `/api/recorridos` | Crear recorrido | Autenticado |
| `PUT` | `/api/recorridos/{id}` | Actualizar recorrido | Autenticado |
| `DELETE` | `/api/recorridos/{id}` | Eliminar recorrido (lógico, revierte odómetro/combustible) | Autenticado |

### 6.9 Gestión de Tarjetas de Combustible

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/tarjetas-combustible` | Listar tarjetas (paginado, filtrable) | Autenticado |
| `GET` | `/api/tarjetas-combustible/{id}` | Obtener por ID | Autenticado |
| `GET` | `/api/tarjetas-combustible/numero/{numero}` | Buscar por número de tarjeta | Autenticado |
| `GET` | `/api/tarjetas-combustible/empresa/{empresaId}` | Tarjetas por empresa (paginado, filtrable) | Autenticado |
| `POST` | `/api/tarjetas-combustible` | Crear tarjeta | Autenticado |
| `PUT` | `/api/tarjetas-combustible/{id}` | Actualizar tarjeta | Autenticado |
| `DELETE` | `/api/tarjetas-combustible/{id}` | Eliminar tarjeta (lógico) | Autenticado |

### 6.10 Gestión de Monedas

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/currencies` | Listar monedas (paginado) | Autenticado |
| `GET` | `/api/currencies/{id}` | Obtener por ID | Autenticado |
| `GET` | `/api/currencies/iso-code/{isoCode}` | Buscar por código ISO | Autenticado |
| `POST` | `/api/currencies` | Crear moneda | Autenticado |
| `PUT` | `/api/currencies/{id}` | Actualizar moneda | Autenticado |
| `DELETE` | `/api/currencies/{id}` | Eliminar moneda (lógico) | Autenticado |

### 6.11 Catálogos Maestros

Todos los catálogos siguen el patrón CRUD estándar con paginación y requieren autenticación:

- **Tipos de Vehículo:** `/api/tipos-vehiculo`
- **Marcas:** `/api/marcas`
- **Tipos de Combustible:** `/api/tipos-combustible`
- **Categorías de Licencia:** `/api/categorias-licencia`
- **Categorías de Chofer:** `/api/choferes-categorias`

### 6.12 Parámetros de Paginación y Filtrado

| Parámetro | Default | Descripción |
|---|---|---|
| `page` | `0` | Número de página (1-based desde el cliente, convertido a 0-based internamente) |
| `perPage` | `20` | Elementos por página |
| `sort` | `id` | Campo de ordenamiento |
| `sortOrder` | `ASC` | Dirección: `ASC` o `DESC` |
| `filter` | (none) | Texto libre para filtrado por nombre/código (solo en endpoints que lo soportan) |

**Respuesta paginada estándar (Spring Page):**

```json
{
  "content": [...],
  "pageable": { "pageNumber": 0, "pageSize": 20, ... },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "empty": false
}
```

---

## 7. Seguridad y Autenticación

### 7.1 Flujo de Autenticación JWT

```
1. Cliente → POST /api/auth/login { email, password }
2. AuthenticationManager → authenticate(username, password)
3. JwtService → generateToken(user)
   - subject: email del usuario
   - claim "roles": lista de nombres de roles
   - expiración: 24 horas
   - algoritmo: HMAC-SHA
4. Respuesta → { token: "eyJhbG..." }
5. Cliente → GET /api/vehiculos con Header: Authorization: Bearer eyJhbG...
6. JwtAuthenticationFilter → extrae token → valida → carga AuthenticatedUser
7. AuthenticatedUser envuelve la entidad User de JPA
8. SecurityContext populated → Authorized
```

### 7.2 Componentes de Seguridad

| Componente | Responsabilidad |
|---|---|
| `SecurityConfig` | Configura la cadena de filtros, CORS, reglas de acceso por rol. Usa `@EnableMethodSecurity` para autorización a nivel de método |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` que extrae y valida el token Bearer en cada petición |
| `JwtService` | Genera tokens JWT con claims de roles; valida firma, expiración y sujeto |
| `AuthenticatedUser` | Implementa `UserDetails` envolviendo la entidad `User` de JPA, permitiendo acceso a la entidad completa desde el `SecurityContext` |
| `CustomUserDetailsService` | Carga el usuario desde la base de datos, asignando autoridades `ROLE_` + permisos. Verifica que el usuario esté activo |

### 7.3 Reglas de Acceso

| Ruta | Acceso |
|---|---|
| `/api/auth/**` | Público (sin autenticación) |
| `/api/users/**` | Requiere rol `SUPER_ADMIN` o `ADMIN` |
| `/api/roles/**` (GET) | Requiere rol `SUPER_ADMIN` o `ADMIN` |
| `/api/roles/**` (POST/PUT/DELETE) | Requiere rol `SUPER_ADMIN` |
| `/api/permissions/**` | Requiere rol `SUPER_ADMIN` |
| `/swagger-ui/**`, `/v3/api-docs/**` | Público |
| `/h2-console/**` | Público (solo desarrollo) |
| Cualquier otra ruta `/api/**` | Requiere autenticación (cualquier rol) |

### 7.4 Configuración CORS

| Propiedad | Valor |
|---|---|
| Orígenes permitidos | `http://localhost:4200`, `http://localhost:3000`, `http://localhost:5173` |
| Métodos permitidos | GET, POST, PUT, PATCH, DELETE, OPTIONS |
| Headers permitidos | `*` (todos) |
| Credenciales | Permitidas |
| Max-Age | 3600 segundos |

### 7.5 Autoridades Granted

El `CustomUserDetailsService` asigna dos tipos de autoridades al autenticar un usuario:

1. **Autoridades de rol:** `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_USER` (por cada rol asignado al usuario)
2. **Autoridades de permiso:** `vehiculo:read`, `vehiculo:write`, etc. (por cada permiso en cada rol)

Esto permite el uso de `@PreAuthorize("hasRole('SUPER_ADMIN')")` o `@PreAuthorize("hasAuthority('vehiculo:write')")` en futuros endpoints granulares.

---

## 8. Capa de Servicios — Lógica de Negocio

### 8.1 Patrón CRUD Estándar

Todos los servicios implementan el siguiente patrón, con variaciones específicas por dominio:

| Operación | Comportamiento general |
|---|---|
| `findAll(Pageable)` | `repository.findAllByActivoTrue(pageable)` → mapea a `Page<XResponse>` |
| `findAll(String filter, Pageable)` | Búsqueda con filtro opcional por texto libre (nombre/código) |
| `findById(Long)` | Busca por ID, lanza `ResourceNotFoundException` si no existe o está inactivo |
| `create(XRequest)` | Valida campos únicos, lanza `BusinessException` si hay duplicados, guarda y retorna respuesta |
| `update(Long, XRequest)` | Valida campos únicos excluyendo la entidad actual, actualiza y retorna respuesta |
| `delete(Long)` | Baja lógica: establece `activo = false` y guarda |

### 8.2 Lógica de Negocio Específica por Servicio

#### 8.2.1 RecorridoServiceImpl — La lógica más compleja

El servicio de recorridos es el núcleo del dominio de negocio y contiene las reglas transaccionales más críticas del sistema:

**Creación de recorrido (`create`):**
1. Valida que el vehículo exista
2. Verifica unicidad: no puede existir más de un recorrido para el mismo vehículo en la misma fecha
3. Verifica orden cronológico: no se puede insertar un recorrido con fecha anterior si ya existe un recorrido posterior para el mismo vehículo
4. Registra el `combustibleInicial` como el combustible actual del vehículo
5. Calcula automáticamente el consumo: `consumo = (índiceConsumo × kilómetros) / 100`, redondeado a 2 decimales
6. Valida disponibilidad de combustible: `combustibleRestante = combustibleActual - consumo + litrosAbastecidos >= 0`
7. Registra el `odometroInicial` como el odómetro actual del vehículo
8. Si se usa tarjeta de combustible, valida que el `importeAbastecido` no exceda el saldo disponible y descuenta del saldo
9. Actualiza atómicamente el vehículo: incrementa el odómetro y decrementa el combustible
10. Todas las operaciones se ejecutan dentro de una transacción `@Transactional`

**Actualización de recorrido (`update`):**
1. No permite cambiar el vehículo ni la fecha del recorrido
2. Restaura el consumo antiguo al vehículo antes de calcular el nuevo
3. Restaura el combustible inicial y el saldo de la tarjeta (si aplica)
4. Recalcula el consumo con los nuevos kilómetros
5. Verifica disponibilidad de combustible y saldo de tarjeta con los nuevos valores
6. Restaura y recalcula el odómetro del vehículo
7. Actualiza atómicamente la entidad y el vehículo

**Eliminación de recorrido (`delete`):**
1. Restaura los kilómetros al odómetro del vehículo (resta)
2. Restaura el combustible consumido al vehículo (suma)
3. Restaura el saldo de la tarjeta de combustible (si aplica)
4. Ejecuta la baja lógica del recorrido (`activo = false`)

#### 8.2.2 ChoferServiceImpl — Creación anidada de categorías

- El `ChoferRequest` incluye una lista de `CategoriaConFechaRequest` (pares `categoriaLicenciaId` + `fechaEmision`)
- Al crear un chofer, se crean automáticamente las relaciones `ChoferCategoria` con sus fechas de emisión
- Al actualizar, se eliminan todas las categorías existentes y se recrean con la nueva lista
- Al eliminar, se desactivan también todas las categorías del chofer

#### 8.2.3 VehiculoServiceImpl — Validación de doble unicidad

- Valida que tanto la `matrícula` como el `número de motor` sean únicos al crear y actualizar
- Al actualizar, excluye el ID del vehículo actual de la verificación de unicidad
- Delega al servicio de recorridos la generación del reporte mensual de movimiento

#### 8.2.4 TarjetaCombustibleServiceImpl — Control de saldo

- Al crear una tarjeta, el saldo inicial debe ser positivo
- La validación de saldo en recorridos se realiza en `RecorridoServiceImpl`, no aquí
- Soporta filtrado por empresa y búsqueda por número

#### 8.2.5 AuthServiceImpl — Autenticación y cambio de contraseña

- `login`: Autentica vía `AuthenticationManager`, genera token JWT, retorna `AuthResponseDto` con token, email, roles y permisos
- `getCurrentUser`: Retorna la entidad `User` del `SecurityContext`
- `cambiarPassword`: Valida contraseña anterior, verifica coincidencia de nueva contraseña y confirmación, encripta y guarda

### 8.3 Mapeo Entidad ↔ DTO

El mapeo se realiza de forma **manual** en cada `XServiceImpl` mediante métodos privados `toResponse()` y `toEntity()` o `toResumido()`. No se utiliza MapStruct ni ninguna librería de mapeo automático.

**Patrón típico:**

```java
private XResponse toResponse(X entity) {
    return XResponse.builder()
            .id(entity.getId())
            .campo1(entity.getCampo1())
            .campo2(entity.getCampo2())
            .activo(entity.getActivo())
            .fechaCreacion(entity.getFechaCreacion())
            .fechaActualizacion(entity.getFechaActualizacion())
            .creadoPor(AuditMapper.toAuditResponse(entity.getCreadoPor()))
            .modificadoPor(AuditMapper.toAuditResponse(entity.getModificadoPor()))
            .build();
}
```

**Para relaciones anidadas**, se crean métodos auxiliares como `toEmpresaResumido()` que construyen respuestas parciales para evitar referencias circulares.

---

## 9. Reportes

### 9.1 Reporte de Movimiento Mensual por Vehículo

**Endpoint:** `GET /api/vehiculos/reporte-movimiento-mensual/{vehiculoId}?mes={mes}&anio={anio}`

Este reporte genera un análisis completo del consumo de combustible de un vehículo durante un mes específico. Se calcula de forma iterativa día a día simulando el estado del depósito de combustible y el odómetro.

**Parámetros:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `vehiculoId` | Long (path) | ID del vehículo |
| `mes` | Integer (query) | Mes (1-12) |
| `anio` | Integer (query) | Año (ej: 2026) |

**Respuesta (`ReporteMovimientoMensualResponse`):**

```json
{
  "vehiculo": {
    "marca": "Toyota",
    "numeroMotor": "ABC123",
    "tipoCombustible": "Diesel",
    "normaConsumo": 8.50,
    "matricula": "A-123-456",
    "chofer": { ... }
  },
  "lecturas": [
    {
      "dia": 1,
      "odometro": 15000,
      "combustibleEnDeposito": 50.00,
      "combustibleConsumido": 8.50,
      "combustibleAbastecido": 0.00,
      "saldoCombustible": 41.50,
      "kilometrosRecorridos": 100
    }
  ],
  "analisis": {
    "combustibleInicial": 50.00,
    "combustibleRecibido": 20.00,
    "combustibleConsumido": 45.50,
    "existenciaFinal": 24.50,
    "kilometrosRecorridos": 535,
    "consumidoSegunNorma": 45.48
  }
}
```

**DTOs del reporte:**

| DTO | Campos | Descripción |
|---|---|---|
| `VehiculoReporteData` | `marca`, `numeroMotor`, `tipoCombustible`, `normaConsumo`, `matricula`, `chofer` | Datos del vehículo para el encabezado del reporte |
| `LecturaDiariaResponse` | `dia`, `odometro`, `combustibleEnDeposito`, `combustibleConsumido`, `combustibleAbastecido`, `saldoCombustible`, `kilometrosRecorridos` | Estado diario simulado del depósito |
| `AnalisisConsumoResponse` | `combustibleInicial`, `combustibleRecibido`, `combustibleConsumido`, `existenciaFinal`, `kilometrosRecorridos`, `consumidoSegunNorma` | Resumen mensual con comparación contra la norma |

**Lógica de cálculo:**

El reporte procesa los recorridos del vehículo ordenados por fecha dentro del mes solicitado. Para cada día del mes, se calcula:
- Si hay recorrido ese día: se registra el consumo real, los kilómetros recorridos y el abastecimiento
- Si no hay recorrido: se copian los valores del día anterior (odómetro y combustible se mantienen)
- El `saldoCombustible` se calcula como: `combustibleEnDeposito - combustibleConsumido + combustibleAbastecido`
- El análisis final compara el consumo real total con el consumo esperado según la norma (índiceConsumo)

---

## 10. Manejo de Excepciones

### 10.1 Excepciones Personalizadas

| Excepción | Código HTTP | Uso |
|---|---|---|
| `ResourceNotFoundException` | 404 Not Found | Entidad no encontrada por ID o búsqueda |
| `BusinessException` | 400 Bad Request | Violación de regla de negocio (duplicados, reglas de recorrido, etc.) |
| `MethodArgumentNotValidException` | 400 Bad Request | Errores de validación de campos (Jakarta Bean Validation) |
| `Exception` (catch-all) | 500 Internal Server Error | Errores no esperados |

### 10.2 Formato de Respuesta de Error (ApiError)

```json
{
  "timestamp": "2026-08-26T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Vehiculo no encontrado con id: 999",
  "path": "/api/vehiculos/999",
  "fieldErrors": null
}
```

Para errores de validación, `fieldErrors` contiene un mapa campo → mensaje:

```json
{
  "timestamp": "2026-08-26T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Errores de validacion",
  "path": "/api/vehiculos",
  "fieldErrors": {
    "matricula": "La matricula es obligatoria",
    "odometro": "El odometro no puede ser negativo"
  }
}
```

---

## 11. Utilidades y Patrones Comunes

### 11.1 PaginationUtils

Clase utilitaria estática que centraliza la lógica de paginación:

- **`params(int page, int perPage, String sortBy, String sortOrder)`:** Convierte los parámetros del controlador (1-based) a `PaginationParams` (0-based). Resta 1 al número de página para convertir de base-1 a base-0, con un mínimo de 0.
- **`of(PaginationParams)`:** Convierte `PaginationParams` a Spring `PageRequest` con ordenamiento.

### 11.2 AuditMapper

Utilidad estática que convierte una entidad `User` a `UserAuditResponse` (solo `id` + `email`), evitando referencias circulares en las respuestas DTO que incluyen campos de auditoría.

### 11.3 Patrón de Nombres

| Capa | Convención | Ejemplo |
|---|---|---|
| Entidad | Sustantivo singular | `Vehiculo` |
| Repositorio | Entidad + `Repository` | `VehiculoRepository` |
| Servicio (interfaz) | Entidad + `Service` | `VehiculoService` |
| Servicio (impl) | Entidad + `ServiceImpl` | `VehiculoServiceImpl` |
| Controlador | Entidad + `Controller` | `VehiculoController` |
| DTO Request | Entidad + `Request` | `VehiculoRequest` |
| DTO Response | Entidad + `Response` | `VehiculoResponse` |
| DTO de auditoría | `UserAuditResponse` | Solo `id` + `email` |

### 11.4 Nomenclatura de Tablas

Las tablas en la base de datos siguen la convención de **plural en español con snake_case**:

| Entidad | Tabla |
|---|---|
| `User` | `users` |
| `Role` | `roles` |
| `Permission` | `permissions` |
| `Empresa` | `empresas` |
| `Vehiculo` | `vehiculos` |
| `Chofer` | `choferes` |
| `ChoferCategoria` | `choferes_categorias` |
| `CategoriaLicencia` | `categorias_licencia` |
| `TipoVehiculo` | `tipos_vehiculo` |
| `Marca` | `marcas` |
| `TipoCombustible` | `tipos_combustible` |
| `Recorrido` | `recorridos` |
| `TarjetaCombustible` | `tarjetas_combustible` |
| `Currency` | `currencies` |
| `User` ↔ `Role` | `user_roles` |
| `Role` ↔ `Permission` | `role_permissions` |

---

## 12. Inicialización de Datos

### 12.1 DataInitializer (Bootstrap)

Se ejecuta automáticamente al iniciar la aplicación mediante `CommandLineRunner`. Crea los datos iniciales solo si no existen (idempotente):

**Empresa por defecto:**

| Campo | Valor |
|---|---|
| Código | `EMP-ADMIN` |
| Nombre | `Empresa de Administracion` |

**31 Permisos creados:**

| Dominio | Permisos |
|---|---|
| `user` | `user:read`, `user:write`, `user:delete` |
| `role` | `role:read`, `role:write`, `role:delete` |
| `permission` | `permission:read`, `permission:write`, `permission:delete` |
| `vehiculo` | `vehiculo:read`, `vehiculo:write`, `vehiculo:delete` |
| `chofer` | `chofer:read`, `chofer:write`, `chofer:delete` |
| `recorrido` | `recorrido:read`, `recorrido:write`, `recorrido:delete` |
| `empresa` | `empresa:read`, `empresa:write`, `empresa:delete` |
| `marca` | `marca:read`, `marca:write`, `marca:delete` |
| `tipo_vehiculo` | `tipo_vehiculo:read`, `tipo_vehiculo:write`, `tipo_vehiculo:delete` |
| `tipo_combustible` | `tipo_combustible:read`, `tipo_combustible:write`, `tipo_combustible:delete` |
| `categoria_licencia` | `categoria_licencia:read`, `categoria_licencia:write`, `categoria_licencia:delete` |

**Roles creados:**

| Rol | Descripción | Permisos |
|---|---|---|
| `SUPER_ADMIN` | Rol con acceso total al sistema incluyendo roles y permisos | Todos los 31 permisos |
| `ADMIN` | Rol administrativo sin acceso a roles ni permisos | 25 permisos (excluye `role:*` y `permission:*`) |
| `USER` | Rol de usuario estándar con acceso de lectura | Ninguno (por defecto) |

**Usuario administrador:**

| Campo | Valor |
|---|---|
| Email | `admin@fleet.com` |
| Password | `admin123` (encriptada con BCrypt) |
| Rol | `SUPER_ADMIN` |
| Empresa | `EMP-ADMIN` (Empresa de Administracion) |

---

## 13. Decisiones de Diseño

### 13.1 Baja Lógica (Soft Delete) Universal

**Decisión:** Todas las entidades incluyen un campo `activo` (Boolean, default `true`). La eliminación nunca borra registros físicamente; simplemente cambia `activo` a `false`.

**Justificación:** En un sistema de gestión de flotas, la integridad histórica de los datos es crítica. Un vehículo dado de baja puede tener recorridos asociados; eliminarlo físicamente destruiría la cadena de auditoría. La baja lógica permite mantener la trazabilidad completa.

**Implementación:** Todos los repositorios exponen `findAllByActivoTrue(Pageable)` y los servicios filtran exclusivamente por `activo = true` en las operaciones de lectura.

### 13.2 Auditoría Automática vía BaseEntity + SecurityContext

**Decisión:** Los campos de auditoría (`creadoPor`, `modificadoPor`, `fechaCreacion`, `fechaActualizacion`) se pueblan automáticamente en los callbacks JPA `@PrePersist` y `@PreUpdate` de `BaseEntity`, leyendo el `AuthenticatedUser` del `SecurityContextHolder`.

**Justificación:** A diferencia del enfoque estándar de Spring Data JPA Auditing (`@CreatedBy`, `@LastModifiedBy`), este diseño permite capturar la entidad `User` completa (no solo el username) como referencia de auditoría. Esto habilita mostrar el nombre y email del auditor en las respuestas DTO sin consultas adicionales.

**Trade-off:** El `BaseEntity` tiene una dependencia directa al `SecurityContextHolder`, lo que acopla la capa de modelo al contexto de seguridad de Spring. Durante el bootstrap (`DataInitializer`), el `SecurityContext` está vacío, por lo que los campos de auditoría quedan como `null`.

### 13.3 AuthenticatedUser envuelve la entidad User

**Decisión:** `AuthenticatedUser` implementa `UserDetails` y envuelve la entidad JPA `User` completa en lugar de solo almacenar credenciales.

**Justificación:** Necesario para que `BaseEntity.getAuthenticatedUser()` pueda retornar la entidad `User` y asignarla directamente a los campos `creadoPor`/`modificadoPor`. Esto evita una consulta adicional al repositorio en cada operación de auditoría.

### 13.4 Mapeo DTO Manual (sin MapStruct)

**Decisión:** El mapeo entre entidades y DTOs se realiza manualmente en cada `ServiceImpl`.

**Justificación (presumida):** Simplicidad inicial del proyecto, evitar una dependencia adicional.

**Trade-off:** Genera duplicación significativa de código. Por ejemplo, `toEmpresaResponse()` está duplicado en múltiples servicios. Una refactorización futura con MapStruct o con mappers compartidos reduciría el código y mejoraría el mantenimiento.

### 13.5 Tres Niveles de Roles (SUPER_ADMIN / ADMIN / USER)

**Decisión:** Se introdujo un tercer rol `SUPER_ADMIN` con acceso total, diferenciándolo del rol `ADMIN` que no puede gestionar roles ni permisos.

**Justificación:** En un sistema multi-empresa, el rol `ADMIN` se reserva para administradores de empresa que no deben poder modificar la estructura de roles y permisos del sistema. Solo el `SUPER_ADMIN` tiene control total sobre la configuración de seguridad.

**Implementación:** `SecurityConfig` utiliza reglas diferenciadas: `GET /api/roles/**` permite ambos roles, pero operaciones de escritura requieren `SUPER_ADMIN` exclusivamente.

### 13.6 Asociación Usuario-Empresa

**Decisión:** Se agregó un campo `empresa` (nullable, LAZY) a la entidad `User`, permitiendo asociar usuarios a empresas.

**Justificación:** Necesario para el modelo multi-empresa donde los usuarios (choferes, administradores de empresa) pertenecen a una empresa específica.

**Protección:** El `DataInitializer` crea automáticamente la empresa por defecto (`EMP-ADMIN`) y la asigna al usuario administrador. El endpoint `/api/auth/me` retorna la información de la empresa del usuario autenticado.

### 13.7 Filtrado por Texto Libre en Endpoints de Listado

**Decisión:** Los endpoints de listado principales soportan un parámetro `filter` opcional que permite buscar por texto libre (coincidencia parcial, case-insensitive) en campos de nombre o código.

**Justificación:** Mejora significativamente la experiencia del usuario al permitir búsquedas rápidas sin necesidad de endpoints dedicados para cada criterio de búsqueda.

### 13.8 Sin Migraciones de Base de Datos (ddl-auto=update)

**Decisión:** Se utiliza `spring.jpa.hibernate.ddl-auto=update` para generar y actualizar el esquema automáticamente.

**Justificación:** Rapidez en el desarrollo inicial.

**Trade-off:** Este enfoque es frágil para cambios de esquema en producción (no genera `ALTER TABLE` para renombrar o eliminar columnas). Para producción, se recomienda migrar a Flyway o Liquibase.

### 13.9 Respuestas DTO Anidadas Completas

**Decisión:** Los DTOs de respuesta incluyen objetos anidados completos. Por ejemplo, `RecorridoResponse` contiene un `VehiculoResponse` completo, que a su vez contiene `EmpresaResponse`, `MarcaResponse`, etc.

**Justificación:** Simplifica el consumo del API para el frontend, evitando múltiples llamadas para obtener datos relacionados.

**Trade-off:** Riesgo de problemas de rendimiento por lazy loading (N+1 queries) y respuestas muy grandes. El setting `spring.jpa.open-in-view=false` ayuda a detectar estos problemas en tiempo de desarrollo.

### 13.10 Paginación 1-based desde el Cliente

**Decisión:** Los controladores exponen parámetros de paginación 1-based (page=1 es la primera página) y `PaginationUtils.params()` convierte internamente a 0-based para Spring Data.

**Justificación:** Más intuitivo para los consumidores del API.

**Trade-off:** Si el cliente envía `page=0`, `PaginationUtils.params()` lo convierte a `Math.max(0, 0-1) = 0`, lo que es correcto pero podría confundir. La consistencia del API depende de la documentación.

### 13.11 Sesiones Stateless con JWT

**Decisión:** La configuración de seguridad establece `SessionCreationPolicy.STATELESS` y no se almacena estado de sesión en el servidor.

**Justificación:** Facilita la escalabilidad horizontal y es el patrón estándar para APIs REST consumidas por frontends SPA o móviles. El estado se codifica en el propio token JWT.

---

## 14. Pruebas Unitarias

### 14.1 Configuración de Pruebas

- **Framework:** JUnit 5 + Mockito
- **Estrategia:** Pruebas unitarias puras con `@ExtendWith(MockitoExtension.class)`
- **Mocking:** `@Mock` para dependencias, `@InjectMocks` para la clase bajo prueba
- **No hay pruebas de integración** (`@SpringBootTest` no se utiliza)

### 14.2 Cobertura de Pruebas

| Clase de Prueba | Operaciones Probadas | Estado |
|---|---|---|
| `AuthServiceImplTest` | login, getCurrentUser, cambiarPassword (~10 tests) | ✅ Funcional |
| `RecorridoServiceImplTest` | CRUD + reglas de negocio (~15 tests, anidados) | ✅ Funcional |
| `UserServiceImplTest` | CRUD estándar + filtro (~12 tests) | ✅ Funcional |
| `VehiculoServiceImplTest` | CRUD + validaciones + filtro (~14 tests) | ✅ Funcional |
| `ChoferServiceImplTest` | CRUD + categorías + filtro (~12 tests) | ✅ Funcional |
| `EmpresaServiceImplTest` | CRUD estándar + filtro (~11 tests) | ✅ Funcional |
| `RoleServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |
| `PermissionServiceImplTest` | CRUD estándar (~11 tests) | ✅ Funcional |
| `MarcaServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |
| `TipoVehiculoServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |
| `TipoCombustibleServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |
| `CategoriaLicenciaServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |
| `ChoferCategoriaServiceImplTest` | CRUD estándar (~10 tests) | ✅ Funcional |

**Nota:** No existen pruebas unitarias para `CurrencyServiceImpl` ni `TarjetaCombustibleServiceImpl`.

---

## 15. Configuración de Entornos

### 15.1 Configuración Común (`application.properties`)

| Propiedad | Valor | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto del servidor |
| `spring.application.name` | `fleet-management` | Nombre de la aplicación |
| `spring.jpa.hibernate.ddl-auto` | `update` | Generación automática de esquema |
| `spring.jpa.show-sql` | `true` | Muestra SQL en consola |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Formatea SQL en consola |
| `spring.jpa.open-in-view` | `false` | Desactiva Open Session in View |
| `jwt.secret` | (64-char hex string) | Clave HMAC para JWT (hardcoded) |
| `jwt.expiration` | `86400000` (24h) | Expiración del token en milisegundos |
| `spring.profiles.active` | `dev` | Perfil activo por defecto |

### 15.2 Perfil Desarrollo (`application-dev.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:fleetdb` |
| `spring.datasource.driver-class-name` | `org.h2.Driver` |
| `spring.h2.console.enabled` | `true` |
| `spring.h2.console.path` | `/h2-console` |
| Logging | DEBUG para `com.fleet.management` y SQL Hibernate |

### 15.3 Perfil Producción (`application-prod.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/fleetdb` |
| `spring.datasource.driver-class-name` | `org.postgresql.Driver` |
| `spring.datasource.username` | `fleet_user` |
| `spring.datasource.password` | `cambiar_password` (placeholder) |
| `spring.jpa.hibernate.ddl-auto` | `update` |
| `spring.h2.console.enabled` | `false` |
| Logging | Nivel INFO |

---

## 16. Problemas Conocidos y Mejoras Pendientes

### 16.1 Problemas Conocidos

| # | Severidad | Problema | Descripción |
|---|---|---|---|
| 1 | 🟡 Media | JWT secret hardcoded | La clave JWT está en `application.properties` como texto plano. Debería usarse variable de entorno o vault |
| 2 | 🟡 Media | Contraseña admin hardcoded | `admin123` en `DataInitializer.java` es una contraseña por defecto insegura |
| 3 | 🟡 Media | Duplicación de mapeo DTO | `toEmpresaResponse()` y otros mappers están duplicados en múltiples servicios |
| 4 | 🟠 Baja | Sin migraciones DB | `ddl-auto=update` es frágil para producción; no controla cambios de esquema |
| 5 | 🟠 Baja | Riesgo N+1 queries | Respuestas DTO con anidamiento profundo + lazy loading pueden causar problemas de rendimiento |
| 6 | 🟠 Baja | Sin caché | Cada llamada al servicio impacta la base de datos sin capa de caché |
| 7 | 🟠 Baja | Page 0 API inconsistency | Si el cliente envía `page=0`, la conversión 1→0-based da 0 (funciona pero es confuso) |
| 8 | 🟠 Baja | Pruebas faltantes | No existen pruebas para `CurrencyServiceImpl` ni `TarjetaCombustibleServiceImpl` |

### 16.2 Mejoras Recomendadas

1. **Introducir MapStruct** — Eliminar duplicación de mapeo DTO, generar mappers en tiempo de compilación
2. **Externalizar credenciales** — Mover JWT secret y contraseña admin a variables de entorno
3. **Adoptar Flyway o Liquibase** — Control versionado del esquema de base de datos para producción
4. **Implementar `@EntityGraph` o FETCH JOIN** — Evitar problemas N+1 en respuestas anidadas
5. **Crear mappers compartidos** — Como paso intermedio antes de MapStruct, extraer mapeos comunes a clases utilitarias
6. **Agregar pruebas de integración** — Al menos para los flujos críticos (recorridos, autenticación)
7. **Implementar rate limiting** — Proteger los endpoints de autenticación contra ataques de fuerza bruta
8. **Completar cobertura de pruebas** — Agregar tests para `CurrencyServiceImpl` y `TarjetaCombustibleServiceImpl`
9. **Generación de reportes PDF** — Implementar exportación a PDF del reporte de movimiento mensual usando Thymeleaf + Flying Saucer (OpenPDF)
10. **Sistema de suscripciones/planes** — Implementar modelo de suscripción por empresa con límites de vehículos y planes de facturación
11. **Modelo geográfico (Provincia/Municipio)** — Agregar provincias y municipios como catálogos maestros y asociarlos a la entidad Empresa
