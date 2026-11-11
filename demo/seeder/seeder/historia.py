"""La historia de seis meses, escrita directo en las bases (SPEC-10, sección 4).

Cada escritor respeta las reglas de su servicio, tal como las dejan sus propias escrituras:
- Donaciones: horas UTC sin zona; historial de estados encadenado; solicitud_entrega en hora de
  Buenos Aires (así la guarda el servicio).
- Notificaciones: fecha_creacion en UTC y el historial de estados en hora de Buenos Aires.
- Incentivos: listas ordenadas contiguas desde 0; fechas sin hora.
- Logística: UUID binarios estándar, GeoJSON [lon, lat], version 0 y el _class de Spring Data.
"""

from __future__ import annotations

import io
import logging
import random
import uuid
from datetime import date, datetime, timedelta, timezone

import psycopg
from minio import Minio
from bson import Int64
from pymongo import MongoClient

from . import catalogo
from .config import Config
from .guion import Donacion, Independiente, Mundo, detalle_de, ranking_de
from .imagenes import foto_de_recepcion

log = logging.getLogger("seeder")

ID_ADMIN = "00000000-0000-0000-0000-000000000001"
BUCKET_FOTOS = "logistica-fotos-recepcion"
URL_SEGUIMIENTO = "http://localhost:3000/seguimiento/"
_ACTORES = {"admin": "SISTEMA", "sistema": "SISTEMA"}
_DOCUMENTOS = "grupo5.logistica.infrastructure.persistencia.mongo.documents."
BA = timedelta(hours=3)


def _uuid(valor: str) -> uuid.UUID:
    return uuid.UUID(valor)


def _ba(utc: datetime) -> datetime:
    return utc - BA


class Ids:
    """Ids derivados de otro id: estables entre corridas y sin chocar entre tablas."""

    @staticmethod
    def de(*partes: str) -> str:
        return str(uuid.uuid5(uuid.NAMESPACE_URL, "donatrack-demo:" + ":".join(partes)))


def conectar(config: Config, base: str) -> psycopg.Connection:
    return psycopg.connect(host=config.postgres_host, user=config.postgres_usuario,
                           password=config.postgres_clave, dbname=base,
                           options=f"-c search_path={base}")


# Columnas que escribe el seeder, por base: si una migración las cambia, se frena antes de escribir
COLUMNAS = {
    "donaciones": {
        "direccion": "id calle altura piso departamento codigo_postal localidad provincia pais",
        "deposito": "id id_direccion nombre",
        "necesidad": "id tipo_necesidad subcategoria_id entidad_beneficiaria_id cantidad_necesitada "
                     "descripcion fecha_inicio activa periodo version fecha_limite",
        "periodo_necesidad": "id necesidad_id fecha_inicio fecha_fin cantidad_objetivo",
        "donacion": "id id_donante descripcion fecha estado_donacion id_deposito_donde_esta "
                    "nombre_oculto",
        "cambio_estado_donacion": "id id_donacion estado_anterior estado_nuevo timestamp",
        "item_donacion": "id id_donacion descripcion fecha_vencimiento estado peso_unitario "
                         "volumen_unitario cantidad",
        "item_donacion_normalizado": "id id_donacion cantidad segmentado bien_descripcion "
                                     "fecha_vencimiento estado peso_unitario volumen_unitario "
                                     "id_subcategoria confianza_normalizacion estado_normalizacion",
        "donacion_independiente": "id id_donacion estado_actual id_necesidad fecha_registro "
                                  "id_beneficiario_asignado id_periodo version cantidad_no_recibida "
                                  "fecha_entrega cancelacion_pedida",
        "item_donacion_independiente": "id id_donacion_independiente id_item_normalizado cantidad "
                                       "bien_descripcion orden",
        "estado_donacion_independiente": "id id_donacion_independiente tipo patente_camion",
        "cambio_estado_donacion_independiente": "id id_donacion_independiente id_estado_anterior "
                                                "id_estado_nuevo timestamp actor justificacion",
        "foto_recepcion_donacion_independiente": "id_donacion_independiente orden url",
        "propuesta": "id id_necesidad_que_satisface estado fecha_creacion",
        "posible_fragmentacion": "id id_propuesta id_donacion_independiente cantidad",
        "solicitud_entrega": "id id_donacion_independiente proveedor_actual estado ronda "
                             "fecha_creacion",
        "aceptacion_legales": "persona_id aceptada_en",
    },
    "incentivos": {
        "donante_incentivos": "id categoria fecha_registro total_donaciones_historicas "
                              "total_donaciones_exitosas ultima_donacion",
        "mision": "id donante_id numero_mision progreso_actual completada fecha_completada "
                  "ultimo_mes_donado fecha_ultimo_donacion insignia_nombre insignia_descripcion "
                  "insignia_imagen_url",
        "mision_categorias_donadas": "mision_id categoria",
        "donante_historial_categoria": "donante_id orden categoria_anterior categoria_nueva fecha",
        "donante_insignia_ganada": "donante_id orden nombre descripcion imagen_url visible "
                                   "fecha_obtenida",
        "donante_organizacion_ayudada": "donante_id organizacion_id",
        "donante_donaciones_por_periodo": "donante_id periodo cantidad",
        "ranking_mensual": "id periodo calculado_en",
        "ranking_mensual_entrada": "ranking_id posicion donante_id nombre_donante "
                                   "misiones_completadas",
    },
    "notificaciones": {
        "notificacion": "id persona_id mensaje fecha_creacion estado_notificacion leida "
                        "fecha_lectura tipo_evento referencia_id",
        "notificacion_historial_estado": "notificacion_id estado_anterior estado_nuevo timestamp",
        "mensaje_contacto": "clave_envio tema texto correo ip_origen estado recibido_en respuesta "
                            "respondido_en",
    },
}


