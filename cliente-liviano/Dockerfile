# ==============================================================================
# Etapa 1: Build de Maven (Para desarrollo local si no se tiene el JAR)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=cliente-liviano

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
# socat: solo lo usa docker-entrypoint.sh cuando KEYCLOAK_LOCAL_FORWARD_TO está definida (emisor único).
RUN apk add --no-cache curl socat && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
WORKDIR /app

ARG JAR_FILE
COPY --chown=appuser:appgroup ${JAR_FILE} app.jar
# El script se corre con `sh` y se le quitan los CR: un checkout de Windows con autocrlf no lo rompe.
COPY --chown=appuser:appgroup cliente-liviano/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh

USER 1001:1001
EXPOSE 8090
ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]


# ==============================================================================
# Etapa 3: Imagen final - Desarrollo local (Por defecto, compila desde builder)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS local
RUN apk add --no-cache curl socat && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=cliente-liviano

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder --chown=appuser:appgroup /app/${SERVICE_NAME}/target/*.jar app.jar
COPY --chown=appuser:appgroup cliente-liviano/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh

# /app queda root:root al crearse por WORKDIR; --chown solo cubre el jar copiado,
# no el directorio. Sin esto, el perfil de logging local no puede escribir en logs/.
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

USER 1001:1001
EXPOSE 8090
ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]
