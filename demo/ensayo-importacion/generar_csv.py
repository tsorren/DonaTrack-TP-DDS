"""Genera el CSV de donantes para el ensayo de importación (R11-03).

Solo biblioteca estándar. Con la misma semilla sale siempre el mismo archivo, byte a byte. Todo es
ficticio: correos en @ensayo.donatrack.test, DNI de la serie 88 y CUIT de la serie 3088, fuera de la
serie 90 que usa el seeder de demo, para que el ensayo no se pise con la historia cargada. Para
repetir el ensayo sobre la misma base se elige otra serie (--serie), así no se cruzan los documentos.

Qué trae el archivo (la guía está en docs/testing/ensayo-importacion-20000.md):
  - donantes nuevos, humanos y empresas, cada uno con su documento y su correo;
  - duplicados: repiten el documento y el correo de un donante anterior con otro teléfono, así que
    Donaciones los cuenta como actualizados y no como creados;
  - filas con error: con una columna de menos o de más (formato inválido), o con el correo de otro
    donante y un documento distinto (duplicado en conflicto).

Lo mismo que lee Donaciones: una línea por fila, columnas separadas por coma, sin comillas.
"""

from __future__ import annotations

import argparse
import random
import re
import sys
import unicodedata
from dataclasses import dataclass
from pathlib import Path

CABECERA = "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono"
BOM = "﻿"
DOMINIO = "ensayo.donatrack.test"

FILAS_POR_DEFECTO = 20000
DUPLICADOS_POR_DEFECTO = 400
CON_ERROR_POR_DEFECTO = 200
SEMILLA_POR_DEFECTO = 2026
SERIE_POR_DEFECTO = 88
SERIE_DEL_SEEDER = 90
PORCENTAJE_EMPRESAS = 15

_NOMBRES = [
    "Ana", "Benjamín", "Camila", "Diego", "Elena", "Facundo", "Gabriela", "Hernán", "Inés", "Joaquín",
    "Karina", "Lautaro", "Martina", "Nicolás", "Olivia", "Pablo", "Rocío", "Santiago", "Tamara",
    "Valentín", "Agustina", "Bruno", "Carolina", "Damián", "Emilia", "Federico", "Guadalupe", "Ignacio",
    "Julieta", "Lucas", "Mariana", "Nahuel", "Paula", "Ramiro", "Sofía", "Tomás", "Violeta", "Matías",
]
_APELLIDOS = [
    "Navarro", "Castro", "García", "Torres", "Fernández", "Gómez", "Ruiz", "Pereyra", "Sosa", "Molina",
    "Acosta", "Benítez", "Cabrera", "Domínguez", "Escobar", "Ferreyra", "Giménez", "Herrera", "Ibáñez",
    "Juárez", "Luna", "Medina", "Núñez", "Ortiz", "Peralta", "Quiroga", "Ramírez", "Silva", "Vega",
    "Villalba", "Aguirre", "Bustos", "Correa", "Díaz", "Figueroa", "Godoy", "Ledesma", "Maldonado",
]
_EMPRESA_RUBROS = [
    "Panadería", "Distribuidora", "Ferretería", "Librería", "Verdulería", "Transportes", "Textil",
    "Almacén", "Droguería", "Cooperativa", "Imprenta", "Carpintería", "Lavandería", "Papelera",
]
_EMPRESA_NOMBRES = [
    "Los Andes", "del Sur", "San Martín", "La Esquina", "Río Plata", "Don Pedro", "El Progreso",
    "Cuatro Vientos", "La Unión", "Santa Rosa", "Del Valle", "Buenos Aires", "La Estrella", "Norte",
]
_EMPRESA_SUFIJOS = ["S.A.", "S.R.L.", "S.A.S.", "y Cía."]
_CAMPOS_ERROR_FORMATO = ("menos", "mas")


