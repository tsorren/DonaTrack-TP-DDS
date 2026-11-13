#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Copia al tema de Keycloak lo que viene del sitio (SPEC-06 E2-09, D20).

`tokens.css` del sitio es la fuente de verdad de la identidad Raíces (D20). El tema de Keycloak vive en otra
imagen y no puede leer los archivos del sitio, así que lleva una copia **idéntica** de los tokens, de las
tres tipografías con sus licencias y del logo. Cuando el sitio cambia alguno de ellos hay que volver a
copiarlo; `test_tema_raices.py` falla si la copia quedó vieja.

Uso, desde la raíz del repo:
  python scripts/sincronizar_tema.py              copia (deja el tema igual al sitio)
  python scripts/sincronizar_tema.py --verificar  no copia: sale con 1 si el tema quedó distinto
"""

import filecmp
import os
import shutil
import sys

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ORIGEN = os.path.join(RAIZ, "cliente-liviano", "src", "main", "resources", "static")
DESTINO = os.path.join(RAIZ, "auth-service", "themes", "donatrack", "login", "resources")

# (ruta en el sitio, ruta en el tema): las mismas subcarpetas, así las rutas relativas de los @font-face valen igual.
ARCHIVOS = (
    ("css/tokens.css", "css/tokens.css"),
    ("fuentes/archivo-latin.woff2", "fuentes/archivo-latin.woff2"),
    ("fuentes/archivo-cursiva-latin.woff2", "fuentes/archivo-cursiva-latin.woff2"),
    ("fuentes/dm-sans-latin.woff2", "fuentes/dm-sans-latin.woff2"),
    ("fuentes/OFL-archivo.txt", "fuentes/OFL-archivo.txt"),
    ("fuentes/OFL-dm-sans.txt", "fuentes/OFL-dm-sans.txt"),
    ("img/logo.svg", "img/logo.svg"),
)


def distintos():
    """Los archivos del tema que no existen o no son idénticos a los del sitio."""
    return [d for o, d in ARCHIVOS
            if not os.path.isfile(os.path.join(DESTINO, d)) or not filecmp.cmp(os.path.join(ORIGEN, o), os.path.join(DESTINO, d), shallow=False)]


def copiar():
    for origen, destino in ARCHIVOS:
        ruta = os.path.join(DESTINO, destino)
        os.makedirs(os.path.dirname(ruta), exist_ok=True)
        shutil.copyfile(os.path.join(ORIGEN, origen), ruta)


def main(argumentos):
    if "--verificar" in argumentos:
        desviados = distintos()
        for d in desviados:
            print("distinto del sitio: %s" % d)
        print("el tema está al día con el sitio" if not desviados else "volvé a copiar: python scripts/sincronizar_tema.py")
        return 1 if desviados else 0
    copiar()
    print("copiados %d archivos del sitio al tema" % len(ARCHIVOS))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
