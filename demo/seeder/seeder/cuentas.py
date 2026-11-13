"""Catálogo y cuentas por API, con el mismo recorrido que el registro del sitio.

Así se publican persona.sincronizada y donante.registrado, e Incentivos y Notificaciones crean sus
copias de cada persona (y las seis misiones de cada donante).
"""

from __future__ import annotations

import logging

from . import catalogo
from .api import Api
from .guion import Donante, Entidad, Mundo
from .keycloak import Keycloak

log = logging.getLogger("seeder")

LEGALES_VERSION = 1


def ampliar_catalogo(donaciones: Api) -> dict[str, str]:
    """Suma las categorías que faltan y devuelve el id de cada subcategoría por nombre."""
    existentes = {c["nombre"] for c in donaciones.get("/api/categorias")}
    for categoria in catalogo.CATALOGO:
        if categoria.nombre in existentes:
            continue
        creada = donaciones.post("/api/categorias", {
            "nombre": categoria.nombre, "conUso": categoria.con_uso,
            "conVencimiento": categoria.con_vencimiento, "unidad": categoria.unidad,
        })
        for sub in categoria.subcategorias:
            donaciones.post("/api/subcategorias", {
                "nombre": sub.nombre, "idCategoria": creada["id"], "unidad": categoria.unidad,
                "aliases": [{"alias": a} for a in sub.alias],
            })
        log.info("Categoría %s con %d subcategorías", categoria.nombre,
                 len(categoria.subcategorias))
    ids = {s["nombre"]: s["id"] for s in donaciones.get("/api/subcategorias")}
    faltantes = set(catalogo.todas_las_subcategorias()) - set(ids)
    if faltantes:
        raise RuntimeError(f"El catálogo no tiene estas subcategorías: {sorted(faltantes)}")
    return ids


def _medios(correo: str, telefono: str, whatsapp: bool) -> list[dict]:
    return [
        {"tipo": "CORREO", "esPredeterminado": True, "direccionCorreo": correo},
        {"tipo": "WHATSAPP" if whatsapp else "TELEFONO", "esPredeterminado": False,
         "caracteristica": "54", "codigoArea": "11", "numero": telefono[2:]},
    ]


def _humana(nombre: str, apellido: str, correo: str, telefono: str, documento: str | None,
            direccion: dict | None, genero: str = "PREFIERO_NO_DECIR", nacimiento=None) -> dict:
    return {
        "tipo": "HUMANA", "nombre": nombre, "apellido": apellido, "genero": genero,
        "fechaNacimiento": nacimiento.isoformat() if nacimiento else None,
        "tipoDocumento": "DNI" if documento else None, "documento": documento,
        "direccion": direccion, "mediosDeContacto": _medios(correo, telefono, True),
    }


def _persona_de_donante(d: Donante) -> dict:
    if d.tipo == "HUMANA":
        return _humana(d.nombre, d.apellido, d.correo, d.telefono, d.documento,
                       d.direccion.dto(), d.genero, d.nacimiento)
    nombre, apellido = d.contacto
    return {
        "tipo": "JURIDICA", "razonSocial": d.nombre, "tipoJuridico": "EMPRESA",
        "rubro": d.rubro, "tipoDocumento": "CUIT", "documento": d.documento,
        "direccion": d.direccion.dto(), "mediosDeContacto": _medios(d.correo, d.telefono, False),
        "representantes": [_humana(nombre, apellido, d.correo, d.telefono, None, None)],
    }


def _persona_de_entidad(e: Entidad) -> dict:
    nombre, apellido = e.representante
    return {
        "tipo": "JURIDICA", "razonSocial": e.razon_social, "tipoJuridico": e.tipo_juridico,
        "rubro": e.rubro, "tipoDocumento": "CUIT", "documento": e.documento,
        "direccion": e.direccion.dto(), "mediosDeContacto": _medios(e.correo, e.telefono, True),
        "representantes": [_humana(nombre, apellido, e.correo, e.telefono, None, None)],
    }


TAMANIO_PAGINA = 100  # el máximo de los listados de Donaciones (R11-01)


def ya_sembrado(donaciones: Api, mundo: Mundo) -> bool:
    """Busca el correo del primer donante recorriendo /api/personas de a una página (R11-01:
    ningún listado devuelve la lista completa). Corta en cuanto lo encuentra."""
    correo = mundo.donantes[0].correo
    pagina = 0
    while True:
        personas = donaciones.get(
            "/api/personas", params={"page": pagina, "size": TAMANIO_PAGINA}) or []
        if any(correo in str(p) for p in personas):
            return True
        if len(personas) < TAMANIO_PAGINA:
            return False
        pagina += 1


def crear_cuentas(mundo: Mundo, donaciones: Api, keycloak: Keycloak, clave: str) -> None:
    for d in mundo.donantes:
        persona = donaciones.post("/api/personas", _persona_de_donante(d))
        d.persona_id = persona["id"]
        donaciones.post("/api/donantes", {"idPersona": d.persona_id})
        donaciones.put(f"/api/personas/{d.persona_id}/legales", {"version": LEGALES_VERSION})
        if not d.mostrar_nombre:
            donaciones.patch(f"/api/donantes/{d.persona_id}/preferencias",
                             {"mostrarNombre": False})
        nombre, apellido = (d.nombre, d.apellido) if d.tipo == "HUMANA" else d.contacto
        keycloak.crear_usuario(
            d.correo, nombre, apellido, clave, {"personaId": d.persona_id},
            plantilla="DONANTE_PERSONA" if d.tipo == "HUMANA" else "RESPONSABLE_DONANTE")
    log.info("%d donantes con cuenta", len(mundo.donantes))

    for e in mundo.entidades:
        persona = donaciones.post("/api/personas", _persona_de_entidad(e))
        e.persona_id = persona["id"]
        donaciones.put(f"/api/personas/{e.persona_id}/legales", {"version": LEGALES_VERSION})
        nombre, apellido = e.representante
        if e.estado == "pendiente":
            # Como en el sitio: la cuenta espera la aprobación del equipo, sin rol ni permisos
            keycloak.crear_usuario(e.correo, nombre, apellido, clave,
                                   {"personaId": e.persona_id, "solicitudEntidad": "pendiente"})
            continue
        donaciones.post("/api/entidades", {"juridicaId": e.persona_id})
        keycloak.crear_usuario(e.correo, nombre, apellido, clave, {"personaId": e.persona_id},
                               rol_realm="ENTIDAD", plantilla="RESPONSABLE_ENTIDAD")
    log.info("%d entidades con cuenta", len(mundo.entidades))
