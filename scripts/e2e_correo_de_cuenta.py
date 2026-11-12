#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Prueba de punta a punta del correo de cuenta (SPEC-06 E2-06): Keycloak -> SPI -> RabbitMQ.

Hace lo que haría una persona con un navegador, pero contra un stack ya levantado: pide recuperar la clave,
lee el evento que publicó el SPI, abre el enlace de ese evento, cambia la clave y comprueba que entra con la
nueva y no con la vieja. También comprueba que una cuenta inexistente recibe la misma respuesta y no genera
ningún mensaje (RNF-AUTH-10), y que la verificación del email usa el mismo camino (RF-AUTH-16).

Los mensajes se leen con una cola espía propia, atada a `auth.exchange`, así que sirve aunque
`notificaciones-service` esté consumiendo. Crea una cuenta de prueba por la Admin API y la borra al final.
Solo usa la biblioteca estándar.

Las credenciales salen del `.env` de la raíz del repo (el del compose local) y no hace falta exportar
nada. Cualquiera de estas variables de entorno pisa lo que diga el `.env`:
  KC_URL            http://localhost:8180      (KC_HOSTNAME del .env)
  KC_REALM          donatrack
  KC_ADMIN_USER     kc-admin                   (KC_BOOTSTRAP_ADMIN_USERNAME del .env)
  KC_ADMIN_PASSWORD (obligatoria)              (KC_BOOTSTRAP_ADMIN_PASSWORD del .env)
  KC_CLIENT_ID      cliente-liviano
  KC_REDIRECT_URI   http://localhost:8090/login/oauth2/code/keycloak
  RABBIT_API        http://localhost:15672     (API de administración de RabbitMQ)
  RABBIT_USER       donatrack
  RABBIT_PASS       (obligatoria)              (RABBITMQ_PASS del .env)
  RABBIT_VHOST      /

Uso, con el stack del compose local levantado y parado en la raíz del repo:
  python scripts/e2e_correo_de_cuenta.py          (en Windows: `py scripts\e2e_correo_de_cuenta.py`)
"""

import base64
import html
import http.cookiejar
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

def completar_desde_el_env():
    """Completa lo que falte del entorno con el `.env` de la raíz del repo (el variable de entorno gana)."""
    ruta = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".env")
    if not os.path.isfile(ruta):
        return
    valores = {}
    with open(ruta, encoding="utf-8") as f:
        for linea in f:
            m = re.match(r"^([A-Z][A-Z0-9_]*)=(.*)$", linea.strip())
            if m:
                valores[m.group(1)] = m.group(2)
    for destino, origen in (("KC_URL", "KC_HOSTNAME"), ("KC_ADMIN_USER", "KC_BOOTSTRAP_ADMIN_USERNAME"),
                            ("KC_ADMIN_PASSWORD", "KC_BOOTSTRAP_ADMIN_PASSWORD"), ("RABBIT_PASS", "RABBITMQ_PASS")):
        if not os.environ.get(destino) and valores.get(origen):
            os.environ[destino] = valores[origen]


completar_desde_el_env()
KC_URL = os.environ.get("KC_URL", "http://localhost:8180").rstrip("/")
REALM = os.environ.get("KC_REALM", "donatrack")
ADMIN_USER = os.environ.get("KC_ADMIN_USER", "kc-admin")
ADMIN_PASSWORD = os.environ.get("KC_ADMIN_PASSWORD", "")
CLIENT_ID = os.environ.get("KC_CLIENT_ID", "cliente-liviano")
REDIRECT_URI = os.environ.get("KC_REDIRECT_URI", "http://localhost:8090/login/oauth2/code/keycloak")
RABBIT_API = os.environ.get("RABBIT_API", "http://localhost:15672").rstrip("/")
RABBIT_USER = os.environ.get("RABBIT_USER", "donatrack")
RABBIT_PASS = os.environ.get("RABBIT_PASS", "")
VHOST = urllib.parse.quote(os.environ.get("RABBIT_VHOST", "/"), safe="")
EXCHANGE = "auth.exchange"
COLA_ESPIA = "e2e.correo-de-cuenta.%s" % uuid.uuid4().hex[:8]

pasaron = 0
fallaron = 0


def check(nombre, ok, detalle=""):
    global pasaron, fallaron
    if ok:
        pasaron += 1
        print("  [PASS] %s" % nombre)
    else:
        fallaron += 1
        print("  [FAIL] %s%s" % (nombre, (" -> " + detalle) if detalle else ""))
    return ok


class SinRedireccion(urllib.request.HTTPRedirectHandler):
    """No sigue las redirecciones: las sigue el script, para ver cada paso."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class PoliticaDeCookies(http.cookiejar.DefaultCookiePolicy):
    """Keycloak marca sus cookies como Secure aunque se acceda por http://localhost; un navegador las
    devuelve igual (trata a localhost como origen confiable) y el cliente de cookies de Python no."""

    def return_ok_secure(self, cookie, request):
        return True


