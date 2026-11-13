"""Llamadas HTTP a los servicios, con el token de la cuenta que actúa y errores legibles."""

from __future__ import annotations

import time
import uuid
from typing import Any, Callable

import requests

Token = Callable[[], str | None]


class ErrorDeApi(Exception):
    def __init__(self, metodo: str, url: str, estado: int, cuerpo: str):
        super().__init__(f"{metodo} {url} respondió {estado}: {cuerpo[:600]}")
        self.estado = estado


class Api:
    """Un servicio: su URL base y quién firma los pedidos (o nadie, para lo público)."""

    def __init__(self, base: str, token: Token | None = None, sesion: requests.Session | None = None):
        self.base = base.rstrip("/")
        self.token = token
        self.sesion = sesion or requests.Session()

    def como(self, token: Token | None) -> "Api":
        return Api(self.base, token, self.sesion)

    def pedir(
        self,
        metodo: str,
        ruta: str,
        *,
        json: Any = None,
        files: Any = None,
        params: dict | None = None,
        headers: dict | None = None,
        esperado: tuple[int, ...] = (200, 201, 202, 204),
        intentos: int = 3,
    ) -> requests.Response:
        url = self.base + ruta
        encabezados = {"X-Trace-Id": uuid.uuid4().hex}
        if self.token is not None:
            valor = self.token()
            if valor:
                encabezados["Authorization"] = f"Bearer {valor}"
        if headers:
            encabezados.update(headers)
        for intento in range(1, intentos + 1):
            try:
                respuesta = self.sesion.request(
                    metodo, url, json=json, files=files, params=params,
                    headers=encabezados, timeout=60,
                )
            except requests.ConnectionError:
                if intento == intentos:
                    raise
                time.sleep(2 * intento)
                continue
            # 502/503: el servicio todavía arranca o reinicia; se reintenta
            if respuesta.status_code in (502, 503) and intento < intentos:
                time.sleep(2 * intento)
                continue
            if respuesta.status_code not in esperado:
                raise ErrorDeApi(metodo, url, respuesta.status_code, respuesta.text)
            return respuesta
        raise AssertionError("inalcanzable")

    def get(self, ruta: str, **kw) -> Any:
        respuesta = self.pedir("GET", ruta, **kw)
        return respuesta.json() if respuesta.content else None

    def post(self, ruta: str, cuerpo: Any = None, **kw) -> Any:
        respuesta = self.pedir("POST", ruta, json=cuerpo, **kw)
        return respuesta.json() if respuesta.content else None

    def put(self, ruta: str, cuerpo: Any = None, **kw) -> Any:
        respuesta = self.pedir("PUT", ruta, json=cuerpo, **kw)
        return respuesta.json() if respuesta.content else None

    def patch(self, ruta: str, cuerpo: Any = None, **kw) -> Any:
        respuesta = self.pedir("PATCH", ruta, json=cuerpo, **kw)
        return respuesta.json() if respuesta.content else None

    def delete(self, ruta: str, **kw) -> None:
        self.pedir("DELETE", ruta, **kw)


def esperar_salud(nombre: str, url: str, limite_segundos: int = 600) -> None:
    """Espera a que el servicio responda 200 en su URL de salud."""
    fin = time.monotonic() + limite_segundos
    while True:
        try:
            if requests.get(url, timeout=5).status_code == 200:
                return
        except requests.RequestException:
            pass
        if time.monotonic() > fin:
            raise TimeoutError(f"{nombre} no respondió en {limite_segundos} s ({url})")
        time.sleep(3)
