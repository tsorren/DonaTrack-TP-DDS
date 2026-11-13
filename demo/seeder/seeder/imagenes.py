"""Fotos de recepción generadas: cajas apiladas en la sede, con una etiqueta de lo que llegó.

Son dibujos procedurales (sin fotos reales ni personas). La misma semilla da la misma imagen.
"""

from __future__ import annotations

import io
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ANCHO, ALTO = 960, 720

_PAREDES = [(214, 200, 178), (190, 205, 200), (222, 214, 196), (200, 196, 214), (229, 221, 203)]
_PISOS = [(120, 96, 72), (150, 140, 128), (98, 88, 80), (164, 132, 100)]
_CARTON = [(196, 154, 104), (184, 142, 92), (205, 166, 118), (176, 136, 90)]
_ETIQUETA = [(34, 94, 168), (186, 64, 48), (46, 125, 80), (120, 72, 150), (200, 132, 30)]


def _fuente(tamanio: int) -> ImageFont.ImageFont:
    for nombre in ("DejaVuSans-Bold.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"):
        try:
            return ImageFont.truetype(nombre, tamanio)
        except OSError:
            continue
    return ImageFont.load_default(size=tamanio)


def _caja(dibujo: ImageDraw.ImageDraw, x: int, y: int, ancho: int, alto: int,
          color: tuple[int, int, int], azar: random.Random) -> None:
    sombra = tuple(max(0, c - 45) for c in color)
    tapa = tuple(min(255, c + 22) for c in color)
    fondo = int(ancho * 0.18)
    # Frente, costado y tapa: una caja en perspectiva simple
    dibujo.polygon([(x, y), (x + ancho, y), (x + ancho, y + alto), (x, y + alto)], fill=color)
    dibujo.polygon([(x + ancho, y), (x + ancho + fondo, y - fondo // 2),
                    (x + ancho + fondo, y + alto - fondo // 2), (x + ancho, y + alto)], fill=sombra)
    dibujo.polygon([(x, y), (x + fondo, y - fondo // 2), (x + ancho + fondo, y - fondo // 2),
                    (x + ancho, y)], fill=tapa)
    cinta = (226, 214, 180)
    centro = x + ancho // 2
    dibujo.rectangle([centro - 9, y, centro + 9, y + alto], fill=cinta)
    dibujo.polygon([(centro - 9, y), (centro - 9 + fondo, y - fondo // 2),
                    (centro + 9 + fondo, y - fondo // 2), (centro + 9, y)], fill=cinta)
    if azar.random() < 0.5:
        dibujo.line([(x + 14, y + alto - 30), (x + 70, y + alto - 30)], fill=sombra, width=3)


def foto_de_recepcion(semilla: int, etiqueta: str, leyenda: str) -> bytes:
    azar = random.Random(semilla)
    imagen = Image.new("RGB", (ANCHO, ALTO), azar.choice(_PAREDES))
    dibujo = ImageDraw.Draw(imagen)
    horizonte = int(ALTO * azar.uniform(0.55, 0.62))
    piso = azar.choice(_PISOS)
    dibujo.rectangle([0, horizonte, ANCHO, ALTO], fill=piso)
    for i in range(0, ANCHO, 120):
        dibujo.line([(i, horizonte), (i - 200, ALTO)], fill=tuple(max(0, c - 18) for c in piso),
                    width=2)
    # Una ventana o una puerta al fondo
    vx = azar.randint(60, ANCHO - 260)
    dibujo.rectangle([vx, 80, vx + 190, horizonte - 60], fill=(236, 240, 242),
                     outline=(150, 140, 130), width=8)
    dibujo.line([(vx + 95, 80), (vx + 95, horizonte - 60)], fill=(150, 140, 130), width=6)

    # Pila de cajas apoyadas en el piso
    x = azar.randint(90, 200)
    base = horizonte + azar.randint(120, 170)
    for columna in range(azar.randint(2, 4)):
        ancho = azar.randint(150, 210)
        y = base
        for _ in range(azar.randint(1, 3)):
            alto = azar.randint(95, 140)
            y -= alto
            _caja(dibujo, x, y, ancho, alto, azar.choice(_CARTON), azar)
        x += ancho + azar.randint(30, 50)
        if x > ANCHO - 200:
            break

    # Etiqueta de lo recibido, pegada en la caja de adelante
    color = azar.choice(_ETIQUETA)
    fuente = _fuente(30)
    chica = _fuente(20)
    texto = etiqueta[:28]
    caja = dibujo.textbbox((0, 0), texto, font=fuente)
    ancho_etiqueta = caja[2] - caja[0] + 40
    ex, ey = 110, base - 90
    dibujo.rounded_rectangle([ex, ey, ex + ancho_etiqueta, ey + 64], radius=10, fill=(250, 248, 240),
                             outline=color, width=5)
    dibujo.text((ex + 20, ey + 14), texto, fill=color, font=fuente)

    imagen = imagen.filter(ImageFilter.GaussianBlur(0.8))
    # Grano de cámara y la leyenda de la fecha, como en una foto de celular
    ruido = Image.effect_noise((ANCHO, ALTO), 18).convert("RGB")
    imagen = Image.blend(imagen, ruido, 0.06)
    dibujo = ImageDraw.Draw(imagen)
    dibujo.text((ANCHO - 330, ALTO - 42), leyenda, fill=(255, 236, 160), font=chica)
    salida = io.BytesIO()
    imagen.save(salida, "JPEG", quality=82)
    return salida.getvalue()
