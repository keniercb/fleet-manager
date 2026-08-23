# Documento Técnico — Sistema de Gestión del Parque de Vehículos (Fleet Management)

**Versión:** 1.0  
**Fecha:** 2026-08-24  
**Estado:** Desarrollo activo

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
9. [Manejo de Excepciones](#9-manejo-de-excepciones)
10. [Utilidades y Patrones Comunes](#10-utilidades-y-patrones-comunes)
11. [Inicialización de Datos](#11-inicialización-de-datos)
12. [Decisiones de Diseño](#12-decisiones-de-diseño)
13. [Pruebas Unitarias](#13-pruebas-unitarias)
14. [Configuración de Entornos](#14-configuración-de-entornos)
15. [Problemas Conocidos y Mejoras Pendientes](#15-problemas-conocidos-y-mejoras-pendientes)
16. [Funcionalidades Pendientes / Eliminadas](#16-funcionalidades-pendientes--eliminadas)

---

## 1. Introducción

El **Sistema de Gestión del Parque de Vehículos** (Fleet Management) es una aplicación backend construida con Spring Boot que proporciona una API REST para la gestión integral de vehículos, choferes, empresas, recorridos y los catálogos maestros asociados (marcas, tipos de vehículos, tipos de combustible, categorías de licencia). El sistema implementa control de acceso basado en roles (RBAC), auditoría automática de entidades y lógica de negocio transaccional para la gestión de recorridos y consumo de combustible.

El sistema está diseñado como un microservicio backend que puede ser consumido por cualquier frontend (Angular, React, Vue) a través de su API REST documentada con OpenAPI/Swagger.

### Alcance Funcional

- Gestión de **Empresas** propietarias de vehículos
- Gestión de **Vehículos** con control de odómetro y combustible
- Gestión de **Choferes** con categorías de licencia
- Registro y control de **Recorridos** con cálculo automático de consumo
- Gestión de catálogos maestros: **Marcas**, **Tipos de Vehículo**, **Tipos de Combustible**, **Categorías de Licencia**
- Sistema de **Autenticación JWT** con roles y permisos granulares
- **Auditoría automática** en todas las entidades (creador, modificador, timestamps)
- **Baja lógica** (soft delete) en todas las entidades

---

## 2. Stack Tecnológico

| Componente | Tecnología | Versión | Observaciones |
|---|---|---|---|
| Lenguaje | Java | 17 | LTS |
| Framework | Spring Boot | 3.3.5 | |
| Build Tool | Maven | — | `pom.xml` |
| ORM | Spring Data JPA / Hibernate | (managed by Spring Boot) | `ddl-auto=update` |
| Base de datos (desarrollo) | H2 | (in-memory) | Consola habilitada en `/h2-console` |
| Base de datos (producción) | PostgreSQL | — | Requiere instalación externa |
| Seguridad | Spring Security + JWT (jjwt) | 0.12.6 | Sesiones stateless |
| Validación | Jakarta Bean Validation | (Jakarta EE 10) | Anotaciones en entidades y DTOs |
| Reducción de boilerplate | Lombok | (managed by Spring Boot) | `@Getter`, `@Setter`, `@Builder`, `@SuperBuilder` |
| Documentación API | OpenAPI / Swagger v3 | (springdoc) | Tags en controladores |
| Testing | JUnit 5 + Mockito | (Spring Boot Starter Test) | Solo pruebas unitarias |
| Logs | SLF4J + Logback | (Spring Boot default) | DEBUG en desarrollo |

**Dependencias NO utilizadas (decisiones conscientes):**

- **MapStruct:** Mapeo entidad-DTO realizado manualmente
- **QueryDSL:** Consultas construidas con métodos Spring Data JPA derivados y `@Query`
- **Flyway / Liquibase:** Sin migraciones versionadas de base de datos
- **Spring Cache:** Sin mecanismo de caché

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
│      mapeo request → servicio                 │
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
- Todos los endpoints de listado soportan paginación con parámetros estándar

#### Servicios (`service/` + `service/impl/`)

- Cada dominio tiene una interfaz `XService` y su implementación `XServiceImpl`
- Contienen toda la lógica de negocio y reglas de validación
- Realizan el mapeo manual entre entidades y DTOs mediante métodos `toResponse()` privados
- Anotados con `@Transactional` para garantizar atomicidad

#### Repositorios (`repository/`)

- Interfaces Spring Data JPA que extienden `JpaRepository<Entity, Long>`
- Proveen métodos de búsqueda por claves naturales (`findByCodigo`, `findByEmail`, `findByMatricula`, etc.)
- Todos incluyen `findAllByActivoTrue(Pageable)` para soportar baja lógica con paginación
- Algunos incluyen consultas personalizadas con `@Query`

#### Modelo (`model/`)

- 13 entidades JPA que heredan de `BaseEntity`
- Relaciones mapeadas con `FetchType.LAZY` (excepto `User.roles` y `Role.permissions` que son `EAGER`)
- Validaciones con anotaciones Jakarta (`@NotNull`, `@NotBlank`, `@Size`, `@Past`, `@DecimalMin`)

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
│   │   │   ├── model/                                # 13 entidades JPA
│   │   │   │   ├── BaseEntity.java                   # Superclase abstracta (id, audit, soft-delete)
│   │   │   │   ├── User.java                         # Usuario del sistema
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
│   │   │   │   └── Recorrido.java                    # Recorrido / Viaje
│   │   │   ├── dto/                                  # DTOs de request/response por dominio
│   │   │   │   ├── auth/                             # LoginRequest, AuthResponse
│   │   │   │   ├── user/                             # UserRequest, UserResponse
│   │   │   │   ├── role/                             # RoleRequest, RoleResponse
│   │   │   │   ├── permission/                       # PermissionRequest, PermissionResponse
│   │   │   │   ├── empresa/                          # EmpresaRequest, EmpresaResponse
│   │   │   │   ├── vehiculo/                         # VehiculoRequest, VehiculoResponse
│   │   │   │   ├── chofer/                           # ChoferRequest, ChoferResponse
│   │   │   │   ├── chofercategoria/                  # ChoferCategoriaRequest, etc.
│   │   │   │   ├── recorriddo/                      # RecorridoRequest, RecorridoResponse
│   │   │   │   ├── tipovehiculo/                     # TipoVehiculoRequest, etc.
│   │   │   │   ├── marca/                            # MarcaRequest, MarcaResponse
│   │   │   │   ├── tipocombustible/                  # TipoCombustibleRequest, etc.
│   │   │   │   └── categorialicencia/               # CategoriaLicenciaRequest, etc.
│   │   │   ├── repository/                           # 13 repositorios Spring Data JPA
│   │   │   ├── service/                              # 13 interfaces de servicio
│   │   │   ├── service/impl/                         # 13 implementaciones de servicio
│   │   │   ├── controller/                           # 13 controladores REST
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
                          ┌──────▼──────┐     ┌─────────────┐
                          │    Role     │────▶│    User     │
                          ├─────────────┤ M:N ├─────────────┤
                          │ id          │     │ id          │
                          │ name (UQ)   │     │ email (UQ)  │
                          │ description │     │ password    │
                          │ [audit...]  │     │ roles (M:N) │
                          └─────────────┘     │ [audit...]  │
                                              └──────┬──────┘
                                                     │ (audita todas)

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
│ consumo       │  │ fechaEmision      │     │    │
│ litrosAbast.  │  └────────┬──────────┘     │    │
│ numeroChip    │           │ N:1             │    │
│ lugarAbast.   │  ┌────────▼──────────┐     │    │
│ [audit...]    │  │CategoriaLicencia  │     │    │
└───────────────┘  ├───────────────────┤     │    │
                   │ id                │     │    │
                   │ codigo (UQ, 1 char)│     │    │
                   │ denominacion      │     │    │
                   │ descripcion       │     │    │
                   │ [audit...]        │     │    │
                   └───────────────────┘     │    │
                                              │    │
       Vehiculo ──┬── empresa (FK, NOT NULL)  │    │
                  ├── tipoVehiculo (FK, NOT NULL)  │
                  ├── marca (FK, NOT NULL) ──────┘    │
                  ├── chofer (FK, nullable)             │
                  └── tipoCombustible (FK, NOT NULL) ───┘

Unicidad compuesta en Recorrido: (vehiculo_id, fecha)
```

### 5.3 Detalle de Entidades

#### 5.3.1 User

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `email` | `String(100)` | `NOT NULL`, `UNIQUE` | Correo electrónico (login) |
| `password` | `String(255)` | `NOT NULL` | Contraseña encriptada con BCrypt |
| `roles` | `Set<Role>` | M:N via `user_roles` | Roles asignados al usuario |

#### 5.3.2 Role

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `name` | `String(50)` | `NOT NULL`, `UNIQUE` | Nombre del rol (ej: ADMIN, USER) |
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
| `codigo` | `String` | `UNIQUE` | Código identificador de la empresa |
| `nombre` | `String(100)` | `NOT NULL` | Nombre de la empresa |
| `direccion` | `String` | nullable | Dirección física |
| `telefono` | `String(20)` | nullable | Teléfono de contacto |
| `email` | `String(100)` | nullable | Correo electrónico |

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
| `fechaEmision` | `LocalDate` | nullable | Fecha de emisión de la categoría |

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
| `fecha` | `LocalDate` | FK (parte de UK compuesto) | Fecha del recorrido |
| `kilometros` | `Integer` | — | Kilómetros recorridos |
| `odometroInicial` | `BigInteger` | auto-calculado | Odómetro del vehículo antes del recorrido |
| `consumo` | `BigDecimal(10,2)` | auto-calculado | Combustible consumido = (índiceConsumo × km) / 100 |
| `litrosAbastecidos` | `BigDecimal(10,2)` | nullable | Litros repostados durante el recorrido |
| `numeroChip` | `String` | nullable | Número de chip de la tarjeta de combustible |
| `lugarAbastecimiento` | `String` | nullable | Lugar donde se repostó |

**Unicidad compuesta:** `(vehiculo_id, fecha)`

---

## 6. API REST — Endpoints

### 6.1 Autenticación

| Método | Ruta | Descripción | Auth | Rol requerido |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión, retorna JWT | No | — |
| `POST` | `/api/auth/logout` | Cerrar sesión (no-op, stateless) | No | — |
| `GET` | `/api/auth/me` | Obtener usuario actual | Sí | Cualquiera |

### 6.2 Gestión de Usuarios

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/users` | Listar usuarios (paginado) | ADMIN |
| `GET` | `/api/users/{id}` | Obtener usuario por ID | ADMIN |
| `POST` | `/api/users` | Crear usuario | ADMIN |
| `PUT` | `/api/users/{id}` | Actualizar usuario | ADMIN |
| `DELETE` | `/api/users/{id}` | Eliminar usuario (lógico) | ADMIN |

### 6.3 Gestión de Roles

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/roles` | Listar roles (paginado) | ADMIN |
| `GET` | `/api/roles/{id}` | Obtener rol por ID | ADMIN |
| `POST` | `/api/roles` | Crear rol | ADMIN |
| `PUT` | `/api/roles/{id}` | Actualizar rol | ADMIN |
| `DELETE` | `/api/roles/{id}` | Eliminar rol (lógico) | ADMIN |

### 6.4 Gestión de Permisos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/permissions` | Listar permisos (paginado) | ADMIN |
| `GET` | `/api/permissions/{id}` | Obtener permiso por ID | ADMIN |
| `POST` | `/api/permissions` | Crear permiso | ADMIN |
| `PUT` | `/api/permissions/{id}` | Actualizar permiso | ADMIN |
| `DELETE` | `/api/permissions/{id}` | Eliminar permiso (lógico) | ADMIN |

### 6.5 Gestión de Empresas

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/empresas` | Listar empresas (paginado) | Autenticado |
| `GET` | `/api/empresas/{id}` | Obtener empresa por ID | Autenticado |
| `POST` | `/api/empresas` | Crear empresa | Autenticado |
| `PUT` | `/api/empresas/{id}` | Actualizar empresa | Autenticado |
| `DELETE` | `/api/empresas/{id}` | Eliminar empresa (lógico) | Autenticado |

### 6.6 Gestión de Vehículos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/vehiculos` | Listar vehículos (paginado) | Autenticado |
| `GET` | `/api/vehiculos/{id}` | Obtener vehículo por ID | Autenticado |
| `POST` | `/api/vehiculos` | Crear vehículo | Autenticado |
| `PUT` | `/api/vehiculos/{id}` | Actualizar vehículo | Autenticado |
| `DELETE` | `/api/vehiculos/{id}` | Eliminar vehículo (lógico) | Autenticado |
| `GET` | `/api/vehiculos/sin-chofer` | Vehículos sin chofer asignado | Autenticado |
| `GET` | `/api/vehiculos/chofer/{id}` | Vehículos por chofer | Autenticado |
| `GET` | `/api/vehiculos/empresa/{id}` | Vehículos por empresa | Autenticado |

### 6.7 Gestión de Choferes

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/choferes` | Listar choferes (paginado) | Autenticado |
| `GET` | `/api/choferes/{id}` | Obtener chofer por ID | Autenticado |
| `POST` | `/api/choferes` | Crear chofer (con categorías) | Autenticado |
| `PUT` | `/api/choferes/{id}` | Actualizar chofer (reemplaza categorías) | Autenticado |
| `DELETE` | `/api/choferes/{id}` | Eliminar chofer (lógico + categorías) | Autenticado |

### 6.8 Gestión de Categorías de Licencia de Choferes

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/choferes-categorias` | Listar (paginado) | Autenticado |
| `GET` | `/api/choferes-categorias/{id}` | Obtener por ID | Autenticado |
| `POST` | `/api/choferes-categorias` | Crear | Autenticado |
| `PUT` | `/api/choferes-categorias/{id}` | Actualizar | Autenticado |
| `DELETE` | `/api/choferes-categorias/{id}` | Eliminar (lógico) | Autenticado |

### 6.9 Gestión de Recorridos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/recorridos` | Listar recorridos (paginado) | Autenticado |
| `GET` | `/api/recorridos/{id}` | Obtener recorrido por ID | Autenticado |
| `POST` | `/api/recorridos` | Crear recorrido | Autenticado |
| `PUT` | `/api/recorridos/{id}` | Actualizar recorrido | Autenticado |
| `DELETE` | `/api/recorridos/{id}` | Eliminar recorrido (lógico, revierte odómetro/combustible) | Autenticado |
| `GET` | `/api/recorridos/vehiculo/{id}/rango?desde=...&hasta=...` | Recorridos por vehículo y rango de fechas | Autenticado |

### 6.10 Catálogos Maestros

Todos los catálogos siguen el patrón CRUD estándar con paginación y requieren autenticación:

- **Tipos de Vehículo:** `/api/tipos-vehiculo`
- **Marcas:** `/api/marcas`
- **Tipos de Combustible:** `/api/tipos-combustible`
- **Categorías de Licencia:** `/api/categorias-licencia`

### 6.11 Parámetros de Paginación (todos los endpoints de listado)

| Parámetro | Default | Descripción |
|---|---|---|
| `page` | `0` | Número de página (1-based desde el cliente, convertido a 0-based internamente) |
| `perPage` | `20` | Elementos por página |
| `sort` | `id` | Campo de ordenamiento |
| `sortOrder` | `ASC` | Dirección: `ASC` o `DESC` |

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
| `SecurityConfig` | Configura la cadena de filtros, CORS, reglas de acceso por rol |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` que extrae y valida el token Bearer en cada petición |
| `JwtService` | Genera tokens JWT con claims de roles; valida firma, expiración y sujeto |
| `AuthenticatedUser` | Implementa `UserDetails` envolviendo la entidad `User` de JPA, permitiendo acceso a la entidad completa desde el `SecurityContext` |
| `CustomUserDetailsService` | Carga el usuario desde la base de datos, asignando autoridades `ROLE_` + permisos |

### 7.3 Reglas de Acceso

| Ruta | Acceso |
|---|---|
| `/api/auth/**` | Público (sin autenticación) |
| `/api/users/**` | Requiere rol `ADMIN` |
| `/api/roles/**` | Requiere rol `ADMIN` |
| `/api/permissions/**` | Requiere rol `ADMIN` |
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

### 7.5 Autoridades Grantes

El `CustomUserDetailsService` asigna dos tipos de autoridades al autenticar un usuario:

1. **Autoridades de rol:** `ROLE_ADMIN`, `ROLE_USER` (por cada rol asignado al usuario)
2. **Autoridades de permiso:** `vehiculo:read`, `vehiculo:write`, etc. (por cada permiso en cada rol)

Esto permite el uso de `@PreAuthorize("hasRole('ADMIN')")` o `@PreAuthorize("hasAuthority('vehiculo:write')")` en futuros endpoints granulares.

---

## 8. Capa de Servicios — Lógica de Negocio

### 8.1 Patrón CRUD Estándar

Todos los servicios implementan el siguiente patrón, con variaciones específicas por dominio:

| Operación | Comportamiento general |
|---|---|
| `findAll(Pageable)` | `repository.findAllByActivoTrue(pageable)` → mapea a `Page<XResponse>` |
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
4. Calcula automáticamente el consumo: `consumo = (índiceConsumo × kilómetros) / 100`, redondeado a 2 decimales
5. Valida disponibilidad de combustible: `combustibleRestante = combustibleActual - consumo + litrosAbastecidos >= 0`
6. Registra el `odometroInicial` como el odómetro actual del vehículo
7. Actualiza atómicamente el vehículo: incrementa el odómetro y decrementa el combustible
8. Todas las operaciones se ejecutan dentro de una transacción `@Transactional`

**Actualización de recorrido (`update`):**
1. No permite cambiar el vehículo ni la fecha del recorrido
2. Restaura el consumo antiguo al vehículo antes de calcular el nuevo
3. Recalcula el consumo con los nuevos kilómetros
4. Verifica disponibilidad de combustible con el nuevo consumo
5. Restaura y recalcula el odómetro del vehículo
6. Actualiza atómicamente la entidad y el vehículo

**Eliminación de recorrido (`delete`):**
1. Restaura los kilómetros al odómetro del vehículo (resta)
2. Restaura el combustible consumido al vehículo (suma)
3. Ejecuta la baja lógica del recorrido (`activo = false`)

#### 8.2.2 ChoferServiceImpl — Creación anidada de categorías

- El `ChoferRequest` incluye una lista de `CategoriaConFechaRequest` (pares `categoriaLicenciaId` + `fechaEmision`)
- Al crear un chofer, se crean automáticamente las relaciones `ChoferCategoria` con sus fechas de emisión
- Al actualizar, se eliminan todas las categorías existentes y se recrean con la nueva lista
- Al eliminar, se desactivan también todas las categorías del chofer

#### 8.2.3 VehiculoServiceImpl — Validación de doble unicidad

- Valida que tanto la `matrícula` como el `número de motor` sean únicos al crear y actualizar
- Al actualizar, excluye el ID del vehículo actual de la verificación de unicidad

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

## 9. Manejo de Excepciones

### 9.1 Excepciones Personalizadas

| Excepción | Código HTTP | Uso |
|---|---|---|
| `ResourceNotFoundException` | 404 Not Found | Entidad no encontrada por ID o búsqueda |
| `BusinessException` | 400 Bad Request | Violación de regla de negocio (duplicados, reglas de recorrido, etc.) |
| `MethodArgumentNotValidException` | 400 Bad Request | Errores de validación de campos (Jakarta Bean Validation) |
| `Exception` (catch-all) | 500 Internal Server Error | Errores no esperados |

### 9.2 Formato de Respuesta de Error (ApiError)

```json
{
  "timestamp": "2026-08-24T10:30:00",
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
  "timestamp": "2026-08-24T10:30:00",
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

## 10. Utilidades y Patrones Comunes

### 10.1 PaginationUtils

Clase utilitaria estática que centraliza la lógica de paginación:

- **`params(int page, int perPage, String sortBy, String sortOrder)`:** Convierte los parámetros del controlador (1-based) a `PaginationParams` (0-based). Restra 1 al número de página para convertir de base-1 a base-0, con un mínimo de 0.
- **`of(PaginationParams)`:** Convierte `PaginationParams` a Spring `PageRequest` con ordenamiento.

### 10.2 AuditMapper

Utilidad estática que convierte una entidad `User` a `UserAuditResponse` (solo `id` + `email`), evitando referencias circulares en las respuestas DTO que incluyen campos de auditoría.

### 10.3 Patrón de Nombres

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

### 10.4 Nomenclatura de Tablas

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
| `User` ↔ `Role` | `user_roles` |
| `Role` ↔ `Permission` | `role_permissions` |

---

## 11. Inicialización de Datos

### 11.1 DataInitializer (Bootstrap)

Se ejecuta automáticamente al iniciar la aplicación mediante `CommandLineRunner`. Crea los datos iniciales solo si no existen (idempotente):

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
| `ADMIN` | Rol con acceso total al sistema | Todos los 31 permisos |
| `USER` | Rol de usuario estándar con acceso de lectura | Ninguno (por defecto) |

**Usuario administrador:**

| Campo | Valor |
|---|---|
| Email | `admin@fleet.com` |
| Password | `admin123` (encriptada con BCrypt) |
| Rol | `ADMIN` |

---

## 12. Decisiones de Diseño

### 12.1 Baja Lógica (Soft Delete) Universal

**Decisión:** Todas las entidades incluyen un campo `activo` (Boolean, default `true`). La eliminación nunca borra registros físicamente; simplemente cambia `activo` a `false`.

**Justificación:** En un sistema de gestión de flotas, la integridad histórica de los datos es crítica. Un vehículo dado de baja puede tener recorridos asociados; eliminarlo físicamente destruiría la cadena de auditoría. La baja lógica permite mantener la trazabilidad completa.

**Implementación:** Todos los repositorios exponen `findAllByActivoTrue(Pageable)` y los servicios filtran exclusivamente por `activo = true` en las operaciones de lectura.

### 12.2 Auditoría Automática vía BaseEntity + SecurityContext

**Decisión:** Los campos de auditoría (`creadoPor`, `modificadoPor`, `fechaCreacion`, `fechaActualizacion`) se pueblan automáticamente en los callbacks JPA `@PrePersist` y `@PreUpdate` de `BaseEntity`, leyendo el `AuthenticatedUser` del `SecurityContextHolder`.

**Justificación:** A diferencia del enfoque estándar de Spring Data JPA Auditing (`@CreatedBy`, `@LastModifiedBy`), este diseño permite capturar la entidad `User` completa (no solo el username) como referencia de auditoría. Esto habilita mostrar el nombre y email del auditor en las respuestas DTO sin consultas adicionales.

**Trade-off:** El `BaseEntity` tiene una dependencia directa al `SecurityContextHolder`, lo que acopla la capa de modelo al contexto de seguridad de Spring. Durante el bootstrap (`DataInitializer`), el `SecurityContext` está vacío, por lo que los campos de auditoría quedan como `null`.

### 12.3 AuthenticatedUser envuelve la entidad User

**Decisión:** `AuthenticatedUser` implementa `UserDetails` y envuelve la entidad JPA `User` completa en lugar de solo almacenar credenciales.

**Justificación:** Necesario para que `BaseEntity.getAuthenticatedUser()` pueda retornar la entidad `User` y asignarla directamente a los campos `creadoPor`/`modificadoPor`. Esto evita una consulta adicional al repositorio en cada operación de auditoría.

### 12.4 Mapeo DTO Manual (sin MapStruct)

**Decisión:** El mapeo entre entidades y DTOs se realiza manualmente en cada `ServiceImpl`.

**Justificación (presumida):** Simplicidad inicial del proyecto, evitar una dependencia adicional.

**Trade-off:** Genera duplicación significativa de código. Por ejemplo, `toEmpresaResponse()` está duplicado en al menos 4 servicios diferentes (`VehiculoServiceImpl`, `ChoferServiceImpl`, `RecorridoServiceImpl` y posiblemente otros). Una refactorización futura con MapStruct o con mappers compartidos reduciría el código y mejoraría el mantenimiento.

### 12.5 Sin Migraciones de Base de Datos (ddl-auto=update)

**Decisión:** Se utiliza `spring.jpa.hibernate.ddl-auto=update` para generar y actualizar el esquema automáticamente.

**Justificación:** Rapidez en el desarrollo inicial.

**Trade-off:** Este enfoque es frágil para cambios de esquema en producción (no genera `ALTER TABLE` para renombrar o eliminar columnas). Para producción, se recomienda migrar a Flyway o Liquibase.

### 12.6 Respuestas DTO Anidadas Completas

**Decisión:** Los DTOs de respuesta incluyen objetos anidados completos. Por ejemplo, `RecorridoResponse` contiene un `VehiculoResponse` completo, que a su vez contiene `EmpresaResponse`, `MarcaResponse`, etc.

**Justificación:** Simplifica el consumo del API para el frontend, evitando múltiples llamadas para obtener datos relacionados.

**Trade-off:** Riesgo de problemas de rendimiento por lazy loading (N+1 queries) y respuestas muy grandes. El setting `spring.jpa.open-in-view=false` ayuda a detectar estos problemas en tiempo de desarrollo.

### 12.7 Paginación 1-based desde el Cliente

**Decisión:** Los controladores exponen parámetros de paginación 1-based (page=1 es la primera página) y `PaginationUtils.params()` convierte internamente a 0-based para Spring Data.

**Justificación:** Más intuitivo para los consumidores del API.

**Trade-off:** Si el cliente envía `page=0`, `PaginationUtils.params()` lo convierte a `Math.max(0, 0-1) = 0`, lo que es correcto pero podría confundir. La consistencia del API depende de la documentación.

### 12.8 Sesiones Stateless con JWT

**Decisión:** La configuración de seguridad establece `SessionCreationPolicy.STATELESS` y no se almacena estado de sesión en el servidor.

**Justificación:** Facilita la escalabilidad horizontal y es el patrón estándar para APIs REST consumidas por frontends SPA o móviles. El estado se codifica en el propio token JWT.

---

## 13. Pruebas Unitarias

### 13.1 Configuración de Pruebas

- **Framework:** JUnit 5 + Mockito
- **Estrategia:** Pruebas unitarias puras con `@ExtendWith(MockitoExtension.class)`
- **Mocking:** `@Mock` para dependencias, `@InjectMocks` para la clase bajo prueba
- **No hay pruebas de integración** (`@SpringBootTest` no se utiliza)

### 13.2 Cobertura de Pruebas

| Clase de Prueba | Operaciones Probadas | Estado |
|---|---|---|
| `AuthServiceImplTest` | login, getCurrentUser (7 tests) | ✅ Funcional |
| `RecorridoServiceImplTest` | CRUD + reglas de negocio (~15 tests, anidados) | ✅ Funcional |
| `UserServiceImplTest` | CRUD estándar (11 tests) | ❌ Bug de mock |
| `VehiculoServiceImplTest` | CRUD + validaciones (13 tests) | ❌ Bug de mock |
| `ChoferServiceImplTest` | CRUD + categorías (11 tests) | ❌ Bug de mock |
| `EmpresaServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `RoleServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `PermissionServiceImplTest` | CRUD estándar (11 tests) | ❌ Bug de mock |
| `MarcaServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `TipoVehiculoServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `TipoCombustibleServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `CategoriaLicenciaServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |
| `ChoferCategoriaServiceImplTest` | CRUD estándar (~10 tests) | ❌ Bug de mock |

### 13.3 Bug Crítico en Pruebas

**11 de 13 archivos de prueba** tienen un error de mocking consistente: el método de prueba `findAllShouldReturnPagedResponses()` mocka `repository.findAll(pageable)`, pero la implementación real del servicio llama a `repository.findAllByActivoTrue(pageable)`. Como Mockito retorna `null` para métodos no mockeados, se produce un `NullPointerException` al invocar `.map()` sobre el resultado `null`.

**Fix necesario:** Reemplazar `when(repository.findAll(pageable)).thenReturn(page)` con `when(repository.findAllByActivoTrue(pageable)).thenReturn(page)` en los 11 archivos afectados, y actualizar las llamadas `verify()` correspondientes.

---

## 14. Configuración de Entornos

### 14.1 Configuración Común (`application.properties`)

| Propiedad | Valor | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto del servidor |
| `spring.jpa.hibernate.ddl-auto` | `update` | Generación automática de esquema |
| `spring.jpa.show-sql` | `true` | Muestra SQL en consola |
| `spring.jpa.open-in-view` | `false` | Desactiva Open Session in View |
| `jwt.secret` | (64-char hex string) | Clave HMAC para JWT (hardcoded) |
| `jwt.expiration` | `86400000` (24h) | Expiración del token en milisegundos |
| `spring.profiles.active` | `dev` | Perfil activo por defecto |

### 14.2 Perfil Desarrollo (`application-dev.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:fleetdb` |
| `spring.datasource.driver-class-name` | `org.h2.Driver` |
| `spring.h2.console.enabled` | `true` |
| `spring.h2.console.path` | `/h2-console` |
| Logging | DEBUG para `com.fleet.management` y SQL Hibernate |

### 14.3 Perfil Producción (`application-prod.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/fleetdb` |
| `spring.datasource.driver-class-name` | `org.postgresql.Driver` |
| `spring.datasource.username` | `fleet_user` |
| `spring.datasource.password` | `cambiar_password` (placeholder) |
| `spring.jpa.hibernate.ddl-auto` | `update` |
| Logging | Nivel INFO |

---

## 15. Problemas Conocidos y Mejoras Pendientes

### 15.1 Problemas Conocidos

| # | Severidad | Problema | Descripción |
|---|---|---|---|
| 1 | 🔴 Alta | Bug en pruebas unitarias | 11 de 13 archivos de prueba fallan por mock incorrecto (`findAll` vs `findAllByActivoTrue`) |
| 2 | 🟡 Media | JWT secret hardcoded | La clave JWT está en `application.properties` como texto plano. Debería usarse variable de entorno o vault |
| 3 | 🟡 Media | Contraseña admin hardcoded | `admin123` en `DataInitializer.java` es una contraseña por defecto insegura |
| 4 | 🟡 Media | Duplicación de mapeo DTO | `toEmpresaResponse()` y otros mappers están duplicados en múltiples servicios |
| 5 | 🟠 Baja | Sin migraciones DB | `ddl-auto=update` es frágil para producción; no controla cambios de esquema |
| 6 | 🟠 Baja | Riesgo N+1 queries | Respuestas DTO con anidamiento profundo + lazy loading pueden causar problemas de rendimiento |
| 7 | 🟠 Baja | Sin caché | Cada llamada al servicio impacta la base de datos sin capa de caché |
| 8 | 🟠 Baja | Page 0 API inconsistency | Si el cliente envía `page=0`, la conversión 1→0-based da 0 (funciona pero es confuso) |

### 15.2 Mejoras Recomendadas

1. **Corregir las 11 pruebas unitarias** — Cambiar el mock de `findAll` a `findAllByActivoTrue`
2. **Introducir MapStruct** — Eliminar duplicación de mapeo DTO, generar mappers en tiempo de compilación
3. **Externalizar credenciales** — Mover JWT secret y contraseña admin a variables de entorno
4. **Adoptar Flyway o Liquibase** — Control versionado del esquema de base de datos para producción
5. **Implementar `@EntityGraph` o FETCH JOIN** — Evitar problemas N+1 en respuestas anidadas
6. **Crear mappers compartidos** — Como paso intermedio antes de MapStruct, extraer mapeos comunes a clases utilitarias
7. **Agregar pruebas de integración** — Al menos para los flujos críticos (recorridos, autenticación)
8. **Implementar rate limiting** — Proteger los endpoints de autenticación contra ataques de fuerza bruta

---

## 16. Funcionalidades Pendientes / Eliminadas

### 16.1 Funcionalidades presentes en la versión anterior y eliminadas en la versión actual

El proyecto contiene una copía anterior en `/fleet-manager/fleet-management/` que incluye funcionalidades no presentes en la versión de trabajo principal:

#### TarjetaCombustible (Tarjetas de Combustible)

- **Entidad:** `TarjetaCombustible` con campos `numero`, `saldo`, `currencyId`, `empresaId`
- **Relación:** Empresa 1:N TarjetaCombustible, Currency 1:N TarjetaCombustible
- **Endpoints CRUD:** Búsqueda/filtrado por `numero` y `empresaId`
- **Estado:** Eliminado de la versión principal, disponible en la versión anterior

#### Currency (Monedas)

- **Entidad:** `Currency` con campos `codigoISO`, `descripcion`
- **Endpoints CRUD estándar**
- **Estado:** Eliminado de la versión principal, disponible en la versión anterior

#### Reportes de Movimiento Mensual

- **Método:** `RecorridoServiceImpl.reporteMovimientoMensual(vehiculoId, mes, anio)`
- **Funcionalidad:** Genera reportes de consumo mensual con:
  - Datos del vehículo y empresa
  - Lecturas diarias de combustible
  - Análisis de consumo comparado con la norma (índice de consumo)
  - Balance de combustible diario
- **DTOs asociados:** `ReporteMovimientoMensualResponse`, `VehiculoReporteData`, `LecturaDiariaResponse`, `AnalisisConsumoResponse`
- **Estado:** Eliminado de la versión principal, disponible en la versión anterior

---