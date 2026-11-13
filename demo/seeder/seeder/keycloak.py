"""Keycloak: el cliente demo-seeder, las cuentas de la demo y sus tokens.

El cliente del sitio (cliente-liviano) no deja pedir un token con usuario y clave, y está bien
que no lo deje. El seeder crea, solo en el Keycloak que está corriendo, un cliente propio con
login directo y los mismos mappers (roles, permisos, personaId y la audiencia de la API), con un
secreto aleatorio en cada corrida. El realm del repo no cambia.
"""

from __future__ import annotations

import secrets
import time

import requests

from .api import ErrorDeApi

CLIENTE_SEEDER = "demo-seeder"
CLIENTE_API = "donatrack-api"
_CLAIMS = {
    "access.token.claim": "true",
    "id.token.claim": "true",
    "userinfo.token.claim": "true",
    "introspection.token.claim": "true",
}


def _mappers() -> list[dict]:
    return [
        {
            "name": "roles",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-realm-role-mapper",
            "config": {"claim.name": "roles", "multivalued": "true",
                       "jsonType.label": "String", **_CLAIMS},
        },
        {
            "name": "permisos",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-client-role-mapper",
            "config": {"usermodel.clientRoleMapping.clientId": CLIENTE_API,
                       "claim.name": "permisos", "multivalued": "true",
                       "jsonType.label": "String", **_CLAIMS},
        },
        {
            "name": "personaId",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-usermodel-attribute-mapper",
            "config": {"user.attribute": "personaId", "claim.name": "personaId",
                       "multivalued": "false", "jsonType.label": "String", **_CLAIMS},
        },
        {
            "name": "audiencia-api",
            "protocol": "openid-connect",
            "protocolMapper": "oidc-audience-mapper",
            "config": {"included.custom.audience": CLIENTE_API,
                       "access.token.claim": "true", "id.token.claim": "false",
                       "introspection.token.claim": "true"},
        },
    ]