def validar_columnas(config: Config) -> None:
    faltan = []
    for base, tablas in COLUMNAS.items():
        with conectar(config, base) as conexion:
            existentes = {(t, c) for t, c in conexion.execute(
                """SELECT table_name, column_name FROM information_schema.columns
                   WHERE table_schema = %s""", (base,))}
        faltan += [f"{base}.{tabla}.{columna}" for tabla, columnas in tablas.items()
                   for columna in columnas.split() if (tabla, columna) not in existentes]
    if faltan:
        raise RuntimeError("El esquema cambió; el seeder no conoce: " + ", ".join(faltan))


# --- Fotos -------------------------------------------------------------------------------


def urls_de_fotos(config: Config, ind: Independiente) -> list[tuple[str, str]]:
    """(clave en MinIO, URL estable) de cada foto de recepción de la entrega."""
    resultado = []
    for n in range(ind.fotos):
        foto = Ids.de("foto", ind.id, str(n))
        clave = f"entregas/{ind.id_entrega}/{foto}.jpg"
        resultado.append((clave, f"{config.fotos_url_base}/api/entregas/{ind.id_entrega}/fotos/{foto}"))
    return resultado


def subir_fotos(config: Config, mundo: Mundo) -> int:
    cliente = Minio(config.minio_endpoint, access_key=config.minio_usuario,
                    secret_key=config.minio_clave, secure=False)
    if not cliente.bucket_exists(BUCKET_FOTOS):
        cliente.make_bucket(BUCKET_FOTOS)
    subidas = 0
    for _, ind in mundo.independientes():
        if ind.estado_actual != "ENTREGADA":
            continue
        etiqueta = ind.subcategoria
        for n, (clave, _) in enumerate(urls_de_fotos(config, ind)):
            leyenda = _ba(ind.fecha_entrega).strftime("%d/%m/%Y %H:%M")
            contenido = foto_de_recepcion(hash((ind.id, n)) & 0xFFFFFFFF, etiqueta, leyenda)
            cliente.put_object(BUCKET_FOTOS, clave, io.BytesIO(contenido), len(contenido),
                               content_type="image/jpeg")
            subidas += 1
    return subidas


# --- Donaciones --------------------------------------------------------------------------