class Navegador:
    """Una 'persona': su propio juego de cookies."""

    def __init__(self):
        self.cookies = http.cookiejar.CookieJar(policy=PoliticaDeCookies())
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies), SinRedireccion())

    def pedir(self, url, datos=None):
        cuerpo = urllib.parse.urlencode(datos).encode() if datos is not None else None
        pedido = urllib.request.Request(url, data=cuerpo)
        try:
            r = self.opener.open(pedido, timeout=30)
        except urllib.error.HTTPError as e:
            r = e
        return r.status if hasattr(r, "status") else r.code, dict(r.headers), r.read().decode("utf-8", "replace")

    def seguir(self, url, datos=None, saltos=8):
        """Sigue redirecciones; devuelve (estado, url final, html). Corta si la redirección sale de Keycloak."""
        estado, headers, cuerpo = self.pedir(url, datos)
        for _ in range(saltos):
            destino = headers.get("Location")
            if estado not in (301, 302, 303, 307) or not destino:
                break
            destino = urllib.parse.urljoin(url, destino)
            if not destino.startswith(KC_URL):
                return estado, destino, cuerpo
            url = destino
            estado, headers, cuerpo = self.pedir(url)
        return estado, url, cuerpo


def texto_visible(pagina):
    sin_scripts = re.sub(r"(?is)<(script|style).*?</\1>", " ", pagina)
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"(?s)<[^>]+>", " ", sin_scripts))).strip()


def formulario(pagina):
    """(action, campos ocultos) del primer formulario de la página."""
    m = re.search(r'(?is)<form[^>]*\saction="([^"]+)"', pagina)
    if not m:
        return None, {}
    ocultos = {n: html.unescape(v) for n, v in re.findall(r'(?i)<input[^>]*type="hidden"[^>]*name="([^"]+)"[^>]*value="([^"]*)"', pagina)}
    return html.unescape(m.group(1)), ocultos


# ── Admin API y RabbitMQ ─────────────────────────────────────────────────────────────────────────

def json_http(metodo, url, cuerpo=None, cabeceras=None, esperado=(200, 201, 204)):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    pedido = urllib.request.Request(url, data=datos, method=metodo, headers=dict({"Content-Type": "application/json"}, **(cabeceras or {})))
    try:
        with urllib.request.urlopen(pedido, timeout=30) as r:
            crudo = r.read().decode() or "null"
            return r.status, json.loads(crudo) if crudo.strip()[:1] in "{[" else crudo
    except urllib.error.HTTPError as e:
        crudo = e.read().decode()
        if e.code in esperado:
            return e.code, crudo
        raise RuntimeError("%s %s -> HTTP %s: %s" % (metodo, url, e.code, crudo[:200]))


def token_admin():
    cuerpo = urllib.parse.urlencode({"grant_type": "password", "client_id": "admin-cli", "username": ADMIN_USER, "password": ADMIN_PASSWORD}).encode()
    with urllib.request.urlopen(urllib.request.Request("%s/realms/master/protocol/openid-connect/token" % KC_URL, data=cuerpo), timeout=30) as r:
        return json.loads(r.read())["access_token"]


def rabbit(metodo, ruta, cuerpo=None):
    auth = base64.b64encode(("%s:%s" % (RABBIT_USER, RABBIT_PASS)).encode()).decode()
    return json_http(metodo, "%s/api/%s" % (RABBIT_API, ruta), cuerpo, {"Authorization": "Basic " + auth})


def crear_cola_espia():
    rabbit("PUT", "queues/%s/%s" % (VHOST, COLA_ESPIA), {"durable": False, "auto_delete": False, "arguments": {"x-expires": 600000}})
    rabbit("POST", "bindings/%s/e/%s/q/%s" % (VHOST, EXCHANGE, COLA_ESPIA), {"routing_key": "cuenta.#"})


def leer_mensajes(esperar_s=6, minimo=1):
    """Lee (y consume) los mensajes de la cola espía; espera hasta `esperar_s` por `minimo`."""
    limite = time.time() + esperar_s
    mensajes = []
    while time.time() < limite:
        _, lote = rabbit("POST", "queues/%s/%s/get" % (VHOST, COLA_ESPIA), {"count": 10, "ackmode": "ack_requeue_false", "encoding": "auto"})
        mensajes.extend(lote if isinstance(lote, list) else [])
        if len(mensajes) >= minimo:
            break
        time.sleep(0.5)
    return mensajes