class Keycloak:
    def __init__(self, base: str, realm: str, admin_usuario: str, admin_clave: str):
        self.base = base.rstrip("/")
        self.realm = realm
        self._admin_usuario = admin_usuario
        self._admin_clave = admin_clave
        self._admin_token: tuple[str, float] | None = None
        self._secreto: str | None = None
        self._tokens: dict[str, tuple[str, float]] = {}
        self._id_api: str | None = None
        self.sesion = requests.Session()

    # --- Admin REST ------------------------------------------------------------------------

    def _token_admin(self) -> str:
        if self._admin_token and self._admin_token[1] > time.monotonic():
            return self._admin_token[0]
        respuesta = self.sesion.post(
            f"{self.base}/realms/master/protocol/openid-connect/token",
            data={"grant_type": "password", "client_id": "admin-cli",
                  "username": self._admin_usuario, "password": self._admin_clave},
            timeout=30,
        )
        if respuesta.status_code != 200:
            raise ErrorDeApi("POST", "token del admin de la consola", respuesta.status_code,
                             respuesta.text)
        cuerpo = respuesta.json()
        self._admin_token = (cuerpo["access_token"], time.monotonic() + cuerpo["expires_in"] - 15)
        return self._admin_token[0]

    def _admin(self, metodo: str, ruta: str, json=None, esperado=(200, 201, 204), params=None):
        url = f"{self.base}/admin/realms/{self.realm}{ruta}"
        respuesta = self.sesion.request(
            metodo, url, json=json, params=params, timeout=30,
            headers={"Authorization": f"Bearer {self._token_admin()}"},
        )
        if respuesta.status_code not in esperado:
            raise ErrorDeApi(metodo, url, respuesta.status_code, respuesta.text)
        return respuesta

    def _id_cliente(self, client_id: str) -> str | None:
        encontrados = self._admin("GET", "/clients", params={"clientId": client_id}).json()
        return encontrados[0]["id"] if encontrados else None

    def preparar_cliente_seeder(self) -> None:
        """Crea (o recrea) el cliente demo-seeder con un secreto nuevo."""
        existente = self._id_cliente(CLIENTE_SEEDER)
        if existente:
            self._admin("DELETE", f"/clients/{existente}")
        self._secreto = secrets.token_urlsafe(32)
        self._admin("POST", "/clients", json={
            "clientId": CLIENTE_SEEDER,
            "name": "Seeder de demo (SPEC-10)",
            "description": "Solo para sembrar la demo. No existe en el realm del repo.",
            "enabled": True,
            "publicClient": False,
            "secret": self._secreto,
            "standardFlowEnabled": False,
            "directAccessGrantsEnabled": True,
            "serviceAccountsEnabled": False,
            "fullScopeAllowed": True,
            "protocol": "openid-connect",
            "protocolMappers": _mappers(),
        }, esperado=(201,))

    def borrar_cliente_seeder(self) -> None:
        existente = self._id_cliente(CLIENTE_SEEDER)
        if existente:
            self._admin("DELETE", f"/clients/{existente}")

    def id_api(self) -> str:
        if self._id_api is None:
            self._id_api = self._id_cliente(CLIENTE_API)
        return self._id_api

    def buscar_usuario(self, correo: str) -> dict | None:
        encontrados = self._admin(
            "GET", "/users", params={"email": correo, "exact": "true"}
        ).json()
        return encontrados[0] if encontrados else None

    def crear_usuario(
        self,
        correo: str,
        nombre: str,
        apellido: str,
        clave: str,
        atributos: dict[str, str],
        rol_realm: str | None = None,
        plantilla: str | None = None,
    ) -> str:
        respuesta = self._admin("POST", "/users", json={
            "username": correo,
            "email": correo,
            "firstName": nombre,
            "lastName": apellido,
            "enabled": True,
            "emailVerified": True,
            "attributes": {k: [v] for k, v in atributos.items()},
            "credentials": [{"type": "password", "value": clave, "temporary": False}],
        }, esperado=(201,))
        id_usuario = respuesta.headers["Location"].rstrip("/").rsplit("/", 1)[-1]
        if rol_realm:
            self._asignar(f"/users/{id_usuario}/role-mappings/realm", rol_realm)
        if plantilla:
            self._asignar(f"/users/{id_usuario}/role-mappings/clients/{self.id_api()}", plantilla)
        return id_usuario

    def _asignar(self, ruta: str, rol: str) -> None:
        disponibles = self._admin("GET", ruta + "/available").json()
        elegido = [r for r in disponibles if r["name"] == rol]
        if not elegido:
            raise LookupError(f"El rol {rol} no está disponible en {ruta}")
        self._admin("POST", ruta, json=elegido)

    def asignar_plantilla(self, id_usuario: str, plantilla: str) -> None:
        self._asignar(f"/users/{id_usuario}/role-mappings/clients/{self.id_api()}", plantilla)

    def asignar_rol(self, id_usuario: str, rol: str) -> None:
        self._asignar(f"/users/{id_usuario}/role-mappings/realm", rol)

    def quitar_atributo(self, id_usuario: str, atributo: str) -> None:
        cuenta = self._admin("GET", f"/users/{id_usuario}").json()
        atributos = dict(cuenta.get("attributes") or {})
        atributos.pop(atributo, None)
        cuenta["attributes"] = atributos
        self._admin("PUT", f"/users/{id_usuario}", json=cuenta)

    # --- Tokens de las cuentas -------------------------------------------------------------

    def token_de(self, correo: str, clave: str) -> str:
        guardado = self._tokens.get(correo)
        if guardado and guardado[1] > time.monotonic():
            return guardado[0]
        if self._secreto is None:
            raise RuntimeError("Primero hay que preparar el cliente demo-seeder")
        respuesta = self.sesion.post(
            f"{self.base}/realms/{self.realm}/protocol/openid-connect/token",
            data={"grant_type": "password", "client_id": CLIENTE_SEEDER,
                  "client_secret": self._secreto, "username": correo, "password": clave,
                  "scope": "openid"},
            timeout=30,
        )
        if respuesta.status_code != 200:
            raise ErrorDeApi("POST", f"token de {correo}", respuesta.status_code, respuesta.text)
        cuerpo = respuesta.json()
        self._tokens[correo] = (cuerpo["access_token"], time.monotonic() + cuerpo["expires_in"] - 15)
        return self._tokens[correo][0]

    def olvidar_token(self, correo: str) -> None:
        """Después de cambiar los roles de una cuenta, el token viejo ya no sirve."""
        self._tokens.pop(correo, None)
