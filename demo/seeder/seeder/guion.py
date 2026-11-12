"""El guion de la demo: quiénes son, qué donaron y qué pasó con cada donación en seis meses.

Todo es puro y determinístico: con la misma semilla y el mismo día da el mismo mundo. Los ids de
personas los pone la API después (persona_id), los demás salen de la semilla. Las horas son UTC
sin zona, como las guardan Donaciones y Logística.
"""

from __future__ import annotations

import random
import uuid
from dataclasses import dataclass, field
from datetime import date, datetime, time, timedelta

from . import catalogo

VENTANA_DIAS = 182


# --- Elenco ------------------------------------------------------------------------------------


@dataclass(frozen=True)
class Direccion:
    calle: str
    altura: int
    codigo_postal: str
    localidad: str
    provincia: str
    lat: float
    lon: float
    piso: int | None = None
    departamento: str | None = None

    def dto(self) -> dict:
        return {
            "calle": self.calle, "altura": self.altura, "piso": self.piso,
            "departamento": self.departamento, "codigoPostal": self.codigo_postal,
            "localidad": self.localidad, "provincia": self.provincia, "pais": "Argentina",
        }


@dataclass
class Donante:
    clave: str
    correo: str
    tipo: str  # HUMANA o JURIDICA
    nombre: str  # nombre de pila o razón social
    apellido: str | None
    documento: str
    telefono: str
    direccion: Direccion
    perfil: str  # estrella, constante, empresa, inactivo, nuevo, relleno, misionero_a, misionero_b
    alta: date
    genero: str = "PREFIERO_NO_DECIR"
    nacimiento: date | None = None
    rubro: str | None = None
    contacto: tuple[str, str] | None = None  # representante de una empresa (nombre, apellido)
    mostrar_nombre: bool = True
    protagonista: bool = False
    persona_id: str | None = None

    @property
    def denominacion(self) -> str:
        return self.nombre if self.tipo == "JURIDICA" else f"{self.nombre} {self.apellido}"


@dataclass
class Entidad:
    clave: str
    correo: str
    razon_social: str
    tipo_juridico: str
    rubro: str
    documento: str
    telefono: str
    direccion: Direccion
    representante: tuple[str, str]
    estado: str  # activa, pendiente o baja
    alta: date
    protagonista: bool = False
    persona_id: str | None = None


# --- Historia ----------------------------------------------------------------------------------


@dataclass
class Periodo:
    id: str
    inicio: datetime
    fin: datetime
    objetivo: int


@dataclass
class Necesidad:
    id: str
    clave: str
    entidad: str
    subcategoria: str
    tipo: str  # RECURRENTE o EXTRAORDINARIA
    cantidad: int
    descripcion: str
    inicio: datetime
    periodicidad: str | None = None  # SEMANAL, QUINCENAL o MENSUAL
    fecha_limite: date | None = None
    activa: bool = True
    periodos: list[Periodo] = field(default_factory=list)
    # Cómo tiene que verse hoy: VIGENTE, CUMPLIDA, TERMINADA o DADA_DE_BAJA
    estado_esperado: str = "VIGENTE"
    # Cuánto del objetivo cubre la historia (en la vigente o en el período actual)
    cobertura: float = 0.5
    # Desde cuándo está dada de baja (la de una entidad que se dio de baja)
    baja: date | None = None
    # La baja la hace el seeder en vivo por la API, con su entrega cancelada
    baja_en_vivo: bool = False

    def periodo_en(self, momento: datetime) -> Periodo | None:
        for periodo in self.periodos:
            if periodo.inicio <= momento < periodo.fin:
                return periodo
        return None


@dataclass
class Paso:
    """Un cambio de estado de la donación independiente."""
    estado: str
    momento: datetime
    actor: str
    justificacion: str | None = None
    patente: str | None = None


@dataclass
class Independiente:
    id: str
    id_item_normalizado: str
    descripcion: str
    subcategoria: str
    cantidad: int
    peso_kg: float
    volumen_m3: float
    estado_bien: str | None
    vencimiento: date | None
    pasos: list[Paso] = field(default_factory=list)
    necesidad: str | None = None  # id
    periodo: str | None = None  # id
    entidad: str | None = None  # clave
    fecha_entrega: datetime | None = None
    cantidad_no_recibida: int | None = None
    fotos: int = 0  # cuántas fotos de recepción
    reporte: str | None = None  # INCOMPLETA o DANADA, si la entidad informó un problema
    reporte_detalle: str | None = None
    patente: str | None = None
    fallo: str | None = None  # motivo de una entrega fallida antes de la exitosa
    propuesta: str | None = None  # id
    id_entrega: str | None = None  # Logística
    id_ruta: str | None = None

    @property
    def estado_actual(self) -> str:
        return self.pasos[-1].estado

    def paso(self, estado: str) -> Paso | None:
        for p in self.pasos:
            if p.estado == estado:
                return p
        return None


@dataclass
class Donacion:
    id: str
    donante: str  # clave
    fecha: datetime
    descripcion: str
    deposito: str
    nombre_oculto: bool
    independientes: list[Independiente] = field(default_factory=list)

    @property
    def cantidad_bienes(self) -> int:
        return sum(i.cantidad for i in self.independientes)

    @property
    def categorias(self) -> list[str]:
        vistas: list[str] = []
        for i in self.independientes:
            nombre = catalogo.categoria_de(i.subcategoria).nombre
            if nombre not in vistas:
                vistas.append(nombre)
        return vistas


@dataclass
class Camion:
    id: str
    patente: str
    capacidad_volumen: float
    capacidad_peso: float
    altura: float
    habilitado: bool = True


@dataclass
class Chofer:
    id: str
    nombre: str
    apellido: str
    licencia: str
    telefono: str


@dataclass
class Ruta:
    id: str
    fecha: date
    camion: str  # id
    chofer: str  # id
    inicio: datetime
    fin: datetime
    entregas: list[str] = field(default_factory=list)  # ids de independientes, en orden


@dataclass
class Mision:
    numero: int
    tipo: str
    categoria: str
    objetivo: int
    insignia: str
    progreso: int = 0
    completada: bool = False
    fecha_completada: date | None = None
    categorias_donadas: set[str] = field(default_factory=set)
    ultimo_mes: tuple[int, int] | None = None  # RACHA
    fecha_ultima_donacion: date | None = None  # EXITOSAS


@dataclass
class Incentivos:
    donante: str
    categoria: str = "COLABORADOR"
    misiones: list[Mision] = field(default_factory=list)
    ascensos: list[tuple[str, str, date]] = field(default_factory=list)
    insignias: list[tuple[str, date]] = field(default_factory=list)
    total_donaciones: int = 0
    total_exitosas: int = 0
    ultima_donacion: date | None = None
    por_mes: dict[str, int] = field(default_factory=dict)
    organizaciones: set[str] = field(default_factory=set)  # claves de entidad


@dataclass
class Mundo:
    hoy: date
    inicio: date
    donantes: list[Donante]
    entidades: list[Entidad]
    necesidades: list[Necesidad]
    donaciones: list[Donacion]
    camiones: list[Camion]
    choferes: list[Chofer]
    rutas: list[Ruta]
    incentivos: dict[str, Incentivos]
    depositos: dict[str, Direccion]

    def donante(self, clave: str) -> Donante:
        return next(d for d in self.donantes if d.clave == clave)

    def entidad(self, clave: str) -> Entidad:
        return next(e for e in self.entidades if e.clave == clave)

    def necesidad(self, id_necesidad: str) -> Necesidad:
        return next(n for n in self.necesidades if n.id == id_necesidad)

    def independientes(self):
        for donacion in self.donaciones:
            for independiente in donacion.independientes:
                yield donacion, independiente