@dataclass(frozen=True)
class Resultado:
    """El archivo y lo que Donaciones debería contar al importarlo."""

    contenido: str
    filas: int
    creadas: int
    actualizadas: int
    con_error: int
    con_error_de_formato: int

    @property
    def con_error_de_correo(self) -> int:
        return self.con_error - self.con_error_de_formato


def _sin_tildes(texto: str) -> str:
    base = unicodedata.normalize("NFD", texto.lower())
    return re.sub(r"[^a-z0-9]", "", "".join(c for c in base if not unicodedata.combining(c)))


class _Fabrica:
    """Arma donantes ficticios; todo el azar sale de un solo generador con semilla."""

    def __init__(self, rng: random.Random, serie: int):
        self._rng = rng
        self._serie = serie
        self._documentos: set[str] = set()
        self._contador = 0

    def _documento(self, tipo: str) -> str:
        while True:
            n = self._rng.randrange(1_000_000)
            doc = f"{self._serie * 1_000_000 + n}" if tipo == "HUMANA" else f"30{self._serie}{n:07d}"
            if doc not in self._documentos:
                self._documentos.add(doc)
                return doc

    def _nombre(self, tipo: str) -> str:
        if tipo == "HUMANA":
            return f"{self._rng.choice(_NOMBRES)} {self._rng.choice(_APELLIDOS)}"
        return (f"{self._rng.choice(_EMPRESA_RUBROS)} {self._rng.choice(_EMPRESA_NOMBRES)} "
                f"{self._rng.choice(_EMPRESA_SUFIJOS)}")

    def telefono(self) -> str:
        return f"+54 11 5550-{self._rng.randrange(10_000):04d}"

    def donante(self) -> list[str]:
        """Un donante nuevo con documento y correo propios."""
        tipo = "JURIDICA" if self._rng.randrange(100) < PORCENTAJE_EMPRESAS else "HUMANA"
        nombre = self._nombre(tipo)
        self._contador += 1
        correo = f"{_sin_tildes(nombre)}{self._contador}-{self._serie}@{DOMINIO}"
        tipo_doc = "CUIT" if tipo == "JURIDICA" else "DNI"
        return [tipo, tipo_doc, self._documento(tipo), nombre, correo, self.telefono()]

    def duplicado_de(self, original: list[str]) -> list[str]:
        """El mismo donante con otro teléfono: Donaciones lo actualiza."""
        fila = list(original)
        fila[5] = self.telefono()
        return fila

    def con_correo_de_otro(self, original: list[str]) -> list[str]:
        """Otro donante (otro documento) con el correo de uno anterior: queda en conflicto."""
        fila = self.donante()
        fila[4] = original[4]
        return fila


def _con_error_de_formato(fila: list[str], variante: str) -> str:
    if variante == "menos":
        return ",".join(fila[:5])  # sin teléfono: 5 columnas
    return ",".join(fila + ["extra"])  # 7 columnas


def _validar(filas: int, duplicados: int, con_error: int, serie: int) -> None:
    if not 10 <= serie <= 99 or serie == SERIE_DEL_SEEDER:
        raise ValueError(
            f"la serie tiene que ir de 10 a 99 y no puede ser {SERIE_DEL_SEEDER} (la del seeder)")
    if filas < 1:
        raise ValueError("filas tiene que ser al menos 1")
    if duplicados < 0 or con_error < 0:
        raise ValueError("duplicados y con_error no pueden ser negativos")
    if duplicados + con_error >= filas:
        raise ValueError("duplicados y filas con error tienen que dejar al menos una fila nueva")


