# ============================================================================
# Dockerfile — Fleet Management Backend
# Multi-stage build con Eclipse Temurin JDK 25 (LTS)
#
# Stage 1 (builder): compila con Maven + JDK 25, genera el JAR
# Stage 2 (runtime): JRE 25 ligero, solo runtime (sin compiladores)
#
# Imagen final: ~230 MB (vs ~520 MB con JDK completo)
#
# Nota de compatibilidad:
#   Spring Boot 3.5.5 tiene soporte oficial para Java 17, 21 y 25 LTS.
#   Java 25 LTS (publicado septiembre 2025) es el baseline recomendado.
# ============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Builder — compila con Maven usando un cache de dependencias
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /workspace

# Instalar Maven (la imagen JDK de Temurin no lo incluye)
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# Copiar primero solo el pom.xml para cache de dependencias (acelera builds
# repetitivos: si el pom no cambia, Docker reutiliza la capa de descarga).
COPY pom.xml ./

# Descargar dependencias (se cachea si pom.xml no cambia)
# --no-transfer-progress: silencia el progreso de descarga (logs más limpios)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline || true

# Copiar el resto del código fuente
COPY src ./src

# Compilar y empaquetar, omitiendo tests (se ejecutan en CI separado)
# -DskipTests: no compila ni ejecuta tests
# -B: batch mode (sin prompts interactivos)
# El JAR se genera en target/fleet-management-1.0.0-SNAPSHOT.jar
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests clean package

# Extraer capas Spring Boot para optimizar el caching de la imagen final
# (cada capa se copia solo si cambia, acelerando rebuilds)
RUN mkdir -p target/extracted && \
    java -Djarmode=layertools -jar target/fleet-management-*.jar extract --destination target/extracted

# ---------------------------------------------------------------------------
# Stage 2: Runtime — JRE ligero, solo lo necesario para ejecutar
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble AS runtime

# Metadatos OCI para trazabilidad
LABEL org.opencontainers.image.title="fleet-management-backend" \
      org.opencontainers.image.description="Sistema de Gestión del Parque de Vehículos — Spring Boot 3.5.5 / JDK 25" \
      org.opencontainers.image.source="https://github.com/keniercb/fleet-manager" \
      org.opencontainers.image.licenses="MIT"

# Usuario no-root por seguridad (evita que attackers que obtengan shell
# puedan escalar a root en el host)
RUN groupadd -r fleet && useradd -r -g fleet -d /app -s /sbin/nologin fleet

WORKDIR /app

# Copiar las capas extraídas en orden (de menor a mayor frecuencia de cambio):
# 1. dependencies (jaras de Maven, rara vez cambian)
# 2. spring-boot-loader (internals, casi nunca cambian)
# 3. snapshot-dependencies (SNAPSHOT deps, cambian a veces)
# 4. application (tu código, cambia en cada build)
COPY --from=builder /workspace/target/extracted/dependencies ./
COPY --from=builder /workspace/target/extracted/spring-boot-loader ./
COPY --from=builder /workspace/target/extracted/snapshot-dependencies ./
COPY --from=builder /workspace/target/extracted/application ./

# Cambiar ownership al usuario no-root
RUN chown -R fleet:fleet /app
USER fleet

# Puerto expuesto (server.port por defecto)
EXPOSE 8080

# Healthcheck: consulta el endpoint /api/auth/login con POST (espera 401)
# Si la app responde (incluso con 401), está viva y escuchando.
# --timeout 3s: si no responde en 3s, falla
# --start-period 40s: tiempo de gracia para arranque de Spring Boot
# --retries 3: 3 fallos consecutivos = unhealthy
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- --post-data='{"email":"x","password":"x"}' \
    --header='Content-Type: application/json' \
    http://localhost:8080/api/auth/login > /dev/null 2>&1 || exit 1

# Configuración JVM para contenedores (JDK 25):
# -XX:+UseContainerSupport: respeta limits de cgroup (default desde JDK 10)
# -XX:MaxRAMPercentage=75.0: usa 75% de la memoria del contenedor para heap
#   (deja 25% para metaspace, threads, off-heap)
# -XX:+UseZGC: ZGC es el GC por defecto recomendado en JDK 25 para
#   contenedores (mejor latencia que G1 en heaps grandes; JEP 457)
#   Alternativa: -XX:+UseG1GC si prefieres estabilidad probada
# -Djava.security.egd=file:/dev/./urandom: más rápido que /dev/random
#   (importante para Spring Security y JWT en contenedores)
# -Dspring.profiles.active=prod: perfil de producción obligatorio
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseZGC \
    -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod

# Punto de entrada: usa el launcher de Spring Boot (soporta layertools)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
