#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""Prueba de punta a punta del ingreso con Google y de la vinculación por contraseña (SPEC-06 E2-05).

No usa el Google verdadero (haría falta una cuenta y credenciales de Google): lo simula con un segundo realm
dentro del mismo Keycloak (`google-falso`), que es un proveedor OIDC más, y le conecta un proveedor temporal
`google-prueba` **con el mismo flujo de primer ingreso que el proveedor `google` del realm**. Lo que se prueba
es ese flujo (que es el mismo para cualquier proveedor), con tokens OIDC verdaderos, incluido el
`email_verified`. Lo que no se prueba es lo propio de Google: sus pantallas y sus credenciales.

Casos (D4, RF-AUTH-02, RF-AUTH-03):
  1. Un email que no existe en DonaTrack: se crea la cuenta y se entra.
  2. Un email que ya tiene cuenta con contraseña: se pide esa contraseña; la incorrecta no vincula, la
     correcta sí. También si Google dice que el email NO está verificado: la contraseña se pide igual.
  3. Una vez vinculada, entrar con Google no vuelve a pedir la contraseña.
  4. Con SMTP configurado en el realm sigue pidiendo la contraseña: el flujo de fábrica de Keycloak se
     habría ido por el camino por email (y falla, porque el correo de cuenta sale por RabbitMQ, no por SMTP).
  5. Una cuenta SIN contraseña no puede vincularse (RF-AUTH-04 queda pendiente de D12): se comprueba que no se
     vincula y que, una vez que tiene contraseña (por ejemplo, tras recuperar la clave), sí.

Crea cuentas y un realm de prueba y los borra al final; devuelve el SMTP del realm como estaba.

Configuración (la misma que `e2e_correo_de_cuenta.py`, del `.env` de la raíz o del entorno):
  KC_URL, KC_REALM, KC_ADMIN_USER, KC_ADMIN_PASSWORD, KC_CLIENT_ID, KC_REDIRECT_URI
  KC_INTERNAL_URL  Cómo se ve Keycloak desde Keycloak (el servidor llama al Google falso). Por defecto,
                   http://localhost:8080, que es como lo ve dentro de su contenedor el del compose (escucha en
                   el 8080 y se publica en el 8180). Si Keycloak corre directo en el puerto público, ponerlo
                   igual a KC_URL.

Uso, con el stack del compose local levantado y parado en la raíz del repo:
  python scripts/e2e_google_vinculacion.py          (en Windows: `py scripts\e2e_google_vinculacion.py`)
