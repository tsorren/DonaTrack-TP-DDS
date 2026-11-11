"""Lo que pasa hoy, por las APIs del sistema (SPEC-10, sección 5).

Todo lo de este módulo recorre los flujos reales: el algoritmo arma las propuestas, el
administrador aprueba, Logística recibe las solicitudes y el simulador mueve el camión.
El único atajo es el que el sistema no ofrece: Logística no geocodifica (DTI-15) ni tiene alta
de rutas por API, así que el seeder pone las coordenadas y crea la ruta en Mongo.

Se puede repetir con `seeder vivo` justo antes de presentar, para que el camión arranque de nuevo.
"""

from __future__ import annotations

import logging
import time
import uuid
from datetime import date, datetime, timedelta, timezone

from bson import Int64

from . import catalogo
from .api import Api, ErrorDeApi
from .config import Config
from .guion import RESERVADAS_VIVO, Mundo, Necesidad
from .historia import Ids, conectar, mongo
from .imagenes import foto_de_recepcion
from .keycloak import Keycloak

log = logging.getLogger("seeder")

_DOCUMENTOS = "grupo5.logistica.infrastructure.persistencia.mongo.documents."
ESPERA_MAXIMA = 90


def _esperar(descripcion: str, condicion, limite: int = ESPERA_MAXIMA):
    fin = time.monotonic() + limite
    while time.monotonic() < fin:
        valor = condicion()
        if valor:
            return valor
        time.sleep(2)
    raise TimeoutError(f"No llegó a tiempo: {descripcion}")


