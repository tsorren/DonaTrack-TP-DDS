# ==============================================================================
# Etapa 1: Build de Maven (Para desarrollo local si no se tiene el JAR)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=incentivos-service

# Copiar los archivos de configuración de Maven
COPY . .

# Compilar el proyecto (sin tests)
# -pl: Solo tu servicio
# -am: También sus dependencias (common-lib)
RUN mvn clean package -pl ${SERVICE_NAME} -am -DskipTests


# ==============================================================================
# Etapa 2: Imagen final - CI (Copia el JAR pre-compilado de GitHub Actions)
# ==============================================================================
FROM eclipse-temurin:21-jre AS ci
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


# ==============================================================================
# Etapa 3: Imagen final - Desarrollo local (Por defecto, compila desde builder)
# ==============================================================================
FROM eclipse-temurin:21-jre AS local
WORKDIR /app

# nombre del directorio físico del servicio,
ARG SERVICE_NAME=incentivos-service

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /app/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]