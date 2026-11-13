"""Configuración del seeder: todo sale de variables de entorno, sin claves en el código."""

from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone

# Argentina no tiene horario de verano: un desfase fijo alcanza y no depende de tzdata
BUENOS_AIRES = timezone(timedelta(hours=-3))


class ConfiguracionInvalida(Exception):
    """Falta una variable obligatoria o tiene un valor que no sirve."""


def _obligatoria(nombre: str) -> str:
    valor = os.environ.get(nombre, "").strip()
    if not valor:
        raise ConfiguracionInvalida(f"Falta la variable {nombre} (ver .env.example)")
    return valor


def _opcional(nombre: str, por_defecto: str) -> str:
    return os.environ.get(nombre, "").strip() or por_defecto


@dataclass(frozen=True)
class Config:
    donaciones_url: str
    incentivos_url: str
    notificaciones_url: str
    logistica_url: str
    keycloak_url: str
    realm: str
    kc_admin_usuario: str
    kc_admin_clave: str
    admin_correo: str
    admin_clave: str
    clave_demo: str
    postgres_host: str
    postgres_usuario: str
    postgres_clave: str
    mongo_uri: str
    minio_endpoint: str
    minio_usuario: str
    minio_clave: str
    fotos_url_base: str
    semilla: int
    hoy: date
    ahora: datetime

    @staticmethod
    def desde_entorno() -> "Config":
        clave_demo = _obligatoria("DEMO_PASSWORD")
        if len(clave_demo) < 10 or "@" in clave_demo:
            # La política del realm: largo 10 como mínimo y que no sea un correo
            raise ConfiguracionInvalida(
                "DEMO_PASSWORD tiene que tener 10 caracteres o más y no ser un correo"
            )
        mongo_usuario = _obligatoria("LOGISTICA_MONGO_USER")
        mongo_clave = _obligatoria("LOGISTICA_MONGO_PASSWORD")
        mongo_host = _opcional("SEEDER_MONGO_HOST", "mongodb:27017")
        ahora = datetime.now(timezone.utc).replace(microsecond=0)
        return Config(
            donaciones_url=_opcional("SEEDER_DONACIONES_URL", "http://donaciones-service:8080"),
            incentivos_url=_opcional("SEEDER_INCENTIVOS_URL", "http://incentivos-service:8082"),
            notificaciones_url=_opcional(
                "SEEDER_NOTIFICACIONES_URL", "http://notificaciones-service:8081"
            ),
            logistica_url=_opcional("SEEDER_LOGISTICA_URL", "http://logistica-service:8083"),
            keycloak_url=_opcional("SEEDER_KEYCLOAK_URL", "http://keycloak:8080"),
            realm=_opcional("SEEDER_REALM", "donatrack"),
            kc_admin_usuario=_obligatoria("KC_BOOTSTRAP_ADMIN_USERNAME"),
            kc_admin_clave=_obligatoria("KC_BOOTSTRAP_ADMIN_PASSWORD"),
            admin_correo=_obligatoria("AUTH_ADMIN_EMAIL"),
            admin_clave=_obligatoria("AUTH_ADMIN_PASSWORD"),
            clave_demo=clave_demo,
            postgres_host=_opcional("SEEDER_POSTGRES_HOST", "postgres"),
            postgres_usuario=_opcional("SEEDER_POSTGRES_USER", "admin"),
            postgres_clave=_obligatoria("POSTGRES_PASSWORD"),
            mongo_uri=(
                f"mongodb://{mongo_usuario}:{mongo_clave}@{mongo_host}/logistica"
                "?authSource=logistica&directConnection=true"
            ),
            minio_endpoint=_opcional("SEEDER_MINIO_ENDPOINT", "minio:9000"),
            minio_usuario=_opcional("SEEDER_MINIO_USER", "logistica-service"),
            minio_clave=_obligatoria("LOGISTICA_MINIO_PASSWORD"),
            fotos_url_base=_opcional("SEEDER_FOTOS_URL_BASE", "http://localhost:8086"),
            semilla=int(_opcional("SEEDER_SEMILLA", "2026")),
            hoy=ahora.astimezone(BUENOS_AIRES).date(),
            ahora=ahora,
        )
