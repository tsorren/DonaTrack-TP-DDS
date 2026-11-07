# Keycloak de DonaTrack (SPEC-06, ADR 20261104). La imagen se fija por digest del índice
# multi-arquitectura (amd64 y arm64) de quay.io/keycloak/keycloak:26.8.1.
# Contexto de build: esta carpeta (`context: ./auth-service` en docker-compose.yml).
FROM quay.io/keycloak/keycloak:26.8.1@sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332

# Opciones de build: se fijan acá para que `start --optimized` no vuelva a construir al arrancar.
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

# El realm se importa al arrancar (--import-realm) y el tema se publica junto con la imagen.
COPY realm/donatrack-realm.json /opt/keycloak/data/import/donatrack-realm.json
COPY themes/donatrack /opt/keycloak/themes/donatrack

RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized", "--import-realm"]
