# ==============================================================================
# Etapa 1: Build de Maven (Para desarrollo local si no se tiene el JAR)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=logistica-service

# Copiar los archivos de configuración de Maven
COPY . .

# Compilar el proyecto (sin tests)
# -pl: Solo tu servicio
# -am: También sus dependencias (common-lib)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -pl ${SERVICE_NAME} -am -DskipTests


# ==============================================================================
# Etapa 2: Imagen final - CI (Copia el JAR pre-compilado de GitHub Actions)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS ci
RUN apk add --no-cache curl && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
WORKDIR /app

ARG JAR_FILE
COPY --chown=appuser:appgroup ${JAR_FILE} app.jar

USER 1001:1001
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]


# ==============================================================================
# Etapa 3: Imagen final - Desarrollo local (Por defecto, compila desde builder)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS local
RUN apk add --no-cache curl && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=logistica-service

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder --chown=appuser:appgroup /app/${SERVICE_NAME}/target/*.jar app.jar

# /app queda root:root al crearse por WORKDIR; --chown solo cubre el jar copiado,
# no el directorio. Sin esto, el perfil de logging local no puede escribir en logs/.
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

USER 1001:1001
EXPOSE 8083
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
