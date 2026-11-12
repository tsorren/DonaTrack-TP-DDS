# Etapa 1: el SPI de correo (SPEC-06 E2-06, D8). Se compila acá y no antes, para que `docker compose up --build`
# no pida Maven aparte. Sin `RUN --mount`: el contrato del realm construye esta imagen con Testcontainers, y el
# constructor clásico de Docker no lo entiende.
FROM maven:3.9.6-eclipse-temurin-21 AS spi
WORKDIR /spi
COPY spi/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY spi/src ./src
RUN mvn -B -q package -Dmaven.test.skip=true

# Etapa 2: Keycloak de DonaTrack (SPEC-06, ADR 20261104). La imagen se fija por digest del índice
# multi-arquitectura (amd64 y arm64) de quay.io/keycloak/keycloak:26.8.1.
# Contexto de build: esta carpeta (`context: ./auth-service` en docker-compose.yml).
FROM quay.io/keycloak/keycloak:26.8.1@sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332

# Opciones de build: se fijan acá para que `start --optimized` no vuelva a construir al arrancar.
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
# El correo de la cuenta sale por RabbitMQ y no por SMTP (SPEC-06 E2-06). Elegir el proveedor es una opción de
# BUILD: con `start --optimized` un valor puesto solo al arrancar no se usa y Keycloak se niega a iniciar. La
# conexión a RabbitMQ (host, usuario, clave) sí es de ejecución y la pasa el compose.
ENV KC_SPI_EMAIL_TEMPLATE__PROVIDER=donatrack-amqp

# El realm se importa al arrancar (--import-realm) y el tema se publica junto con la imagen.
COPY realm/donatrack-realm.json /opt/keycloak/data/import/donatrack-realm.json
COPY themes/donatrack /opt/keycloak/themes/donatrack
COPY --from=spi /spi/target/correo-spi.jar /opt/keycloak/providers/correo-spi.jar

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