class EnVivo:
    def __init__(self, config: Config, mundo: Mundo, keycloak: Keycloak):
        self.config = config
        self.mundo = mundo
        self.kc = keycloak
        admin = lambda: keycloak.token_de(config.admin_correo, config.admin_clave)  # noqa: E731
        self.donaciones = Api(config.donaciones_url, admin)
        self.logistica = Api(config.logistica_url, admin)
        self.notificaciones = Api(config.notificaciones_url)
        self.db = mongo(config)

    def como(self, servicio: Api, correo: str) -> Api:
        return servicio.como(lambda: self.kc.token_de(correo, self.config.clave_demo))

    # --- Donaciones de hoy ---------------------------------------------------------------

    def _item(self, subcategoria: str, cantidad: int | None = None) -> dict:
        categoria = catalogo.categoria_de(subcategoria)
        bien = catalogo.subcategoria(subcategoria).bienes[0]
        return {
            "descripcionBien": bien.descripcion,
            "fechaVencimiento": (self.mundo.hoy + timedelta(days=120)).isoformat()
            if categoria.con_vencimiento else None,
            "estadoBien": ("NUEVO" if categoria.nombre != "Ropa" else "USADO")
            if categoria.con_uso else None,
            "pesoUnitario": bien.peso_kg, "volumenUnitario": bien.volumen_m3,
            "cantidad": cantidad or bien.minimo,
        }

    def donar(self, clave_donante: str, descripcion: str, items: list[dict]) -> str:
        donante = self.mundo.donante(clave_donante)
        creada = self.como(self.donaciones, donante.correo).post("/api/donaciones", {
            "idDonante": donante.persona_id, "descripcion": descripcion, "items": items,
            "nombreDeposito": "Depósito Central Barracas",
            "direccion": self.mundo.depositos["Depósito Central Barracas"].dto(),
        })
        return creada["id"]

    def _abiertas(self, entidad: str) -> list[Necesidad]:
        return [n for n in self.mundo.necesidades
                if n.entidad == entidad and n.estado_esperado == "VIGENTE" and n.activa
                and not n.baja_en_vivo]

    def _subcategorias_con_stock(self) -> set[str]:
        with conectar(self.config, "donaciones") as conexion:
            return {fila[0] for fila in conexion.execute(
                """SELECT DISTINCT s.nombre FROM donacion_independiente i
                   JOIN item_donacion_independiente ii ON ii.id_donacion_independiente = i.id
                   JOIN item_donacion_normalizado n ON n.id = ii.id_item_normalizado
                   JOIN subcategoria s ON s.id = n.id_subcategoria
                   WHERE i.estado_actual = 'EN_DEPOSITO'""")}

    def exclusivas(self, entidad: str, usadas: set[str]) -> list[Necesidad]:
        """Necesidades abiertas cuya subcategoría no pide nadie más ni tiene stock esperando.

        Donaciones corre dos algoritmos y se queda con las propuestas en las que coinciden; el
        segundo prioriza a las entidades menos atendidas. Para que la ruta de hoy pase por las
        protagonistas, cada bien tiene que tener un único destino posible.
        """
        otras = {n.subcategoria for n in self.mundo.necesidades
                 if n.entidad != entidad and n.activa and n.estado_esperado == "VIGENTE"}
        # El stock de una subcategoría reservada solo puede ir a la misma necesidad: no compite
        stock = self._subcategorias_con_stock() - set(RESERVADAS_VIVO)
        ocupadas = otras | stock | usadas
        return [n for n in self._abiertas(entidad) if n.subcategoria not in ocupadas]

    def _segmentada(self, id_donacion: str) -> bool:
        with conectar(self.config, "donaciones") as conexion:
            fila = conexion.execute("SELECT estado_donacion FROM donacion WHERE id = %s",
                                    (id_donacion,)).fetchone()
        return bool(fila) and fila[0] == "SEGMENTADA"

    def _asignadas_a(self, necesidades: list[str]) -> dict[str, str]:
        """Donaciones independientes ASIGNACION_REALIZADA de esas necesidades -> necesidad."""
        with conectar(self.config, "donaciones") as conexion:
            return {str(i): str(n) for i, n in conexion.execute(
                """SELECT id, id_necesidad FROM donacion_independiente
                   WHERE estado_actual = 'ASIGNACION_REALIZADA' AND id_necesidad = ANY(%s::uuid[])""",
                (necesidades,))}

    def pendientes(self) -> list[dict]:
        return [p for p in self.donaciones.get("/api/asignaciones/propuestas") or []
                if p.get("estado") == "PENDIENTE"]

    def propuestas_para(self, destinos: dict[str, str], previas: set[str]) -> list[dict]:
        return [p for p in self.pendientes()
                if p["id"] not in previas and p["necesidad"]["id"] in destinos]

    # --- La ruta de hoy ------------------------------------------------------------------

    def _entrega_de(self, id_independiente: str) -> dict | None:
        return self.db.entregas.find_one({"idDonacion": uuid.UUID(id_independiente),
                                          "estado": "PENDIENTE"})

    def _ubicar(self, entrega: dict, entidad_clave: str) -> None:
        """Lo que haría el geocodificador que falta (DTI-15): el punto de la sede."""
        d = self.mundo.entidad(entidad_clave).direccion
        self.db.entregas.update_one({"_id": entrega["_id"]}, {"$set": {
            "destino.ubicacion": {"type": "Point", "coordinates": [d.lon, d.lat]}}})

    def _estado_en_donaciones(self, id_independiente: str) -> str:
        return self.donaciones.get(f"/donaciones-independientes/{id_independiente}")["estadoActual"]

    def armar_ruta(self, paradas: list[tuple[str, str]]) -> str:
        """paradas: (id de la donación independiente, clave de la entidad), en orden de visita."""
        # Un camión libre: si una corrida anterior de `vivo` dejó otro en ruta, sale el siguiente
        libres = [c for c in self.logistica.get("/api/camiones")
                  if c.get("estado") == "DISPONIBLE" and not c.get("rutaId")]
        choferes = [c for c in self.logistica.get("/api/choferes") if not c.get("rutaId")]
        if not libres or not choferes:
            raise RuntimeError("No hay camión o chofer libre para la ruta de hoy")
        hoy = datetime.combine(self.mundo.hoy, datetime.min.time())
        id_ruta = str(uuid.uuid4())
        self.db.rutas.insert_one({
            "_id": uuid.UUID(id_ruta), "idChofer": uuid.UUID(choferes[0]["id"]),
            "idCamion": uuid.UUID(libres[0]["id"]), "fecha": hoy, "estado": "PENDIENTE",
            "version": Int64(0), "paradas": [], "historialEstado": [],
            "_class": _DOCUMENTOS + "RutaDocument"})
        for id_ind, entidad in paradas:
            entrega = _esperar(f"la entrega de {id_ind} en Logística",
                               lambda i=id_ind: self._entrega_de(i))
            self._ubicar(entrega, entidad)
            self.logistica.post(f"/api/rutas/{id_ruta}/entregas", {"entregaId": str(entrega["_id"])})
        # Donaciones procesa ruta.asignada y ruta.iniciada por colas distintas: si se arranca la
        # ruta antes de que la asignación llegue, la donación queda atrás (hallazgo del seeder).
        for id_ind, _ in paradas:
            _esperar(f"{id_ind} lista para entregar",
                     lambda i=id_ind: self._estado_en_donaciones(i) == "ListaParaEntregar")
        self.logistica.patch(f"/api/rutas/{id_ruta}/estado",
                             {"estado": "EN_TRASLADO", "actor": "admin"})
        log.info("Ruta de hoy en marcha con %d paradas: %s/seguimiento/%s", len(paradas),
                 "http://localhost:3000", id_ruta)
        return id_ruta

    def confirmar(self, entidad_clave: str, id_entrega: str, etiqueta: str,
                  reporte: dict | None = None) -> None:
        entidad = self.mundo.entidad(entidad_clave)
        api = self.como(self.logistica, entidad.correo)
        foto = foto_de_recepcion(hash(id_entrega) & 0xFFFF, etiqueta,
                                 (datetime.now(timezone.utc) - timedelta(hours=3))
                                 .strftime("%d/%m/%Y %H:%M"))
        subidas = api.pedir("POST", f"/api/entregas/{id_entrega}/fotos",
                            files=[("foto", ("recepcion.jpg", foto, "image/jpeg"))]).json()
        cuerpo = {"estado": "ENTREGADA", "actor": " ".join(entidad.representante),
                  "fotosRecepcion": [f["url"] for f in subidas],
                  "numeroRemito": f"R-0001-{abs(hash(id_entrega)) % 10**8:08d}"}
        if reporte:
            cuerpo["reporte"] = reporte
        api.patch(f"/api/entregas/{id_entrega}/estado", cuerpo)

    def no_recibida(self, entidad_clave: str, id_entrega: str, motivo: str) -> None:
        entidad = self.mundo.entidad(entidad_clave)
        self.como(self.logistica, entidad.correo).patch(f"/api/entregas/{id_entrega}/estado", {
            "estado": "NO_RECIBIDA", "actor": " ".join(entidad.representante),
            "justificacion": motivo, "replanificable": True})

    # --- El recorrido completo -----------------------------------------------------------

    def correr(self) -> None:
        self.ruta_de_hoy()
        self._pendientes_del_admin()
        self._bajas()
        self._csv()
        self._contacto()

    def _asignadas(self, id_donacion: str, destinos: dict[str, str]) -> list[tuple[str, str]]:
        """Aprueba las propuestas hacia las necesidades elegidas; devuelve (independiente, entidad)."""
        # El algoritmo corre solo de madrugada (PlanificadorDeAlgoritmos): el admin lo corre a mano
        _esperar("la donación segmentada", lambda: self._segmentada(id_donacion), 30)
        previas = {p["id"] for p in self.pendientes()}
        self.donaciones.post("/api/asignaciones/ejecuciones")
        propuestas = _esperar("las propuestas del algoritmo",
                              lambda: self.propuestas_para(destinos, previas), 20)
        antes = self._asignadas_a(list(destinos))
        # Orden de visita: el de los destinos (la protagonista primero, en la puerta)
        orden = list(destinos)
        propuestas = sorted(propuestas, key=lambda p: orden.index(p["necesidad"]["id"])
                            if p["necesidad"]["id"] in orden else len(orden))
        for propuesta in propuestas:
            entidad = destinos.get(propuesta["necesidad"]["id"])
            if not entidad:
                continue
            self.donaciones.put(f"/api/asignaciones/propuestas/{propuesta['id']}/estado",
                                {"estado": "APROBADA", "justificacion": "Aprobada en la demo"})
        # Al aprobar una parte de un bien, Donaciones crea otra donación independiente con lo
        # asignado: las paradas son las asignaciones nuevas, no los fragmentos de la propuesta
        nuevas = {i: n for i, n in self._asignadas_a(list(destinos)).items() if i not in antes}
        paradas = sorted(((i, destinos[n]) for i, n in nuevas.items()),
                         key=lambda par: orden.index(nuevas[par[0]]))
        # Cada corrida vuelve a proponer el stock que ya estaba en propuestas pendientes: el admin
        # descarta las de esta corrida para que la siguiente deje un solo juego pendiente
        for propuesta in self.pendientes():
            if propuesta["id"] not in previas:
                self.donaciones.put(f"/api/asignaciones/propuestas/{propuesta['id']}/estado",
                                    {"estado": "DESCARTADA",
                                     "justificacion": "Se revisa en la próxima corrida"})
        return paradas

    def ruta_de_hoy(self) -> None:
        objetivos, usadas = [], set()
        for entidad in ("comedor", "escuela", "escuela"):
            candidatas = self.exclusivas(entidad, usadas)
            if candidatas:
                objetivos.append((entidad, candidatas[0]))
                usadas.add(candidatas[0].subcategoria)
        if not objetivos:
            raise RuntimeError("Ninguna necesidad de las protagonistas tiene un destino único hoy")
        destinos = {n.id: entidad for entidad, n in objetivos}
        items = [self._item(n.subcategoria) for _, n in objetivos]
        id_donacion = self.donar("lucia", "Lo que juntamos en el edificio esta semana", items)
        paradas = self._asignadas(id_donacion, destinos)
        if len(paradas) < 2:
            log.warning("El algoritmo propuso %d paradas; la ruta de hoy sale más corta",
                        len(paradas))
        if not paradas:
            return
        self.armar_ruta(paradas)
        ids = [e for e, _ in paradas]
        entregas = {i: self.db.entregas.find_one({"idDonacion": uuid.UUID(i),
                                                  "estado": {"$ne": "CANCELADA"}})["_id"]
                    for i in ids}
        # Segunda parada: no había nadie (la entrega vuelve a planificarse)
        if len(paradas) > 1:
            segunda, entidad = paradas[1]
            self.no_recibida(entidad, str(entregas[segunda]),
                             "No había nadie en la sede para recibir")
        # La primera queda EN_PUERTA a los ~30 s: la confirma el presentador, en vivo
        _esperar("el camión en la primera parada", lambda: self.db.entregas.find_one(
            {"_id": entregas[ids[0]], "estado": "EN_PUERTA"}), 120)
        log.info("Camión en la puerta de %s: listo para confirmar en vivo",
                 self.mundo.entidad(paradas[0][1]).razon_social)

    def _pendientes_del_admin(self) -> None:
        """Una donación de hoy con propuestas sin revisar y un bien que no se reconoce."""
        nuevo = next(d for d in self.mundo.donantes if d.perfil == "nuevo")
        abiertas = [n for e in ("comedor", "escuela") for n in self._abiertas(e)[2:4]]
        items = [self._item(n.subcategoria) for n in abiertas]
        id_donacion = self.donar(nuevo.clave, "Mi primera donación", items)
        _esperar("la donación segmentada", lambda: self._segmentada(id_donacion), 30)
        self.donaciones.post("/api/asignaciones/ejecuciones")
        # Aparte: una donación entera espera la revisión de un bien que no se reconoce
        self.donar("martin", "Cosas de la casa de mi abuela", [{
            "descripcionBien": "Lámpara de pie antigua de bronce", "estadoBien": "USADO",
            "pesoUnitario": 4.0, "volumenUnitario": 0.05, "cantidad": 1}])
        log.info("Pendientes del admin: propuestas de la donación %s y un bien sin reconocer",
                 id_donacion)

    def _bajas(self) -> None:
        for n in self.mundo.necesidades:
            if n.baja_en_vivo:
                entidad = self.mundo.entidad(n.entidad)
                self.como(self.donaciones, entidad.correo).delete(f"/api/necesidades/{n.id}")
                log.info("Necesidad dada de baja: %s", n.descripcion)
        for e in self.mundo.entidades:
            if e.estado == "baja":
                try:
                    self.donaciones.delete(f"/api/entidades/{e.persona_id}")
                    log.info("Entidad dada de baja: %s", e.razon_social)
                except ErrorDeApi as error:
                    if error.estado not in (404, 409):
                        raise
                    log.info("La entidad %s ya estaba dada de baja", e.razon_social)

    def _csv(self) -> None:
        filas = ["TIPO_PERSONA,NOMBRE,APELLIDO,FECHA_NACIMIENTO,TIPO_DOCUMENTO,DOCUMENTO,EMAIL,"
                 "TELEFONO"]
        for n, (nombre, apellido) in enumerate([("Valeria", "Ortiz"), ("Gonzalo", "Ibarra"),
                                                ("Marina", "Quiroga")]):
            filas.append(f"HUMANA,{nombre},{apellido},199{n}-0{n + 3}-1{n},DNI,9030000{n},"
                         f"{nombre.lower()}.{apellido.lower()}@demo.donatrack.test,11555504{n:02d}")
        contenido = ("\n".join(filas) + "\n").encode()
        try:
            self.donaciones.pedir("POST", "/api/donantes/archivos",
                                  files=[("archivo", ("donantes-feria.csv", contenido, "text/csv"))])
            log.info("CSV de donantes importado (3 filas)")
        except ErrorDeApi as error:
            if error.estado != 409:
                raise
            log.info("El CSV de donantes ya estaba importado")

    def _contacto(self) -> None:
        for tema, texto, usuario in [
            ("ENTREGA", "¿Puedo cambiar el horario en que reciben las donaciones?", "comedor.sur"),
            ("SITIO", "No encuentro dónde descargar el comprobante de mi donación.", "vecino.boedo"),
        ]:
            self.notificaciones.post("/api/contacto", {
                "tema": tema, "texto": texto,
                "correo": f"{usuario}@demo.donatrack.test",
                "claveEnvio": Ids.de("contacto-vivo", usuario, str(self.mundo.hoy))})
        log.info("Dos mensajes de contacto sin responder")


def correr(config: Config, mundo: Mundo, keycloak: Keycloak) -> None:
    EnVivo(config, mundo, keycloak).correr()


def hoy_local() -> date:
    return (datetime.now(timezone.utc) - timedelta(hours=3)).date()