def sin_mensajes(esperar_s=3):
    time.sleep(esperar_s)
    return leer_mensajes(esperar_s=0, minimo=0)


# ── Pasos de la persona ────────────────────────────────────────────────────────────────────────

def abrir_login(persona):
    url = "%s/realms/%s/protocol/openid-connect/auth?%s" % (KC_URL, REALM, urllib.parse.urlencode({
        "client_id": CLIENT_ID, "redirect_uri": REDIRECT_URI, "response_type": "code", "scope": "openid", "state": uuid.uuid4().hex, "nonce": uuid.uuid4().hex}))
    return persona.seguir(url)


def ingresar(persona, email, clave):
    """Devuelve (código de autorización o None, página): llega al redirect_uri con un code si la clave sirve."""
    _, _, pagina = abrir_login(persona)
    accion, ocultos = formulario(pagina)
    if not accion:
        return None, pagina
    estado, url, cuerpo = persona.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, username=email, password=clave))
    if url.startswith(REDIRECT_URI):
        return urllib.parse.parse_qs(urllib.parse.urlparse(url).query).get("code", [None])[0], cuerpo
    return None, cuerpo


def pedir_recuperar_clave(persona, email):
    _, url, pagina = abrir_login(persona)
    enlace = re.search(r'href="([^"]*reset-credentials[^"]*)"', pagina)
    if not enlace:
        return None, pagina
    _, _, formulario_reset = persona.seguir(urllib.parse.urljoin(url, html.unescape(enlace.group(1))))
    accion, ocultos = formulario(formulario_reset)
    estado, _, respuesta = persona.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, username=email))
    return estado, respuesta


