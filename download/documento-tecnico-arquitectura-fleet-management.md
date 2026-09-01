# Documento Técnico — Sistema de Gestión del Parque de Vehículos (Fleet Management)

**Versión:** 3.0  
**Fecha:** 2026-09-01  
**Estado:** Desarrollo activo  
**Rama:** `developSuscription`

---

## Tabla de Contenido

1. [Introducción](#1-introducción)
2. [Requisitos Funcionales](#2-requisitos-funcionales)
3. [Stack Tecnológico](#3-stack-tecnológico)
4. [Arquitectura del Sistema](#4-arquitectura-del-sistema)
5. [Estructura del Proyecto](#5-estructura-del-proyecto)
6. [Modelo de Datos](#6-modelo-de-datos)
7. [API REST — Endpoints](#7-api-rest--endpoints)
8. [Seguridad y Autenticación](#8-seguridad-y-autenticación)
9. [Capa de Servicios — Lógica de Negocio](#9-capa-de-servicios--lógica-de-negocio)
10. [Reportes](#10-reportes)
11. [Manejo de Excepciones](#11-manejo-de-excepciones)
12. [Utilidades y Patrones Comunes](#12-utilidades-y-patrones-comunes)
13. [Inicialización de Datos](#13-inicialización-de-datos)
14. [Decisiones de Diseño](#14-decisiones-de-diseño)
15. [Pruebas Unitarias](#15-pruebas-unitarias)
16. [Configuración de Entornos](#16-configuración-de-entornos)
17. [Problemas Conocidos y Mejoras Pendientes](#17-problemas-conocidos-y-mejoras-pendientes)

---

## 1. Introducción

El **Sistema de Gestión del Parque de Vehículos** (Fleet Management) es una aplicación backend construida con Spring Boot que proporciona una API REST para la gestión integral de vehículos, choferes, empresas, recorridos, tarjetas de combustible y los catálogos maestros asociados (marcas, tipos de vehículos, tipos de combustible, categorías de licencia, monedas, provincias, municipios). El sistema implementa control de acceso basado en roles (RBAC) con tres niveles (SUPER_ADMIN, ADMIN, USER), auditoría automática de entidades, lógica de negocio transaccional para la gestión de recorridos y consumo de combustible, un módulo de suscripciones con planes y límites por empresa, y un módulo de reportes de transporte.

El sistema está diseñado como un microservicio backend que puede ser consumido por cualquier frontend (Angular, React, Vue) a través de su API REST documentada con OpenAPI/Swagger.

### Alcance Funcional

- Gestión de **Empresas** propietarias de vehículos (con provincia y municipio)
- Gestión de **Vehículos** con control de odómetro, combustible y seguimiento de mantenimiento
- Gestión de **Choferes** con categorías de licencia
- Registro y control de **Recorridos** con cálculo automático de consumo y validación de saldo de tarjetas de combustible
- Gestión de **Tarjetas de Combustible** con control de saldo y relación a monedas
- Gestión de **Monedas (Currency)** como catálogo maestro
- Modelo geográfico de **Provincias** y **Municipios**
- Sistema de **Suscripciones y Planes** con límites de vehículos y usuarios por empresa
- **Reportes de transporte**: consumo por vehículo, abastecimiento, mantenimiento, consumo por tipo de combustible
- **Reporte de movimiento mensual** por vehículo con análisis de consumo comparado con la norma
- Gestión de catálogos maestros: **Marcas**, **Tipos de Vehículo**, **Tipos de Combustible**, **Categorías de Licencia**, **Features**
- Sistema de **Autenticación JWT** con tres roles, permisos granulares y rate limiting
- **Auditoría automática** en todas las entidades (creador, modificador, timestamps)
- **Baja lógica** (soft delete) en todas las entidades
- **Filtrado** en endpoints de listado por texto libre
- Cambio de **contraseña** autenticado
- Asociación de **usuarios a empresas** con protección de empresa del administrador
- **Generación de reportes PDF** vía Thymeleaf + OpenPDF

---

## 2. Requisitos Funcionales

### RF-01 — Autenticación y Autorización

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-01.01 | Inicio de sesión | El sistema debe autenticar usuarios mediante email y contraseña, retornando un token JWT con validez de 24 horas | Alta
| RF-01.02 | Cierre de sesión | El sistema debe soportar el cierre de sesión (stateless, sin estado en servidor) | Media
| RF-01.03 | Usuario actual | El sistema debe permitir consultar los datos del usuario autenticado incluyendo su empresa asociada | Alta
| RF-01.04 | Cambio de contraseña | El sistema debe permitir al usuario cambiar su contraseña validando la contraseña anterior | Alta
| RF-01.05 | Protección contra fuerza bruta | El sistema debe limitar los intentos de login a 5 por minuto y los cambios de contraseña a 5 por minuto por IP | Alta
| RF-01.06 | RBAC con tres roles | El sistema debe implementar tres roles: SUPER_ADMIN (acceso total), ADMIN (gestión sin roles/permisos), USER (lectura) | Alta
| RF-01.07 | Permisos granulares | El sistema debe soportar 31 permisos organizados por dominio (user, role, permission, vehiculo, chofer, recorrido, empresa, marca, tipo_vehiculo, tipo_combustible, categoria_licencia) | Alta

### RF-02 — Gestión de Empresas

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-02.01 | CRUD de empresas | Crear, leer, actualizar y eliminar (lógico) empresas con código, nombre, dirección, teléfono, email | Alta
| RF-02.02 | Asociación geográfica | Cada empresa debe poder asociarse a una provincia y un municipio del catálogo geográfico | Media
| RF-02.03 | Filtrado por texto | Las listas de empresas deben soportar filtrado por texto libre (nombre, código) y paginación | Media
| RF-02.04 | Búsqueda por código | Se debe poder buscar una empresa por su código único | Media

### RF-03 — Gestión de Vehículos

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-03.01 | CRUD de vehículos | Crear, leer, actualizar y eliminar (lógico) vehículos con todos sus atributos | Alta
| RF-03.02 | Doble unicidad | El sistema debe validar que matrícula y número de motor sean únicos en toda la base de datos | Alta
| RF-03.03 | Relaciones obligatorias | Cada vehículo debe estar asociado a una empresa, tipo de vehículo, marca y tipo de combustible | Alta
| RF-03.04 | Chofer asignado | El vehículo puede tener un chofer asignado (nullable) | Baja
| RF-03.05 | Seguimiento de mantenimiento | El vehículo debe registrar fecha del último mantenimiento y odómetro en dicho momento | Media
| RF-03.06 | Índice de consumo | Cada vehículo debe tener un índice de consumo (L/100km) utilizado para calcular el consumo teórico | Alta
| RF-03.07 | Consultas por relación | Listar vehículos por chofer, tipo de vehículo, tipo de combustible y empresa | Media
| RF-03.08 | Vehículos sin chofer | Consulta específica para obtener vehículos sin chofer asignado | Baja
| RF-03.09 | Límite por suscripción | La creación de vehículos debe respetar el límite del plan de suscripción activo de la empresa | Alta

### RF-04 — Gestión de Choferes

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-04.01 | CRUD de choferes | Crear, leer, actualizar y eliminar (lógico) choferes con sus datos personales | Alta
| RF-04.02 | Categorías de licencia | Cada chofer puede tener múltiples categorías de licencia con fecha de emisión (relación 1:N) | Alta
| RF-04.03 | Unicidad de identidad | El carné de identidad y el número de licencia deben ser únicos | Alta
| RF-04.04 | Reemplazo de categorías | Al actualizar un chofer, las categorías existentes se eliminan y se recrean con la nueva lista | Media
| RF-04.05 | Baja en cascada | Al eliminar un chofer, sus categorías de licencia también se desactivan | Media
| RF-04.06 | Filtro por empresa | Listar choferes filtrados por empresa | Media

### RF-05 — Gestión de Recorridos

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-05.01 | CRUD de recorridos | Crear, leer, actualizar y eliminar (lógico) recorridos | Alta
| RF-05.02 | Cálculo automático de consumo | El consumo se calcula como `(índiceConsumo × kilómetros) / 100`, redondeado a 2 decimales | Alta
| RF-05.03 | Validación de combustible | No se puede crear un recorrido si el combustible disponible (inicial - consumo + abastecido) es negativo | Alta
| RF-05.04 | Validación de tarjeta | Si se usa tarjeta de combustible, el importe no puede exceder el saldo disponible; se descuenta automáticamente | Alta
| RF-05.05 | Actualización de odómetro | Cada recorrido actualiza automáticamente el odómetro del vehículo sumando los kilómetros | Alta
| RF-05.06 | Unicidad por vehículo-fecha | No puede existir más de un recorrido para el mismo vehículo en la misma fecha | Alta
| RF-05.07 | Orden cronológico | No se puede insertar un recorrido con fecha anterior si ya existe un recorrido posterior para el mismo vehículo | Alta
| RF-05.08 | Reversión en actualización | Al actualizar un recorrido, se restauran los valores anteriores de combustible, odómetro y saldo de tarjeta antes de recalcular | Alta
| RF-05.09 | Reversión en eliminación | Al eliminar un recorrido, se restauran los kilómetros, combustible y saldo de tarjeta al vehículo | Alta
| RF-05.10 | Transaccionalidad | Todas las operaciones de recorrido deben ser atómicas (`@Transactional`) | Alta

### RF-06 — Gestión de Tarjetas de Combustible

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-06.01 | CRUD de tarjetas | Crear, leer, actualizar y eliminar (lógico) tarjetas de combustible | Alta
| RF-06.02 | Control de saldo | Cada tarjeta tiene un saldo que se descuenta al registrar recorridos con abastecimiento | Alta
| RF-06.03 | Relación con moneda | Cada tarjeta está asociada a una moneda (Currency) | Alta
| RF-06.04 | Filtrado por empresa | Listar tarjetas filtradas por empresa | Media

### RF-07 — Suscripciones y Planes

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-07.01 | Gestión de planes | CRUD de planes con nombre, precio mensual, máximos de usuarios/vehículos, duración, descuento anual y features asociadas | Alta
| RF-07.02 | Planes públicos de lectura | Los planes deben ser visibles sin autenticación (GET); escritura requiere SUPER_ADMIN | Media
| RF-07.03 | Cálculo de importe | Calcular el importe de facturación mensual o anual (con descuento) para un plan determinado | Alta
| RF-07.04 | Creación de suscripción | Crear suscripciones asociando una empresa a un plan con fechas de inicio/fin y límites | Alta
| RF-07.05 | Consulta de suscripción propia | El usuario puede consultar la suscripción activa de su empresa | Alta
| RF-07.06 | Contadores de uso | El sistema debe llevar contadores actuales de vehículos y usuarios por suscripción, incrementándolos al crear entidades | Alta
| RF-07.07 | Expiración automática | Un scheduler diario (2 AM) debe expirar automáticamente las suscripciones cuya fecha de fin haya pasado | Alta
| RF-07.08 | Bloqueo optimista | Las suscripciones deben usar `@Version` para evitar actualizaciones concurrentes en los contadores | Media
| RF-07.09 | Suscripción de prueba | El sistema debe poder crear una suscripción de prueba (trial) al registrar una nueva empresa | Media

### RF-08 — Modelo Geográfico

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-08.01 | CRUD de provincias | Gestión completa de provincias con código (1-99) y nombre | Media
| RF-08.02 | CRUD de municipios | Gestión completa de municipios asociados a una provincia con código (1-999) y nombre | Media
| RF-08.03 | Listado por provincia | Los municipios pueden listarse filtrados por provincia, tanto paginados como lista completa | Media
| RF-08.04 | Unicidad compuesta | Los municipios tienen unicidad compuesta por (provincia_id, codigo) | Alta

### RF-09 — Catálogos Maestros

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-09.01 | Marcas | CRUD con nombre, descripción y país de origen | Baja
| RF-09.02 | Tipos de Vehículo | CRUD con nombre y descripción | Baja
| RF-09.03 | Tipos de Combustible | CRUD con código, denominación y descripción | Baja
| RF-09.04 | Categorías de Licencia | CRUD con código (1 carácter), denominación y descripción | Baja
| RF-09.05 | Monedas | CRUD con código ISO y descripción, con búsqueda por ISO code | Baja
| RF-09.06 | Features | CRUD de features (funcionalidades del sistema) con nombre y descripción | Media

### RF-10 — Reportes de Transporte

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-10.01 | Consumo por vehículo | Reporte paginado de consumo por vehículo con filtros por fechas, tipo de vehículo, marca y tipo de combustible. Incluye: km totales, litros totales, consumo teórico, consumo real, desviación, eficiencia | Alta
| RF-10.02 | Abastecimiento por vehículo | Reporte paginado de abastecimiento por vehículo con filtros por fechas, vehículo y lugar. Incluye: total de cargas, litros totales, promedio por carga, frecuencia en días, lugar más frecuente | Alta
| RF-10.03 | Mantenimiento por vehículo | Reporte paginado del estado de mantenimiento con umbrales configurables. Incluye: km desde último mantenimiento, clasificación (AL DÍA, PRÓXIMO, VENCIDO), días transcurridos | Alta
| RF-10.04 | Consumo por tipo de combustible | Reporte con resumen ejecutivo y detalle por tipo de combustible. Incluye: volumen consumido/abastecido, costo estimado, porcentaje del total, variación vs período anterior | Alta
| RF-10.05 | Movimiento mensual por vehículo | Reporte diario simulado de un vehículo en un mes específico con análisis comparativo contra la norma de consumo | Media
| RF-10.06 | Aislamiento por empresa | Todos los reportes de transporte deben filtrar automáticamente por la empresa del usuario autenticado | Alta
| RF-10.07 | Generación PDF | El sistema debe soportar la generación de reportes en formato PDF vía plantillas Thymeleaf | Media

### RF-11 — Gestión de Usuarios y Roles

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-11.01 | CRUD de usuarios | Crear, leer, actualizar y eliminar (lógico) usuarios con asignación de roles y empresa | Alta
| RF-11.02 | CRUD de roles | SUPER_ADMIN gestiona roles completos; ADMIN solo lectura | Alta
| RF-11.03 | CRUD de permisos | Solo SUPER_ADMIN puede gestionar permisos | Alta
| RF-11.04 | Asociación usuario-empresa | Cada usuario puede estar asociado a una empresa (nullable) | Alta

### RF-12 — Requisitos Transversales

| ID | Requisito | Descripción | Prioridad
|---|---|---|---|
| RF-12.01 | Baja lógica universal | Ninguna entidad se elimina físicamente; todas usan soft delete (`activo = false`) | Alta
| RF-12.02 | Auditoría automática | Todas las entidades registran automáticamente quién creó/modificó y cuándo | Alta
| RF-12.03 | Paginación estándar | Todos los endpoints de listado soportan paginación (page, perPage, sort, sortOrder) | Alta
| RF-12.04 | Filtrado por texto libre | Los endpoints principales soportan búsqueda por texto parcial case-insensitive | Media
| RF-12.05 | Validación de entrada | Todos los request DTOs usan anotaciones Jakarta Bean Validation | Alta
| RF-12.06 | Manejo de errores estandarizado | Respuestas de error con formato consistente (ApiError) para 400, 404 y 500 | Alta
| RF-12.07 | Documentación API | Toda la API está documentada con OpenAPI/Swagger v3 | Media
| RF-12.08 | Multi-empresa | Los datos están aislados por empresa; los reportes y consultas respetan el scope de la empresa del usuario autenticado | Alta

---

## 3. Stack Tecnológico

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
| Reducción de boilerplate | Lombok | (managed by Spring Boot) | `@Getter`, `@Setter`, `@SuperBuilder`, `@Builder` |
| Mapeo DTO | MapStruct | (managed by Spring Boot) | Mappers con `BaseMapperConfig` compartido |
| Documentación API | OpenAPI / Swagger v3 | (springdoc) | Tags en controladores |
| Generación PDF | Thymeleaf + OpenPDF | — | Reportes PDF desde plantillas HTML |
| Testing | JUnit 5 + Mockito | (Spring Boot Starter Test) | Solo pruebas unitarias |
| Logs | SLF4J + Logback | (Spring Boot default) | DEBUG en desarrollo |

---

## 4. Arquitectura del Sistema

### 4.1 Patrón Arquitectónico

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
│      mapeo entidad ↔ DTO (MapStruct)          │
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

### 4.2 Capas Detalladas

#### Controladores (`controller/`)

- Exponen endpoints REST bajo el prefijo `/api/`
- Reciben `XRequest` DTOs con anotaciones de validación Jakarta
- Delegan al servicio correspondiente
- Retornan `ResponseEntity<XResponse>` con códigos HTTP apropiados
- Todos los endpoints de listado soportan **paginación** y algunos soportan **filtrado** por texto libre

#### Servicios (`service/` + `service/impl/`)

- Cada dominio tiene una interfaz `XService` y su implementación `XServiceImpl`
- Contienen toda la lógica de negocio y reglas de validación
- El mapeo entidad ↔ DTO se realiza mediante **MapStruct** (mappers en `mapper/`)
- Anotados con `@Transactional` para garantizar atomicidad

#### Repositorios (`repository/`)

- Interfaces Spring Data JPA que extienden `JpaRepository<Entity, Long>`
- Proveen métodos de búsqueda por claves naturales
- Todos incluyen `findAllByActivoTrue(Pageable)` para soportar baja lógica con paginación
- Algunos incluyen consultas personalizadas con `@Query` (JPQL y SQL nativo para reportes)
- Interfaces de proyección para consultas de agregación

#### Modelo (`model/`)

- Entidades JPA que heredan de `BaseEntity`
- Relaciones mapeadas con `FetchType.LAZY` (excepto `User.roles` y `Role.permissions` que son `EAGER`)
- Validaciones con anotaciones Jakarta

---

## 5. Estructura del Proyecto

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
│   │   │   │   ├── SecurityConfig.java               # Cadena de filtros JWT, CORS, RBAC
│   │   │   │   ├── RateLimitFilter.java             # Filtro de rate limiting por IP
│   │   │   │   └── SubscriptionExpirationScheduler.java # Expiración automática de suscripciones
│   │   │   ├── security/                             # Componentes de seguridad
│   │   │   │   ├── AuthenticatedUser.java            # UserDetails personalizado (envuelve User)
│   │   │   │   ├── CustomUserDetailsService.java     # Carga de usuario + autoridades
│   │   │   │   ├── JwtAuthenticationFilter.java      # Filtro OncePerRequest para JWT
│   │   │   │   └── JwtService.java                   # Generación y validación de tokens JWT
│   │   │   ├── model/                                # Entidades JPA
│   │   │   │   ├── BaseEntity.java                   # Superclase abstracta (id, audit, soft-delete)
│   │   │   │   ├── User.java, Role.java, Permission.java
│   │   │   │   ├── Empresa.java                      # Con provincia y municipio
│   │   │   │   ├── Vehiculo.java                     # Con odometroUltimoMantenimiento
│   │   │   │   ├── Chofer.java, ChoferCategoria.java
│   │   │   │   ├── CategoriaLicencia.java, TipoVehiculo.java
│   │   │   │   ├── Marca.java, TipoCombustible.java
│   │   │   │   ├── Recorrido.java
│   │   │   │   ├── TarjetaCombustible.java, Currency.java
│   │   │   │   ├── Plan.java                         # Planes de suscripción
│   │   │   │   ├── Subscription.java                 # Suscripciones de empresas
│   │   │   │   ├── SubscriptionStatus.java          # Enum: ACTIVE, EXPIRED
│   │   │   │   ├── Feature.java                      # Features del sistema
│   │   │   │   ├── Provincia.java, Municipio.java    # Modelo geográfico
│   │   │   ├── dto/                                  # DTOs de request/response por dominio
│   │   │   │   ├── auth/, user/, role/, permission/
│   │   │   │   ├── empresa/, vehiculo/, chofer/
│   │   │   │   ├── recorrido/, tipovehiculo/, marca/
│   │   │   │   ├── tipocombustible/, categorialicencia/
│   │   │   │   ├── tarjetacombustible/, currency/
│   │   │   │   ├── subscription/, reporte/
│   │   │   │   └── provincia/, municipio/
│   │   │   ├── mapper/                               # Mappers MapStruct
│   │   │   │   ├── BaseMapperConfig.java             # Config compartida + toAuditResponse
│   │   │   │   └── ... (21 mappers por dominio)
│   │   │   ├── repository/                           # Repositorios Spring Data JPA
│   │   │   │   ├── *Repository.java                   # Interfaces de repositorio
│   │   │   │   └── *Projection.java                   # Proyecciones para consultas de agregación
│   │   │   ├── service/                              # Interfaces de servicio
│   │   │   ├── service/impl/                         # Implementaciones de servicio
│   │   │   ├── controller/                           # Controladores REST
│   │   │   ├── exception/                            # Manejo global de excepciones
│   │   │   └── util/                                 # Utilidades compartidas
│   │   │       ├── PaginationUtils.java              # Conversión de paginación
│   │   │       ├── PaginationParams.java             # POJO de parámetros de paginación
│   │   │       ├── SecurityUtils.java                # resolveEmpresaId() desde SecurityContext
│   │   │       ├── AuditMapper.java                  # Mapeo User → UserAuditResponse
│   │   │       └── ErrorResponse.java               # DTO de error estandarizado
│   │   └── resources/
│   │       ├── application.properties                # Configuración común
│   │       ├── application-dev.properties            # Perfil desarrollo (H2)
│   │       ├── application-prod.properties           # Perfil producción (PostgreSQL)
│   │       └── templates/reports/                    # Plantillas Thymeleaf para PDF
│   └── test/
│       └── java/com/fleet/management/service/impl/   # Pruebas unitarias
```

---

## 6. Modelo de Datos

### 6.1 BaseEntity — Superclase Abstracta

Todas las entidades heredan de `BaseEntity`, la cual proporciona campos comunes de identificación, auditoría y eliminación lógica:

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | `Long` | PK, auto-increment `IDENTITY` | Identificador único |
| `activo` | `Boolean` | `NOT NULL`, default `true` | Flag de baja lógica (soft delete) |
| `fechaCreacion` | `LocalDateTime` | `NOT NULL`, `updatable=false` | Fecha de creación (auto vía `@PrePersist`) |
| `fechaActualizacion` | `LocalDateTime` | nullable | Fecha de última modificación (auto vía `@PreUpdate`) |
| `creadoPor` | `User` (lazy) | FK `creado_por_id` | Usuario que creó la entidad |
| `modificadoPor` | `User` (lazy) | FK `modificado_por_id` | Usuario que modificó la entidad por última vez |

**Mecanismo de auditoría:** Los campos `creadoPor` y `modificadoPor` se pueblan automáticamente en los callbacks `@PrePersist` y `@PreUpdate` de `BaseEntity`, leyendo el `AuthenticatedUser` del `SecurityContextHolder`.

### 6.2 Entidades del Modelo de Suscripción

#### Plan

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `nombre` | `String(100)` | `NOT NULL`, `UNIQUE` | Nombre del plan |
| `precioMensual` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Precio mensual del plan |
| `maxUsuarios` | `Integer` | `NOT NULL`, `>= 1` | Máximo de usuarios permitidos |
| `maxVehiculos` | `Integer` | `NOT NULL`, `>= 1` | Máximo de vehículos permitidos |
| `duracion` | `Integer` | `NOT NULL`, `>= 1` | Duración en meses |
| `porcientoDescuentoAnual` | `BigDecimal(5,2)` | `0-100` | Descuento por pago anual |
| `features` | `Set<Feature>` | M:N via `sub_plan_features` | Features incluidas en el plan |

**Tabla:** `sub_plans`

#### Subscription

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `empresa` | `Empresa` | `NOT NULL`, FK | Empresa suscrita |
| `plan` | `Plan` | `NOT NULL`, FK | Plan contratado |
| `startDate` | `LocalDate` | `NOT NULL` | Fecha de inicio |
| `endDate` | `LocalDate` | `NOT NULL` | Fecha de fin |
| `status` | `SubscriptionStatus` | `NOT NULL` | ACTIVE o EXPIRED |
| `maxVehiculos` | `Integer` | `>= 0` | Límite de vehículos de esta suscripción |
| `maxUsuarios` | `Integer` | `>= 0` | Límite de usuarios de esta suscripción |
| `currentVehicleCount` | `Integer` | `>= 0` | Vehículos actualmente registrados |
| `currentUserCount` | `Integer` | `>= 0` | Usuarios actualmente registrados |
| `porcientoDescuentoAnual` | `BigDecimal(5,2)` | `0-100` | Descuento aplicado |
| `version` | `Long` | `@Version` | Bloqueo optimista |

**Tabla:** `sub_subscriptions`

#### SubscriptionStatus (Enum)

| Valor | Descripción |
|---|---|
| `ACTIVE` | Suscripción vigente |
| `EXPIRED` | Suscripción expirada (transición automática vía scheduler) |

#### Feature

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `name` | `String(100)` | `NOT NULL`, `UNIQUE` | Nombre de la feature |
| `descripcion` | `String(255)` | nullable | Descripción |

**Tabla:** `sub_features`

### 6.3 Entidades del Modelo Geográfico

#### Provincia

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `codigo` | `Integer` | `NOT NULL`, `UNIQUE`, 1-99 | Código de la provincia |
| `nombre` | `String(100)` | `NOT NULL` | Nombre de la provincia |

**Tabla:** `provincias`

#### Municipio

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `provincia` | `Provincia` | `NOT NULL`, FK | Provincia a la que pertenece |
| `codigo` | `Integer` | `NOT NULL`, 1-999 | Código del municipio |
| `nombre` | `String(100)` | `NOT NULL` | Nombre del municipio |

**Tabla:** `municipios` | **UK compuesto:** `(provincia_id, codigo)`

### 6.4 Entidades Principales

#### Empresa

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `codigo` | `String(30)` | `NOT NULL`, `UNIQUE` | Código identificador |
| `nombre` | `String(150)` | `NOT NULL` | Nombre |
| `direccion` | `String(255)` | nullable | Dirección física |
| `telefono` | `String(20)` | nullable | Teléfono |
| `email` | `String(100)` | nullable, `@Email` | Correo |
| `provincia` | `Provincia` | nullable, FK | Provincia (modelo geográfico) |
| `municipio` | `Municipio` | nullable, FK | Municipio (modelo geográfico) |

#### Vehiculo

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `empresa` | `Empresa` | `NOT NULL`, FK | Empresa propietaria |
| `tipoVehiculo` | `TipoVehiculo` | `NOT NULL`, FK | Tipo de vehículo |
| `marca` | `Marca` | `NOT NULL`, FK | Marca |
| `chofer` | `Chofer` | nullable, FK | Chofer asignado |
| `tipoCombustible` | `TipoCombustible` | `NOT NULL`, FK | Tipo de combustible |
| `matricula` | `String(20)` | `NOT NULL`, `UNIQUE` | Matrícula / placa |
| `modelo` | `String(100)` | nullable | Modelo |
| `numeroMotor` | `String(50)` | `NOT NULL`, `UNIQUE` | Número de motor |
| `odometro` | `BigInteger` | `NOT NULL`, `>= 0` | Kilometraje actual |
| `combustible` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Nivel de combustible (litros) |
| `ultimoMantenimiento` | `LocalDate` | `@PastOrPresent` | Fecha último mantenimiento |
| `odometroUltimoMantenimiento` | `BigInteger` | `>= 0` | Odómetro en último mantenimiento |
| `indiceConsumo` | `BigDecimal(10,2)` | `NOT NULL`, `> 0` | Consumo L/100km |

#### Recorrido

| Campo | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `vehiculo` | `Vehiculo` | FK (parte de UK) | Vehículo |
| `fecha` | `LocalDate` | FK (parte de UK) | Fecha |
| `kilometros` | `Integer` | `NOT NULL`, `>= 1` | Kilómetros recorridos |
| `odometroInicial` | `BigInteger` | `NOT NULL`, `>= 0` | Odómetro antes del recorrido |
| `combustibleInicial` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Combustible antes del recorrido |
| `consumo` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Consumo calculado |
| `litrosAbastecidos` | `BigDecimal(10,2)` | `NOT NULL`, `>= 0` | Litros repostados |
| `chofer` | `Chofer` | nullable, FK | Chofer del recorrido |
| `numeroChip` | `String(50)` | nullable | Número de chip de tarjeta |
| `lugarAbastecimiento` | `String(100)` | nullable | Lugar de repostaje |
| `tarjetaCombustible` | `TarjetaCombustible` | nullable, FK | Tarjeta utilizada |
| `importeAbastecido` | `Double` | nullable | Importe del abastecimiento |

**UK compuesto:** `(vehiculo_id, fecha)`

---

## 7. API REST — Endpoints

### 7.1 Autenticación

| Método | Ruta | Descripción | Auth | Rol requerido |
|---|---|---|---|---|
| `POST` | `/api/auth/login` | Iniciar sesión, retorna JWT | No | — |
| `POST` | `/api/auth/logout` | Cerrar sesión (no-op, stateless) | No | — |
| `GET` | `/api/auth/me` | Obtener usuario actual | Sí | Cualquiera |
| `PUT` | `/api/auth/cambiar-password` | Cambiar contraseña | Sí | Cualquiera |

### 7.2 Gestión de Usuarios

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/users` | Listar usuarios (paginado, filtrable) | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/{id}` | Obtener usuario por ID | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/email/{email}` | Obtener por email | SUPER_ADMIN, ADMIN |
| `GET` | `/api/users/empresa/{empresaId}` | Usuarios por empresa | SUPER_ADMIN, ADMIN |
| `POST` | `/api/users` | Crear usuario | SUPER_ADMIN, ADMIN |
| `PUT` | `/api/users/{id}` | Actualizar usuario | SUPER_ADMIN, ADMIN |
| `DELETE` | `/api/users/{id}` | Eliminar usuario (lógico) | SUPER_ADMIN, ADMIN |

### 7.3 Gestión de Roles

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/roles` | Listar roles (paginado) | SUPER_ADMIN, ADMIN |
| `GET` | `/api/roles/{id}` | Obtener rol por ID | SUPER_ADMIN, ADMIN |
| `GET` | `/api/roles/name/{name}` | Buscar por nombre | SUPER_ADMIN, ADMIN |
| `GET` | `/api/roles/permission/{permissionId}` | Roles por permiso (paginado) | SUPER_ADMIN, ADMIN |
| `POST` | `/api/roles` | Crear rol | SUPER_ADMIN |
| `PUT` | `/api/roles/{id}` | Actualizar rol | SUPER_ADMIN |
| `DELETE` | `/api/roles/{id}` | Eliminar rol | SUPER_ADMIN |

### 7.4 Gestión de Permisos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| Todos | `/api/permissions/**` | CRUD completo | SUPER_ADMIN |

### 7.5 Gestión de Empresas

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/empresas/**` | CRUD estándar + búsqueda por código | Autenticado |

### 7.6 Gestión de Vehículos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/vehiculos/**` | CRUD estándar | Autenticado |
| `GET` | `/api/vehiculos/sin-chofer` | Vehículos sin chofer | Autenticado |
| `GET` | `/api/vehiculos/chofer/{choferId}` | Por chofer | Autenticado |
| `GET` | `/api/vehiculos/tipo-vehiculo/{id}` | Por tipo de vehículo | Autenticado |
| `GET` | `/api/vehiculos/tipo-combustible/{id}` | Por tipo de combustible | Autenticado |
| `GET` | `/api/vehiculos/empresa/{empresaId}` | Por empresa (paginado) | Autenticado |
| `GET` | `/api/vehiculos/reporte-movimiento-mensual/{id}?mes=&anio=` | Reporte mensual | Autenticado |

### 7.7 Gestión de Choferes

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/choferes/**` | CRUD estándar (con categorías anidadas) | Autenticado |
| `GET` | `/api/choferes/empresa/{empresaId}` | Por empresa | Autenticado |

### 7.8 Gestión de Recorridos

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/recorridos/**` | CRUD estándar | Autenticado |
| `GET` | `/api/recorridos/vehiculo/{vehiculoId}?from=&to=` | Por vehículo con rango de fechas | Autenticado |

### 7.9 Gestión de Tarjetas de Combustible

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/tarjetas-combustible/**` | CRUD estándar + búsqueda por número y empresa | Autenticado |

### 7.10 Gestión de Monedas

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/currencies/**` | CRUD estándar + búsqueda por ISO code | Autenticado |

### 7.11 Gestión de Planes

| Método | Ruta | Descripción | Auth | Rol requerido |
|---|---|---|---|---|
| `GET` | `/api/plans` | Listar planes (paginado) | No | — |
| `GET` | `/api/plans/{id}` | Obtener plan por ID | No | — |
| `GET` | `/api/plans/{id}/calcular-importe?facturarAnual=` | Calcular importe de facturación | No | — |
| `POST` | `/api/plans` | Crear plan | Sí | SUPER_ADMIN |
| `PUT` | `/api/plans/{id}` | Actualizar plan | Sí | SUPER_ADMIN |
| `DELETE` | `/api/plans/{id}` | Eliminar plan | Sí | SUPER_ADMIN |

### 7.12 Gestión de Suscripciones

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| `GET` | `/api/subscriptions` | Listar suscripciones (paginado) | Autenticado |
| `GET` | `/api/subscriptions/my-company` | Suscripción de la empresa del usuario | Autenticado |
| `GET` | `/api/subscriptions/{id}` | Obtener por ID | Autenticado |
| `GET` | `/api/subscriptions/empresa/{empresaId}` | Por empresa (paginado) | Autenticado |
| `GET` | `/api/subscriptions/plan/{planId}` | Por plan (paginado) | Autenticado |
| `POST` | `/api/subscriptions` | Crear suscripción | Autenticado |
| `PUT` | `/api/subscriptions/{id}` | Actualizar suscripción | Autenticado |
| `DELETE` | `/api/subscriptions/{id}` | Eliminar suscripción | Autenticado |

### 7.13 Modelo Geográfico

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/provincias/**` | CRUD estándar de provincias | Autenticado |
| CRUD | `/api/municipios/**` | CRUD estándar de municipios | Autenticado |
| `GET` | `/api/municipios/provincia/{provinciaId}` | Municipios por provincia (paginado) | Autenticado |
| `GET` | `/api/municipios/provincia/{provinciaId}/list` | Municipios por provincia (lista completa) | Autenticado |

### 7.14 Gestión de Features

| Método | Ruta | Descripción | Rol requerido |
|---|---|---|---|
| CRUD | `/api/features/**` | CRUD estándar | Autenticado |

### 7.15 Catálogos Maestros

- **Tipos de Vehículo:** `/api/tipos-vehiculo`
- **Marcas:** `/api/marcas`
- **Tipos de Combustible:** `/api/tipos-combustible`
- **Categorías de Licencia:** `/api/categorias-licencia`
- **Categorías de Chofer:** `/api/choferes-categorias`

### 7.16 Parámetros de Paginación y Filtrado

| Parámetro | Default | Descripción |
|---|---|---|
| `page` | `0` | Número de página (1-based desde el cliente, convertido a 0-based internamente) |
| `perPage` | `20` | Elementos por página |
| `sort` | `id` | Campo de ordenamiento |
| `sortOrder` | `ASC` | Dirección: `ASC` o `DESC` |
| `filter` | (none) | Texto libre para filtrado |

---

## 8. Seguridad y Autenticación

### 8.1 Flujo de Autenticación JWT

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
7. SecurityContext populated → Authorized
```

### 8.2 Componentes de Seguridad

| Componente | Responsabilidad |
|---|---|
| `SecurityConfig` | Cadena de filtros, CORS, reglas de acceso por rol, `@EnableMethodSecurity` |
| `RateLimitFilter` | Filtro de rate limiting por IP (5 intentos / 60s) para login y cambio de contraseña |
| `JwtAuthenticationFilter` | `OncePerRequestFilter` que extrae y valida el token Bearer |
| `JwtService` | Genera y valida tokens JWT con claims de roles |
| `AuthenticatedUser` | `UserDetails` que envuelve la entidad `User` completa |
| `CustomUserDetailsService` | Carga usuario desde BD, asigna autoridades `ROLE_` + permisos |
| `SecurityUtils` | Utilidad estática para extraer `empresaId` del `SecurityContext` |

### 8.3 Rate Limiting

| Propiedad | Default | Descripción |
|---|---|---|
| `fleet.security.rate-limit.max-attempts` | `5` | Máximo de intentos por ventana |
| `fleet.security.rate-limit.window-seconds` | `60` | Ventana de tiempo en segundos |

Endpoints protegidos: `POST /api/auth/login`, `PUT /api/auth/cambiar-password`

### 8.4 Reglas de Acceso

| Ruta | Acceso |
|---|---|
| `/api/v1/plans/**` (GET) | Público |
| `/api/v1/plans/**` (POST/PUT/DELETE) | SUPER_ADMIN |
| `/api/auth/**` | Público |
| `/api/users/**` | SUPER_ADMIN, ADMIN |
| `/api/roles/**` (GET) | SUPER_ADMIN, ADMIN |
| `/api/roles/**` (POST/PUT/DELETE) | SUPER_ADMIN |
| `/api/permissions/**` | SUPER_ADMIN |
| `/swagger-ui/**`, `/v3/api-docs/**` | Público |
| `/h2-console/**` | Público (solo desarrollo) |
| Cualquier otra ruta `/api/**` | Autenticado |

---

## 9. Capa de Servicios — Lógica de Negocio

### 9.1 Patrón CRUD Estándar

Todos los servicios implementan el siguiente patrón:

| Operación | Comportamiento general |
|---|---|
| `findAll(Pageable)` | `repository.findAllByActivoTrue(pageable)` → mapea a `Page<XResponse>` |
| `findById(Long)` | Busca por ID, lanza `ResourceNotFoundException` si no existe o está inactivo |
| `create(XRequest)` | Valida campos únicos, guarda y retorna respuesta |
| `update(Long, XRequest)` | Valida campos únicos excluyendo la entidad actual, actualiza |
| `delete(Long)` | Baja lógica: `activo = false` |

### 9.2 Lógica Específica por Servicio

#### RecorridoServiceImpl

El servicio de recorridos es el núcleo del dominio con reglas transaccionales críticas:

**Creación:** Valida unicidad vehiculo+fecha, orden cronológico, calcula consumo automáticamente `(índiceConsumo × km) / 100`, valida disponibilidad de combustible, valida saldo de tarjeta, actualiza odómetro y combustible del vehículo atómicamente.

**Actualización:** Restaura consumo/odómetro/saldo anteriores, recalcula con nuevos valores, verifica disponibilidad.

**Eliminación:** Restaura km, combustible y saldo al vehículo, ejecuta baja lógica.

#### SubscriptionServiceImpl

- `createTrialSubscription(Empresa)`: Crea suscripción de prueba al registrar empresa
- `incrementVehicleCount/decrementVehicleCount`: Actualizan contadores de uso al crear/eliminar vehículos
- `incrementUserCount/decrementUserCount`: Actualizan contadores al crear/eliminar usuarios
- `findActiveByEmpresa`: Retorna la suscripción activa de una empresa

#### PlanServiceImpl

- `calcularImporteFacturacion(Long planId, boolean facturarAnual)`: Calcula importe mensual o anual (con descuento por pago anual)

---

## 10. Reportes

### 10.1 Reporte de Consumo por Vehículo

**Endpoint:** `GET /api/reportes-transporte/consumo-vehiculo`

Reporte paginado con agregación JPQL (GROUP BY vehículo). Calcula: km totales, litros totales, consumo teórico (índiceConsumo × km / 100), consumo real (litros totales), desviación (real - teórico), porcentaje de desviación, eficiencia (km/lt).

**Filtros:** fechaDesde, fechaHasta, tipoVehiculoId, marcaId, tipoCombustibleId.

### 10.2 Reporte de Abastecimiento por Vehículo

**Endpoint:** `GET /api/reportes-transporte/abastecimiento`

Reporte paginado con dos consultas JPQL: agregación principal (GROUP BY vehículo con COUNT, SUM, MIN/MAX fecha) + lugar más frecuente (GROUP BY vehiculo+lugar). Calcula: total de abastecimientos, litros totales, promedio por carga, frecuencia en días, lugar más frecuente.

**Filtros:** desde, hasta, vehiculoId, lugarAbastecimiento.

### 10.3 Reporte de Mantenimiento por Vehículo

**Endpoint:** `GET /api/reportes-transporte/mantenimiento`

Reporte paginado que evalúa el estado de mantenimiento de cada vehículo activo. Clasifica en tres estados: **AL DÍA** (< 80% del umbral), **PRÓXIMO** (>= 80% pero < 100%), **VENCIDO** (>= 100%). Incluye km desde último mantenimiento, días transcurridos.

**Configuración:** `fleet.reporte.mantenimiento.umbral-km` (default 10000), `fleet.reporte.mantenimiento.porcentaje-proximo` (default 80).

### 10.4 Reporte de Consumo por Tipo de Combustible

**Endpoint:** `GET /api/reportes-transporte/consumo-por-combustible`

Reporte con SQL nativo que agrupa por `TipoCombustible.denominacion`. Incluye resumen ejecutivo (totales, costo promedio) y detalle por tipo con: volumen consumido, volumen abastecido, costo estimado, porcentaje del total, variación vs período anterior (mismo rango de fechas del ciclo inmediatamente anterior), costo promedio por litro.

**Filtros:** fechaDesde, fechaHasta, tipoVehiculoId.

**Servicio dedicado:** `ReporteConsumoCombustibleService` (interfaz + impl separados).

### 10.5 Reporte de Movimiento Mensual por Vehículo

**Endpoint:** `GET /api/vehiculos/reporte-movimiento-mensual/{vehiculoId}?mes=&anio=`

Reporte diario simulado con análisis comparativo contra la norma de consumo del vehículo.

---

## 11. Manejo de Excepciones

| Excepción | Código HTTP | Uso |
|---|---|---|
| `ResourceNotFoundException` | 404 | Entidad no encontrada |
| `BusinessException` | 400 | Violación de regla de negocio |
| `MethodArgumentNotValidException` | 400 | Errores de validación de campos |
| `Exception` (catch-all) | 500 | Errores no esperados |

**Formato de respuesta de error (`ErrorResponse`):**

```json
{
  "timestamp": "2026-09-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Vehiculo no encontrado con id: 999",
  "path": "/api/vehiculos/999",
  "fieldErrors": null
}
```

---

## 12. Utilidades y Patrones Comunes

### 12.1 SecurityUtils

Clase utilitaria `final` con constructor privado que centraliza la extracción del `empresaId` del usuario autenticado:

- **`resolveEmpresaId()`**: Método estático que obtiene el `Authentication` del `SecurityContextHolder`, extrae `AuthenticatedUser → User → Empresa → id`. Lanza `BusinessException` si no hay autenticación o si el usuario no tiene empresa.

### 12.2 PaginationUtils

- **`params(int page, int perPage, String sortBy, String sortOrder)`**: Convierte parámetros 1-based a `PaginationParams` 0-based.
- **`of(PaginationParams)`**: Convierte a Spring `PageRequest`.

### 12.3 BaseMapperConfig

Interfaz MapStruct `@MapperConfig` compartida por todos los mappers:

- `componentModel = "spring"`, `unmappedTargetPolicy = IGNORE`
- Método `default` `toAuditResponse(User)`: Mapea `User` → `UserAuditResponse` (id, email).

### 12.4 Mappers

21 mappers MapStruct (uno por dominio) que generan automáticamente la conversión entidad ↔ DTO en tiempo de compilación.

### 12.5 Proyecciones de Repositorio

Interfaces Java en `repository/` para consultas de agregación en reportes:

- `ConsumoVehiculoProjection` — Consumo por vehículo (km, litros, consumo teórico)
- `AbastecimientoVehiculoProjection` — Abastecimiento por vehículo (totales, fechas)
- `AbastecimientoLugarProjection` — Lugares de abastecimiento por vehículo
- `ConsumoPorCombustibleProjection` — Consumo por tipo de combustible (SQL nativo)


---

## 13. Inicialización de Datos

### 13.1 DataInitializer (Bootstrap)

Se ejecuta automáticamente mediante `CommandLineRunner`. Crea datos iniciales solo si no existen (idempotente):

**Empresa por defecto:** `EMP-ADMIN` — `Empresa de Administracion`

**31 Permisos** organizados por dominio (user, role, permission, vehiculo, chofer, recorrido, empresa, marca, tipo_vehiculo, tipo_combustible, categoria_licencia).

**Roles:** SUPER_ADMIN (todos los permisos), ADMIN (25 permisos, sin role/permission), USER (ninguno por defecto).

**Usuario admin:** `admin@fleet.com` / `admin123` / SUPER_ADMIN / EMP-ADMIN.

---

## 14. Decisiones de Diseño

### 14.1 Baja Lógica Universal

Todas las entidades incluyen `activo` (Boolean, default `true`). La eliminación nunca borra físicamente. La integridad histórica se preserva para auditoría y trazabilidad.

### 14.2 Auditoría Automática vía BaseEntity + SecurityContext

Los campos de auditoría se pueblan en `@PrePersist`/`@PreUpdate` leyendo `SecurityContextHolder`. Permite capturar la entidad `User` completa, no solo el username.

### 14.3 AuthenticatedUser envuelve la entidad User

Implementa `UserDetails` envolviendo la entidad JPA `User` completa, evitando consultas adicionales para auditoría.

### 14.4 Mapeo DTO con MapStruct

Mapeo automático mediante MapStruct con `BaseMapperConfig` compartida. Cada dominio tiene su mapper interface anotado con `@Mapper`. El método `toAuditResponse` está definido como `default` en `BaseMapperConfig` para reutilización.

### 14.5 SecurityUtils — Resolución centralizada de empresa

El método `SecurityUtils.resolveEmpresaId()` centraliza la lógica de extracción del `empresaId` del contexto de seguridad, evitando duplicación en múltiples servicios.

### 14.6 Tres Niveles de Roles

SUPER_ADMIN (acceso total), ADMIN (gestión sin roles/permisos), USER (lectura).

### 14.7 Asociación Usuario-Empresa

Campo `empresa` nullable en `User` para el modelo multi-empresa.

### 14.8 Rate Limiting en Autenticación

`RateLimitFilter` protege endpoints de login y cambio de contraseña contra fuerza bruta (5 intentos / 60s por IP).

### 14.9 Paginación 1-based desde el Cliente

Los controladores exponen parámetros 1-based; `PaginationUtils.params()` convierte a 0-based para Spring Data.

### 14.10 Sesiones Stateless con JWT

`SessionCreationPolicy.STATELESS`. El estado se codifica en el token JWT.

### 14.11 Suscripciones con Expiración Automática

`SubscriptionExpirationScheduler` ejecuta un job diario a las 2 AM que expira suscripciones vencidas. `Subscription` usa `@Version` para bloqueo optimista en contadores.

### 14.12 Reportes con SQL Nativo y JPQL

Los reportes de transporte usan consultas de agregación (GROUP BY) con proyecciones interfaces. El reporte de consumo por tipo de combustible usa SQL nativo; los demás usan JPQL. La paginación y ordenamiento se realiza en memoria para las consultas GROUP BY.

---

## 15. Pruebas Unitarias

### 15.1 Configuración

- **Framework:** JUnit 5 + Mockito
- **Estrategia:** Pruebas unitarias puras con `@ExtendWith(MockitoExtension.class)`

### 15.2 Cobertura

| Clase de Prueba | Operaciones Probadas | Estado |
|---|---|---|
| `AuthServiceImplTest` | login, getCurrentUser, cambiarPassword | Funcional |
| `RecorridoServiceImplTest` | CRUD + reglas de negocio | Funcional |
| `UserServiceImplTest` | CRUD estándar + filtro | Funcional |
| `VehiculoServiceImplTest` | CRUD + validaciones + filtro | Funcional |
| `ChoferServiceImplTest` | CRUD + categorías + filtro | Funcional |
| `EmpresaServiceImplTest` | CRUD estándar + filtro | Funcional |
| `RoleServiceImplTest` | CRUD estándar | Funcional |
| `PermissionServiceImplTest` | CRUD estándar | Funcional |
| `MarcaServiceImplTest` | CRUD estándar | Funcional |
| `TipoVehiculoServiceImplTest` | CRUD estándar | Funcional |
| `TipoCombustibleServiceImplTest` | CRUD estándar | Funcional |
| `CategoriaLicenciaServiceImplTest` | CRUD estándar | Funcional |
| `ChoferCategoriaServiceImplTest` | CRUD estándar | Funcional |

**Faltantes:** `CurrencyServiceImpl`, `TarjetaCombustibleServiceImpl`, servicios de suscripción, reportes, geografía.

---

## 16. Configuración de Entornos

### 16.1 Configuración Común

| Propiedad | Valor | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto del servidor |
| `spring.jpa.hibernate.ddl-auto` | `update` | Generación automática de esquema |
| `spring.jpa.open-in-view` | `false` | Desactiva Open Session in View |
| `jwt.secret` | (64-char hex) | Clave HMAC para JWT |
| `jwt.expiration` | `86400000` | Expiración del token (24h) |
| `spring.profiles.active` | `dev` | Perfil activo por defecto |
| `fleet.security.rate-limit.max-attempts` | `5` | Máximos intentos de login |
| `fleet.security.rate-limit.window-seconds` | `60` | Ventana de rate limiting |
| `fleet.reporte.mantenimiento.umbral-km` | `10000` | Umbral de km para mantenimiento |
| `fleet.reporte.mantenimiento.porcentaje-proximo` | `80` | Porcentaje para estado PRÓXIMO |

### 16.2 Perfil Desarrollo (`application-dev.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:h2:mem:fleetdb` |
| `spring.h2.console.enabled` | `true` |
| Logging | DEBUG para `com.fleet.management` |

### 16.3 Perfil Producción (`application-prod.properties`)

| Propiedad | Valor |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/fleetdb` |
| `spring.jpa.hibernate.ddl-auto` | `update` |
| `spring.h2.console.enabled` | `false` |
| Logging | Nivel INFO |

---

## 17. Problemas Conocidos y Mejoras Pendientes

### 17.1 Problemas Conocidos

| # | Severidad | Problema | Estado |
|---|---|---|---|
| 1 | Media | JWT secret hardcoded | Pendiente |
| 2 | Media | Contraseña admin hardcoded | Pendiente |
| 3 | Media | Duplicación residual de mapeo DTO en servicios de reportes | Pendiente |
| 4 | Baja | Sin migraciones DB (`ddl-auto=update`) | Pendiente |
| 5 | Baja | Riesgo N+1 queries en respuestas anidadas | Pendiente |
| 6 | Baja | Sin caché | Pendiente |
| 7 | Baja | Pruebas faltantes (suscripciones, reportes, geografía) | Pendiente |
| 8 | Info | Inconsistencia ruta planes: SecurityConfig usa `/api/v1/plans/**`, controller usa `/api/plans` | Pendiente |

### 17.2 Mejoras Recomendadas

1. **Externalizar credenciales** — Mover JWT secret y contraseña admin a variables de entorno
2. **Adoptar Flyway o Liquibase** — Control versionado del esquema
3. **Implementar `@EntityGraph` o FETCH JOIN** — Evitar problemas N+1
4. **Completar cobertura de pruebas** — Suscripciones, reportes, geografía, Currency, TarjetaCombustible
5. **Corregir inconsistencia de rutas de planes** — Alinear SecurityConfig con PlanController
6. **Introducir `@Scheduled` explícito** — Añadir `@EnableScheduling` si no está presente
7. **Localizar SubscriptionStatus** — Cambiar ACTIVE/EXPIRED a español (ACTIVA/EXPIRADA)
8. **Mejorar ER con entidades nuevas** — Actualizar diagrama con Plan, Subscription, Feature, Provincia, Municipio
9. **Migrar mapeo manual restante** — Los servicios de reportes aún tienen mapeo manual; podrían usar MapStruct
10. **API de generación PDF** — Completar la integración Thymeleaf + OpenPDF para exportación de reportes