# --- Datos fijos del elenco ----------------------------------------------------------------

DOMINIO = "demo.donatrack.test"

# Direcciones de la zona de Buenos Aires, sin altura real de nadie: calles públicas y alturas
# redondeadas, con la coordenada aproximada de la cuadra para el mapa.
_DIRECCIONES_ENTIDAD = [
    Direccion("Av. Iriarte", 3500, "C1437", "Barracas", "CABA", -34.6468, -58.3913),
    Direccion("Av. Rivadavia", 7200, "C1406", "Flores", "CABA", -34.6297, -58.4635),
    Direccion("Av. Mitre", 2400, "B1870", "Avellaneda", "Buenos Aires", -34.6720, -58.3530),
    Direccion("Calle 12", 1100, "B1900", "La Plata", "Buenos Aires", -34.9150, -57.9540),
    Direccion("Av. Maipú", 1800, "B1602", "Florida", "Buenos Aires", -34.5290, -58.4900),
    Direccion("Av. Hipólito Yrigoyen", 8800, "B1832", "Lomas de Zamora", "Buenos Aires",
              -34.7600, -58.4030),
    Direccion("Av. Eva Perón", 4200, "C1407", "Parque Avellaneda", "CABA", -34.6430, -58.4800),
    Direccion("Av. Pres. Perón", 1500, "B1663", "San Miguel", "Buenos Aires", -34.5430, -58.7120),
    Direccion("Av. Corrientes", 5400, "C1414", "Villa Crespo", "CABA", -34.5990, -58.4380),
    Direccion("Av. San Martín", 2900, "B1678", "Caseros", "Buenos Aires", -34.6060, -58.5630),
    Direccion("Av. Rigolleau", 600, "B1884", "Berazategui", "Buenos Aires", -34.7630, -58.2120),
    Direccion("Av. Gaona", 3300, "C1416", "Floresta", "CABA", -34.6170, -58.4700),
]

_DIRECCIONES_DONANTE = [
    Direccion("Av. Cabildo", 2200, "C1428", "Belgrano", "CABA", -34.5610, -58.4560, 4, "B"),
    Direccion("Av. Santa Fe", 3100, "C1425", "Palermo", "CABA", -34.5880, -58.4110, 7, "A"),
    Direccion("Av. Belgrano", 1500, "C1093", "Monserrat", "CABA", -34.6130, -58.3860),
    Direccion("Av. del Libertador", 14500, "B1640", "Martínez", "Buenos Aires", -34.4930,
              -58.5000),
    Direccion("Av. Alem", 900, "B1704", "Ramos Mejía", "Buenos Aires", -34.6420, -58.5650),
    Direccion("Av. Directorio", 2500, "C1406", "Caballito", "CABA", -34.6330, -58.4500),
]

_DEPOSITOS = {
    "Depósito Central Barracas": Direccion(
        "California", 2000, "C1289", "Barracas", "CABA", -34.6420, -58.3800),
    "Depósito Norte Munro": Direccion(
        "Av. Vélez Sarsfield", 4600, "B1605", "Munro", "Buenos Aires", -34.5280, -58.5230),
}

_NOMBRES = [
    ("Valentina", "Acosta", "MUJER"), ("Tomás", "Benítez", "HOMBRE"),
    ("Camila", "Castro", "MUJER"), ("Joaquín", "Domínguez", "HOMBRE"),
    ("Florencia", "Escobar", "MUJER"), ("Matías", "Figueroa", "HOMBRE"),
    ("Agustina", "Giménez", "MUJER"), ("Nicolás", "Herrera", "HOMBRE"),
    ("Julieta", "Ibarra", "MUJER"), ("Facundo", "Juárez", "HOMBRE"),
    ("Milagros", "Ledesma", "MUJER"), ("Santiago", "Molina", "HOMBRE"),
    ("Rocío", "Navarro", "MUJER"), ("Federico", "Ojeda", "HOMBRE"),
    ("Antonella", "Paz", "MUJER"), ("Gonzalo", "Quiroga", "HOMBRE"),
    ("Brenda", "Ríos", "MUJER"), ("Ezequiel", "Sosa", "HOMBRE"),
    ("Micaela", "Toledo", "MUJER"), ("Lautaro", "Vega", "HOMBRE"),
    ("Abril", "Zárate", "MUJER"), ("Ramiro", "Aguirre", "HOMBRE"),
    ("Daniela", "Bustos", "MUJER"), ("Leandro", "Cabrera", "HOMBRE"),
    ("Paula", "Díaz", "MUJER"), ("Hernán", "Espinoza", "HOMBRE"),
    ("Lucila", "Ferreyra", "MUJER"), ("Iván", "Godoy", "HOMBRE"),
    ("Sol", "Luna", "NO_BINARIO"), ("Mariano", "Medina", "HOMBRE"),
]

# Quienes completan misiones para que el Ranking tenga puestos: 12 por cada grupo (ver
# _fechas_misionero). Todos sintéticos, con el mismo dominio de demo que el resto del elenco.
_MISIONEROS = [
    ("Candela", "Arce", "MUJER"), ("Bruno", "Barrios", "HOMBRE"),
    ("Renata", "Campos", "MUJER"), ("Thiago", "Duarte", "HOMBRE"),
    ("Malena", "Echeverría", "MUJER"), ("Bautista", "Fariña", "HOMBRE"),
    ("Pilar", "Gallo", "MUJER"), ("Lisandro", "Heredia", "HOMBRE"),
    ("Josefina", "Iglesias", "MUJER"), ("Dante", "Jara", "HOMBRE"),
    ("Emilia", "Krause", "MUJER"), ("Gael", "Lucero", "HOMBRE"),
    ("Carolina", "Maidana", "MUJER"), ("Franco", "Núñez", "HOMBRE"),
    ("Delfina", "Orellana", "MUJER"), ("Ciro", "Peralta", "HOMBRE"),
    ("Luna", "Quinteros", "MUJER"), ("Manuel", "Rolón", "HOMBRE"),
    ("Ailén", "Salazar", "MUJER"), ("Tobías", "Trejo", "HOMBRE"),
    ("Zoe", "Urquiza", "MUJER"), ("Ulises", "Valdez", "HOMBRE"),
    ("Mora", "Wagner", "MUJER"), ("Axel", "Yáñez", "HOMBRE"),
]

_EMPRESAS_RELLENO = [
    ("Panificadora La Espiga S.R.L.", "Alimentos"),
    ("Textil Riachuelo S.A.", "Indumentaria"),
    ("Librería El Ateneo Escolar S.R.L.", "Librería"),
]

_ENTIDADES_RELLENO = [
    ("Merendero Sonrisas del Sur", "ONG", "Merendero comunitario"),
    ("Hogar de Ancianos San Roque", "ONG", "Hogar de adultos mayores"),
    ("Jardín de Infantes N.º 905", "INSTITUCION", "Educación inicial"),
    ("Centro Comunitario Villa Crespo", "ONG", "Centro comunitario"),
    ("Parroquia Nuestra Señora de Luján - Cáritas", "ONG", "Asistencia social"),
    ("Club Social y Deportivo Caseros", "ONG", "Club de barrio"),
    ("Biblioteca Popular Berazategui", "ONG", "Biblioteca popular"),
    ("Dirección de Desarrollo Social de Floresta", "GUBERNAMENTAL", "Asistencia social"),
]

