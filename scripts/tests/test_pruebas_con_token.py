#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos de SPEC-06 E2-07 (iteración 5): pruebas con la seguridad encendida y retiro de X-Actor.

Corren sin Java, Docker ni Node (stdlib; `bash` solo para validar la sintaxis del runner). Fijan lo que cruza
archivos: que la cuenta de pruebas se llame igual en el realm, integration-tests, k6 y Postman; que el
secreto no salga por la salida ni por la lista de procesos; que los cinco servicios tengan el interruptor
del preprod; y que el contrato refleje el `X-Actor` opcional. Que la lógica de token de Postman y k6 haga
lo que dice lo prueba `scripts/tests/postman-k6-token.test.js`; que la seguridad quede de verdad encendida
lo prueba el propio runner (`run-preprod-tests.sh --auth`) al verificar el 401 sin token.
"""

import glob
import json
import os
import re
import shutil
import subprocess
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
CLIENTE = "donatrack-pruebas"
SERVICIOS = {"donaciones-service": "DONACIONES", "incentivos-service": "INCENTIVOS",
             "notificaciones-service": "NOTIFICACIONES", "logistica-service": "LOGISTICA", "api-gateway": "GATEWAY"}


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8", errors="replace") as f:
        return f.read()


def bloque(compose, servicio):
    texto = leer(compose)
    inicio = texto.index("\n  %s:\n" % servicio)
    fin = re.search(r"\n  \S", texto[inicio + 3:]).start() + inicio + 3
    return texto[inicio:fin]


class UnSoloNombreParaLaCuentaTests(unittest.TestCase):
    def test_el_cliente_de_pruebas_se_llama_igual_en_todos_lados(self):
        realm = json.loads(leer("auth-service", "realm", "donatrack-realm.json"))
        self.assertIn(CLIENTE, [c["clientId"] for c in realm["clients"]])
        token_java = leer("integration-tests", "src", "test", "java", "grupo5", "tests", "utils", "TokenDePruebas.java")
        self.assertIn('CLIENTE_POR_DEFECTO = "%s"' % CLIENTE, token_java)
        self.assertIn("client_id: __ENV.PRUEBAS_CLIENT_ID || '%s'" % CLIENTE, leer("tests", "performance", "k6", "token.js"))
        for f in glob.glob(os.path.join(RAIZ, "postman", "flujo-*.json")):
            self.assertIn("'%s'" % CLIENTE, leer("postman", os.path.basename(f)), f)

    def test_el_issuer_del_token_coincide_con_la_url_donde_se_pide(self):
        # Los servicios validan iss = ${KC_HOSTNAME}/realms/donatrack; los tests piden el token en esa misma URL.
        token_java = leer("integration-tests", "src", "test", "java", "grupo5", "tests", "utils", "TokenDePruebas.java")
        por_defecto = re.search(r'TOKEN_URL_POR_DEFECTO =\s*"([^"]+)"', token_java).group(1)
        kc = re.search(r"(?m)^KC_HOSTNAME=(\S+)$", leer(".env.example")).group(1)
        self.assertEqual(por_defecto, kc + "/realms/donatrack/protocol/openid-connect/token")
        for compose in ("docker-compose.yml", "docker-compose.preprod.yml"):
            for servicio in SERVICIOS:
                self.assertIn("JWT_ISSUER_URI=${KC_HOSTNAME:-%s}/realms/donatrack" % kc, bloque(compose, servicio), "%s %s" % (compose, servicio))


class IntegrationTestsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        base = ("integration-tests", "src", "test", "java", "grupo5", "tests")
        cls.token = leer(*base, "utils", "TokenDePruebas.java")
        cls.filtro = leer(*base, "utils", "FiltroDeToken.java")
        cls.base_it = leer(*base, "BaseIT.java")
        cls.test = leer(*base, "utils", "TokenDePruebasTest.java")

    def test_base_it_instala_el_filtro_una_sola_vez_y_solo_si_hay_secreto(self):
        self.assertIn("TOKEN_INSTALADO.compareAndSet(false, true)", self.base_it)
        self.assertIn("TokenDePruebas.delEntorno().ifPresent(", self.base_it)
        self.assertIn("RestAssured.filters(new FiltroDeToken(token))", self.base_it)
        self.assertNotIn("replaceFiltersWith", self.base_it, "no pisar filtros que otros hayan instalado")

    def test_sin_secreto_no_hay_token(self):
        self.assertRegex(self.token, r"if \(secreto\.isBlank\(\)\) \{\s*return Optional\.empty\(\);")

    def test_el_secreto_nunca_sale_en_un_mensaje_ni_en_la_salida(self):
        for m in re.finditer(r"IllegalStateException\((.*?)\);", self.token, re.S):
            self.assertNotIn("secreto", m.group(1))
            self.assertNotIn("respuesta.body()", m.group(1), "no se vuelca la respuesta de Keycloak")
        self.assertNotIn("System.out", self.token)
        self.assertNotIn("System.err", self.token)

    def test_el_secreto_solo_se_usa_para_armar_el_pedido(self):
        # Solo código: los comentarios y el Javadoc pueden nombrar el secreto para explicarlo.
        codigo = [l.strip() for l in self.token.splitlines() if not l.strip().startswith(("*", "/*", "//"))]
        usos = [l for l in codigo if "secreto" in l and "this.secreto" not in l]
        self.assertTrue(usos)
        for l in usos:
            self.assertTrue(
                any(t in l for t in ("String secreto", "codificar(secreto)", "secreto.isBlank()", "secreto,", "secreto =", "secreto)")),
                l)

    def test_el_filtro_no_pisa_un_authorization_propio(self):
        self.assertIn('hasHeaderWithName("Authorization")', self.filtro)
        self.assertIn("!pedido.getHeaders()", self.filtro)

    def test_el_test_del_token_corre_en_el_gate_de_humo(self):
        self.assertIn('@Tag("smoke")', self.test)

    def test_renueva_antes_de_que_venza(self):
        self.assertIn("MARGEN = Duration.ofSeconds(30)", self.token)
        self.assertIn("vence.minus(MARGEN)", self.token)


class K6Tests(unittest.TestCase):
    SCRIPTS = ("donaciones-creacion-carga.js", "incentivos-eventos-saturacion.js")

    def test_los_dos_scripts_usan_el_token_y_ya_no_arman_sus_cabeceras(self):
        for nombre in self.SCRIPTS:
            texto = leer("tests", "performance", "k6", nombre)
            self.assertIn("import { cabeceras, tokenDePruebas } from './token.js';", texto, nombre)
            self.assertIn("export function setup() {\n  return { token: tokenDePruebas() };\n}", texto, nombre)
            self.assertIn("export default function (data) {", texto, nombre)
            self.assertIn("headers: cabeceras(data.token),", texto, nombre)
            self.assertNotIn("headers: { 'Content-Type'", texto, nombre)

    def test_se_conserva_el_bom_de_los_scripts(self):
        for nombre in self.SCRIPTS:
            with open(os.path.join(RAIZ, "tests", "performance", "k6", nombre), "rb") as f:
                self.assertEqual(f.read(3), b"\xef\xbb\xbf", nombre)

    def test_k6_pide_el_token_a_keycloak_por_la_red_del_compose(self):
        self.assertIn("http://keycloak:8080/realms/donatrack/protocol/openid-connect/token", leer("tests", "performance", "k6", "token.js"))

    def test_el_runner_le_pasa_el_secreto_a_k6_solo_con_auth(self):
        runner = leer("run-preprod-tests.sh")
        self.assertIn('[[ "$AUTH" == "true" ]] && K6_ENV=(-e PRUEBAS_CLIENT_SECRET)', runner)
        self.assertIn('"${K6_ENV[@]}"', runner)


class PostmanTests(unittest.TestCase):
    def setUp(self):
        self.colecciones = {os.path.basename(f): json.loads(leer("postman", os.path.basename(f)))
                            for f in glob.glob(os.path.join(RAIZ, "postman", "flujo-*.json"))}

    def test_las_nueve_colecciones_traen_el_script_y_las_variables(self):
        self.assertEqual(len(self.colecciones), 9)
        for nombre, d in self.colecciones.items():
            eventos = [e for e in d["event"] if e["listen"] == "prerequest"]
            self.assertEqual(len(eventos), 1, nombre)
            self.assertIn("pruebasClientSecret", "\n".join(eventos[0]["script"]["exec"]), nombre)
            claves = {v["key"]: v["value"] for v in d["variable"]}
            self.assertEqual(claves.get("pruebasClientSecret"), "", "el secreto no se versiona: %s" % nombre)
            self.assertEqual(claves.get("pruebasClientId"), CLIENTE, nombre)

    def test_el_token_solo_va_a_los_servicios_propios_nunca_al_proveedor_externo(self):
        for nombre, d in self.colecciones.items():
            codigo = "\n".join([e for e in d["event"] if e["listen"] == "prerequest"][0]["script"]["exec"])
            propias = re.search(r"const propias = \[(.*?)\]", codigo).group(1)
            self.assertEqual(set(re.findall(r"'(\w+)'", propias)),
                             {"baseUrl", "donacionesUrl", "notificacionesUrl", "incentivosUrl", "logisticaUrl"}, nombre)
            self.assertNotIn("Externo", propias, nombre)

    def test_la_comparacion_exige_el_limite_de_la_url_y_no_un_prefijo_suelto(self):
        for nombre, d in self.colecciones.items():
            codigo = "\n".join([e for e in d["event"] if e["listen"] == "prerequest"][0]["script"]["exec"])
            self.assertIn("destino === base || destino.startsWith(base + '/') || destino.startsWith(base + '?')", codigo, nombre)
            self.assertIn("propias.some(esPropia)", codigo, nombre)
            self.assertNotIn("startsWith(base))", codigo, nombre)

    def test_sin_secretos_versionados_en_ninguna_coleccion(self):
        for nombre in self.colecciones:
            self.assertNotRegex(leer("postman", nombre), r'"pruebasClientSecret",\s*"value":\s*"[^"]', nombre)


class RunnerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.runner = leer("run-preprod-tests.sh")
        m = re.search(r"^verificar_seguridad_encendida\(\) \{\n.*?^\}\n", cls.runner, re.S | re.M)
        cls.funcion = m.group(0) if m else ""

    @unittest.skipIf(shutil.which("bash") is None, "bash no disponible")
    def test_la_sintaxis_del_runner_es_valida(self):
        r = subprocess.run(["bash", "-n", os.path.join(RAIZ, "run-preprod-tests.sh")], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)

    def test_auth_se_pide_por_opcion_o_por_variable_y_apagada_por_defecto(self):
        self.assertIn('AUTH="${PREPROD_AUTH:-false}"', self.runner)
        self.assertIn("--auth)       AUTH=true; shift ;;", self.runner)
        self.assertIn('[[ "$AUTH" == "true" ]] || AUTH=false', self.runner)
        self.assertIn('export PREPROD_SEGURIDAD="$AUTH"', self.runner)

    def test_verifica_el_401_sin_token_en_el_gateway_y_en_los_cuatro_servicios(self):
        self.assertTrue(self.funcion, "falta verificar_seguridad_encendida")
        for url in ("$GATEWAY_URL/api/personas", "$DONACIONES_URL/api/personas", "$NOTIFICACIONES_URL/api/notificaciones",
                    "$INCENTIVOS_URL/api/incentivos/ranking/historial", "$LOGISTICA_URL/api/entregas"):
            self.assertIn(url, self.funcion)
        self.assertIn('[[ "$codigo" != "401" ]]', self.funcion)

    def test_comprueba_que_el_token_abre_una_ruta_protegida_directo_y_por_el_gateway(self):
        self.assertIn('for url in "$DONACIONES_URL/api/personas" "$GATEWAY_URL/api/personas"; do', self.funcion)
        self.assertIn('[[ "$codigo" != "200" ]]', self.funcion)

    def test_el_secreto_viaja_por_stdin_y_no_en_los_argumentos(self):
        self.assertIn("printf 'grant_type=client_credentials&client_id=donatrack-pruebas&client_secret=%s' \"$secreto\"", self.funcion)
        self.assertIn("curl -s -w '\\n%{http_code}' -d @- \"$token_url\"", self.funcion)
        self.assertIn("curl -s -o /dev/null -w '%{http_code}' -H @- \"$url\"", self.funcion)
        for linea in self.funcion.splitlines():
            if linea.strip().startswith(("ok ", "fail ", "echo ")) or "ok \"" in linea or "fail \"" in linea:
                self.assertNotIn("$secreto", linea, linea)
                self.assertNotIn("$token\"", linea, linea)
        self.assertNotIn("--data-urlencode", self.funcion)
        self.assertNotRegex(self.funcion, r"curl[^\n|]*client_secret=\$")

    def test_corta_antes_de_pedir_el_token_si_falta_la_configuracion(self):
        i_falta = self.funcion.index('if [[ -z "$kc_host" || -z "$secreto" ]]')
        i_pide = self.funcion.index("curl -s -w '\\n%{http_code}' -d @-")
        self.assertLess(i_falta, i_pide)

    def test_exporta_lo_que_leen_integration_tests_y_k6_y_se_llama_con_auth(self):
        self.assertIn('export PRUEBAS_CLIENT_SECRET="$secreto"', self.funcion)
        self.assertIn('export KEYCLOAK_TOKEN_URL="$token_url"', self.funcion)
        self.assertRegex(self.runner, r'if \[\[ "\$AUTH" == "true" \]\]; then\n  step "Verificando que la seguridad quedó encendida \(--auth\)"\n  verificar_seguridad_encendida')

    def test_la_verificacion_corre_despues_de_levantar_el_stack_y_antes_de_las_pruebas(self):
        i_up = self.runner.index('ok "Todos los servicios están healthy."')
        i_verif = self.runner.index("verificar_seguridad_encendida\n  ok ")
        i_suite = self.runner.index("run_suite() {")
        self.assertLess(i_up, i_verif)
        self.assertLess(i_verif, i_suite)


class PreprodYWorkflowsTests(unittest.TestCase):
    def test_cada_servicio_y_el_gateway_tienen_el_interruptor_apagado_por_defecto(self):
        for servicio, prefijo in SERVICIOS.items():
            b = bloque("docker-compose.preprod.yml", servicio)
            entorno = [l.strip() for l in b.splitlines() if l.strip().startswith("- ")]
            self.assertIn("- %s_SEGURIDAD_HABILITADA=${PREPROD_SEGURIDAD:-false}" % prefijo, entorno, servicio)
            self.assertFalse(any(re.match(r"- %s_SEGURIDAD_HABILITADA=true" % prefijo, l) for l in entorno), servicio)
            self.assertTrue(any(l.startswith("- SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/") for l in entorno), servicio)

    def test_los_dos_workflows_pasan_la_variable_de_repositorio_al_runner(self):
        for wf in ("main.yml", "merge.yml"):
            texto = leer(".github", "workflows", wf)
            self.assertIn("PREPROD_AUTH: ${{ vars.PREPROD_AUTH }}", texto, wf)
        self.assertNotRegex(leer(".github", "workflows", "main.yml"), r"PREPROD_AUTH:\s*[\"']?true")
        self.assertNotRegex(leer(".github", "workflows", "merge.yml"), r"PREPROD_AUTH:\s*[\"']?true")

    def test_el_ci_corre_los_tests_python_y_node_de_esta_iteracion(self):
        wf = leer(".github", "workflows", "agent-governance.yml")
        self.assertIn("python3 -m unittest discover -s scripts/tests -p 'test_*.py'", wf)
        self.assertIn("node scripts/tests/postman-k6-token.test.js", wf)
        self.assertRegex(wf, r"(?m)^  tests-estaticos:")


class RetiroDeXActorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        d = ("donaciones-service", "src", "main", "java", "grupo5", "donaciones")
        cls.controller = leer(*d, "controllers", "impl", "DonacionesIndependientesController.java")
        cls.interfaz = leer(*d, "controllers", "IDonacionesIndependientesController.java")
        cls.helper = leer(*d, "seguridad", "AuditoriaDelActor.java")
        cls.contrato = leer("docs", "arquitectura", "contratos", "openapi-donaciones.yaml")

    def test_el_encabezado_deja_de_ser_obligatorio_y_el_actor_sale_del_helper(self):
        self.assertIn('@RequestHeader(value = "X-Actor", required = false) String actorLegado', self.controller)
        self.assertIn("AuditoriaDelActor.de(autenticacion, actorLegado)", self.controller)
        self.assertNotIn('@RequestHeader("X-Actor")', self.controller)

    def test_el_controller_y_su_interfaz_coinciden(self):
        for texto in (self.controller, self.interfaz):
            self.assertIn("String actorLegado", texto)
            self.assertIn("Authentication autenticacion", texto)

    def test_con_token_manda_el_sub_y_sin_nada_se_mantiene_el_400(self):
        self.assertIn("autenticacion.getCredentials() instanceof Jwt token", self.helper)
        self.assertIn("return token.getSubject();", self.helper)
        self.assertIn('throw new IllegalArgumentException("El encabezado \'X-Actor\' es obligatorio.");', self.helper)

    def test_el_contrato_dice_que_es_opcional(self):
        m = re.search(r"- name: X-Actor\n\s+in: header\n\s+required: (\w+)", self.contrato)
        self.assertEqual(m.group(1), "false")
        self.assertIn("`sub`", self.contrato)

    def test_hay_tests_unitarios_y_de_punta_a_punta_del_actor(self):
        base = ("donaciones-service", "src", "test", "java", "grupo5", "donaciones", "seguridad")
        self.assertTrue(os.path.isfile(os.path.join(RAIZ, *base, "AuditoriaDelActorTest.java")))
        e2e = leer(*base, "SeguridadDonacionesTest.java")
        self.assertIn("elActorDeLaAuditoriaSaleDelTokenYNoDelEncabezado", e2e)
        self.assertIn("sinSubEnElTokenSeUsaElEncabezadoLegadoYSinEncabezadoEsObligatorio", e2e)


if __name__ == "__main__":
    unittest.main()