"""

import os
import sys
import urllib.parse
import uuid

from e2e_correo_de_cuenta import (CLIENT_ID, KC_URL, REALM, REDIRECT_URI, Navegador, abrir_login, formulario,
                                  json_http, texto_visible, token_admin)
import e2e_correo_de_cuenta as base

INTERNA = os.environ.get("KC_INTERNAL_URL", "http://localhost:8080").rstrip("/")
FALSO = "google-falso"
ALIAS = "google-prueba"
SECRETO = "secreto-del-google-falso-%s" % uuid.uuid4().hex[:8]
CLAVE_DE_GOOGLE = "Clave-de-Google-%s!aA1" % uuid.uuid4().hex[:8]
FLUJO_ESPERADO = "donatrack first broker login"

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


ADMIN = {}


def api(metodo, ruta, cuerpo=None, ok=(200, 201, 204, 409)):
    return json_http(metodo, KC_URL + "/admin/realms" + ruta, cuerpo, ADMIN, esperado=ok)


def usuario_por_email(realm, email):
    lista = api("GET", "/%s/users?email=%s&exact=true" % (realm, urllib.parse.quote(email)))[1]
    return lista[0] if lista else None


def identidades(email):
    u = usuario_por_email(REALM, email)
    if not u:
        return None
    return [i["identityProvider"] for i in api("GET", "/%s/users/%s/federated-identity" % (REALM, u["id"]))[1]]


def preparar_google_falso(real):
    api("DELETE", "/" + FALSO, None, (200, 204, 404))
    api("POST", "", {"realm": FALSO, "enabled": True, "registrationEmailAsUsername": True, "loginWithEmailAllowed": True})
    api("POST", "/%s/clients" % FALSO, {"clientId": "donatrack", "secret": SECRETO, "publicClient": False,
                                        "standardFlowEnabled": True,
                                        "redirectUris": [KC_URL + "/realms/%s/broker/%s/endpoint" % (REALM, ALIAS)]})
    publica = KC_URL + "/realms/%s/protocol/openid-connect" % FALSO
    interna = INTERNA + "/realms/%s/protocol/openid-connect" % FALSO
    api("DELETE", "/%s/identity-provider/instances/%s" % (REALM, ALIAS), None, (200, 204, 404))
    api("POST", "/%s/identity-provider/instances" % REALM, {
        "alias": ALIAS, "providerId": "oidc", "enabled": True, "trustEmail": real.get("trustEmail", False),
        "firstBrokerLoginFlowAlias": real["firstBrokerLoginFlowAlias"],
        "config": {"authorizationUrl": publica + "/auth", "tokenUrl": interna + "/token", "userInfoUrl": interna + "/userinfo",
                   "jwksUrl": interna + "/certs", "issuer": KC_URL + "/realms/" + FALSO, "clientId": "donatrack",
                   "clientSecret": SECRETO, "clientAuthMethod": "client_secret_post", "defaultScope": "openid email",
                   "validateSignature": "true", "useJwksUrl": "true", "syncMode": "IMPORT"}})


def usuario_de_google(email, verificado=True):
    api("POST", "/%s/users" % FALSO, {"username": email, "email": email, "firstName": "Ana", "lastName": "Prueba", "enabled": True,
                                      "emailVerified": verificado,
                                      "credentials": [{"type": "password", "value": CLAVE_DE_GOOGLE, "temporary": False}]})


def cuenta_local(email, clave):
    credenciales = [{"type": "password", "value": clave, "temporary": False}] if clave else []
    api("POST", "/%s/users" % REALM, {"username": email, "email": email, "enabled": True, "emailVerified": True, "credentials": credenciales})


def entrar_con_google(persona, email):
    """Abre el login de DonaTrack pidiendo el proveedor de prueba, ingresa en el Google falso y devuelve
    (url, html) de donde termina: el cliente (con un code) o una pantalla del flujo de primer ingreso."""
    url = "%s/realms/%s/protocol/openid-connect/auth?%s" % (KC_URL, REALM, urllib.parse.urlencode({
        "client_id": CLIENT_ID, "redirect_uri": REDIRECT_URI, "response_type": "code", "scope": "openid",
        "state": uuid.uuid4().hex, "nonce": uuid.uuid4().hex, "kc_idp_hint": ALIAS}))
    _, _, pagina = persona.seguir(url)
    accion, ocultos = formulario(pagina)
    _, destino, pagina = persona.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, username=email, password=CLAVE_DE_GOOGLE))
    return destino, pagina


def entro(url):
    return url.startswith(REDIRECT_URI) and "code=" in url


def confirmar_vinculo(persona, pagina):
    accion, ocultos = formulario(pagina)
    _, destino, siguiente = persona.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, submitAction="linkAccount"))
    return destino, siguiente


def con_contrasena(persona, pagina, email, clave):
    accion, ocultos = formulario(pagina)
    _, destino, siguiente = persona.seguir(urllib.parse.urljoin(KC_URL, accion), dict(ocultos, username=email, password=clave))
    return destino, siguiente


def pide_contrasena(pagina):
    return 'name="password"' in pagina


def main():
    if not base.ADMIN_PASSWORD:
        print("Falta KC_ADMIN_PASSWORD: corré el script desde la raíz del repo, donde está el .env (o definí la variable).")
        return 2
    ADMIN["Authorization"] = "Bearer " + token_admin()
    real = api("GET", "/%s/identity-provider/instances/google" % REALM)[1]
    smtp_original = api("GET", "/" + REALM)[1].get("smtpServer") or {}
    creados = []
    try:
        print("\n── El proveedor Google del realm ──")
        check("usa el flujo propio, sin el camino por email de Keycloak", real["firstBrokerLoginFlowAlias"] == FLUJO_ESPERADO, real["firstBrokerLoginFlowAlias"])
        flujos = {f["alias"] for f in api("GET", "/%s/authentication/flows" % REALM)[1]}
        check("el flujo propio y el de fábrica están en el realm", {FLUJO_ESPERADO, "first broker login"} <= flujos, str(sorted(flujos)))
        _, _, login = abrir_login(Navegador())
        activo = str(real["enabled"]).lower() == "true"
        check("el login muestra \"Continuar con Google\" si y solo si Google está habilitado (%s)" % ("sí" if activo else "no"),
              ("Continuar con Google" in texto_visible(login)) == activo, texto_visible(login)[:160])
        if activo:
            check("y lo dice en español, no en inglés", "Sign in with" not in texto_visible(login), texto_visible(login)[:160])

        preparar_google_falso(real)

        print("\n── Un email que no existe en DonaTrack (RF-AUTH-02) ──")
        nuevo = "google.nuevo.%s@example.com" % uuid.uuid4().hex[:8]
        usuario_de_google(nuevo)
        creados.append(nuevo)
        destino, _ = entrar_con_google(Navegador(), nuevo)
        check("entra directo, sin pasos intermedios", entro(destino),
              "%s | si Keycloak no alcanza al Google falso, revisá KC_INTERNAL_URL (hoy: %s)" % (destino[:100], INTERNA))
        check("queda creada y vinculada a Google", identidades(nuevo) == [ALIAS], str(identidades(nuevo)))

        print("\n── Un email que ya tiene cuenta con contraseña (RF-AUTH-03, D4) ──")
        for verificado in (True, False):
            etiqueta = "Google dice que el email está verificado" if verificado else "Google dice que el email NO está verificado"
            email = "google.existente.%s@example.com" % uuid.uuid4().hex[:8]
            clave = "Clave-Local-%s!aA1" % uuid.uuid4().hex[:8]
            cuenta_local(email, clave)
            usuario_de_google(email, verificado)
            creados.append(email)
            persona = Navegador()
            destino, pagina = entrar_con_google(persona, email)
            check("(%s) no entra solo: pide confirmar el vínculo" % etiqueta, not entro(destino) and "linkAccount" in pagina, texto_visible(pagina)[:120])
            destino, pagina = confirmar_vinculo(persona, pagina)
            check("(%s) pide la contraseña de la cuenta" % etiqueta, pide_contrasena(pagina), texto_visible(pagina)[:120])
            check("(%s) no intentó mandar un email" % etiqueta, "envío del email" not in texto_visible(pagina), texto_visible(pagina)[:120])
            destino, pagina_mal = con_contrasena(persona, pagina, email, "Clave-equivocada-1234!")
            check("(%s) una contraseña incorrecta no vincula" % etiqueta, not entro(destino) and pide_contrasena(pagina_mal) and not identidades(email),
                  str(identidades(email)))
            destino, _ = con_contrasena(persona, pagina_mal, email, clave)
            check("(%s) la contraseña correcta vincula y entra" % etiqueta, entro(destino) and identidades(email) == [ALIAS], "%s %s" % (destino[:60], identidades(email)))
            if verificado:
                destino_2, pagina_2 = entrar_con_google(Navegador(), email)
                check("ya vinculada, entrar con Google no vuelve a pedir la contraseña", entro(destino_2) and not pide_contrasena(pagina_2), destino_2[:100])

        print("\n── Con SMTP configurado en el realm sigue pidiendo la contraseña ──")
        email = "google.smtp.%s@example.com" % uuid.uuid4().hex[:8]
        clave = "Clave-Local-%s!aA1" % uuid.uuid4().hex[:8]
        cuenta_local(email, clave)
        usuario_de_google(email)
        creados.append(email)
        api("PUT", "/" + REALM, dict(api("GET", "/" + REALM)[1], smtpServer={"host": "localhost", "port": "1", "from": "prueba@example.com"}))
        persona = Navegador()
        destino, pagina = entrar_con_google(persona, email)
        destino, pagina = confirmar_vinculo(persona, pagina)
        check("pide la contraseña y no el camino por email", pide_contrasena(pagina) and "envío del email" not in texto_visible(pagina), texto_visible(pagina)[:140])
        api("PUT", "/" + REALM, dict(api("GET", "/" + REALM)[1], smtpServer=smtp_original))

        print("\n── Una cuenta sin contraseña (RF-AUTH-04, pendiente de D12) ──")
        email = "google.sinclave.%s@example.com" % uuid.uuid4().hex[:8]
        cuenta_local(email, None)
        usuario_de_google(email, True)
        creados.append(email)
        persona = Navegador()
        destino, pagina = entrar_con_google(persona, email)
        destino, pagina = confirmar_vinculo(persona, pagina)
        destino, pagina = con_contrasena(persona, pagina, email, "Clave-cualquiera-1234!")
        check("sin contraseña no hay con qué vincularla (hoy; falta decidir D12)", not entro(destino) and not identidades(email), str(identidades(email)))
        nueva = "Clave-Nueva-%s!aA1" % uuid.uuid4().hex[:8]
        api("PUT", "/%s/users/%s/reset-password" % (REALM, usuario_por_email(REALM, email)["id"]), {"type": "password", "value": nueva, "temporary": False})
        persona = Navegador()
        destino, pagina = entrar_con_google(persona, email)
        destino, pagina = confirmar_vinculo(persona, pagina)
        destino, _ = con_contrasena(persona, pagina, email, nueva)
        check("cuando la cuenta tiene contraseña (p. ej. al recuperar la clave), se vincula", entro(destino) and identidades(email) == [ALIAS], str(identidades(email)))
    finally:
        try:
            api("PUT", "/" + REALM, dict(api("GET", "/" + REALM)[1], smtpServer=smtp_original))
        finally:
            api("DELETE", "/%s/identity-provider/instances/%s" % (REALM, ALIAS), None, (200, 204, 404))
            api("DELETE", "/" + FALSO, None, (200, 204, 404))
            for email in creados:
                u = usuario_por_email(REALM, email)
                if u:
                    api("DELETE", "/%s/users/%s" % (REALM, u["id"]), None, (200, 204, 404))

    print("\nPASS: %d  |  FAIL: %d" % (pasaron, fallaron))
    return 1 if fallaron else 0


if __name__ == "__main__":
    sys.exit(main())