def generar(filas: int = FILAS_POR_DEFECTO, duplicados: int = DUPLICADOS_POR_DEFECTO,
            con_error: int = CON_ERROR_POR_DEFECTO, semilla: int = SEMILLA_POR_DEFECTO,
            con_bom: bool = True, serie: int = SERIE_POR_DEFECTO) -> Resultado:
    _validar(filas, duplicados, con_error, serie)
    rng = random.Random(semilla)
    fabrica = _Fabrica(rng, serie)

    de_formato = con_error * 2 // 3
    de_correo = con_error - de_formato
    nuevas = filas - duplicados - con_error
    clases = (["nueva"] * nuevas + ["duplicado"] * duplicados + ["correo"] * de_correo
              + [_CAMPOS_ERROR_FORMATO[i % 2] for i in range(de_formato)])
    rng.shuffle(clases)
    # Todo duplicado o conflicto necesita un donante anterior: la primera fila siempre es nueva
    primera = clases.index("nueva")
    clases[0], clases[primera] = clases[primera], clases[0]

    originales: list[list[str]] = []
    lineas = [CABECERA]
    for clase in clases:
        if clase == "nueva":
            fila = fabrica.donante()
            originales.append(fila)
            lineas.append(",".join(fila))
        elif clase == "duplicado":
            lineas.append(",".join(fabrica.duplicado_de(rng.choice(originales))))
        elif clase == "correo":
            lineas.append(",".join(fabrica.con_correo_de_otro(rng.choice(originales))))
        else:
            lineas.append(_con_error_de_formato(fabrica.donante(), clase))

    contenido = ("\n".join(lineas) + "\n")
    if con_bom:
        contenido = BOM + contenido
    return Resultado(contenido=contenido, filas=filas, creadas=nuevas, actualizadas=duplicados,
                     con_error=con_error, con_error_de_formato=de_formato)


def escribir(resultado: Resultado, destino: Path) -> None:
    # newline="" para que Windows no convierta los saltos de línea
    with open(destino, "w", encoding="utf-8", newline="") as f:
        f.write(resultado.contenido)


def _argumentos(argv: list[str] | None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Genera el CSV de donantes del ensayo de importación.")
    p.add_argument("--filas", type=int, default=FILAS_POR_DEFECTO,
                   help="filas de datos, sin contar la cabecera (por defecto %(default)s)")
    p.add_argument("--duplicados", type=int, default=DUPLICADOS_POR_DEFECTO,
                   help="filas que repiten un donante anterior (por defecto %(default)s)")
    p.add_argument("--con-error", type=int, default=CON_ERROR_POR_DEFECTO,
                   help="filas con error de formato o de correo repetido (por defecto %(default)s)")
    p.add_argument("--semilla", type=int, default=SEMILLA_POR_DEFECTO,
                   help="con la misma semilla sale el mismo archivo (por defecto %(default)s)")
    p.add_argument("--serie", type=int, default=SERIE_POR_DEFECTO,
                   help="dos dígitos con los que empiezan los documentos, de 10 a 99 salvo 90; "
                        "usar otra para repetir el ensayo sobre la misma base (por defecto %(default)s)")
    p.add_argument("--sin-bom", action="store_true", help="no escribe la marca BOM de UTF-8")
    p.add_argument("--salida", type=Path, default=None,
                   help="archivo de salida (por defecto donantes-ensayo-<filas>.csv)")
    return p.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    a = _argumentos(argv)
    try:
        r = generar(a.filas, a.duplicados, a.con_error, a.semilla, not a.sin_bom, a.serie)
    except ValueError as e:
        print(f"No se pudo generar el archivo: {e}", file=sys.stderr)
        return 2
    destino = a.salida or Path(f"donantes-ensayo-{a.filas}.csv")
    escribir(r, destino)
    print(f"Archivo: {destino} ({destino.stat().st_size / 1024 / 1024:.2f} MB)")
    print(f"Filas de datos: {r.filas}")
    print("Al importarlo, Donaciones debería contar:")
    print(f"  creadas      {r.creadas}")
    print(f"  actualizadas {r.actualizadas}")
    print(f"  con error    {r.con_error} "
          f"({r.con_error_de_formato} de formato, {r.con_error_de_correo} de correo repetido)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
