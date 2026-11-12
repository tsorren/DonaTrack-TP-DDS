"""Seeder de demo de DonaTrack (SPEC-10).

    python -m seeder todo    carga seis meses de historia y arma lo que pasa hoy (base vacía)
    python -m seeder vivo    vuelve a armar solo la ruta de hoy, justo antes de presentar
"""

from __future__ import annotations

import logging
import sys
import time

import psycopg

from . import cuentas, historia, vivo
from .api import Api, esperar_salud
from .config import Config
from .guion import Mundo, construir_mundo
from .keycloak import Keycloak

log = logging.getLogger("seeder")


def _esperar_servicios(config: Config) -> None:
    for nombre, url in [("Keycloak", f"{config.keycloak_url}/realms/{config.realm}"),
                        ("Donaciones", f"{config.donaciones_url}/actuator/health"),
                        ("Incentivos", f"{config.incentivos_url}/actuator/health"),
                        ("Notificaciones", f"{config.notificaciones_url}/actuator/health"),
                        ("Logística", f"{config.logistica_url}/actuator/health")]:
        esperar_salud(nombre, url)
        log.info("%s listo", nombre)


def _esperar_copias(config: Config, mundo: Mundo) -> None:
    """Incentivos y Notificaciones crean sus copias de cada persona al recibir los eventos."""
    donantes = [d.persona_id for d in mundo.donantes]
    personas = donantes + [e.persona_id for e in mundo.entidades]
    for base, consulta, ids in [
        ("incentivos", "SELECT count(*) FROM donante_incentivos WHERE id = ANY(%s::uuid[])", donantes),
        ("notificaciones", "SELECT count(*) FROM persona WHERE id = ANY(%s::uuid[])", personas),
    ]:
        fin = time.monotonic() + 120
        while True:
            with historia.conectar(config, base) as conexion:
                cantidad = conexion.execute(consulta, (ids,)).fetchone()[0]
            if cantidad == len(ids):
                break
            if time.monotonic() > fin:
                raise TimeoutError(f"{base} tiene {cantidad} de {len(ids)} personas")
            time.sleep(2)
    log.info("Incentivos y Notificaciones tienen a las %d personas", len(personas))


def _recuperar_ids(config: Config, mundo: Mundo) -> None:
    """Para `vivo`: el id de cada persona, buscado por su correo en Donaciones."""
    with historia.conectar(config, "donaciones") as conexion:
        for persona in [*mundo.donantes, *mundo.entidades]:
            # El representante comparte el correo de su jurídica: se busca la persona del tipo
            juridica = getattr(persona, "tipo", "JURIDICA") == "JURIDICA"
            fila = conexion.execute(
                """SELECT p.id FROM correo c JOIN medio_de_contacto m ON m.id = c.id
                   JOIN persona p ON p.id = m.id_persona
                   LEFT JOIN humana h ON h.id = p.id
                   WHERE c.direccion_correo = %s AND p.tipo_persona = %s
                     AND h.representante_de IS NULL LIMIT 1""",
                (persona.correo, "JURIDICA" if juridica else "HUMANA")).fetchone()
            if not fila:
                raise RuntimeError(f"No está {persona.correo}: primero corré `seeder todo`")
            persona.persona_id = str(fila[0])


def _tabla_de_cuentas(config: Config, mundo: Mundo, keycloak: Keycloak) -> None:
    filas = [("Administración", config.admin_correo, "(la de AUTH_ADMIN_PASSWORD)")]
    filas += [(f"Donante · {d.perfil}", d.correo, "DEMO_PASSWORD")
              for d in mundo.donantes if d.protagonista]
    filas += [(f"Entidad · {e.estado}", e.correo, "DEMO_PASSWORD")
              for e in mundo.entidades if e.protagonista]
    for _, correo, clave in filas[1:]:
        keycloak.token_de(correo, config.clave_demo)  # falla acá si la cuenta no entra
    ancho = max(len(c) for _, c, _ in filas)
    print("\nCuentas para la demo (todas entran con usuario y clave en http://localhost:3000):")
    for rol, correo, clave in filas:
        print(f"  {rol:<24} {correo:<{ancho}}  {clave}")
    print()


def todo(config: Config) -> None:
    _esperar_servicios(config)
    keycloak = Keycloak(config.keycloak_url, config.realm, config.kc_admin_usuario,
                        config.kc_admin_clave)
    keycloak.preparar_cliente_seeder()
    try:
        mundo = construir_mundo(config.hoy, config.semilla)
        admin = Api(config.donaciones_url,
                    lambda: keycloak.token_de(config.admin_correo, config.admin_clave))
        if cuentas.ya_sembrado(admin, mundo):
            log.error("La base ya tiene la demo. Para empezar de cero: docker compose down -v "
                      "y volver a levantar. Para rearmar solo la ruta de hoy: `seeder vivo`.")
            sys.exit(2)
        historia.validar_columnas(config)
        subcategorias = cuentas.ampliar_catalogo(admin)
        cuentas.crear_cuentas(mundo, admin, keycloak, config.clave_demo)
        _esperar_copias(config, mundo)
        logistica = Api(config.logistica_url, admin.token)
        camiones, choferes = historia.crear_flota(logistica, mundo)
        log.info("Fotos de recepción: %d subidas", historia.subir_fotos(config, mundo))
        historia.escribir_donaciones(config, mundo, subcategorias)
        historia.escribir_logistica(config, mundo, camiones, choferes)
        historia.escribir_incentivos(config, mundo)
        historia.escribir_notificaciones(config, mundo, config.ahora)
        vivo.correr(config, mundo, keycloak)
        _tabla_de_cuentas(config, mundo, keycloak)
    finally:
        keycloak.borrar_cliente_seeder()


def solo_vivo(config: Config) -> None:
    _esperar_servicios(config)
    keycloak = Keycloak(config.keycloak_url, config.realm, config.kc_admin_usuario,
                        config.kc_admin_clave)
    keycloak.preparar_cliente_seeder()
    try:
        mundo = construir_mundo(config.hoy, config.semilla)
        _recuperar_ids(config, mundo)
        vivo.EnVivo(config, mundo, keycloak).ruta_de_hoy()
    finally:
        keycloak.borrar_cliente_seeder()


def main(argumentos: list[str]) -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s", datefmt="%H:%M:%S")
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    comando = argumentos[0] if argumentos else "todo"
    acciones = {"todo": todo, "vivo": solo_vivo}
    if comando not in acciones:
        print(__doc__)
        return 1
    try:
        acciones[comando](Config.desde_entorno())
    except (psycopg.Error, RuntimeError, TimeoutError) as error:
        log.error("El seeder se detuvo: %s", error)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