# Necesidades de cada entidad: (subcategoría, tipo, cantidad, periodicidad o días hasta el
# límite, descripción, estado esperado, cobertura)
_NECESIDADES_PROTAGONISTA = [
    ("No Perecederos", "RECURRENTE", 120, "MENSUAL",
     "Alimentos secos para las 80 viandas diarias del comedor", "VIGENTE", 0.55),
    ("Frutas", "RECURRENTE", 30, "SEMANAL",
     "Fruta fresca para el postre de los chicos", "VIGENTE", 1.0),
    ("Ropa de cama", "EXTRAORDINARIA", 40, -20,
     "Frazadas para las familias que pasaron el invierno en la sede", "TERMINADA", 0.6),
    ("Ropa de Invierno", "EXTRAORDINARIA", 30, 25,
     "Camperas y buzos para el ropero comunitario", "CUMPLIDA", 1.0),
    ("Higiene personal", "EXTRAORDINARIA", 60, 30,
     "Kits de higiene para las familias del barrio", "VIGENTE", 0.4),
    ("Muebles de Oficina", "EXTRAORDINARIA", 3, 40,
     "Escritorios para el aula de apoyo escolar", "DADA_DE_BAJA", 0.0),
]

_NECESIDADES_ESCUELA = [
    ("Cuadernos y papelería", "RECURRENTE", 150, "MENSUAL",
     "Cuadernos y resmas para los tres turnos", "VIGENTE", 0.5),
    ("Muebles Escolares", "EXTRAORDINARIA", 20, 35,
     "Sillas para el aula nueva de primer grado", "VIGENTE", 0.35),
    ("Mochilas y cartucheras", "EXTRAORDINARIA", 30, -40,
     "Mochilas para el inicio del ciclo lectivo", "CUMPLIDA", 1.0),
    ("Juegos de mesa", "EXTRAORDINARIA", 12, 45,
     "Juegos para la ludoteca del recreo", "VIGENTE", 0.25),
]

# Subcategorías que solo piden las protagonistas y sin stock esperando: la ruta de hoy pasa por
# ellas porque Donaciones solo propone lo que sus dos algoritmos eligen igual, y el segundo
# prioriza a las entidades menos atendidas (las de relleno, si pidieran lo mismo).
STOCK_CON_CANDIDATAS = 4
RESERVADAS_VIVO = ("Higiene personal", "Muebles Escolares", "Juegos de mesa")

_NECESIDADES_RELLENO = [
    ("No Perecederos", "RECURRENTE", 80, "QUINCENAL", "Mercadería para la copa de leche"),
    ("Frutas", "RECURRENTE", 20, "SEMANAL", "Fruta para la merienda"),
    ("Ropa de Invierno", "EXTRAORDINARIA", 25, 30, "Abrigo para la temporada de frío"),
    ("Ropa de Verano", "EXTRAORDINARIA", 30, 50, "Ropa liviana para la colonia de verano"),
    ("Limpieza del hogar", "RECURRENTE", 24, "MENSUAL", "Artículos de limpieza para la sede"),
    ("Higiene personal", "RECURRENTE", 40, "MENSUAL", "Higiene para los residentes"),
    ("Peluches y pelotas", "EXTRAORDINARIA", 20, 40, "Juguetes para el Día de las Infancias"),
    ("Toallas", "EXTRAORDINARIA", 30, 35, "Toallas para el vestuario"),
    ("Calefacción", "EXTRAORDINARIA", 4, 60, "Estufas para el salón principal"),
    ("Cocina", "EXTRAORDINARIA", 2, 45, "Una heladera para la cocina comunitaria"),
    ("Cuadernos y papelería", "RECURRENTE", 60, "MENSUAL", "Útiles para el apoyo escolar"),
    ("Mesa de luz", "EXTRAORDINARIA", 6, 50, "Mesas de luz para las habitaciones"),
    ("Velador", "EXTRAORDINARIA", 6, 50, "Veladores para la sala de lectura"),
    ("Juegos de mesa", "EXTRAORDINARIA", 10, 40, "Juegos para la sala de recreación"),
]

PERIODOS = {"SEMANAL": timedelta(days=7), "QUINCENAL": timedelta(days=15)}

# Misiones estándar de Incentivos (MisionFactory): número, tipo, categoría, objetivo, insignia
MISIONES = [
    (1, "RACHA", "COLABORADOR", 3, "Racha Inicial"),
    (2, "COMPLETITUD", "COLABORADOR", 3, "Explorador"),
    (3, "HABIL", "SOSTENEDOR", 50, "Gran Aporte"),
    (4, "RACHA", "SOSTENEDOR", 6, "Constancia"),
    (5, "EXITOSAS", "TRANSFORMADOR", 10, "Impacto Real"),
    (6, "COMPLETITUD", "TRANSFORMADOR", 8, "Diversidad Solidaria"),
]
SIGUIENTE = {"COLABORADOR": "SOSTENEDOR", "SOSTENEDOR": "TRANSFORMADOR"}


# --- Construcción --------------------------------------------------------------------------


class _Ids:
    def __init__(self, azar: random.Random):
        self._azar = azar

    def nuevo(self) -> str:
        return str(uuid.UUID(int=self._azar.getrandbits(128), version=4))


def _sumar_meses(dia: date, meses: int) -> date:
    mes = dia.month - 1 + meses
    anio = dia.year + mes // 12
    mes = mes % 12 + 1
    ultimo = [31, 29 if anio % 4 == 0 and (anio % 100 != 0 or anio % 400 == 0) else 28,
              31, 30, 31, 30, 31, 31, 30, 31, 30, 31][mes - 1]
    return date(anio, mes, min(dia.day, ultimo))


def _a_las(dia: date, hora: int, minuto: int = 0) -> datetime:
    """Hora de Buenos Aires del día, expresada en UTC sin zona (UTC = BA + 3 h)."""
    return datetime.combine(dia, time(hora, minuto)) + timedelta(hours=3)


