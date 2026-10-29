# [PROPOSED] Reconstruido desde la descripción del prompt (§3). Reemplazar por copia de
# notificaciones-service/Dockerfile cambiando SERVICE_NAME y el puerto.
ARG SERVICE_NAME=auth-service

FROM maven:3-eclipse-temurin-21 AS builder
ARG SERVICE_NAME
WORKDIR /build
COPY . .
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime-base
RUN apk add --no-cache curl && adduser -D -u 1001 app
WORKDIR /app

FROM runtime-base AS ci
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
USER 1001
EXPOSE 8084
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]

FROM runtime-base AS local
ARG SERVICE_NAME
COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar
USER 1001
EXPOSE 8084
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