def main():
    if not ADMIN_PASSWORD or not RABBIT_PASS:
        print("Faltan KC_ADMIN_PASSWORD o RABBIT_PASS: corré el script desde la raíz del repo, donde está el .env "
              "(o definí las variables; ver el docstring de este script).")
        return 2
    admin = {"Authorization": "Bearer " + token_admin()}
    email = "e2e.correo.%s@example.com" % uuid.uuid4().hex[:10]
    clave = "Clave-E2e-%s!aA1" % uuid.uuid4().hex[:12]
    clave_nueva = "Nueva-E2e-%s!bB2" % uuid.uuid4().hex[:12]
    usuarios = "%s/admin/realms/%s/users" % (KC_URL, REALM)

    json_http("POST", usuarios, {"username": email, "email": email, "enabled": True, "emailVerified": True,
                                 "credentials": [{"type": "password", "value": clave, "temporary": False}]}, admin)
    cuenta_id = json_http("GET", "%s?email=%s&exact=true" % (usuarios, urllib.parse.quote(email)), None, admin)[1][0]["id"]
    crear_cola_espia()
    try:
        print("\n── Recuperar la clave (RF-AUTH-13) ──")
        estado, respuesta_existente = pedir_recuperar_clave(Navegador(), email)
        check("Keycloak ofrece el enlace de recuperación en el login", estado is not None, "no hay enlace reset-credentials")
        check("la respuesta es una página (200)", estado == 200, str(estado))
        check("y no es un mensaje de error de Keycloak", "alert-error" not in respuesta_existente, texto_visible(respuesta_existente)[:140])

        mensajes = leer_mensajes()
        check("se publicó exactamente un evento", len(mensajes) == 1, "mensajes: %d" % len(mensajes))
        evento, props, enlace = {}, {}, ""
        if mensajes:
            props = mensajes[0]["properties"]
            evento = json.loads(mensajes[0]["payload"])
            enlace = evento.get("enlace", "")
        check("tipo RECUPERAR_CLAVE y la cuenta correcta", evento.get("tipo") == "RECUPERAR_CLAVE" and evento.get("cuentaId") == cuenta_id, str(evento.get("tipo")))
        check("el destinatario es el email de la cuenta", evento.get("destinatario") == email)
        check("el enlace es de Keycloak y es un action-token", enlace.startswith("%s/realms/%s/login-actions/action-token" % (KC_URL, REALM)), enlace[:80])
        check("vence en 30 minutos y viene el idioma", evento.get("venceEnMinutos") == 30 and evento.get("idioma") == "es", "%s/%s" % (evento.get("venceEnMinutos"), evento.get("idioma")))
        check("la fecha es un instante ISO", bool(re.match(r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d", evento.get("fecha", ""))))
        encabezados = props.get("headers", {})
        check("alias de tipo __TypeId__ del contrato", encabezados.get("__TypeId__") == "cuenta.enlace-solicitado.v1", str(encabezados.get("__TypeId__")))
        check("X-Trace-Id de 32 hex", bool(re.match(r"^[0-9a-f]{32}$", str(encabezados.get("X-Trace-Id", "")))), str(encabezados.get("X-Trace-Id")))
        check("message_id es un UUID (clave del inbox)", bool(re.match(r"^[0-9a-f-]{36}$", str(props.get("message_id", "")))))
        check("JSON, persistente y con vencimiento igual al del enlace",
              props.get("content_type") == "application/json" and props.get("delivery_mode") == 2 and str(props.get("expiration")) == "1800000",
              "%s/%s/%s" % (props.get("content_type"), props.get("delivery_mode"), props.get("expiration")))

        print("\n── El enlace del evento sirve (y una sola vez) ──")
        if enlace:
            desde_el_correo = Navegador()
            _, _, pagina = desde_el_correo.seguir(enlace)
            accion, ocultos = formulario(pagina)
            check("abrir el enlace muestra el formulario para elegir la clave nueva", "password-new" in pagina, texto_visible(pagina)[:100])
            if accion:
                desde_el_correo.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, **{"password-new": clave_nueva, "password-confirm": clave_nueva}))
            codigo_nuevo, _ = ingresar(Navegador(), email, clave_nueva)
            check("con la clave nueva se puede ingresar", codigo_nuevo is not None)
            codigo_viejo, _ = ingresar(Navegador(), email, clave)
            check("con la clave vieja ya no", codigo_viejo is None)
            _, _, reutilizado = Navegador().seguir(enlace)
            check("el enlace no se puede usar por segunda vez", "password-new" not in reutilizado, texto_visible(reutilizado)[:100])
        else:
            check("hay un enlace para probar", False, "no se publicó el evento: se saltean las comprobaciones del enlace")
        leer_mensajes(esperar_s=0, minimo=0)

        print("\n── Una cuenta que no existe (RNF-AUTH-10) ──")
        estado_inexistente, respuesta_inexistente = pedir_recuperar_clave(Navegador(), "no.existe.%s@example.com" % uuid.uuid4().hex[:8])
        check("misma respuesta que para una cuenta que existe", texto_visible(respuesta_inexistente) == texto_visible(respuesta_existente),
              "\n  existente:   %s\n  inexistente: %s" % (texto_visible(respuesta_existente)[:90], texto_visible(respuesta_inexistente)[:90]))
        check("y no se publica ningún mensaje", len(sin_mensajes()) == 0)

        print("\n── Verificar el email (RF-AUTH-16): el mismo camino ──")
        json_http("PUT", "%s/%s" % (usuarios, cuenta_id), {"emailVerified": False, "requiredActions": ["VERIFY_EMAIL"]}, admin)
        persona = Navegador()
        ingresar(persona, email, clave_nueva)
        verificacion = leer_mensajes()
        check("se publicó un evento de verificación", len(verificacion) == 1, "mensajes: %d" % len(verificacion))
        if verificacion:
            evento_v = json.loads(verificacion[0]["payload"])
            check("tipo VERIFICAR_EMAIL para la misma cuenta", evento_v.get("tipo") == "VERIFICAR_EMAIL" and evento_v.get("cuentaId") == cuenta_id, str(evento_v.get("tipo")))
            # Desde otro navegador (el caso normal: el mail se abre en el celular), Keycloak pide una
            # confirmación antes de verificar; desde el mismo navegador verifica directo.
            otro = Navegador()
            _, _, pagina_v = otro.seguir(evento_v["enlace"])
            confirmar = re.search(r'href="([^"]*action-token[^"]*)"', pagina_v)
            check("desde otro navegador Keycloak pide confirmar antes de verificar", confirmar is not None, texto_visible(pagina_v)[:100])
            if confirmar:
                otro.seguir(html.unescape(confirmar.group(1)))
            verificado = json_http("GET", "%s/%s" % (usuarios, cuenta_id), None, admin)[1].get("emailVerified")
            check("confirmar deja el email verificado", verificado is True, str(verificado))
    finally:
        try:
            json_http("DELETE", "%s/%s" % (usuarios, cuenta_id), None, admin)
        finally:
            rabbit("DELETE", "queues/%s/%s" % (VHOST, COLA_ESPIA))

    print("\nPASS: %d  |  FAIL: %d" % (pasaron, fallaron))
    return 1 if fallaron else 0


if __name__ == "__main__":
    sys.exit(main())
