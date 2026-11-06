#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Contrato estático del realm `donatrack` (SPEC-06 A1-02a: contrato de tokens).

Corre sin Docker (stdlib solamente) y comprueba que el JSON versionado dice lo que el ADR y la SPEC
exigen. El contrato de runtime (pedir un token de verdad y leer sus claims) lo comprueba
`auth-service/src/test/java/.../RealmTokenContractIT.java` con Testcontainers.
"""

import json
import os
import re
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from test_auth_keycloak_compose import RAIZ, bloque_servicio, leer, variables_env  # noqa: E402

API_AUDIENCE = "donatrack-api"
ROLES = {"DONANTE", "ENTIDAD", "ADMIN"}
CLAIMS_CON_PII = {"email", "email_verified", "name", "given_name", "family_name", "preferred_username"}


def cargar_realm():
    with open(os.path.join(RAIZ, "auth-service", "realm", "donatrack-realm.json"), encoding="utf-8") as f:
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

    def test_tiene_exactamente_los_tres_roles(self):
        nombres = {r["name"] for r in self.realm.get("roles", {}).get("realm", [])}
        self.assertEqual(nombres, ROLES)

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
        # Sin ROPC (ADR, alternativa rechazada), sin implicit y sin cuenta de servicio.
        self.assertIs(self.cli.get("directAccessGrantsEnabled"), False)
        self.assertIs(self.cli.get("implicitFlowEnabled"), False)
        self.assertIs(self.cli.get("serviceAccountsEnabled"), False)

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
        """Sin fullScopeAllowed=false, el claim roles traería default-roles-donatrack, offline_access, etc."""
        self.assertIs(self.cli.get("fullScopeAllowed"), False)
        mapeos = [m for m in self.realm.get("scopeMappings", []) if m.get("client") == "cliente-liviano"]
        self.assertEqual(len(mapeos), 1)
        self.assertEqual(set(mapeos[0]["roles"]), ROLES)


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
        for flag in ["access.token.claim", "id.token.claim", "userinfo.token.claim", "lightweight.claim"]:
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


class SinDatosPersonalesTests(unittest.TestCase):
    """RNF-AUTH-09 / RNF-PRIV-05: ni email ni nombre en los tokens."""

    def setUp(self):
        self.cli = cliente(cargar_realm())

    def test_el_cliente_no_recibe_los_scopes_profile_ni_email(self):
        asignados = set(self.cli.get("defaultClientScopes", [])) | set(self.cli.get("optionalClientScopes", []))
        self.assertFalse(asignados & {"profile", "email"}, asignados)
        self.assertEqual(self.cli.get("defaultClientScopes"), ["basic"])  # basic trae el claim sub

    def test_ningun_mapper_emite_claims_con_pii(self):
        for m in self.cli.get("protocolMappers", []):
            claim = m.get("config", {}).get("claim.name")
            self.assertNotIn(claim, CLAIMS_CON_PII, "el mapper %s emite %s" % (m.get("name"), claim))


class SecretosYVariablesTests(unittest.TestCase):
    def setUp(self):
        self.texto = leer("auth-service", "realm", "donatrack-realm.json")

    def test_sin_secretos_literales_en_el_realm(self):
        literales = re.findall(r'"(?:secret|password)"\s*:\s*"([^"]*)"', self.texto)
        for valor in literales:
            self.assertTrue(valor.startswith("${"), "secreto literal en el realm: %r" % valor)

    def test_cada_variable_del_realm_la_recibe_el_servicio_keycloak(self):
        usadas = set(re.findall(r"\$\{(\w+)\}", self.texto))
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
        ruta = os.path.join(RAIZ, "auth-service", "src", "test", "java", "grupo5", "auth",
                            "RealmTokenContractIT.java")
        self.assertTrue(os.path.isfile(ruta), "falta RealmTokenContractIT.java")
        texto = leer("auth-service", "src", "test", "java", "grupo5", "auth", "RealmTokenContractIT.java")
        for fragmento in ["ImageFromDockerfile", "GenericContainer", "authorization_code", "\"roles\"",
                          "\"aud\"", "900", "directAccessGrants"]:
            self.assertIn(fragmento, texto)
        for pii in ["email", "name", "given_name", "family_name", "preferred_username"]:
            self.assertIn('"%s"' % pii, texto, "el IT no verifica la ausencia de %s" % pii)

    def test_el_pom_usa_las_mismas_dependencias_que_el_resto_del_repo(self):
        pom = leer("auth-service", "pom.xml")
        self.assertIn("<groupId>org.testcontainers</groupId>", pom)
        # Solo el núcleo, sin versión (la administra el BOM). `junit-jupiter` mezclaba 1.20.4 con el núcleo 2.0.5.
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
        """Hereda del padre (Spotless, JaCoCo, BOMs) pero no se agrega a <modules> hasta E2-12 (CI):
        así el `mvn` de los demás no compila ni formatea este módulo."""
        self.assertIn("<relativePath>../pom.xml</relativePath>", leer("auth-service", "pom.xml"))


if __name__ == "__main__":
    unittest.main()
