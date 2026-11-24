#!/usr/bin/env bash
# Genera el stack de Docker Swarm desde docker-compose.preprod.yml y lo despliega (ver scripts/swarm/README.md).
# Uso: scripts/swarm/desplegar.sh [--solo-generar]
#   ENV_FILE           .env a usar (por defecto, el de la raíz del repo)
#   DONATRACK_REPLICAS réplicas de los seis módulos propios (1 por defecto, 2 en la defensa)
#   DONATRACK_PUBLICAR_RABBITMQ=true publica el 5672 (pruebas desde el host)
#   DONATRACK_BORRAR_COLAS_SAC=true  lugar del paso que borra las colas con single active consumer;
#                                    hoy solo avisa y corta (el paso lo documenta SL2)
# El stack generado tiene los secretos del .env interpolados: queda en target/swarm/ (ignorado por git).
set -euo pipefail

raiz="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$raiz"
env_file="${ENV_FILE:-$raiz/.env}"
stack_nombre=donatrack
salida=target/swarm/stack.json

[ -f "$env_file" ] || { echo "Falta $env_file (copiá .env.example)" >&2; exit 1; }

# docker stack no lee el .env: se exporta para el override (DONATRACK_REPLICAS) y compose lo interpola.
set -a
# shellcheck disable=SC1090
. "$env_file"
set +a

mkdir -p target/swarm
# -p fija el nombre de la red y del volumen (donatrack_preprod-network, donatrack_n8n_data) sin
# depender de la carpeta del repo.
docker compose -p "$stack_nombre" --env-file "$env_file" -f docker-compose.preprod.yml config --format json \
  | node scripts/swarm/generar-stack.js > "$salida"
echo "Stack generado en $salida"

[ "${1:-}" = "--solo-generar" ] && exit 0

# Decisión D8: siete colas se redeclaran con x-single-active-consumer y el mismo nombre. RabbitMQ no
# deja redeclarar una cola existente con otros argumentos (PRECONDITION_FAILED): al desplegar esa
# versión sobre un stack que ya corre, hay que vaciarlas y borrarlas antes. En un stack nuevo no hace
# falta. El paso y la lista de colas los documenta SL2 en docs/arquitectura/eventos-amqp.md.
if [ "${DONATRACK_BORRAR_COLAS_SAC:-false}" = "true" ]; then
  echo "Borrar las colas con single active consumer: seguí docs/arquitectura/eventos-amqp.md (SL2)." >&2
  echo "Este script todavía no lo hace solo; desplegá de nuevo sin DONATRACK_BORRAR_COLAS_SAC." >&2
  exit 1
fi

# Las imágenes de infraestructura tienen que estar locales: sin ellas el deploy falló con
# "context deadline exceeded". Antes del primer despliegue:
#   docker compose -f docker-compose.preprod.yml pull --ignore-buildable
docker stack deploy --resolve-image never -c "$salida" -c docker-stack.override.yml "$stack_nombre"