def _elenco(hoy: date, azar: random.Random) -> tuple[list[Donante], list[Entidad]]:
    def dias(n: int) -> date:
        return hoy - timedelta(days=n)

    donantes = [
        Donante("lucia", f"lucia.fernandez@{DOMINIO}", "HUMANA", "Lucía", "Fernández",
                "90100001", "1155550101", _DIRECCIONES_DONANTE[0], "estrella",
                _sumar_meses(hoy.replace(day=1), -6).replace(day=12),
                genero="MUJER", nacimiento=date(1988, 3, 14), protagonista=True),
        Donante("martin", f"martin.gomez@{DOMINIO}", "HUMANA", "Martín", "Gómez",
                "90100002", "1155550102", _DIRECCIONES_DONANTE[1], "constante", dias(140),
                genero="HOMBRE", nacimiento=date(1979, 11, 2), protagonista=True),
        Donante("distribuidora", f"donaciones@distribuidora-sur.{DOMINIO}", "JURIDICA",
                "Distribuidora del Sur S.A.", None, "30901000031", "1155550103",
                _DIRECCIONES_DONANTE[2], "empresa", dias(170), rubro="Distribución mayorista",
                contacto=("Graciela", "Moreno"), mostrar_nombre=False, protagonista=True),
        Donante("sofia", f"sofia.ruiz@{DOMINIO}", "HUMANA", "Sofía", "Ruiz",
                "90100004", "1155550104", _DIRECCIONES_DONANTE[3], "nuevo", hoy,
                genero="MUJER", nacimiento=date(2001, 7, 21), protagonista=True),
        Donante("carlos", f"carlos.pereyra@{DOMINIO}", "HUMANA", "Carlos", "Pereyra",
                "90100005", "1155550105", _DIRECCIONES_DONANTE[4], "inactivo", dias(150),
                genero="HOMBRE", nacimiento=date(1965, 5, 30), protagonista=True),
    ]
    for i, (nombre, apellido, genero) in enumerate(_NOMBRES):
        donantes.append(Donante(
            f"relleno{i:02d}", f"{_slug(nombre)}.{_slug(apellido)}@{DOMINIO}", "HUMANA",
            nombre, apellido, f"9020{i:04d}", f"115555{i + 200:04d}",
            _DIRECCIONES_DONANTE[i % len(_DIRECCIONES_DONANTE)], "relleno",
            dias(azar.randint(25, VENTANA_DIAS)), genero=genero,
            nacimiento=date(1960 + azar.randint(0, 40), azar.randint(1, 12), azar.randint(1, 28)),
            mostrar_nombre=azar.random() > 0.25,
        ))
    for i, (razon, rubro) in enumerate(_EMPRESAS_RELLENO):
        donantes.append(Donante(
            f"empresa{i}", f"contacto@{_slug(razon.split()[0])}{i}.{DOMINIO}", "JURIDICA", razon,
            None, f"3090200{i:03d}1", f"115555{i + 300:04d}",
            _DIRECCIONES_DONANTE[(i + 2) % len(_DIRECCIONES_DONANTE)], "relleno",
            dias(azar.randint(60, VENTANA_DIAS)), rubro=rubro, contacto=("Equipo", "RSE"),
        ))

    for i, (nombre, apellido, genero) in enumerate(_MISIONEROS):
        perfil = "misionero_a" if i < len(_MISIONEROS) // 2 else "misionero_b"
        indice = i % (len(_MISIONEROS) // 2)
        donantes.append(Donante(
            f"misionero{i:02d}", f"{_slug(nombre)}.{_slug(apellido)}@{DOMINIO}", "HUMANA",
            nombre, apellido, f"9030{i:04d}", f"115555{i + 500:04d}",
            _DIRECCIONES_DONANTE[(i + 1) % len(_DIRECCIONES_DONANTE)], perfil,
            _fechas_misionero(perfil, indice, hoy)[0] - timedelta(days=1), genero=genero,
            nacimiento=date(1970 + i, (i * 5) % 12 + 1, 3 + i)))

    entidades = [
        Entidad("comedor", f"comedor.lospibes@{DOMINIO}", "Comedor Los Pibes de Barracas", "ONG",
                "Comedor comunitario", "30900100011", "1155550201", _DIRECCIONES_ENTIDAD[0],
                ("Rosa", "Villalba"), "activa", dias(VENTANA_DIAS + 10), protagonista=True),
        Entidad("escuela", f"escuela12@{DOMINIO}", "Escuela Primaria N.º 12 D.E. 11",
                "INSTITUCION", "Educación primaria", "30900100021", "1155550202",
                _DIRECCIONES_ENTIDAD[1], ("Jorge", "Albornoz"), "activa", dias(VENTANA_DIAS),
                protagonista=True),
        Entidad("hogar", f"hogar.esperanza@{DOMINIO}", "Hogar de Tránsito La Esperanza", "ONG",
                "Hogar para mujeres y niños", "30900100031", "1155550203",
                _DIRECCIONES_ENTIDAD[2], ("Marta", "Correa"), "pendiente", dias(2),
                protagonista=True),
        Entidad("fundacion", f"fundacion.horizonte@{DOMINIO}", "Fundación Horizonte Platense",
                "ONG", "Inclusión laboral", "30900100041", "1155550204", _DIRECCIONES_ENTIDAD[3],
                ("Pablo", "Sánchez"), "baja", dias(VENTANA_DIAS - 20), protagonista=True),
    ]
    for i, (razon, tipo, rubro) in enumerate(_ENTIDADES_RELLENO):
        nombre, apellido, _ = _NOMBRES[(i * 7 + 3) % len(_NOMBRES)]
        entidades.append(Entidad(
            f"entidad{i}", f"{_slug(razon.split()[0])}{i}@{DOMINIO}", razon, tipo, rubro,
            f"309003{i:04d}1", f"115555{i + 400:04d}", _DIRECCIONES_ENTIDAD[i + 4],
            (nombre, apellido), "activa", dias(azar.randint(120, VENTANA_DIAS)),
        ))
    return donantes, entidades


def _slug(texto: str) -> str:
    reemplazos = str.maketrans("áéíóúüñÁÉÍÓÚÜÑ", "aeiouunAEIOUUN")
    return "".join(c for c in texto.translate(reemplazos).lower() if c.isalnum())


def _necesidades(hoy: date, entidades: list[Entidad], ids: _Ids,
                 azar: random.Random) -> list[Necesidad]:
    necesidades: list[Necesidad] = []
    inicio_ventana = hoy - timedelta(days=VENTANA_DIAS)

    def agregar(entidad: Entidad, plantilla: tuple, estado: str, cobertura: float, n: int):
        sub, tipo, cantidad, extra, descripcion = plantilla[:5]
        alta = max(entidad.alta, inicio_ventana)
        if tipo == "RECURRENTE":
            # Como la API: la necesidad y sus períodos empiezan a medianoche
            inicio = datetime.combine(alta + timedelta(days=azar.randint(0, 10)), time(0))
            necesidad = Necesidad(ids.nuevo(), f"{entidad.clave}-{n}", entidad.clave, sub, tipo,
                                  cantidad, descripcion, inicio, periodicidad=extra,
                                  estado_esperado=estado, cobertura=cobertura)
            # Períodos encadenados desde el alta hasta uno que cubra hoy (el job de medianoche
            # no tiene nada que renovar)
            desde = inicio
            while True:
                hasta = (datetime.combine(_sumar_meses(desde.date(), 1), time(0))
                         if extra == "MENSUAL" else desde + PERIODOS[extra])
                necesidad.periodos.append(Periodo(ids.nuevo(), desde, hasta, cantidad))
                if hasta.date() > hoy:
                    break
                desde = hasta
        else:
            limite = hoy + timedelta(days=extra)
            duracion = 70 if extra > 0 else 60
            inicio = datetime.combine(max(alta, limite - timedelta(days=duracion + 30)), time(0))
            necesidad = Necesidad(ids.nuevo(), f"{entidad.clave}-{n}", entidad.clave, sub, tipo,
                                  cantidad, descripcion, inicio, fecha_limite=limite,
                                  estado_esperado=estado, cobertura=cobertura)
        if estado == "DADA_DE_BAJA":
            # La del comedor se da de baja en vivo, con una entrega pendiente que se cancela
            necesidad.baja_en_vivo = True
            necesidad.cobertura = 0.0
        if entidad.estado == "baja":
            necesidad.baja = hoy - timedelta(days=45)
            necesidad.activa = False
            necesidad.estado_esperado = "DADA_DE_BAJA"
        necesidades.append(necesidad)

    for entidad in entidades:
        if entidad.estado == "pendiente":
            continue
        if entidad.clave == "comedor":
            for n, p in enumerate(_NECESIDADES_PROTAGONISTA):
                agregar(entidad, p, p[5], p[6], n)
        elif entidad.clave == "escuela":
            for n, p in enumerate(_NECESIDADES_ESCUELA):
                agregar(entidad, p, p[5], p[6], n)
        else:
            elegidas = azar.sample([p for p in _NECESIDADES_RELLENO
                                    if p[0] not in RESERVADAS_VIVO], 4)
            for n, p in enumerate(elegidas):
                limite_vencido = p[1] == "EXTRAORDINARIA" and azar.random() < 0.15
                plantilla = (p[0], p[1], p[2] * 3, -15 if limite_vencido else p[3], p[4])
                agregar(entidad, plantilla, "TERMINADA" if limite_vencido else "VIGENTE",
                        azar.uniform(0.3, 0.8), n)
    return necesidades


# Días hacia atrás de cada donación según el perfil. La estrella dona todos los meses (para
# las rachas) y la empresa hace pocas pero grandes.
_CALENDARIOS = {
    "constante": [128, 101, 74, 45, 17, 6],
    "empresa": [165, 133, 98, 63, 31, 9],
    "inactivo": [140, 96, 41],
    "nuevo": [],
}


# La estrella dona todos los meses, anclada al calendario (las rachas cuentan meses): Racha
# Inicial se cumple el tercer mes y ese mismo mes Explorador, Gran Aporte y el arranque de
# Constancia, que hoy va 5 de 6 ("le falta un mes para ser Transformadora").
_ESTRELLA = [(-6, 20), (-6, 27), (-5, 8), (-5, 22), (-4, 3), (-4, 6), (-4, 9), (-4, 12),
             (-3, 5), (-3, 19), (-2, 3), (-2, 17), (-1, 1), (-1, 15)]


def _fechas_misionero(perfil: str, indice: int, hoy: date) -> list[date]:
    """Las donaciones de quien completa misiones, ancladas a los meses (las rachas cuentan meses).

    El grupo A dona desde seis meses atrás: Racha Inicial se cumple en el mes -4, Explorador (tres
    categorías en una donación) en el -3 y Gran Aporte (50 bienes juntos) en el -2. El grupo B
    dona desde el -3 y cumple Racha Inicial en el -1. Así cada mes cerrado desde el -4 tiene al
    menos 10 puestos (12 por grupo, de sobra). La última donación cae en las últimas dos semanas
    y media, como la de los de relleno: el aviso de inactividad no los alcanza.
    """
    base = hoy.replace(day=1)
    meses = [-6, -5, -4, -3, -2] if perfil == "misionero_a" else [-3, -2, -1]
    fechas = []
    for n, mes in enumerate(meses):
        dia = 20 + indice % 6 if mes == -6 else 2 + (indice * 3 + n * 5) % 24
        fechas.append(_sumar_meses(base, mes).replace(day=dia))
    fechas.append(hoy - timedelta(days=2 + (indice * 5) % 17))
    return sorted(fechas)


def _calendario(donante: Donante, hoy: date, azar: random.Random) -> list[int]:
    if donante.perfil == "estrella":
        fechas = [_sumar_meses(hoy.replace(day=1), meses).replace(day=dia)
                  for meses, dia in _ESTRELLA]
        if hoy.day > 4:
            fechas.append(hoy - timedelta(days=3))
        return [(hoy - f).days for f in fechas]
    if donante.perfil in _CALENDARIOS:
        return _CALENDARIOS[donante.perfil]
    if donante.perfil.startswith("misionero"):
        indice = int(donante.clave.removeprefix("misionero")) % (len(_MISIONEROS) // 2)
        return [(hoy - f).days for f in _fechas_misionero(donante.perfil, indice, hoy)]
    desde = (hoy - donante.alta).days
    cantidad = azar.randint(1, 5)
    dias = sorted({azar.randint(3, max(4, desde)) for _ in range(cantidad)}, reverse=True)
    # La última donación de cada donante de relleno es de las últimas dos semanas y media: el
    # job diario de inactividad (más de 20 días) no los avisa todos los días
    if dias[-1] > 18:
        dias[-1] = azar.randint(2, 18)
    return sorted(set(dias), reverse=True)


# Cinco subcategorías de cinco categorías: la donación de Explorador y la de Gran Aporte llevan
# de sobra (tres categorías, 50 bienes) porque lo que queda en el depósito sin destino se
# reubica después en otra subcategoría y con otra cantidad (_acotar_stock)
_VARIAS = ["No Perecederos", "Ropa de Invierno", "Cuadernos y papelería", "Higiene personal",
           "Toallas"]

# Sesgo de lo que dona cada protagonista, por subcategoría
_PREFERENCIAS = {
    "estrella": [
        ["No Perecederos", "Frutas"], ["Ropa de Invierno", "Higiene personal", "Frutas"],
        ["Cuadernos y papelería"], ["No Perecederos", "Ropa de cama"],
        ["Higiene personal"], ["Frutas", "Toallas", "Juegos de mesa"],
        ["Cuadernos y papelería", "No Perecederos"], ["Ropa de Invierno"],
        ["Calefacción", "No Perecederos"], ["Frutas"],
        ["Higiene personal", "Mochilas y cartucheras"], ["No Perecederos"],
        ["Ropa de Invierno", "Frutas"], ["Muebles Escolares"], ["Frutas", "Higiene personal"],
        ["No Perecederos"],
    ],
    # Una por mes: la cuarta junta tres categorías (Explorador) y la quinta pasa los 50 bienes
    # (Gran Aporte); ver _fechas_misionero
    "misionero_a": [
        ["No Perecederos"], ["Higiene personal"], ["Cuadernos y papelería"], _VARIAS, _VARIAS,
        ["Ropa de Invierno"],
    ],
    "misionero_b": [
        ["No Perecederos"], ["Higiene personal"], ["Ropa de Invierno"],
        ["Cuadernos y papelería"],
    ],
    "empresa": [
        ["No Perecederos", "Limpieza del hogar"], ["Cuadernos y papelería", "Higiene personal"],
        ["No Perecederos", "Ropa de cama"], ["Muebles Escolares", "Cocina"],
        ["Ropa de Invierno", "No Perecederos"], ["Mochilas y cartucheras", "Juegos de mesa"],
    ],
}


class _Constructor:
    def __init__(self, hoy: date, semilla: int):
        self.hoy = hoy
        self.azar = random.Random(semilla)
        self.ids = _Ids(random.Random(semilla * 7919 + 1))
        self.inicio = hoy - timedelta(days=VENTANA_DIAS)

    def construir(self) -> Mundo:
        donantes, entidades = _elenco(self.hoy, self.azar)
        necesidades = _necesidades(self.hoy, entidades, self.ids, self.azar)
        camiones = [
            Camion(self.ids.nuevo(), "AE123BD", 18.0, 4000.0, 3.2),
            Camion(self.ids.nuevo(), "AF456CG", 12.0, 2500.0, 2.9),
            Camion(self.ids.nuevo(), "AD789KM", 25.0, 6000.0, 3.6),
            Camion(self.ids.nuevo(), "KLM345", 9.0, 1500.0, 2.5, habilitado=False),
        ]
        choferes = [
            Chofer(self.ids.nuevo(), "Héctor", "Maldonado", "B2-90010001", "1155550301"),
            Chofer(self.ids.nuevo(), "Silvina", "Rojas", "C1-90010002", "1155550302"),
            Chofer(self.ids.nuevo(), "Damián", "Funes", "C1-90010003", "1155550303"),
        ]
        mundo = Mundo(self.hoy, self.inicio, donantes, entidades, necesidades, [], camiones,
                      choferes, [], {}, dict(_DEPOSITOS))
        for donante in donantes:
            mundo.donaciones.extend(self._donaciones_de(donante, mundo))
        mundo.donaciones.sort(key=lambda d: d.fecha)
        self._destinar(mundo)
        self._ajustar_cumplidas(mundo)
        self._liberar_reservadas(mundo)
        self._acotar_stock(mundo)
        self._por_vencer(mundo)
        self._rutas(mundo)
        for donante in donantes:
            mundo.incentivos[donante.clave] = simular_incentivos(mundo, donante.clave)
        return mundo

    def _donaciones_de(self, donante: Donante, mundo: Mundo) -> list[Donacion]:
        donaciones = []
        dias = _calendario(donante, self.hoy, self.azar)
        preferencias = _PREFERENCIAS.get(donante.perfil)
        demandadas = sorted({n.subcategoria for n in mundo.necesidades})
        for n, dias_atras in enumerate(dias):
            dia = self.hoy - timedelta(days=dias_atras)
            fecha = _a_las(dia, self.azar.randint(9, 19), self.azar.choice([0, 10, 25, 40, 55]))
            if preferencias:
                subcategorias = preferencias[n % len(preferencias)]
            else:
                # Casi siempre, lo que piden las entidades de relleno (sin tope); a veces, lo de
                # las protagonistas, que tienen su cobertura planeada
                solo_relleno = self.azar.random() < 0.75
                abiertas = sorted({
                    x.subcategoria for x in mundo.necesidades
                    if x.inicio <= fecha and x.estado_esperado == "VIGENTE"
                    and not (solo_relleno and mundo.entidad(x.entidad).protagonista)
                    and (x.fecha_limite is None or x.fecha_limite > dia + timedelta(days=5))
                }) or demandadas
                subcategorias = self.azar.sample(
                    abiertas, min(len(abiertas), self.azar.choice([1, 1, 2])))
            grande = (donante.perfil == "empresa" or (donante.perfil == "estrella" and n == 6)
                      or (donante.perfil == "misionero_a" and n == 4))
            donacion = Donacion(
                self.ids.nuevo(), donante.clave, fecha, "", self.azar.choice(list(_DEPOSITOS)),
                nombre_oculto=not donante.mostrar_nombre or (
                    donante.perfil == "empresa" and n % 2 == 0))
            for sub in subcategorias:
                donacion.independientes.append(self._item(sub, dia, grande))
            donacion.descripcion = _descripcion_de(donacion)
            donaciones.append(donacion)
        return donaciones

    def _item(self, nombre_sub: str, dia: date, grande: bool) -> Independiente:
        sub = catalogo.subcategoria(nombre_sub)
        categoria = catalogo.categoria_de(nombre_sub)
        bien = self.azar.choice(sub.bienes)
        cantidad = self.azar.randint(bien.minimo, bien.maximo)
        if grande:
            cantidad = max(cantidad, bien.maximo) * (2 if bien.maximo < 30 else 1)
        estado_bien = None
        if categoria.con_uso:
            estado_bien = self.azar.choices(["NUEVO", "USADO", "DESGASTADO"], [5, 4, 1])[0]
        vencimiento = None
        if categoria.con_vencimiento:
            vencimiento = dia + timedelta(days=self.azar.randint(60, 300)
                                          if nombre_sub == "No Perecederos"
                                          else self.azar.randint(12, 20))
        return Independiente(self.ids.nuevo(), self.ids.nuevo(), bien.descripcion, nombre_sub,
                             cantidad, bien.peso_kg, bien.volumen_m3, estado_bien, vencimiento)

    def _destinar(self, mundo: Mundo) -> None:
        """Decide qué pasó con cada donación independiente, de la más vieja a la más nueva."""
        recibido: dict[tuple[str, str | None], int] = {}
        for donacion in mundo.donaciones:
            for ind in donacion.independientes:
                registro = donacion.fecha + timedelta(minutes=2)
                ind.pasos.append(Paso("EN_DEPOSITO", registro, "sistema"))
                dias_desde = (self.hoy - donacion.fecha.date()).days
                if dias_desde <= 3 or (dias_desde <= 6 and self.azar.random() < 0.5):
                    continue  # stock para el algoritmo de hoy
                if (ind.subcategoria == "Frutas" and dias_desde > 25
                        and self.azar.random() < 0.15):
                    vence = datetime.combine(ind.vencimiento + timedelta(days=1), time(7))
                    ind.pasos.append(Paso("VENCIDA", vence, "sistema",
                                          "Venció en el depósito sin asignarse"))
                    continue
                necesidad = self._necesidad_para(mundo, ind, donacion, recibido)
                if necesidad is None:
                    if ind.vencimiento and ind.vencimiento < self.hoy:
                        vence = datetime.combine(ind.vencimiento + timedelta(days=1), time(7))
                        ind.pasos.append(Paso("VENCIDA", vence, "sistema",
                                              "Venció en el depósito sin asignarse"))
                    elif ind.vencimiento and ind.vencimiento <= self.hoy:
                        ind.vencimiento = self.hoy + timedelta(days=3)
                    continue
                self._entregar(mundo, ind, donacion, necesidad, recibido)

    @staticmethod
    def _ajustar_cumplidas(mundo: Mundo) -> None:
        """Una extraordinaria que tenía que verse cumplida y no llegó: pide lo que recibió."""
        for n in mundo.necesidades:
            if n.estado_esperado != "CUMPLIDA":
                continue
            recibido = sum(i.cantidad - (i.cantidad_no_recibida or 0)
                           for _, i in mundo.independientes()
                           if i.necesidad == n.id and i.estado_actual == "ENTREGADA")
            if 0 < recibido < n.cantidad:
                n.cantidad = recibido

    @staticmethod
    def _entra(recibido: int, cantidad: int, objetivo: int, cobertura: float,
               puede_cumplirse: bool) -> bool:
        """Si la donación cabe en lo que la historia tiene que cubrir de la necesidad."""
        meta = objetivo * cobertura
        if recibido >= meta:
            return False
        if puede_cumplirse:
            return True
        # Una vigente o terminada nunca llega al objetivo: si llegara, se vería cumplida
        return recibido + cantidad < objetivo

    @staticmethod
    def _liberar_reservadas(mundo: Mundo) -> None:
        """Lo que quedó en el depósito de una subcategoría reservada pasa a una hermana."""
        for _, ind in mundo.independientes():
            if ind.estado_actual != "EN_DEPOSITO" or ind.subcategoria not in RESERVADAS_VIVO:
                continue
            hermana = next(s for s in catalogo.categoria_de(ind.subcategoria).subcategorias
                           if s.nombre not in RESERVADAS_VIVO)
            bien = hermana.bienes[0]
            ind.subcategoria, ind.descripcion = hermana.nombre, bien.descripcion
            ind.peso_kg, ind.volumen_m3 = bien.peso_kg, bien.volumen_m3
            ind.cantidad = max(bien.minimo, min(ind.cantidad, bien.maximo))

    @staticmethod
    def _admite(n: Necesidad, hoy: date) -> bool:
        return n.activa and (n.tipo == "RECURRENTE" or (n.fecha_limite or hoy) >= hoy) and \
            n.estado_esperado != "CUMPLIDA"

    def _acotar_stock(self, mundo: Mundo) -> None:
        """Deja stock con candidatas para pocas necesidades (STOCK_CON_CANDIDATAS).

        Cada algoritmo de Donaciones arma hasta 10 propuestas por corrida y el que prioriza a las
        menos atendidas deja a las protagonistas al final: con mucho stock compitiendo, la
        donación de hoy no llega a la ruta. El resto del stock pasa a subcategorías que hoy no pide
        nadie (sigue en el depósito, visible para el admin).
        """
        por_sub: dict[str, int] = {}
        for n in mundo.necesidades:
            if self._admite(n, self.hoy):
                por_sub[n.subcategoria] = por_sub.get(n.subcategoria, 0) + 1
        libres = [s for s in catalogo.todas_las_subcategorias()
                  if s not in por_sub and s not in RESERVADAS_VIVO]
        stock = [i for _, i in mundo.independientes() if i.estado_actual == "EN_DEPOSITO"]
        # Primero los alimentos (los que pueden vencer), después el resto
        candidatas = sorted({i.subcategoria for i in stock if i.subcategoria in por_sub},
                            key=lambda s: (not catalogo.categoria_de(s).con_vencimiento, s))
        quedan, usadas = set(), 0
        for sub in candidatas:
            if usadas + por_sub[sub] <= STOCK_CON_CANDIDATAS:
                quedan.add(sub)
                usadas += por_sub[sub]
        for n, ind in enumerate(i for i in stock if i.subcategoria in por_sub
                                and i.subcategoria not in quedan):
            destino = catalogo.subcategoria(libres[n % len(libres)])
            categoria = catalogo.categoria_de(destino.nombre)
            bien = destino.bienes[0]
            ind.subcategoria, ind.descripcion = destino.nombre, bien.descripcion
            ind.peso_kg, ind.volumen_m3 = bien.peso_kg, bien.volumen_m3
            ind.cantidad = max(bien.minimo, min(ind.cantidad, bien.maximo))
            registro = ind.pasos[0].momento.date()
            ind.vencimiento = registro + timedelta(days=200) if categoria.con_vencimiento else None
            if categoria.con_uso and ind.estado_bien is None:
                ind.estado_bien = "USADO"
            elif not categoria.con_uso:
                ind.estado_bien = None

    def _por_vencer(self, mundo: Mundo) -> None:
        """Dos alimentos del depósito vencen en tres días: el admin los ve en su inicio."""
        en_deposito = [i for _, i in mundo.independientes()
                       if i.estado_actual == "EN_DEPOSITO" and i.vencimiento]
        for ind, dias in zip(en_deposito[:2], (2, 3)):
            ind.vencimiento = self.hoy + timedelta(days=dias)

    def _necesidad_para(self, mundo: Mundo, ind: Independiente, donacion: Donacion,
                        recibido: dict) -> Necesidad | None:
        momento = donacion.fecha + timedelta(days=1)
        candidatas = []
        for n in mundo.necesidades:
            if n.subcategoria != ind.subcategoria or n.inicio > momento or n.cobertura <= 0:
                continue
            if n.baja and momento.date() >= n.baja:
                continue
            if n.tipo == "EXTRAORDINARIA":
                if n.fecha_limite and momento.date() > n.fecha_limite - timedelta(days=4):
                    continue
                if not self._entra(recibido.get((n.id, None), 0), ind.cantidad, n.cantidad,
                                   n.cobertura, n.estado_esperado == "CUMPLIDA"):
                    continue
            else:
                periodo = n.periodo_en(momento)
                if periodo is None:
                    continue
                actual = periodo is n.periodos[-1]
                if not self._entra(recibido.get((n.id, periodo.id), 0), ind.cantidad,
                                   periodo.objetivo, n.cobertura if actual else 1.3, not actual):
                    continue
            candidatas.append(n)
        if not candidatas:
            # Sin lugar en lo planeado: cualquier necesidad vigente de esa subcategoría de una
            # entidad de relleno la recibe igual (un depósito con stock viejo no se ve real)
            candidatas = [
                n for n in mundo.necesidades
                if n.subcategoria == ind.subcategoria and n.inicio <= momento
                and n.estado_esperado == "VIGENTE" and n.cobertura > 0
                and not mundo.entidad(n.entidad).protagonista
                and mundo.entidad(n.entidad).estado == "activa"
                and (n.tipo == "RECURRENTE" and n.periodo_en(momento) is not n.periodos[-1]
                     or recibido.get((n.id, None if n.tipo == "EXTRAORDINARIA"
                                      else n.periodos[-1].id), 0) + ind.cantidad < n.cantidad)
                and (n.tipo == "RECURRENTE" or (n.fecha_limite and momento.date()
                                                < n.fecha_limite - timedelta(days=4)))
            ]
        if not candidatas:
            return None
        # Los protagonistas reciben más: así su historia es la más rica
        pesos = [3 if mundo.entidad(n.entidad).protagonista else 1 for n in candidatas]
        return self.azar.choices(candidatas, pesos)[0]

    def _entregar(self, mundo, ind, donacion, necesidad, recibido) -> None:
        asignada = donacion.fecha + timedelta(days=self.azar.randint(1, 3))
        asignada = asignada.replace(hour=self.azar.randint(12, 20), minute=0)
        salida_dia = asignada.date() + timedelta(days=self.azar.randint(1, 2))
        if salida_dia >= self.hoy:
            return  # no llegó a salir: queda en el depósito, como stock de hoy
        periodo = necesidad.periodo_en(asignada) if necesidad.tipo == "RECURRENTE" else None
        if necesidad.tipo == "RECURRENTE" and periodo is None:
            periodo = necesidad.periodos[-1]
        ind.necesidad = necesidad.id
        ind.periodo = periodo.id if periodo else None
        ind.entidad = necesidad.entidad
        ind.propuesta = self.ids.nuevo()
        ind.pasos.append(Paso("ASIGNACION_REALIZADA", asignada, "admin"))
        lista = _a_las(salida_dia, 2, 5)
        ind.pasos.append(Paso("LISTA_PARA_ENTREGAR", lista, "logistica-service"))
        salida = _a_las(salida_dia, 9, self.azar.choice([0, 15, 30]))
        llegada = salida + timedelta(minutes=self.azar.randint(35, 160))
        ind.pasos.append(Paso("EN_TRASLADO", salida, "logistica-service"))
        ind.patente = self.azar.choice(mundo.camiones[:3]).patente
        if self.azar.random() < 0.07 and salida_dia + timedelta(days=2) < self.hoy:
            # Primero falla y se reprograma; vuelve a salir dos días después
            ind.fallo = self.azar.choice([
                "No había nadie para recibir en la sede",
                "La sede estaba cerrada por un corte de calle",
            ])
            ind.pasos.append(Paso("EN_PUERTA", llegada, "logistica-service"))
            ind.pasos.append(Paso("ENTREGA_FALLIDA", llegada + timedelta(minutes=10),
                                  "logistica-service", ind.fallo))
            ind.pasos.append(Paso("ASIGNACION_REALIZADA", llegada + timedelta(minutes=10),
                                  "logistica-service", "Entrega reprogramada"))
            nuevo_dia = salida_dia + timedelta(days=2)
            ind.pasos.append(Paso("LISTA_PARA_ENTREGAR", _a_las(nuevo_dia, 2, 5),
                                  "logistica-service"))
            salida = _a_las(nuevo_dia, 9, 30)
            llegada = salida + timedelta(minutes=self.azar.randint(35, 160))
            ind.pasos.append(Paso("EN_TRASLADO", salida, "logistica-service"))
        ind.pasos.append(Paso("EN_PUERTA", llegada, "logistica-service"))
        entrega = llegada + timedelta(minutes=self.azar.randint(5, 25))
        ind.pasos.append(Paso("ENTREGADA", entrega, "logistica-service", patente=ind.patente))
        ind.fecha_entrega = entrega
        ind.fotos = self.azar.choice([1, 1, 2, 2, 3])
        clave_recibido = (necesidad.id, ind.periodo)
        if self.azar.random() < 0.08 and ind.cantidad >= 4:
            ind.reporte = self.azar.choice(["INCOMPLETA", "DANADA"])
            faltante = max(1, ind.cantidad // 5)
            ind.cantidad_no_recibida = faltante
            ind.reporte_detalle = (
                f"Llegaron {ind.cantidad - faltante} de {ind.cantidad}"
                if ind.reporte == "INCOMPLETA"
                else f"{faltante} llegaron dañados por la lluvia")
        recibido[clave_recibido] = recibido.get(clave_recibido, 0) + ind.cantidad - (
            ind.cantidad_no_recibida or 0)

    def _rutas(self, mundo: Mundo) -> None:
        """Agrupa las salidas finales de cada día en rutas, por camión."""
        por_dia: dict[tuple[date, str], list[Independiente]] = {}
        for _, ind in mundo.independientes():
            salida = _ultima(ind, "EN_TRASLADO")
            if ind.estado_actual != "ENTREGADA" or salida is None:
                continue
            por_dia.setdefault((salida.momento.date(), ind.patente), []).append(ind)
        camiones = {c.patente: c for c in mundo.camiones}
        for n, ((dia, patente), inds) in enumerate(sorted(por_dia.items())):
            inds.sort(key=lambda i: i.fecha_entrega)
            inicio = min(_ultima(i, "EN_TRASLADO").momento for i in inds)
            fin = max(i.fecha_entrega for i in inds) + timedelta(minutes=40)
            ruta = Ruta(self.ids.nuevo(), dia, camiones[patente].id,
                        mundo.choferes[n % len(mundo.choferes)].id, inicio, fin)
            for ind in inds:
                ind.id_ruta = ruta.id
                ind.id_entrega = self.ids.nuevo()
                ruta.entregas.append(ind.id)
            mundo.rutas.append(ruta)


def _ultima(ind: Independiente, estado: str) -> Paso | None:
    encontrados = [p for p in ind.pasos if p.estado == estado]
    return encontrados[-1] if encontrados else None


def _descripcion_de(donacion: Donacion) -> str:
    partes = [f"{i.cantidad} {i.descripcion.lower()}" for i in donacion.independientes]
    if len(partes) == 1:
        return partes[0].capitalize()
    return (", ".join(partes[:-1]) + " y " + partes[-1]).capitalize()


def detalle_de(ind: Independiente) -> str:
    """Cómo nombran los avisos a la donación independiente."""
    return f"{ind.cantidad} {ind.descripcion.lower()}"


# --- Incentivos: la misma lógica de misiones que incentivos-service ------------------------


def simular_incentivos(mundo: Mundo, clave: str) -> Incentivos:
    resultado = Incentivos(clave, misiones=[Mision(*m) for m in MISIONES])
    eventos: list[tuple[datetime, int, object]] = []
    for donacion in mundo.donaciones:
        if donacion.donante != clave:
            continue
        eventos.append((donacion.fecha + timedelta(minutes=3), 0, donacion))
        for ind in donacion.independientes:
            if ind.estado_actual == "ENTREGADA":
                eventos.append((ind.fecha_entrega, 1, ind))
    eventos.sort(key=lambda e: (e[0], e[1]))
    for momento, tipo, dato in eventos:
        if tipo == 0:
            _registrar_donacion(resultado, dato)
        else:
            _registrar_exitosa(resultado, dato, momento)
    return resultado


def _activa(inc: Incentivos) -> Mision | None:
    pendientes = [m for m in inc.misiones if m.categoria == inc.categoria and not m.completada]
    return min(pendientes, key=lambda m: m.numero) if pendientes else None


def _registrar_donacion(inc: Incentivos, donacion: Donacion) -> None:
    dia = donacion.fecha.date()
    inc.total_donaciones += 1
    inc.ultima_donacion = max(inc.ultima_donacion or dia, dia)
    mes = f"{dia.year:04d}-{dia.month:02d}"
    inc.por_mes[mes] = inc.por_mes.get(mes, 0) + 1
    mision = _activa(inc)
    if mision is None:
        return
    if mision.tipo == "RACHA":
        mision.progreso = _racha(mision, (dia.year, dia.month))
    elif mision.tipo == "COMPLETITUD":
        mision.categorias_donadas.update(c.lower() for c in donacion.categorias)
        mision.progreso = len(mision.categorias_donadas)
    elif mision.tipo == "HABIL":
        mision.progreso = max(mision.progreso, donacion.cantidad_bienes)
    elif mision.tipo == "EXITOSAS":
        mision.fecha_ultima_donacion = dia
    if mision.progreso >= mision.objetivo:
        _completar(inc, mision, dia)


def _registrar_exitosa(inc: Incentivos, ind: Independiente, momento: datetime) -> None:
    inc.total_exitosas += 1
    if ind.entidad:
        inc.organizaciones.add(ind.entidad)
    mision = _activa(inc)
    if mision is None or mision.tipo != "EXITOSAS":
        return
    mision.progreso += 1
    if mision.progreso >= mision.objetivo:
        _completar(inc, mision, mision.fecha_ultima_donacion or momento.date())


def _racha(mision: Mision, mes: tuple[int, int]) -> int:
    def indice(m: tuple[int, int]) -> int:
        return m[0] * 12 + m[1] - 1

    if mision.ultimo_mes is None:
        mision.ultimo_mes = mes
        return 1
    ultimo = indice(mision.ultimo_mes)
    primero = ultimo - (mision.progreso - 1)
    actual = indice(mes)
    if primero <= actual <= ultimo:
        return mision.progreso
    if actual == ultimo + 1:
        mision.ultimo_mes = mes
        return mision.progreso + 1
    if actual == primero - 1:
        return mision.progreso + 1
    if actual < primero:
        return mision.progreso
    mision.ultimo_mes = mes
    return 1


def _completar(inc: Incentivos, mision: Mision, dia: date) -> None:
    mision.completada = True
    mision.fecha_completada = dia
    inc.insignias.append((mision.insignia, dia))
    en_categoria = [m for m in inc.misiones if m.categoria == inc.categoria]
    if all(m.completada for m in en_categoria) and inc.categoria in SIGUIENTE:
        nueva = SIGUIENTE[inc.categoria]
        inc.ascensos.append((inc.categoria, nueva, dia))
        inc.categoria = nueva


def ranking_de(mundo: Mundo, anio: int, mes: int) -> list[tuple[str, int]]:
    """Misiones cumplidas en el mes por donante, de mayor a menor (como RankingService)."""
    filas = []
    for clave, inc in mundo.incentivos.items():
        cantidad = sum(1 for m in inc.misiones if m.completada and m.fecha_completada
                       and (m.fecha_completada.year, m.fecha_completada.month) == (anio, mes))
        if cantidad:
            filas.append((clave, cantidad))
    filas.sort(key=lambda f: (-f[1], mundo.donante(f[0]).denominacion))
    return filas


def rankings_cerrados(mundo: Mundo) -> list[tuple[date, list[tuple[str, int]]]]:
    """Los meses cerrados de la ventana que tienen ranking, con sus puestos, del más viejo al más
    nuevo. Los primeros meses no lo tienen: nadie puede cumplir una misión antes de tres meses
    de racha, y el ranking se arma al cerrar el primer mes con misiones cumplidas."""
    primero = mundo.hoy.replace(day=1)
    mes = mundo.inicio.replace(day=1)
    cerrados = []
    while mes < primero:
        filas = ranking_de(mundo, mes.year, mes.month)
        if filas:
            cerrados.append((mes, filas))
        mes = (mes + timedelta(days=32)).replace(day=1)
    return cerrados


def construir_mundo(hoy: date, semilla: int) -> Mundo:
    return _Constructor(hoy, semilla).construir()
