#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Contrato estático del realm `donatrack` (SPEC-06 A1-02a: contrato de tokens).

Corre sin Docker (stdlib solamente) y comprueba que el JSON versionado dice lo que el ADR y la SPEC
exigen. El contrato de runtime (pedir un token de verdad y leer sus claims) lo comprueba
`auth-service/src/test/java/.../RealmTokenContractIT.java` con Testcontainers.
"""

import glob
import json
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from test_auth_keycloak_compose import RAIZ, bloque_servicio, leer, variables_env  # noqa: E402

API_AUDIENCE = "donatrack-api"
ROLES = {"DONANTE", "ENTIDAD", "ADMIN"}
ROL_TECNICO = "SERVICIO"
ROLES_DE_REALM_MANAGEMENT = {"manage-users", "view-users", "query-users"}
ROLES_DE_SISTEMA = {"default-roles-donatrack", "offline_access", "uma_authorization"}
CLAIMS_CON_PII = {"email", "email_verified", "name", "given_name", "family_name",
                  "preferred_username"}
MATRIZ_DE_PERMISOS = ("docs", "arquitectura", "seguridad", "matriz-permisos.md")
IT_DEL_REALM = ("auth-service", "src", "test", "java", "grupo5", "auth",
                "RealmTokenContractIT.java")


def cargar_realm():
    ruta = os.path.join(RAIZ, "auth-service", "realm", "donatrack-realm.json")
    with open(ruta, encoding="utf-8") as f:
        return json.load(f)


def cliente(realm, client_id="cliente-liviano"):
    encontrados = [c for c in realm.get("clients", []) if c.get("clientId") == client_id]
    return encontrados[0] if encontrados else {}


def mapper(cli, nombre):
    encontrados = [m for m in cli.get("protocolMappers", []) if m.get("name") == nombre]
    return encontrados[0] if encontrados else {}


class RealmBaseTests(unittest.TestCase):
    def setUp(self):
        self.realm = cargar_realm()

    def test_realm_donatrack_habilitado(self):
        self.assertEqual(self.realm.get("realm"), "donatrack")
        self.assertIs(self.realm.get("enabled"), True)

    def test_tiene_los_tres_roles_de_negocio_y_el_tecnico(self):
        """A1-03 suma `default-roles-donatrack` y dos roles de sistema; E6, el técnico SERVICIO."""
        nombres = {r["name"] for r in self.realm.get("roles", {}).get("realm", [])}
        self.assertEqual(nombres - ROLES_DE_SISTEMA, ROLES | {ROL_TECNICO})
        self.assertEqual(nombres & ROLES_DE_SISTEMA, ROLES_DE_SISTEMA)

    def test_login_por_email(self):
        self.assertIs(self.realm.get("registrationEmailAsUsername"), True)
        self.assertIs(self.realm.get("loginWithEmailAllowed"), True)
        self.assertIs(self.realm.get("duplicateEmailsAllowed"), False)

    def test_access_token_de_15_minutos(self):
        self.assertEqual(self.realm.get("accessTokenLifespan"), 900)

    def test_sso_idle_de_8_horas(self):
        self.assertEqual(self.realm.get("ssoSessionIdleTimeout"), 8 * 60 * 60)


class ClienteLivianoTests(unittest.TestCase):
    def setUp(self):
        self.realm = cargar_realm()
        self.cli = cliente(self.realm)

    def test_existe_el_cliente(self):
        self.assertTrue(self.cli, "falta el cliente cliente-liviano")
        self.assertIs(self.cli.get("enabled"), True)
        self.assertEqual(self.cli.get("protocol"), "openid-connect")

    def test_confidencial_con_authorization_code_y_nada_mas(self):
        self.assertIs(self.cli.get("publicClient"), False)
        self.assertEqual(self.cli.get("clientAuthenticatorType"), "client-secret")
        self.assertIs(self.cli.get("standardFlowEnabled"), True)
        # Sin ROPC (ADR, alternativa rechazada) ni implicit. La cuenta de servicio da el token
        # SERVICIO (E6).
        self.assertIs(self.cli.get("directAccessGrantsEnabled"), False)
        self.assertIs(self.cli.get("implicitFlowEnabled"), False)
        self.assertIs(self.cli.get("serviceAccountsEnabled"), True)

    def test_secreto_y_urls_por_variable(self):
        self.assertEqual(self.cli.get("secret"), "${CLIENTE_LIVIANO_SECRET}")
        self.assertEqual(self.cli.get("redirectUris"),
                         ["${CLIENTE_LIVIANO_BASE_URL}/login/oauth2/code/keycloak"])
        self.assertEqual(self.cli.get("attributes", {}).get("post.logout.redirect.uris"),
                         "${CLIENTE_LIVIANO_BASE_URL}/")

    def test_sin_redirect_comodin(self):
        for uri in self.cli.get("redirectUris", []):
            self.assertNotIn("*", uri)

    def test_el_token_solo_lleva_los_roles_de_negocio(self):
        """Sin fullScopeAllowed=false, el claim roles traería default-roles-donatrack y demás."""
        self.assertIs(self.cli.get("fullScopeAllowed"), False)
        mapeos = [m for m in self.realm.get("scopeMappings", [])
                  if m.get("client") == "cliente-liviano"]
        self.assertEqual(len(mapeos), 1)
        self.assertEqual(set(mapeos[0]["roles"]), ROLES | {ROL_TECNICO})

    def test_el_scope_trae_todos_los_permisos_y_ninguna_plantilla(self):
        """Con fullScopeAllowed=false, un permiso fuera del scope no sale en el token. Van todos,
        porque una persona puede tener permisos sueltos; las plantillas no, para que su nombre no
        viaje en `permisos`."""
        mapeos = self.realm.get("clientScopeMappings", {}).get(API_AUDIENCE, [])
        self.assertEqual([m.get("client") for m in mapeos], ["cliente-liviano"])
        self.assertEqual(set(mapeos[0]["roles"]), set(catalogo_de_la_matriz()))

    def test_el_scope_trae_los_roles_minimos_de_realm_management(self):
        """La Admin API exige que el rol esté en el scope del cliente, además de asignado."""
        mapeos = self.realm.get("clientScopeMappings", {}).get("realm-management", [])
        self.assertEqual([m.get("client") for m in mapeos], ["cliente-liviano"])
        self.assertEqual(set(mapeos[0]["roles"]), ROLES_DE_REALM_MANAGEMENT)


class CuentaDeServicioTests(unittest.TestCase):
    """Client credentials con SERVICIO y lo justo para crear, buscar y editar usuarios."""

    def setUp(self):
        cuentas = [u for u in cargar_realm().get("users", [])
                   if u.get("serviceAccountClientId") == "cliente-liviano"]
        self.assertEqual(len(cuentas), 1)
        self.cuenta = cuentas[0]

    def test_es_la_del_cliente_liviano(self):
        self.assertEqual(self.cuenta["serviceAccountClientId"], "cliente-liviano")
        self.assertEqual(self.cuenta.get("username"), "service-account-cliente-liviano")

    def test_solo_el_rol_servicio_y_sin_el_rol_por_defecto(self):
        """Declarada en el realm para que no reciba default-roles-donatrack (y con él, DONANTE)."""
        self.assertEqual(self.cuenta.get("realmRoles"), [ROL_TECNICO])

    def test_roles_minimos_de_realm_management(self):
        self.assertEqual(set(self.cuenta.get("clientRoles", {}).get("realm-management", [])),
                         ROLES_DE_REALM_MANAGEMENT)
        self.assertEqual(set(self.cuenta.get("clientRoles", {})), {"realm-management"})


class MapperRolesTests(unittest.TestCase):
    def setUp(self):
        self.m = mapper(cliente(cargar_realm()), "roles")

    def test_claim_roles_plano_y_multivalor(self):
        self.assertTrue(self.m, "falta el mapper roles")
        self.assertEqual(self.m.get("protocolMapper"), "oidc-usermodel-realm-role-mapper")
        cfg = self.m.get("config", {})
        self.assertEqual(cfg.get("claim.name"), "roles")
        self.assertEqual(cfg.get("multivalued"), "true")
        self.assertEqual(cfg.get("jsonType.label"), "String")

    def test_va_en_access_id_y_token_liviano(self):
        cfg = self.m.get("config", {})
        for flag in ["access.token.claim", "id.token.claim", "userinfo.token.claim",
                     "lightweight.claim"]:
            self.assertEqual(cfg.get(flag), "true", flag)


class MapperAudienciaTests(unittest.TestCase):
    def setUp(self):
        self.m = mapper(cliente(cargar_realm()), "audiencia-api")

    def test_aud_del_api_en_el_access_token(self):
        self.assertTrue(self.m, "falta el mapper audiencia-api")
        self.assertEqual(self.m.get("protocolMapper"), "oidc-audience-mapper")
        cfg = self.m.get("config", {})
        self.assertEqual(cfg.get("included.custom.audience"), API_AUDIENCE)
        self.assertEqual(cfg.get("access.token.claim"), "true")


class MapperPermisosYPersonaTests(unittest.TestCase):
    """RX-05 y RX-06: `permisos` (roles de donatrack-api) y `personaId`, en los dos tokens."""

    def setUp(self):
        self.cli = cliente(cargar_realm())

    def test_permisos_planos_y_multivalor(self):
        m = mapper(self.cli, "permisos")
        self.assertEqual(m.get("protocolMapper"), "oidc-usermodel-client-role-mapper")
        cfg = m.get("config", {})
        self.assertEqual(cfg.get("usermodel.clientRoleMapping.clientId"), API_AUDIENCE)
        self.assertNotIn("usermodel.clientRoleMapping.rolePrefix", cfg)  # plano: sin prefijo
        self.assertEqual(cfg.get("claim.name"), "permisos")
        self.assertEqual(cfg.get("multivalued"), "true")

    def test_persona_id_desde_el_atributo(self):
        m = mapper(self.cli, "personaId")
        self.assertEqual(m.get("protocolMapper"), "oidc-usermodel-attribute-mapper")
        cfg = m.get("config", {})
        self.assertEqual(cfg.get("user.attribute"), "personaId")
        self.assertEqual(cfg.get("claim.name"), "personaId")
        self.assertEqual(cfg.get("multivalued"), "false")

    def test_solicitud_de_entidad_solo_en_el_id_token(self):
        cfg = mapper(self.cli, "solicitudEntidad").get("config", {})
        self.assertEqual(cfg.get("user.attribute"), "solicitudEntidad")
        self.assertEqual(cfg.get("claim.name"), "solicitudEntidad")
        self.assertEqual(cfg.get("id.token.claim"), "true")
        self.assertEqual(cfg.get("access.token.claim"), "false")

    def test_van_en_el_access_y_el_id_token(self):
        for nombre in ["permisos", "personaId"]:
            cfg = mapper(self.cli, nombre).get("config", {})
            for flag in ["access.token.claim", "id.token.claim"]:
                self.assertEqual(cfg.get(flag), "true", "%s: %s" % (nombre, flag))


def seccion(texto, titulo):
    """El cuerpo de una sección `## titulo` de un markdown, hasta la sección siguiente."""
    partes = re.split(r"(?m)^## ", texto)
    encontradas = [p for p in partes if p.startswith(titulo)]
    return encontradas[0] if encontradas else ""


def catalogo_de_la_matriz():
    """Los permisos de la tabla de §2 de la matriz, en orden."""
    texto = seccion(leer(*MATRIZ_DE_PERMISOS), "2. Catálogo de permisos")
    return re.findall(r"(?m)^\| `([A-Z_]+)` \|", texto)


def plantillas_de_la_matriz():
    """{plantilla: {permisos}} desde la tabla de §3 (✔ en la columna de la plantilla)."""
    texto = seccion(leer(*MATRIZ_DE_PERMISOS), "3. Plantillas")
    filas = [l for l in texto.splitlines() if l.startswith("|")]
    nombres = re.findall(r"`([A-Z_]+)`", filas[0])
    plantillas = {n: set() for n in nombres}
    for fila in filas[2:]:
        celdas = [c.strip() for c in fila.strip("|").split("|")]
        permiso = celdas[0].strip("`")
        for nombre, celda in zip(nombres, celdas[1:]):
            if celda == "✔":
                plantillas[nombre].add(permiso)
    return plantillas


class CatalogoDePermisosTests(unittest.TestCase):
    """RX-06 y RX-09: los roles de donatrack-api son el catálogo y las plantillas de la matriz."""

    def setUp(self):
        self.realm = cargar_realm()
        roles = self.realm.get("roles", {}).get("client", {}).get(API_AUDIENCE, [])
        self.roles = {r["name"]: r for r in roles}
        self.catalogo = catalogo_de_la_matriz()
        self.plantillas = plantillas_de_la_matriz()

    def test_la_matriz_se_pudo_leer(self):
        self.assertEqual(len(self.catalogo), 12, self.catalogo)
        self.assertEqual(set(self.plantillas), {"RESPONSABLE_ENTIDAD", "OPERADOR_ENTIDAD",
                                                "RESPONSABLE_DONANTE", "DONANTE_PERSONA"})

    def test_existe_el_cliente_de_la_api_sin_flujos(self):
        api = cliente(self.realm, API_AUDIENCE)
        self.assertTrue(api, "falta el cliente donatrack-api")
        self.assertIs(api.get("bearerOnly"), True)
        # Id fijo: en el alta de perfil, la cuenta de servicio asigna plantillas sin view-clients.
        self.assertEqual(api.get("id"), "7f3c2a9e-5b1d-4e8a-9c6f-2d4b8a1e0c53")
        for flujo in ["standardFlowEnabled", "implicitFlowEnabled", "directAccessGrantsEnabled",
                      "serviceAccountsEnabled", "publicClient"]:
            self.assertIs(api.get(flujo), False, flujo)

    def test_los_roles_son_el_catalogo_mas_las_plantillas(self):
        self.assertEqual(set(self.roles), set(self.catalogo) | set(self.plantillas))

    def test_los_permisos_no_son_compuestos(self):
        for permiso in self.catalogo:
            self.assertFalse(self.roles[permiso].get("composite", False), permiso)

    def test_cada_plantilla_es_el_compuesto_de_la_matriz(self):
        for nombre, permisos in self.plantillas.items():
            rol = self.roles[nombre]
            self.assertIs(rol.get("composite"), True, nombre)
            compuestos = rol.get("composites", {})
            hijos = set(compuestos.get("client", {}).get(API_AUDIENCE, []))
            self.assertEqual(hijos, permisos, nombre)
            self.assertNotIn("realm", compuestos, "%s no debe dar roles de plataforma" % nombre)


    def test_los_enum_de_permisos_de_los_servicios_son_permisos_del_realm(self):
        patron = os.path.join(RAIZ, "*-service", "src", "main", "java", "**", "Permiso*.java")
        enums = glob.glob(patron, recursive=True)
        self.assertTrue(enums, "ningún servicio declara su enum de permisos")
        for ruta in enums:
            cuerpo = re.search(r"enum \w+ implements Permiso \{(.*?)\}", leer(ruta), re.S)
            for permiso in re.findall(r"[A-Z][A-Z_]+", cuerpo.group(1)):
                self.assertIn(permiso, self.catalogo, "%s: %s" % (os.path.basename(ruta), permiso))


class GoogleTests(unittest.TestCase):
    """RX-01: Google como identity provider, credenciales por variable; D4 con el flujo default."""

    def setUp(self):
        proveedores = cargar_realm().get("identityProviders", [])
        self.assertEqual([p.get("alias") for p in proveedores], ["google"])
        self.google = proveedores[0]

    def test_proveedor_google_habilitado(self):
        self.assertEqual(self.google.get("providerId"), "google")
        self.assertIs(self.google.get("enabled"), True)

    def test_credenciales_por_variable(self):
        cfg = self.google.get("config", {})
        self.assertEqual(cfg.get("clientId"), "${GOOGLE_CLIENT_ID}")
        self.assertEqual(cfg.get("clientSecret"), "${GOOGLE_CLIENT_SECRET}")

    def test_primer_ingreso_con_el_flujo_por_defecto(self):
        """`first broker login` pide la clave para vincular una cuenta existente (D4)."""
        self.assertEqual(self.google.get("firstBrokerLoginFlowAlias"), "first broker login")
        self.assertIs(self.google.get("trustEmail"), False)

    def test_variables_opcionales_en_el_compose_y_en_los_env(self):
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        self.assertIn("GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:-}", kc)
        self.assertIn("GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET:-}", kc)
        for archivo in [".env.example", ".env"]:
            env = variables_env(leer(archivo))
            for var in ["GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET"]:
                self.assertIn(var, env, "%s no define %s" % (archivo, var))
        texto = leer("scripts", "generar-env-db.sh")
        self.assertIn('GOOGLE_CLIENT_ID=', texto)
        self.assertIn('GOOGLE_CLIENT_SECRET=', texto)


class EnvTrackeadoTests(unittest.TestCase):
    """RX-13: el `.env` trackeado tiene lo que exige el compose (`up` sin --env-file)."""

    def test_el_env_tiene_las_variables_obligatorias_de_keycloak(self):
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        env = variables_env(leer(".env"))
        for var in set(re.findall(r"\$\{(\w+):\?", kc)):
            self.assertTrue(env.get(var), "%s es obligatoria en el compose y falta en .env" % var)


class SinDatosPersonalesTests(unittest.TestCase):
    """RNF-AUTH-09 / RNF-PRIV-05: ni email ni nombre en los tokens."""

    def setUp(self):
        self.cli = cliente(cargar_realm())

    def test_el_cliente_no_recibe_los_scopes_profile_ni_email(self):
        asignados = (set(self.cli.get("defaultClientScopes", []))
                     | set(self.cli.get("optionalClientScopes", [])))
        self.assertFalse(asignados & {"profile", "email"}, asignados)
        self.assertEqual(self.cli.get("defaultClientScopes"), ["basic"])  # basic trae el claim sub

    def test_ningun_mapper_emite_claims_con_pii(self):
        for m in self.cli.get("protocolMappers", []):
            claim = m.get("config", {}).get("claim.name")
            mensaje = "el mapper %s emite %s" % (m.get("name"), claim)
            self.assertNotIn(claim, CLAIMS_CON_PII, mensaje)


def perfil_de_usuario(realm):
    """El perfil declarativo va como un JSON dentro de un string en components."""
    comps = realm.get("components", {}).get("org.keycloak.userprofile.UserProfileProvider", [])
    if not comps:
        return {}
    return json.loads(comps[0]["config"]["kc.user.profile.config"][0])


def atributo(perfil, nombre):
    encontrados = [a for a in perfil.get("attributes", []) if a.get("name") == nombre]
    return encontrados[0] if encontrados else {}


class PoliticaDeClaveTests(unittest.TestCase):
    def test_politica_de_clave_de_la_spec(self):
        self.assertEqual(cargar_realm().get("passwordPolicy"),
                         "length(10) and maxLength(128) and notEmail")


class FuerzaBrutaTests(unittest.TestCase):
    """RNF-AUTH-07: 5 fallos en 15 minutos bloquean 15 minutos (bloqueo temporal, no permanente)."""

    def setUp(self):
        self.realm = cargar_realm()

    def test_proteccion_activa_y_bloqueo_temporal(self):
        self.assertIs(self.realm.get("bruteForceProtected"), True)
        self.assertIs(self.realm.get("permanentLockout"), False)

    def test_cinco_fallos(self):
        self.assertEqual(self.realm.get("failureFactor"), 5)

    def test_ventana_y_bloqueo_de_15_minutos(self):
        # Ventana de los 5 fallos, cuánto bloquea y sin escalar más allá.
        self.assertEqual(self.realm.get("maxDeltaTimeSeconds"), 15 * 60)
        self.assertEqual(self.realm.get("waitIncrementSeconds"), 15 * 60)
        self.assertEqual(self.realm.get("maxFailureWaitSeconds"), 15 * 60)


class RotacionDeRefreshTests(unittest.TestCase):
    """D7 / RF-AUTH-09: cada refresh entrega uno nuevo y reusar uno viejo corta la sesión."""

    def test_rotacion_sin_reuso(self):
        realm = cargar_realm()
        self.assertIs(realm.get("revokeRefreshToken"), True)
        self.assertEqual(realm.get("refreshTokenMaxReuse"), 0)


class PerfilDeUsuarioTests(unittest.TestCase):
    """Sin nombre ni apellido obligatorios: sirve también para una persona jurídica."""

    def setUp(self):
        self.perfil = perfil_de_usuario(cargar_realm())

    def test_el_perfil_esta_definido(self):
        self.assertTrue(self.perfil, "falta el perfil de usuario en components")
        nombres = {a["name"] for a in self.perfil["attributes"]}
        self.assertEqual(nombres, {"username", "email", "firstName", "lastName", "personaId",
                                   "solicitudEntidad", "administracion", "altaPor"})

    def test_persona_y_solicitud_solo_las_ve_y_edita_el_admin(self):
        """Sin declararlos, Keycloak descarta los atributos. El usuario no cambia en nombre de quién
        opera, ni se marca como cuenta de administración, ni cambia quién lo dio de alta."""
        for nombre in ["personaId", "solicitudEntidad", "administracion", "altaPor"]:
            self.assertEqual(atributo(self.perfil, nombre).get("permissions"),
                             {"view": ["admin"], "edit": ["admin"]}, nombre)

    def test_nombre_y_apellido_no_son_obligatorios(self):
        for nombre in ["firstName", "lastName"]:
            self.assertNotIn("required", atributo(self.perfil, nombre), nombre)

    def test_el_email_sigue_siendo_obligatorio_para_el_usuario(self):
        self.assertEqual(atributo(self.perfil, "email").get("required", {}).get("roles"), ["user"])

    def test_el_usuario_puede_editar_su_nombre_y_el_email(self):
        for nombre in ["email", "firstName", "lastName"]:
            permisos = atributo(self.perfil, nombre).get("permissions", {})
            self.assertIn("user", permisos.get("edit", []))


def rol_del_realm(realm, nombre):
    encontrados = [r for r in realm.get("roles", {}).get("realm", []) if r.get("name") == nombre]
    return encontrados[0] if encontrados else {}


class RolPorDefectoTests(unittest.TestCase):
    """D10: el donante se autorregistra; ENTIDAD y ADMIN no se autoasignan."""

    def setUp(self):
        self.realm = cargar_realm()

    def test_el_registro_publico_esta_habilitado(self):
        self.assertIs(self.realm.get("registrationAllowed"), True)

    def test_el_rol_por_defecto_del_realm(self):
        self.assertEqual(self.realm.get("defaultRole", {}).get("name"), "default-roles-donatrack")

    def test_el_rol_por_defecto_da_donante_y_nada_mas_de_negocio(self):
        """Sin offline_access: nadie pide tokens offline."""
        por_defecto = rol_del_realm(self.realm, "default-roles-donatrack")
        self.assertIs(por_defecto.get("composite"), True)
        self.assertEqual(set(por_defecto["composites"]["realm"]), {"uma_authorization", "DONANTE"})
        self.assertNotIn("composites", rol_del_realm(self.realm, "DONANTE"))


class AdminInicialTests(unittest.TestCase):
    """A1-03: el ADMIN se importa con su email y su clave por variable, solo con el rol ADMIN."""

    def setUp(self):
        self.usuarios = cargar_realm().get("users", [])

    def test_se_importan_el_admin_y_las_cuentas_de_servicio(self):
        self.assertEqual(len(self.usuarios), 3)
        self.assertEqual(self.usuarios[1].get("serviceAccountClientId"), "cliente-liviano")
        self.assertEqual(self.usuarios[2].get("serviceAccountClientId"), "donatrack-pruebas")

    def test_email_por_variable(self):
        admin = self.usuarios[0]
        self.assertEqual(admin.get("username"), "${AUTH_ADMIN_EMAIL}")
        self.assertEqual(admin.get("email"), "${AUTH_ADMIN_EMAIL}")
        self.assertIs(admin.get("enabled"), True)
        self.assertIs(admin.get("emailVerified"), True)

    def test_clave_por_variable_y_no_temporal(self):
        credenciales = self.usuarios[0].get("credentials", [])
        self.assertEqual(len(credenciales), 1)
        self.assertEqual(credenciales[0].get("type"), "password")
        self.assertEqual(credenciales[0].get("value"), "${AUTH_ADMIN_PASSWORD}")
        self.assertIs(credenciales[0].get("temporary"), False)

    def test_solo_el_rol_admin_y_sin_el_rol_por_defecto(self):
        """Un usuario importado solo recibe los roles que se le listan: no hereda DONANTE."""
        self.assertEqual(self.usuarios[0].get("realmRoles"), ["ADMIN"])

    def test_ningun_valor_de_credencial_es_literal(self):
        for usuario in self.usuarios:
            for cred in usuario.get("credentials", []):
                self.assertTrue(str(cred.get("value", "")).startswith("${"), cred)


class VariablesDelAdminTests(unittest.TestCase):
    def test_el_compose_las_exige(self):
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        self.assertIn("AUTH_ADMIN_EMAIL=${AUTH_ADMIN_EMAIL:?", kc)
        self.assertIn("AUTH_ADMIN_PASSWORD=${AUTH_ADMIN_PASSWORD:?", kc)

    def test_env_example_cumple_la_politica_de_clave_del_realm(self):
        """El import valida la política: con una clave corta o igual al email, no arranca."""
        env = variables_env(leer(".env.example"))
        self.assertRegex(env.get("AUTH_ADMIN_EMAIL", ""), r"^[^@\s]+@[^@\s]+\.[a-z]+$")
        clave = env.get("AUTH_ADMIN_PASSWORD", "")
        self.assertTrue(clave.startswith("cambiar-"), clave)
        self.assertGreaterEqual(len(clave), 10)
        self.assertLessEqual(len(clave), 128)
        self.assertNotEqual(clave, env["AUTH_ADMIN_EMAIL"])

    def test_el_generador_hace_aleatoria_la_clave_del_admin(self):
        texto = leer("scripts", "generar-env-db.sh")
        self.assertIn("AUTH_ADMIN_EMAIL=admin@donatrack.local", texto)
        self.assertRegex(texto, r"AUTH_ADMIN_PASSWORD=\$\(pw\)")


class SecretosYVariablesTests(unittest.TestCase):
    def setUp(self):
        self.texto = leer("auth-service", "realm", "donatrack-realm.json")

    def test_sin_secretos_literales_en_el_realm(self):
        literales = re.findall(r'"(?:secret|password)"\s*:\s*"([^"]*)"', self.texto)
        for valor in literales:
            self.assertTrue(valor.startswith("${"), "secreto literal en el realm: %r" % valor)

    def test_cada_variable_del_realm_la_recibe_el_servicio_keycloak(self):
        # Solo variables de entorno (MAYÚSCULAS): las claves de i18n del perfil, como ${email}, son
        # de Keycloak.
        usadas = set(re.findall(r"\$\{([A-Z][A-Z0-9_]*)\}", self.texto))
        self.assertTrue(usadas, "el realm debería usar variables")
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        pasadas = set(re.findall(r"(?m)^\s+- (\w+)=", kc))
        self.assertEqual(usadas - pasadas, set(), "el realm usa variables que el compose no pasa")

    def test_las_variables_nuevas_son_obligatorias_en_el_compose(self):
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        self.assertIn("CLIENTE_LIVIANO_SECRET=${CLIENTE_LIVIANO_SECRET:?", kc)
        self.assertIn("CLIENTE_LIVIANO_BASE_URL=${CLIENTE_LIVIANO_BASE_URL:?", kc)

    def test_env_example_define_las_variables_del_realm(self):
        env = variables_env(leer(".env.example"))
        self.assertTrue(env.get("CLIENTE_LIVIANO_SECRET", "").startswith("cambiar-"))
        self.assertRegex(env.get("CLIENTE_LIVIANO_BASE_URL", ""), r"^http://localhost:\d+$")

    def test_generador_define_el_secreto_aleatorio(self):
        texto = leer("scripts", "generar-env-db.sh")
        self.assertRegex(texto, r'CLIENTE_LIVIANO_SECRET=\$\(pw\)')
        self.assertIn("CLIENTE_LIVIANO_BASE_URL=http://localhost:", texto)


class ClientePruebasTests(unittest.TestCase):
    """E2-07: cuenta de servicio con ADMIN para integration-tests, k6 y Postman con la seguridad encendida.

    Es la credencial más poderosa del realm, así que se fija todo lo que la limita: solo client
    credentials (sin flujos de personas), solo el rol ADMIN en su token, y el secreto por variable.
    """

    def setUp(self):
        self.realm = cargar_realm()
        self.cli = next(c for c in self.realm["clients"] if c["clientId"] == "donatrack-pruebas")
        self.liviano = next(c for c in self.realm["clients"] if c["clientId"] == "cliente-liviano")

    def test_es_confidencial_y_su_secreto_sale_de_una_variable(self):
        self.assertIs(self.cli.get("publicClient"), False)
        self.assertEqual(self.cli.get("clientAuthenticatorType"), "client-secret")
        self.assertEqual(self.cli.get("secret"), "${PRUEBAS_CLIENT_SECRET}")

    def test_solo_client_credentials_y_ningun_flujo_de_personas(self):
        self.assertIs(self.cli.get("serviceAccountsEnabled"), True)
        for flujo in ("standardFlowEnabled", "implicitFlowEnabled", "directAccessGrantsEnabled"):
            self.assertIs(self.cli.get(flujo), False, flujo)
        self.assertEqual(self.cli.get("redirectUris"), [])
        self.assertEqual(self.cli.get("webOrigins"), [])

    def test_el_scope_completo_esta_apagado_y_solo_se_mapea_admin(self):
        # Con fullScopeAllowed en false, un rol que no esté en scopeMappings no llega al token.
        self.assertIs(self.cli.get("fullScopeAllowed"), False)
        mapeos = [m for m in self.realm.get("scopeMappings", []) if m["client"] == "donatrack-pruebas"]
        self.assertEqual(len(mapeos), 1)
        self.assertEqual(mapeos[0]["roles"], ["ADMIN"])

    def test_sin_scopes_de_datos_personales(self):
        self.assertEqual(self.cli.get("defaultClientScopes"), ["basic"])
        self.assertEqual(self.cli.get("optionalClientScopes"), [])

    def test_los_mappers_son_los_del_cliente_liviano_para_roles_y_audiencia(self):
        # Mismos claims que el resto (RNF-AUTH-08): `roles` plano y `aud` con la API. Nada más:
        # sin permisos, sin personaId, sin solicitudEntidad.
        def por_nombre(c):
            return {m["name"]: m for m in c["protocolMappers"]}
        propios, ajenos = por_nombre(self.cli), por_nombre(self.liviano)
        self.assertEqual(set(propios), {"roles", "audiencia-api"})
        for nombre in propios:
            self.assertEqual(propios[nombre], ajenos[nombre], nombre)

    def test_la_cuenta_de_servicio_tiene_solo_admin_y_ningun_rol_de_cliente(self):
        cuenta = next(u for u in self.realm["users"] if u.get("serviceAccountClientId") == "donatrack-pruebas")
        self.assertEqual(cuenta["username"], "service-account-donatrack-pruebas")
        self.assertEqual(cuenta.get("realmRoles"), ["ADMIN"])
        self.assertNotIn("clientRoles", cuenta, "no necesita la Admin API ni permisos de cliente")
        self.assertNotIn("credentials", cuenta)

    def test_hay_exactamente_dos_cuentas_de_servicio(self):
        cuentas = [u["serviceAccountClientId"] for u in self.realm["users"] if u.get("serviceAccountClientId")]
        self.assertEqual(sorted(cuentas), ["cliente-liviano", "donatrack-pruebas"])

    def test_ningun_otro_cliente_de_servicio_recibe_admin(self):
        for u in self.realm["users"]:
            if u.get("serviceAccountClientId") and u["serviceAccountClientId"] != "donatrack-pruebas":
                self.assertNotIn("ADMIN", u.get("realmRoles", []), u["username"])


class SecretoDePruebasTests(unittest.TestCase):
    def test_el_secreto_lo_reciben_el_realm_y_los_dos_compose_y_es_obligatorio(self):
        for compose in ("docker-compose.yml", "docker-compose.preprod.yml"):
            kc = bloque_servicio(leer(compose), "keycloak") or ""
            self.assertTrue("PRUEBAS_CLIENT_SECRET=${PRUEBAS_CLIENT_SECRET:?" in kc, compose)

    def test_env_example_env_y_generador_lo_definen(self):
        for archivo in (".env.example", ".env"):
            env = variables_env(leer(archivo))
            self.assertTrue(env.get("PRUEBAS_CLIENT_SECRET", "").startswith("cambiar-"), archivo)
        self.assertRegex(leer("scripts", "generar-env-db.sh"), r'PRUEBAS_CLIENT_SECRET=\$\(pw\)')


class ContextoDeBuildTests(unittest.TestCase):
    def test_dockerignore_deja_afuera_el_modulo_maven(self):
        """El contexto de build es auth-service/: sin esto viajan src/ y target/ al daemon."""
        ruta = os.path.join(RAIZ, "auth-service", ".dockerignore")
        self.assertTrue(os.path.isfile(ruta), "falta auth-service/.dockerignore")
        lineas = {l.strip() for l in leer("auth-service", ".dockerignore").splitlines()}
        for excluido in ["src", "target", "pom.xml"]:
            self.assertIn(excluido, lineas)


class ModuloDeContratoTests(unittest.TestCase):
    def test_existe_el_test_de_contrato_con_testcontainers(self):
        ruta = os.path.join(RAIZ, *IT_DEL_REALM)
        self.assertTrue(os.path.isfile(ruta), "falta RealmTokenContractIT.java")
        texto = leer(*IT_DEL_REALM)
        for fragmento in ["ImageFromDockerfile", "GenericContainer", "authorization_code",
                          "\"roles\"", "\"aud\"", "900", "directAccessGrants",
                          "reset-password", "attack-detection", "users/profile", "refresh_token",
                          "invalid_grant", "prompt", "login-actions/registration",
                          "password-confirm", "AUTH_ADMIN_EMAIL", "AUTH_ADMIN_PASSWORD",
                          "role-mappings/realm/composite",
                          "\"permisos\"", "\"personaId\"", "client_credentials", "\"SERVICIO\"",
                          "GOOGLE_CLIENT_ID", "offline_access"]:
            self.assertIn(fragmento, texto)
        for pii in ["email", "name", "given_name", "family_name", "preferred_username"]:
            self.assertIn('"%s"' % pii, texto, "el IT no verifica la ausencia de %s" % pii)

    def test_el_pom_usa_las_mismas_dependencias_que_el_resto_del_repo(self):
        pom = leer("auth-service", "pom.xml")
        self.assertIn("<groupId>org.testcontainers</groupId>", pom)
        # Solo el núcleo, sin versión (la administra el BOM). `junit-jupiter` mezclaba 1.20.4 con el
        # núcleo 2.0.5.
        self.assertIn("<artifactId>testcontainers</artifactId>", pom)
        self.assertNotIn("<artifactId>junit-jupiter</artifactId>", pom)
        self.assertNotRegex(pom, r"<artifactId>testcontainers</artifactId>\s*<version>")
        self.assertIn("<artifactId>jackson-databind</artifactId>", pom)  # como integration-tests
        self.assertIn("<artifactId>donatrack</artifactId>", pom)  # hereda del pom raíz

    def test_el_contrato_no_corre_en_el_build_por_defecto(self):
        pom = leer("auth-service", "pom.xml")
        self.assertIn("<id>contrato-auth</id>", pom)
        self.assertIn("maven-failsafe-plugin", pom)
        # failsafe solo dentro del perfil: antes del <profiles> no puede aparecer
        antes_de_perfiles = pom.split("<profiles>", 1)[0]
        self.assertNotIn("maven-failsafe-plugin", antes_de_perfiles)

    def test_el_modulo_hereda_del_pom_raiz(self):
        """Hereda del padre (Spotless, JaCoCo, BOMs) pero no está en <modules> (E2-12 lo dejó así: no
        produce un JAR): el `mvn` de los demás no lo compila y el CI lo trata en su propio job."""
        self.assertIn("<relativePath>../pom.xml</relativePath>", leer("auth-service", "pom.xml"))


if __name__ == "__main__":
    unittest.main()
