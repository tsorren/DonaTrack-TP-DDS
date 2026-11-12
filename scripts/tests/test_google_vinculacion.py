#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de SPEC-06 E2-05 (iteración 9): ingreso con Google y vinculación por contraseña (D4).

Corren sin Java, Docker ni Keycloak (stdlib). Fijan lo que se decidió y lo que cruza archivos:
el flujo propio de primer ingreso del realm (pide la contraseña, no ofrece el camino por email ni vincula
sin pruebas), que todo placeholder `${VAR}` del realm esté definido en cada lugar que lo importa, el
español del botón de Google y la coordinación con el test de contrato del realm y con el script de
punta a punta.

Que el flujo funcione lo probó `scripts/e2e_google_vinculacion.py` contra un Keycloak 26.8.1 real, con un
"Google falso" (otro realm OIDC) conectado al mismo flujo; que el realm se importe lo probó el arranque real.
"""

import json
import os
import re
import unittest
import warnings

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
COMPOSES = ("docker-compose.yml", "docker-compose.preprod.yml")
FLUJO = "donatrack first broker login"
IT = ("auth-service", "src", "test", "java", "grupo5", "auth", "RealmTokenContractIT.java")


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8", errors="replace") as f:
        return f.read()


def realm():
    return json.loads(leer("auth-service", "realm", "donatrack-realm.json"))


def bloque(compose, servicio):
    texto = leer(compose)
    inicio = texto.index("\n  %s:\n" % servicio)
    fin = re.search(r"\n  \S", texto[inicio + 3:]).start() + inicio + 3
    return texto[inicio:fin]


def flujos_por_alias():
    return {f["alias"]: f for f in realm()["authenticationFlows"]}


def ejecuciones(alias):
    return sorted(flujos_por_alias()[alias]["authenticationExecutions"], key=lambda e: e["priority"])


class FlujoDeVinculacionTests(unittest.TestCase):
    """D4: si el email ya tiene cuenta, se pide su contraseña. Sin atajos."""

    def test_hay_un_solo_flujo_de_primer_nivel_y_no_pisa_los_de_fabrica(self):
        flujos = realm()["authenticationFlows"]
        self.assertEqual([f["alias"] for f in flujos if f["topLevel"]], [FLUJO])
        for f in flujos:
            self.assertFalse(f["builtIn"], f["alias"])
            self.assertEqual(f["providerId"], "basic-flow", f["alias"])
            self.assertTrue(f["alias"].startswith("donatrack "), "no debe chocar con un flujo de fábrica: " + f["alias"])

    def test_las_descripciones_y_los_alias_entran_en_la_columna_de_la_base(self):
        # Keycloak las guarda en VARCHAR(255); una más larga hace que el servidor no arranque (pasó de verdad).
        for f in realm()["authenticationFlows"]:
            self.assertLessEqual(len(f["description"]), 255, f["alias"])
            self.assertLessEqual(len(f["alias"]), 255)
        for c in realm()["authenticatorConfig"]:
            self.assertLessEqual(len(c["alias"]), 255)

    def test_todo_subflujo_referenciado_existe_y_ninguno_queda_huerfano(self):
        flujos = flujos_por_alias()
        referenciados = set()
        for f in flujos.values():
            for e in f["authenticationExecutions"]:
                if e["authenticatorFlow"]:
                    self.assertIn(e["flowAlias"], flujos, "subflujo inexistente: " + e["flowAlias"])
                    self.assertFalse(flujos[e["flowAlias"]]["topLevel"], e["flowAlias"])
                    referenciados.add(e["flowAlias"])
        self.assertEqual(set(flujos) - referenciados - {FLUJO}, set(), "flujos que nadie usa")

    def test_cada_ejecucion_tiene_una_prioridad_propia_y_un_requisito_valido(self):
        for alias, f in flujos_por_alias().items():
            prioridades = [e["priority"] for e in f["authenticationExecutions"]]
            self.assertEqual(len(prioridades), len(set(prioridades)), alias)
            for e in f["authenticationExecutions"]:
                self.assertIn(e["requirement"], ("REQUIRED", "ALTERNATIVE"), alias)

    def test_solo_usa_los_autenticadores_que_se_decidieron(self):
        usados = {e["authenticator"] for f in realm()["authenticationFlows"] for e in f["authenticationExecutions"] if not e["authenticatorFlow"]}
        self.assertEqual(usados, {"idp-review-profile", "idp-create-user-if-unique", "idp-confirm-link", "idp-username-password-form"})

    def test_no_ofrece_el_camino_por_email_ni_vincula_sin_prueba(self):
        # El email lo mandaría Keycloak por SMTP (acá no hay: el correo de cuenta sale por RabbitMQ), y un
        # vínculo automático saltearía la contraseña que pide D4.
        texto = leer("auth-service", "realm", "donatrack-realm.json")
        for prohibido in ("idp-email-verification", "idp-auto-link", "idp-detect-existing-broker-user"):
            self.assertNotIn(prohibido, texto)

    def test_con_una_cuenta_existente_se_confirma_el_vinculo_y_despues_se_pide_la_contrasena(self):
        e = ejecuciones("donatrack first broker login Handle Existing Account")
        self.assertEqual([(x["authenticator"], x["requirement"]) for x in e],
                         [("idp-confirm-link", "REQUIRED"), ("idp-username-password-form", "REQUIRED")])

    def test_crear_la_cuenta_es_la_primera_opcion_y_vincular_la_segunda(self):
        e = ejecuciones("donatrack first broker login User creation or linking")
        self.assertEqual(e[0]["authenticator"], "idp-create-user-if-unique")
        self.assertEqual(e[1]["flowAlias"], "donatrack first broker login Handle Existing Account")
        self.assertEqual({x["requirement"] for x in e}, {"ALTERNATIVE"})

    def test_el_flujo_de_primer_nivel_revisa_el_perfil_y_despues_crea_o_vincula(self):
        e = ejecuciones(FLUJO)
        self.assertEqual(e[0]["authenticator"], "idp-review-profile")
        self.assertEqual(e[1]["flowAlias"], "donatrack first broker login User creation or linking")
        self.assertEqual({x["requirement"] for x in e}, {"REQUIRED"})

    def test_las_configuraciones_existen_se_usan_y_dicen_lo_que_se_decidio(self):
        configs = {c["alias"]: c["config"] for c in realm()["authenticatorConfig"]}
        usadas = {e["authenticatorConfig"] for f in realm()["authenticationFlows"] for e in f["authenticationExecutions"] if e.get("authenticatorConfig")}
        self.assertEqual(usadas, set(configs))
        # Solo se pide el perfil si falta algo, y una cuenta nueva por Google no obliga a cambiar la clave.
        self.assertEqual(configs["donatrack review profile config"], {"update.profile.on.first.login": "missing"})
        self.assertEqual(configs["donatrack create unique user config"], {"require.password.update.after.registration": "false"})

    def test_google_usa_ese_flujo_y_no_se_toma_su_email_como_prueba(self):
        google = realm()["identityProviders"][0]
        self.assertEqual(google["alias"], "google")
        self.assertEqual(google["firstBrokerLoginFlowAlias"], FLUJO)
        self.assertIs(google["trustEmail"], False)
        self.assertEqual(google["config"]["syncMode"], "IMPORT")
        self.assertEqual(google["config"]["defaultScope"], "openid email")

    def test_el_flujo_dice_que_una_cuenta_sin_contrasena_queda_pendiente_de_d12(self):
        descripcion = flujos_por_alias()["donatrack first broker login Handle Existing Account"]["description"]
        self.assertIn("RF-AUTH-04", descripcion)
        self.assertIn("D12", descripcion)


class PlaceholdersDelRealmTests(unittest.TestCase):
    """Un `${VAR}` sin valor deja el texto literal en el realm y Keycloak no arranca: pasó con `spi/` y pudo
    pasar con `GOOGLE_HABILITADO`. Cada placeholder tiene que estar definido en todos los lugares que importan el realm."""

    @classmethod
    def setUpClass(cls):
        cls.variables = sorted(set(re.findall(r"\$\{([A-Z][A-Z0-9_]*)\}", leer("auth-service", "realm", "donatrack-realm.json"))))

    def test_hay_placeholders_y_estan_los_que_se_conocen(self):
        self.assertGreaterEqual(len(self.variables), 8)
        self.assertIn("GOOGLE_HABILITADO", self.variables)

    def test_el_test_de_contrato_del_realm_define_todos(self):
        it = leer(*IT)
        for variable in self.variables:
            self.assertIn('.withEnv("%s"' % variable, it, variable)

    def test_el_keycloak_de_los_dos_compose_define_todos(self):
        for compose in COMPOSES:
            entorno = bloque(compose, "keycloak")
            for variable in self.variables:
                self.assertRegex(entorno, r"- %s=" % variable, "%s: %s" % (compose, variable))

    def test_los_env_y_el_generador_definen_todos(self):
        for archivo in (".env.example", ".env"):
            for variable in self.variables:
                self.assertRegex(leer(archivo), r"(?m)^%s=" % variable, "%s: %s" % (archivo, variable))
        for variable in self.variables:
            self.assertIn('"%s=' % variable, leer("scripts", "generar-env-db.sh"), variable)

    def test_google_arranca_apagado_en_todos_lados(self):
        for compose in COMPOSES:
            self.assertIn("- GOOGLE_HABILITADO=${GOOGLE_HABILITADO:-false}", bloque(compose, "keycloak"), compose)
        for archivo in (".env.example", ".env"):
            self.assertRegex(leer(archivo), r"(?m)^GOOGLE_HABILITADO=false$", archivo)
        self.assertIn('"GOOGLE_HABILITADO=false"', leer("scripts", "generar-env-db.sh"))

    def test_el_proveedor_se_habilita_con_esa_variable_y_la_it_lo_enciende(self):
        self.assertEqual(realm()["identityProviders"][0]["enabled"], "${GOOGLE_HABILITADO}")
        self.assertIn('.withEnv("GOOGLE_HABILITADO", "true")', leer(*IT))


class TemaEnEspanolTests(unittest.TestCase):
    """Keycloak 26.8.1 no traduce `signInWithProvider` ni `orLabel`: el botón decía "Sign in with Google"."""

    @classmethod
    def setUpClass(cls):
        cls.mensajes = {}
        for linea in leer("auth-service", "themes", "donatrack", "login", "messages", "messages_es.properties").splitlines():
            if linea.strip() and not linea.startswith("#"):
                clave, valor = linea.split("=", 1)
                cls.mensajes[clave] = valor

    def test_el_boton_de_un_proveedor_dice_continuar_con(self):
        self.assertEqual(self.mensajes["signInWithProvider"], "Continuar con {0}")

    def test_el_separador_dice_o(self):
        self.assertEqual(self.mensajes["orLabel"], "o")

    def test_solo_agrega_lo_que_falta(self):
        self.assertEqual(sorted(self.mensajes), ["orLabel", "signInWithProvider"])

    def test_el_tema_sigue_extendiendo_el_v2(self):
        self.assertIn("parent=keycloak.v2", leer("auth-service", "themes", "donatrack", "login", "theme.properties"))

    def test_el_realm_esta_en_espanol_y_el_texto_pedido_es_el_de_la_spec(self):
        self.assertEqual(realm()["defaultLocale"], "es")
        self.assertIn("Continuar con Google", leer("docs", "requerimientos", "requerimientos-e6.md"))
        self.assertEqual(realm()["identityProviders"][0]["displayName"], "Google")


class ContratoDelRealmYContextoDeBuildTests(unittest.TestCase):
    def test_el_it_espera_el_flujo_propio_y_prueba_que_no_hay_camino_por_email(self):
        it = leer(*IT)
        self.assertIn('assertEquals("donatrack first broker login", google.get("firstBrokerLoginFlowAlias").asText());', it)
        self.assertIn("elFlujoDeVinculacionPideLaClaveYNoOfreceElCaminoPorEmail", it)
        for dato in ('"idp-confirm-link"', '"idp-username-password-form"', 'assertFalse(autenticadores.contains("idp-email-verification")'):
            self.assertIn(dato, it)

    def test_el_it_le_da_a_la_imagen_todo_lo_que_copia_el_dockerfile(self):
        # El IT arma la imagen con Testcontainers y solo le pasa lo que dice `withFileFromPath`. Cuando el
        # Dockerfile empezó a copiar `spi/`, ese contexto quedó incompleto y lo tuvo que arreglar el equipo.
        origenes = set()
        for linea in leer("auth-service", "Dockerfile").splitlines():
            m = re.match(r"^COPY\s+(?!--from)(\S+)\s+\S+", linea)
            if m:
                origenes.add(m.group(1).split("/")[0])
        self.assertTrue({"realm", "themes", "spi"} <= origenes, str(origenes))
        it = leer(*IT)
        for origen in origenes:
            self.assertIn('.withFileFromPath("%s"' % origen, it, "el Dockerfile copia %s y el IT no lo incluye en el contexto" % origen)


class ScriptDePuntaAPuntaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.script = leer("scripts", "e2e_google_vinculacion.py")

    def test_prueba_el_flujo_que_el_realm_declara(self):
        self.assertIn('FLUJO_ESPERADO = "%s"' % FLUJO, self.script)

    def test_cubre_los_casos_decididos(self):
        # Se buscan en el cuerpo de main(): varios también están en el texto de documentación del script,
        # y borrar un caso sin que el test lo note ya pasó.
        cuerpo = self.script[self.script.index("def main():"):]
        for caso in ("Un email que no existe en DonaTrack (RF-AUTH-02)", "Un email que ya tiene cuenta con contraseña (RF-AUTH-03, D4)",
                     "Con SMTP configurado en el realm sigue pidiendo la contraseña", "Una cuenta sin contraseña (RF-AUTH-04, pendiente de D12)",
                     "una contraseña incorrecta no vincula", "no vuelve a pedir la contraseña", "Google dice que el email NO está verificado"):
            self.assertIn(caso, cuerpo)

    def test_limpia_lo_que_crea_y_devuelve_el_smtp_del_realm(self):
        final = self.script[self.script.index("    finally:"):]
        self.assertIn("smtpServer=smtp_original", final)
        self.assertIn('"/%s/identity-provider/instances/%s" % (REALM, ALIAS)', final)
        self.assertIn('api("DELETE", "/" + FALSO', final)
        self.assertIn('"/%s/users/%s" % (REALM, u["id"])', final)

    def test_el_google_falso_usa_el_mismo_flujo_que_el_proveedor_real(self):
        self.assertIn('"firstBrokerLoginFlowAlias": real["firstBrokerLoginFlowAlias"]', self.script)

    def test_keycloak_alcanza_al_google_falso_como_lo_ve_dentro_del_compose(self):
        self.assertIn('os.environ.get("KC_INTERNAL_URL", "http://localhost:8080")', self.script)
        self.assertIn('"8180:8080"', leer("docker-compose.yml"))

    def test_los_dos_scripts_e2e_compilan_sin_avisos_de_sintaxis(self):
        # Un `\e` en un texto que no es raw da un SyntaxWarning hoy y será un error en Python futuros.
        for nombre in ("e2e_google_vinculacion.py", "e2e_correo_de_cuenta.py"):
            ruta = os.path.join(RAIZ, "scripts", nombre)
            with open(ruta, encoding="utf-8") as f:
                codigo = f.read()
            with warnings.catch_warnings(record=True) as avisos:
                warnings.simplefilter("always")
                compile(codigo, ruta, "exec")
            self.assertEqual([str(a.message) for a in avisos], [], nombre)

    def test_no_lo_descubre_unittest_porque_necesita_un_stack(self):
        self.assertFalse("e2e_google_vinculacion.py".startswith("test_"))


if __name__ == "__main__":
    unittest.main()
