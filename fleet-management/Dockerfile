# ============================================================================
# Dockerfile — Fleet Management Backend
# Multi-stage build con Eclipse Temurin JDK 25
# ============================================================================

# ---------------------------------------------------------------------------
# Stage 1: Builder — compila con Maven
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-noble AS builder

WORKDIR /workspace

# Instalar Maven (la imagen JDK de Temurin no lo incluye)
RUN apt-get update && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

# Copiar pom.xml y descargar dependencias (cache de capas)
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline || true

# Copiar código fuente y compilar
COPY src ./src
RUN mvn -B -DskipTests clean package

# ---------------------------------------------------------------------------
# Stage 2: Runtime — JRE ligero
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-noble

LABEL org.opencontainers.image.title="fleet-management-backend" \
      org.opencontainers.image.description="Sistema de Gestión del Parque de Vehículos — Spring Boot 3.5.5 / JDK 25" \
      org.opencontainers.image.source="https://github.com/keniercb/fleet-manager"

# Usuario no-root
RUN groupadd -r fleet && useradd -r -g fleet -d /app -s /sbin/nologin fleet

WORKDIR /app

# Copiar el JAR compilado
COPY --from=builder /workspace/target/fleet-management-*.jar app.jar
RUN chown fleet:fleet app.jar

USER fleet
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- --post-data='{"email":"x","password":"x"}' \
    --header='Content-Type: application/json' \
    http://localhost:8080/api/auth/login > /dev/null 2>&1 || exit 1

# JVM optimizada para contenedores + perfil prod
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom" \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