def _deposito(cur, nombre: str, direccion) -> str:
    d = direccion.dto()
    cur.execute(
        """SELECT id FROM direccion WHERE calle=%s AND altura=%s AND piso IS NOT DISTINCT FROM %s
           AND departamento IS NOT DISTINCT FROM %s AND codigo_postal=%s AND localidad=%s
           AND provincia=%s AND pais=%s""",
        (d["calle"], d["altura"], d["piso"], d["departamento"], d["codigoPostal"],
         d["localidad"], d["provincia"], d["pais"]))
    fila = cur.fetchone()
    id_direccion = str(fila[0]) if fila else Ids.de("direccion", nombre)
    if not fila:
        cur.execute(
            """INSERT INTO direccion (id, calle, altura, piso, departamento, codigo_postal,
               localidad, provincia, pais) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
            (id_direccion, d["calle"], d["altura"], d["piso"], d["departamento"],
             d["codigoPostal"], d["localidad"], d["provincia"], d["pais"]))
    cur.execute("SELECT id FROM deposito WHERE nombre=%s AND id_direccion=%s",
                (nombre, id_direccion))
    fila = cur.fetchone()
    if fila:
        return str(fila[0])
    id_deposito = Ids.de("deposito", nombre)
    cur.execute("INSERT INTO deposito (id, id_direccion, nombre) VALUES (%s,%s,%s)",
                (id_deposito, id_direccion, nombre))
    return id_deposito


def escribir_donaciones(config: Config, mundo: Mundo, subcategorias: dict[str, str]) -> None:
    personas = {d.clave: d.persona_id for d in mundo.donantes}
    entidades = {e.clave: e.persona_id for e in mundo.entidades}
    with conectar(config, "donaciones") as conexion, conexion.cursor() as cur:
        depositos = {nombre: _deposito(cur, nombre, direccion)
                     for nombre, direccion in mundo.depositos.items()}
        _necesidades(cur, mundo, subcategorias, entidades)
        for donacion in mundo.donaciones:
            _donacion(cur, config, donacion, personas, entidades, depositos, subcategorias)
        _fechas_de_alta(cur, mundo)
    log.info("Donaciones: %d donaciones y %d necesidades con su historia",
             len(mundo.donaciones), len(mundo.necesidades))


def _necesidades(cur, mundo: Mundo, subcategorias, entidades) -> None:
    periodos_iso = {"SEMANAL": "P7D", "QUINCENAL": "P15D", "MENSUAL": "P1M"}
    for n in mundo.necesidades:
        cur.execute(
            """INSERT INTO necesidad (id, tipo_necesidad, subcategoria_id, entidad_beneficiaria_id,
               cantidad_necesitada, descripcion, fecha_inicio, activa, periodo, version,
               fecha_limite) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,0,%s)""",
            (n.id, n.tipo, subcategorias[n.subcategoria], entidades[n.entidad], n.cantidad,
             n.descripcion, n.inicio, n.activa, periodos_iso.get(n.periodicidad),
             n.fecha_limite))
        for p in n.periodos:
            cur.execute(
                """INSERT INTO periodo_necesidad (id, necesidad_id, fecha_inicio, fecha_fin,
                   cantidad_objetivo) VALUES (%s,%s,%s,%s,%s)""",
                (p.id, n.id, p.inicio, p.fin, p.objetivo))


def _donacion(cur, config, donacion: Donacion, personas, entidades, depositos,
              subcategorias) -> None:
    cur.execute(
        """INSERT INTO donacion (id, id_donante, descripcion, fecha, estado_donacion,
           id_deposito_donde_esta, nombre_oculto) VALUES (%s,%s,%s,%s,'SEGMENTADA',%s,%s)""",
        (donacion.id, personas[donacion.donante], donacion.descripcion, donacion.fecha,
         depositos[donacion.deposito], donacion.nombre_oculto))
    for n, (anterior, nuevo) in enumerate([("CARGADA", "NORMALIZADA"),
                                           ("NORMALIZADA", "SEGMENTADA")]):
        cur.execute(
            """INSERT INTO cambio_estado_donacion (id, id_donacion, estado_anterior, estado_nuevo,
               "timestamp") VALUES (%s,%s,%s,%s,%s)""",
            (Ids.de("cambio", donacion.id, str(n)), donacion.id, anterior, nuevo,
             donacion.fecha + timedelta(seconds=1 + n)))
    for ind in donacion.independientes:
        _independiente(cur, config, donacion, ind, entidades, subcategorias)


def _independiente(cur, config, donacion, ind: Independiente, entidades, subcategorias) -> None:
    peso = round(ind.peso_kg, 3)
    volumen = max(0.001, round(ind.volumen_m3, 3))
    cur.execute(
        """INSERT INTO item_donacion (id, id_donacion, descripcion, fecha_vencimiento, estado,
           peso_unitario, volumen_unitario, cantidad) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)""",
        (Ids.de("item", ind.id), donacion.id, ind.descripcion, ind.vencimiento, ind.estado_bien,
         peso, volumen, ind.cantidad))
    cur.execute(
        """INSERT INTO item_donacion_normalizado (id, id_donacion, cantidad, segmentado,
           bien_descripcion, fecha_vencimiento, estado, peso_unitario, volumen_unitario,
           id_subcategoria, confianza_normalizacion, estado_normalizacion)
           VALUES (%s,%s,%s,true,%s,%s,%s,%s,%s,%s,1.0,'ACEPTADO')""",
        (ind.id_item_normalizado, donacion.id, ind.cantidad, ind.descripcion, ind.vencimiento,
         ind.estado_bien, peso, volumen, subcategorias[ind.subcategoria]))
    cur.execute(
        """INSERT INTO donacion_independiente (id, id_donacion, estado_actual, id_necesidad,
           fecha_registro, id_beneficiario_asignado, id_periodo, version, cantidad_no_recibida,
           fecha_entrega, cancelacion_pedida) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,false)""",
        (ind.id, donacion.id, ind.estado_actual, ind.necesidad, ind.pasos[0].momento,
         entidades.get(ind.entidad), ind.periodo, len(ind.pasos) - 1, ind.cantidad_no_recibida,
         ind.fecha_entrega))
    cur.execute(
        """INSERT INTO item_donacion_independiente (id, id_donacion_independiente,
           id_item_normalizado, cantidad, bien_descripcion, orden) VALUES (%s,%s,%s,%s,%s,0)""",
        (Ids.de("item-ind", ind.id), ind.id, ind.id_item_normalizado, ind.cantidad,
         ind.descripcion))
    anterior = None
    for n, paso in enumerate(ind.pasos):
        id_estado = Ids.de("estado", ind.id, str(n))
        cur.execute(
            """INSERT INTO estado_donacion_independiente (id, id_donacion_independiente, tipo,
               patente_camion) VALUES (%s,%s,%s,%s)""",
            (id_estado, ind.id, paso.estado, paso.patente))
        if anterior:
            cur.execute(
                """INSERT INTO cambio_estado_donacion_independiente (id, id_donacion_independiente,
                   id_estado_anterior, id_estado_nuevo, "timestamp", actor, justificacion)
                   VALUES (%s,%s,%s,%s,%s,%s,%s)""",
                (Ids.de("cambio-ind", ind.id, str(n)), ind.id, anterior, id_estado,
                 paso.momento, _ACTORES.get(paso.actor, paso.actor), paso.justificacion))
        anterior = id_estado
    for orden, (_, url) in enumerate(urls_de_fotos(config, ind)
                                     if ind.estado_actual == "ENTREGADA" else []):
        cur.execute(
            """INSERT INTO foto_recepcion_donacion_independiente (id_donacion_independiente,
               orden, url) VALUES (%s,%s,%s)""", (ind.id, orden, url))
    if ind.propuesta:
        asignada = ind.paso("ASIGNACION_REALIZADA").momento
        cur.execute(
            """INSERT INTO propuesta (id, id_necesidad_que_satisface, estado, fecha_creacion)
               VALUES (%s,%s,'APROBADA',%s)""",
            (ind.propuesta, ind.necesidad, asignada - timedelta(minutes=12)))
        cur.execute(
            """INSERT INTO posible_fragmentacion (id, id_propuesta, id_donacion_independiente,
               cantidad) VALUES (%s,%s,%s,%s)""",
            (Ids.de("fragmentacion", ind.id), ind.propuesta, ind.id, ind.cantidad))
        cur.execute(
            """INSERT INTO solicitud_entrega (id, id_donacion_independiente, proveedor_actual,
               estado, ronda, fecha_creacion) VALUES (%s,%s,'donatrack','ENVIADA',1,%s)""",
            (Ids.de("solicitud", ind.id), ind.id, _ba(asignada)))


def _fechas_de_alta(cur, mundo: Mundo) -> None:
    """La aceptación de los legales, en el día del alta de cada uno."""
    for persona in [*mundo.donantes, *mundo.entidades]:
        cur.execute("UPDATE aceptacion_legales SET aceptada_en=%s WHERE persona_id=%s",
                    (datetime.combine(persona.alta, datetime.min.time()) + timedelta(hours=14),
                     persona.persona_id))


# --- Incentivos --------------------------------------------------------------------------


def escribir_incentivos(config: Config, mundo: Mundo) -> None:
    entidades = {e.clave: e.persona_id for e in mundo.entidades}
    with conectar(config, "incentivos") as conexion, conexion.cursor() as cur:
        for donante in mundo.donantes:
            inc = mundo.incentivos[donante.clave]
            pid = donante.persona_id
            cur.execute(
                """UPDATE donante_incentivos SET categoria=%s, fecha_registro=%s,
                   total_donaciones_historicas=%s, total_donaciones_exitosas=%s,
                   ultima_donacion=%s WHERE id=%s""",
                (inc.categoria, donante.alta, inc.total_donaciones, inc.total_exitosas,
                 inc.ultima_donacion, pid))
            if cur.rowcount != 1:
                raise RuntimeError(f"Incentivos no tiene al donante {donante.correo}")
            for mision in inc.misiones:
                cur.execute(
                    """UPDATE mision SET progreso_actual=%s, completada=%s, fecha_completada=%s,
                       ultimo_mes_donado=%s, fecha_ultimo_donacion=%s
                       WHERE donante_id=%s AND numero_mision=%s RETURNING id""",
                    (mision.progreso, mision.completada, mision.fecha_completada,
                     f"{mision.ultimo_mes[0]:04d}-{mision.ultimo_mes[1]:02d}"
                     if mision.ultimo_mes else None,
                     mision.fecha_ultima_donacion, pid, mision.numero))
                id_mision = cur.fetchone()[0]
                for categoria in sorted(mision.categorias_donadas):
                    cur.execute(
                        "INSERT INTO mision_categorias_donadas (mision_id, categoria) VALUES (%s,%s)",
                        (id_mision, categoria))
            for orden, (anterior, nueva, dia) in enumerate(inc.ascensos):
                cur.execute(
                    """INSERT INTO donante_historial_categoria (donante_id, orden,
                       categoria_anterior, categoria_nueva, fecha) VALUES (%s,%s,%s,%s,%s)""",
                    (pid, orden, anterior, nueva, dia))
            for orden, (nombre, dia) in enumerate(inc.insignias):
                cur.execute(
                    """INSERT INTO donante_insignia_ganada (donante_id, orden, nombre, descripcion,
                       imagen_url, visible, fecha_obtenida)
                       SELECT donante_id, %s, insignia_nombre, insignia_descripcion,
                              insignia_imagen_url, true, %s
                       FROM mision WHERE donante_id=%s AND insignia_nombre=%s""",
                    (orden, dia, pid, nombre))
            for clave in sorted(inc.organizaciones):
                cur.execute(
                    """INSERT INTO donante_organizacion_ayudada (donante_id, organizacion_id)
                       VALUES (%s,%s)""", (pid, entidades[clave]))
            cur.execute("DELETE FROM donante_donaciones_por_periodo WHERE donante_id=%s", (pid,))
            for periodo, cantidad in sorted(inc.por_mes.items()):
                cur.execute(
                    """INSERT INTO donante_donaciones_por_periodo (donante_id, periodo, cantidad)
                       VALUES (%s,%s,%s)""", (pid, periodo, cantidad))
        _rankings(cur, mundo)
    log.info("Incentivos: misiones, insignias, ascensos y rankings de %d donantes",
             len(mundo.donantes))


def _rankings(cur, mundo: Mundo) -> None:
    """Los rankings de los meses cerrados de la ventana, calculados el día 1 a las 00:05."""
    primero = mundo.hoy.replace(day=1)
    mes = mundo.inicio.replace(day=1)
    while mes < primero:
        siguiente = (mes + timedelta(days=32)).replace(day=1)
        periodo = f"{mes.year:04d}-{mes.month:02d}"
        id_ranking = Ids.de("ranking", periodo)
        calculado = datetime.combine(siguiente, datetime.min.time()).replace(
            tzinfo=timezone(-BA)) + timedelta(minutes=5)
        cur.execute(
            "INSERT INTO ranking_mensual (id, periodo, calculado_en) VALUES (%s,%s,%s)",
            (id_ranking, periodo, calculado))
        for posicion, (clave, cantidad) in enumerate(ranking_de(mundo, mes.year, mes.month), 1):
            donante = mundo.donante(clave)
            cur.execute(
                """INSERT INTO ranking_mensual_entrada (ranking_id, posicion, donante_id,
                   nombre_donante, misiones_completadas) VALUES (%s,%s,%s,%s,%s)""",
                (id_ranking, posicion, donante.persona_id, donante.denominacion, cantidad))
        mes = siguiente


# --- Notificaciones ----------------------------------------------------------------------


def _detalle_aviso(mundo: Mundo, ind: Independiente) -> str:
    """Como lo arma Donaciones para el aviso: descripción, id, peso, volumen y destino."""
    entidad = mundo.entidad(ind.entidad)
    d = entidad.direccion
    # str() de un float escribe como Java (18.0, 0.48)
    peso = float(round(ind.peso_kg * ind.cantidad, 2))
    volumen = float(round(ind.volumen_m3 * ind.cantidad, 3))
    return (f"{ind.descripcion}  (ID: {ind.id}, Peso: {peso} kg, Volumen: {volumen} m³, "
            f"Destino: {d.calle} {d.altura}, {d.localidad}, {d.provincia} "
            f"(CP {d.codigo_postal}), Argentina)")


def _avisos(mundo: Mundo) -> list[tuple]:
    """(persona_id, mensaje, momento UTC, tipo, referencia) de cada aviso histórico."""
    avisos: list[tuple] = []
    for donacion, ind in mundo.independientes():
        donante = mundo.donante(donacion.donante)
        if ind.estado_actual == "VENCIDA":
            vence = ind.pasos[-1]
            avisos.append((ID_ADMIN,
                           f"Atención administrador: La donación de {ind.descripcion}  del donante "
                           f"{donante.denominacion} ha vencido. Motivo: {vence.justificacion}.",
                           vence.momento, "DONACION_VENCIDA", ind.id))
        if not ind.entidad:
            continue
        entidad = mundo.entidad(ind.entidad)
        detalle = _detalle_aviso(mundo, ind)
        # En camino, recibida y fallida llevan solo la descripción (con su espacio final)
        corto = f"{ind.descripcion} "
        don, ent = donante.persona_id, entidad.persona_id
        asignada = ind.paso("ASIGNACION_REALIZADA").momento
        avisos.append((don, f"Tu donación de {detalle} fue asignada a {entidad.razon_social}",
                       asignada, "DONACION_ASIGNADA", ind.id))
        avisos.append((ent, f"Se te ha asignado la donación del donante {donante.denominacion}. "
                            f"Contiene: {detalle}", asignada, "DONACION_ASIGNADA", ind.id))
        enlace = URL_SEGUIMIENTO + (ind.id_ruta or ind.id)
        for paso in [p for p in ind.pasos if p.estado == "EN_TRASLADO"]:
            avisos.append((don, f"El camión salió a entregar tu donación de {corto}. Seguí el "
                                f"recorrido en tiempo real: {enlace}", paso.momento,
                           "DONACION_EN_CAMINO", ind.id))
            avisos.append((ent, f"El camión con la donación de {corto} ya está en camino hacia "
                                f"tu sede. Seguí el recorrido en tiempo real: {enlace}",
                           paso.momento, "DONACION_EN_CAMINO", ind.id))
        fallida = ind.paso("ENTREGA_FALLIDA")
        if fallida:
            motivo = fallida.justificacion
            avisos.append((don, f"No pudimos entregar tu donación de {corto}. Motivo: {motivo}. "
                                "Vamos a reprogramar la entrega.", fallida.momento,
                           "ENTREGA_FALLIDA", ind.id))
            avisos.append((ent, f"No pudimos entregarte la donación {corto}. Motivo: {motivo}. "
                                "La entrega será reprogramada.", fallida.momento,
                           "ENTREGA_FALLIDA", ind.id))
            avisos.append((ID_ADMIN, f"Entrega fallida — donante: {donante.denominacion}, "
                                     f"entidad: {entidad.razon_social}, donación: {corto}. "
                                     f"Motivo: {motivo}. Replanificada: sí", fallida.momento,
                           "ENTREGA_FALLIDA", ind.id))
        if ind.estado_actual == "ENTREGADA":
            # Como el sistema: el comprobante formatea la fecha del evento, que viaja en UTC
            comprobante = (f"Comprobante de entrega: {ind.fecha_entrega:%d/%m/%Y %H:%M} - "
                           f"camión {ind.patente}")
            avisos.append((don, f"¡Gracias! {entidad.razon_social} recibió tu donación de "
                                f"{corto}. {comprobante}", ind.fecha_entrega,
                           "DONACION_RECIBIDA", ind.id))
            avisos.append((ent, f"Ya llegó la donación {corto} a tu sede. {comprobante}",
                           ind.fecha_entrega, "DONACION_RECIBIDA", ind.id))
    for clave, inc in mundo.incentivos.items():
        pid = mundo.donante(clave).persona_id
        for mision in inc.misiones:
            if mision.completada:
                momento = datetime.combine(mision.fecha_completada, datetime.min.time()) + \
                    timedelta(hours=21)
                nombre = {"RACHA": "Racha", "COMPLETITUD": "Completitud",
                          "HABIL": "Habil Donador", "EXITOSAS": "Donaciones Exitosas"}[mision.tipo]
                avisos.append((pid, f"Completaste la misión {nombre}. Recompensa: "
                                    f"{mision.insignia}", momento, "MISION_CUMPLIDA", None))
        for anterior, nueva, dia in inc.ascensos:
            momento = datetime.combine(dia, datetime.min.time()) + timedelta(hours=21, seconds=2)
            avisos.append((pid, f"¡Felicitaciones! Has ascendido de la categoría {anterior} a "
                                f"{nueva}.", momento, "SUBIO_CATEGORIA", None))
    for donante in mundo.donantes:
        if donante.perfil == "inactivo":
            ultima = mundo.incentivos[donante.clave].ultima_donacion
            dias = (mundo.hoy - ultima).days
            avisos.append((donante.persona_id,
                           "¡Te extrañamos en DonaTrack! Hemos notado que han pasado "
                           f"{dias} días desde tu última actividad. Las entidades beneficiarias "
                           "aún necesitan tu ayuda. ¡Vuelve a hacer la diferencia!",
                           datetime.combine(mundo.hoy, datetime.min.time()) + timedelta(hours=11),
                           "DONANTE_INACTIVO", None))
    return avisos


def escribir_notificaciones(config: Config, mundo: Mundo, ahora: datetime) -> None:
    azar = random.Random(config.semilla + 17)
    avisos = _avisos(mundo)
    limite_leidas = ahora.replace(tzinfo=None) - timedelta(days=5)
    with conectar(config, "notificaciones") as conexion, conexion.cursor() as cur:
        for n, (persona, mensaje, momento, tipo, referencia) in enumerate(avisos):
            id_aviso = Ids.de("aviso", str(n), persona, tipo, referencia or "")
            leida = momento < limite_leidas and azar.random() < 0.9
            lectura = momento + timedelta(hours=azar.randint(1, 30)) if leida else None
            cur.execute(
                """INSERT INTO notificacion (id, persona_id, mensaje, fecha_creacion,
                   estado_notificacion, leida, fecha_lectura, tipo_evento, referencia_id)
                   VALUES (%s,%s,%s,%s,'ENVIADA',%s,%s,%s,%s)""",
                (id_aviso, persona, mensaje, momento, leida, lectura, tipo, referencia))
            cur.execute(
                """INSERT INTO notificacion_historial_estado (notificacion_id, estado_anterior,
                   estado_nuevo, "timestamp") VALUES (%s,NULL,'PENDIENTE',%s),
                   (%s,'PENDIENTE','ENVIADA',%s)""",
                (id_aviso, _ba(momento), id_aviso, _ba(momento) + timedelta(milliseconds=300)))
        # La bienvenida real llegó hoy: pasa al día del alta, ya leída
        for donante in mundo.donantes:
            alta = datetime.combine(donante.alta, datetime.min.time()) + timedelta(hours=17)
            if donante.alta < mundo.hoy:
                cur.execute(
                    """UPDATE notificacion SET fecha_creacion=%s, leida=true, fecha_lectura=%s
                       WHERE persona_id=%s AND tipo_evento='DONANTE_REGISTRADO'""",
                    (alta, alta + timedelta(minutes=20), donante.persona_id))
        _contacto(cur, mundo, ahora)
    log.info("Notificaciones: %d avisos históricos", len(avisos))


_MENSAJES_CONTACTO = [
    ("ENTREGA", "Hola, la entrega de ayer llegó con dos cajas golpeadas. ¿Cómo lo informamos?",
     "sede.sur", 26, "Gracias por avisar. Desde la pantalla de la entrega podés confirmar con un "
                     "reporte y adjuntar fotos; ya lo dejamos registrado."),
    ("CUENTA", "No me llega el correo para recuperar la clave de mi cuenta.", "vecina.flores", 19,
     "Revisá la carpeta de correo no deseado. Si sigue sin llegar, respondé este mensaje."),
    ("NECESIDAD", "¿Puedo publicar una necesidad que se repita cada quince días?", "comedor.norte",
     12, "Sí: al crear la necesidad elegí 'Recurrente' y la periodicidad quincenal."),
    ("DONACION", "Quiero donar una heladera usada que funciona bien. ¿La aceptan?", "dona.heladera",
     4, None),
    ("SITIO", "En el celular el mapa de entregas tarda en cargar.", "usuario.mapa", 2, None),
    ("OTRO", "Somos una empresa y queremos sumar a nuestro equipo de voluntariado.",
     "rse.empresa", 1, None),
]


def _contacto(cur, mundo: Mundo, ahora: datetime) -> None:
    for n, (tema, texto, usuario, dias, respuesta) in enumerate(_MENSAJES_CONTACTO):
        recibido = _ba(ahora.replace(tzinfo=None)) - timedelta(days=dias, hours=n * 3)
        cur.execute(
            """INSERT INTO mensaje_contacto (clave_envio, tema, texto, correo, ip_origen, estado,
               recibido_en, respuesta, respondido_en) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)""",
            (Ids.de("contacto", str(n)), tema, texto, f"{usuario}@demo.donatrack.test",
             f"192.0.2.{10 + n}", "RESPONDIDO" if respuesta else "RECIBIDO", recibido,
             respuesta, recibido + timedelta(hours=5) if respuesta else None))


# --- Logística ---------------------------------------------------------------------------


def _destino(direccion) -> dict:
    return {
        "calle": direccion.calle, "altura": direccion.altura,
        "codigoPostal": direccion.codigo_postal, "localidad": direccion.localidad,
        "provincia": direccion.provincia, "pais": "Argentina",
        "ubicacion": {"type": "Point", "coordinates": [direccion.lon, direccion.lat]},
    }


def mongo(config: Config):
    return MongoClient(config.mongo_uri, uuidRepresentation="standard").logistica


def escribir_logistica(config: Config, mundo: Mundo, camiones: dict[str, str],
                       choferes: dict[str, str]) -> None:
    """camiones y choferes: id del guion -> id que les dio la API."""
    db = mongo(config)
    por_id = {i.id: (d, i) for d, i in mundo.independientes()}
    patentes = {c.patente: camiones[c.id] for c in mundo.camiones}
    entregas, eventos = [], []
    for ruta in mundo.rutas:
        paradas = []
        for orden, id_ind in enumerate(ruta.entregas, 1):
            donacion, ind = por_id[id_ind]
            entidad = mundo.entidad(ind.entidad)
            destino = _destino(entidad.direccion)
            paradas.append({"orden": orden, "idEntrega": _uuid(ind.id_entrega),
                            "idDonacion": _uuid(ind.id), "destino": destino})
            entregas.append(_entrega(config, mundo, donacion, ind, ruta, patentes, destino))
            eventos.append({
                "_id": _uuid(Ids.de("evento", ind.id, "exitosa")),
                "idEntrega": _uuid(ind.id_entrega), "idDonacion": _uuid(ind.id),
                "idRuta": _uuid(ruta.id), "ocurrioEn": ind.fecha_entrega,
                "tipo": "ENTREGA_EXITOSA", "_class": _DOCUMENTOS + "EventoEntregaDocument"})
        db.rutas.insert_one({
            "_id": _uuid(ruta.id), "idChofer": _uuid(choferes[ruta.chofer]),
            "idCamion": _uuid(camiones[ruta.camion]),
            "fecha": datetime.combine(ruta.fecha, datetime.min.time()),
            "estado": "COMPLETADA", "horaInicioReal": ruta.inicio, "horaFinReal": ruta.fin,
            "version": Int64(0), "paradas": paradas,
            "historialEstado": [
                {"estadoAnterior": "PENDIENTE", "estadoNuevo": "EN_TRASLADO",
                 "timestamp": ruta.inicio},
                {"estadoAnterior": "EN_TRASLADO", "estadoNuevo": "COMPLETADA",
                 "timestamp": ruta.fin}],
            "_class": _DOCUMENTOS + "RutaDocument"})
    if entregas:
        db.entregas.insert_many(entregas)
        db.eventos_entrega.insert_many(eventos)
    log.info("Logística: %d rutas y %d entregas cerradas", len(mundo.rutas), len(entregas))


def _entrega(config, mundo, donacion, ind: Independiente, ruta, patentes, destino) -> dict:
    historial = []
    anterior = "PENDIENTE"
    for paso in ind.pasos:
        nuevo = {"EN_TRASLADO": "EN_TRASLADO", "EN_PUERTA": "EN_PUERTA",
                 "ENTREGADA": "ENTREGADA", "ENTREGA_FALLIDA": "NO_RECIBIDA"}.get(paso.estado)
        if paso.estado == "ASIGNACION_REALIZADA" and anterior == "NO_RECIBIDA":
            nuevo = "PENDIENTE"
        if nuevo is None or nuevo == anterior:
            continue
        actor = {"EN_PUERTA": "SIMULADOR", "EN_TRASLADO": "admin"}.get(nuevo)
        if actor is None:
            actor = mundo.entidad(ind.entidad).representante[0] + " " + \
                mundo.entidad(ind.entidad).representante[1]
        historial.append({"estadoAnterior": anterior, "estadoNuevo": nuevo,
                          "timestamp": paso.momento, "actor": actor})
        anterior = nuevo
    salida = [p for p in ind.pasos if p.estado == "EN_TRASLADO"][-1].momento
    documento = {
        "_id": _uuid(ind.id_entrega),
        "asignacion": {"idRuta": _uuid(ruta.id),
                       "fecha": datetime.combine(ruta.fecha, datetime.min.time()),
                       "idCamion": _uuid(patentes[ind.patente]), "patente": ind.patente},
        "destino": destino,
        "idDonacion": _uuid(ind.id),
        "idBeneficiaria": _uuid(mundo.entidad(ind.entidad).persona_id),
        "donanteId": _uuid(mundo.donante(donacion.donante).persona_id),
        "estado": "ENTREGADA",
        "horaArribo": ind.fecha_entrega,
        "horaSalida": salida,
        "fotosRecepcion": [{"clave": url, "subidaEn": ind.fecha_entrega - timedelta(minutes=1)}
                           for _, url in urls_de_fotos(config, ind)],
        "numeroRemito": f"R-0001-{abs(hash(ind.id)) % 10**8:08d}",
        "volumenTotalM3": round(ind.volumen_m3 * ind.cantidad, 3),
        "pesoTotalKg": round(ind.peso_kg * ind.cantidad, 2),
        "version": Int64(0),
        "historialEstado": historial,
        "_class": _DOCUMENTOS + "EntregaDocument",
    }
    if ind.reporte:
        documento["reporteMotivo"] = ind.reporte
        documento["reporteDetalle"] = ind.reporte_detalle
        documento["reporteCantidadRecibida"] = float(ind.cantidad - ind.cantidad_no_recibida)
    return documento


def categoria_de_bien(ind: Independiente) -> str:
    return catalogo.categoria_de(ind.subcategoria).nombre


def hoy_utc(fecha: date) -> datetime:
    return datetime.combine(fecha, datetime.min.time())


# --- Flota (por API, como la carga el administrador) --------------------------------------


def crear_flota(logistica, mundo: Mundo) -> tuple[dict[str, str], dict[str, str]]:
    """Da de alta camiones y choferes; devuelve id del guion -> id que asignó Logística."""
    camiones, choferes = {}, {}
    for c in mundo.camiones:
        creado = logistica.post("/api/camiones", {
            "patente": c.patente, "capacidadVolumen": c.capacidad_volumen,
            "altura": c.altura, "capacidadKG": c.capacidad_peso})
        camiones[c.id] = creado["id"]
        if not c.habilitado:
            logistica.patch(f"/api/camiones/{creado['id']}/estado",
                            {"estado": "DESHABILITADO", "motivo": "Service de frenos programado"})
    for ch in mundo.choferes:
        creado = logistica.post("/api/choferes", {
            "nombre": ch.nombre, "apellido": ch.apellido, "licencia": ch.licencia,
            "telefonoContacto": ch.telefono})
        choferes[ch.id] = creado["id"]
    log.info("Logística: %d camiones y %d choferes", len(camiones), len(choferes))
    return camiones, choferes
