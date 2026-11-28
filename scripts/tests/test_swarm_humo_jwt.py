#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de S3 y S4 (E7-S04 A): cadena JWT encendida por defecto en Swarm, encabezados y su prueba de humo.

Corren sin Docker ni Keycloak: `scripts/swarm/humo-jwt.sh` y `desplegar.sh` se ejecutan de verdad con un
`docker` falso en el PATH que simula el gateway, los servicios y Keycloak. Fijan que la prueba de humo
distingue 401, 200 y 403 (y falla si la seguridad está apagada o un servicio no valida), que revisa los
encabezados de S1 (faltan, se repiten) y cada réplica del gateway, que los secretos
y los tokens no aparecen en argumentos ni en la salida, y que el despliegue en Swarm enciende la
validación sin tocar el valor por defecto del compose. Que la cadena funcione con el Keycloak real lo
prueba el propio script contra el stack.
"""

import os
import re
import shutil
import stat
import subprocess
import tempfile
import textwrap
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
HUMO = os.path.join(RAIZ, "scripts", "swarm", "humo-jwt.sh")
DESPLEGAR = os.path.join(RAIZ, "scripts", "swarm", "desplegar.sh")
SECRETO_PRUEBAS = "secreto-pruebas-sintetico-123"
SECRETO_CLIENTE = "secreto-cliente-sintetico-456"
SERVICIOS = ("donaciones-service", "incentivos-service", "logistica-service", "notificaciones-service")

# docker falso: registra cada llamada (argumentos) y simula el contenedor auxiliar (curl), las réplicas del
# gateway (docker ps / inspect) y las respuestas. FAKE_ESCENARIO: correcto | apagada | logistica_sin_validar |
# keycloak_rechaza | falta_encabezado | encabezado_duplicado | cache_sin_no_store | replica_sin_seguridad |
# sin_replicas
DOCKER_FALSO = textwrap.dedent(r"""
    #!/usr/bin/env python3
    import os, sys, json
    args = sys.argv[1:]
    with open(os.environ["FAKE_LOG"], "a") as f:
        f.write(json.dumps(args) + "\n")
    esc = os.environ.get("FAKE_ESCENARIO", "correcto")
    REPLICAS = {"c1": "10.0.1.11", "c2": "10.0.1.12"}
    if args[:1] == ["run"]:
        print("id-falso"); sys.exit(0)
    if args[:1] == ["rm"]:
        sys.exit(0)
    if args[:1] == ["ps"]:
        if esc != "sin_replicas" and any("api-gateway" in a for a in args):
            print("\n".join(REPLICAS))
        sys.exit(0)
    if args[:1] == ["inspect"]:
        print(REPLICAS.get(args[1], "")); sys.exit(0)
    if args[:1] == ["compose"]:
        with open(os.environ["FAKE_ENV"], "w") as f:
            f.write(os.environ.get("PREPROD_SEGURIDAD", "<sin definir>"))
        print('{"name": "donatrack", "services": {}}'); sys.exit(0)
    if args[:1] != ["exec"]:
        sys.exit(0)
    entrada = sys.stdin.read() if "-i" in args else ""
    url = args[-1]
    if "/protocol/openid-connect/token" in url:
        if esc == "keycloak_rechaza":
            sys.stdout.write('{"error":"unauthorized_client"}\n401'); sys.exit(0)
        if "client_id=donatrack-pruebas&client_secret=" + os.environ["FAKE_SECRETO_PRUEBAS"] in entrada:
            sys.stdout.write('{"access_token":"tok-admin-falso","expires_in":300}\n200'); sys.exit(0)
        if "client_id=cliente-liviano&client_secret=" + os.environ["FAKE_SECRETO_CLIENTE"] in entrada:
            sys.stdout.write('{"access_token":"tok-servicio-falso","expires_in":300}\n200'); sys.exit(0)
        sys.stdout.write('{"error":"invalid_client"}\n401'); sys.exit(0)
    token = None
    if entrada.startswith("Authorization: Bearer "):
        token = entrada.strip().split(" ", 2)[2]
    host = url.split("/")[2]
    es_gateway = host.startswith("api-gateway:") or host.startswith("10.0.1.")
    ruta = "/" + url.split("/", 3)[3]
    if esc == "apagada" or ruta in ("/api/necesidades/publicas", "/actuator/health"):
        codigo = "200"
    elif esc == "logistica_sin_validar" and host.startswith("logistica-service:"):
        codigo = "200"
    elif esc == "replica_sin_seguridad" and host == "10.0.1.12:8086":
        codigo = "200"
    else:
        codigo = {None: "401", "tok-admin-falso": "200", "tok-servicio-falso": "403"}.get(token, "401")
    if es_gateway:
        enc = [("X-Content-Type-Options", "nosniff"), ("X-Frame-Options", "DENY"), ("Referrer-Policy", "no-referrer"),
               ("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"),
               ("Permissions-Policy", "camera=(), microphone=(), geolocation=()"),
               ("Strict-Transport-Security", "max-age=31536000")]
        enc.append(("Cache-Control", "no-store") if ruta.startswith("/api/") else ("Cache-Control", "no-cache"))
        if esc == "cache_sin_no_store":
            enc = [e for e in enc if e[0] != "Cache-Control"] + [("Cache-Control", "no-cache")]
        if esc == "falta_encabezado":
            enc = [e for e in enc if e[0] != "Content-Security-Policy"]
        if esc == "encabezado_duplicado":
            enc.append(("x-frame-options", "DENY"))
    else:  # un servicio directo: los de la cadena de common-lib
        enc = [("X-Content-Type-Options", "nosniff"), ("X-Frame-Options", "DENY"), ("Cache-Control", "no-cache")]
    if "-D" in args:
        sys.stdout.write("HTTP/1.1 %s\r\n" % codigo + "".join("%s: %s\r\n" % e for e in enc) + "\r\n")
        sys.stdout.write("\nCODIGO=" + codigo)
    else:
        sys.stdout.write(codigo)
""").lstrip()


def ejecutable(ruta, contenido):
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(contenido)
    os.chmod(ruta, os.stat(ruta).st_mode | stat.S_IEXEC)


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.bin = os.path.join(self.tmp, "bin")
        os.mkdir(self.bin)
        ejecutable(os.path.join(self.bin, "docker"), DOCKER_FALSO)
        self.log = os.path.join(self.tmp, "docker.log")
        self.env_file = os.path.join(self.tmp, ".env")
        with open(self.env_file, "w", encoding="utf-8") as f:
            f.write("KC_HOSTNAME=http://localhost:8180\nPRUEBAS_CLIENT_SECRET=%s\nCLIENTE_LIVIANO_SECRET=%s\n"
                    % (SECRETO_PRUEBAS, SECRETO_CLIENTE))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def entorno(self, **extra):
        env = dict(os.environ)
        env.update({"PATH": self.bin + os.pathsep + env["PATH"], "FAKE_LOG": self.log,
                    "FAKE_SECRETO_PRUEBAS": SECRETO_PRUEBAS, "FAKE_SECRETO_CLIENTE": SECRETO_CLIENTE,
                    "ENV_FILE": self.env_file, "FAKE_ENV": os.path.join(self.tmp, "env.txt")})
        env.update(extra)
        return env

    def argumentos_de_docker(self):
        with open(self.log, encoding="utf-8") as f:
            return f.read()


@unittest.skipUnless(shutil.which("bash") and shutil.which("python3"), "necesita bash y python3")
class HumoJwtTests(Base):
    def correr(self, escenario="correcto", **extra):
        self.informe = os.path.join(self.tmp, "informe.md")
        return subprocess.run(["bash", HUMO], capture_output=True, text=True, timeout=120,
                              env=self.entorno(FAKE_ESCENARIO=escenario, INFORME=self.informe, **extra))

    def informe_texto(self):
        with open(self.informe, encoding="utf-8") as f:
            return f.read()

    def test_la_sintaxis_es_valida(self):
        self.assertEqual(subprocess.run(["bash", "-n", HUMO]).returncode, 0)

    def test_con_la_cadena_encendida_pasan_las_48_comprobaciones(self):
        r = self.correr()
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)
        informe = self.informe_texto()
        # 24 códigos (4 servicios x 2 vías x 3 casos) + 12 de encabezados en el gateway + pública y health
        # (2 códigos y 2 de encabezados) + 2 réplicas x (401 y 200) x (código y encabezados).
        self.assertEqual(informe.count("| OK |"), 48, informe)
        self.assertNotIn("FALLA", informe)
        for servicio in SERVICIOS:
            for via in ("directo", "gateway"):
                for caso, codigo in (("sin token", 401), ("token ADMIN", 200), ("token SERVICIO", 403)):
                    self.assertRegex(informe, r"\| %s \(%s\) GET \S+ %s \| %d \| %d \| OK \|"
                                     % (servicio, via, caso, codigo, codigo))
                    linea_enc = "%s (%s) GET" % (servicio, via)
                    enc = [l for l in informe.splitlines() if l.startswith("| " + linea_enc) and "encabezados" in l]
                    self.assertEqual(len(enc), 3 if via == "gateway" else 0, (servicio, via, enc))
        for ip in ("10.0.1.11", "10.0.1.12"):
            self.assertIn("| réplica %s GET /api/personas token ADMIN | 200 | 200 | OK |" % ip, informe)
            self.assertIn("| réplica %s GET /api/personas sin token: encabezados de seguridad | todos | todos | OK |" % ip, informe)
        self.assertIn("gateway GET /actuator/health sin token: encabezados de seguridad | todos | todos | OK", informe)

    def test_con_la_seguridad_apagada_falla(self):
        r = self.correr("apagada")
        self.assertEqual(r.returncode, 1)
        # Sin token y con SERVICIO da 200 en lugar de 401 y 403: 16 en las rutas y 2 en las réplicas sin token.
        # Los encabezados no dependen de la seguridad: siguen bien.
        fallas = [l for l in self.informe_texto().splitlines() if "**FALLA**" in l]
        self.assertEqual(len(fallas), 18, fallas)
        self.assertFalse(any("encabezados" in l for l in fallas), fallas)

    def fallas(self):
        return [l for l in self.informe_texto().splitlines() if "**FALLA**" in l]

    def test_si_falta_un_encabezado_falla_en_cada_respuesta_del_gateway_y_dice_cual(self):
        r = self.correr("falta_encabezado")
        self.assertEqual(r.returncode, 1)
        fallas = self.fallas()
        # 12 respuestas de las rutas + pública + health + 4 de las réplicas
        self.assertEqual(len(fallas), 18, fallas)
        self.assertTrue(all("falta Content-Security-Policy" in l for l in fallas), fallas)

    def test_un_encabezado_repetido_falla(self):
        r = self.correr("encabezado_duplicado")
        self.assertEqual(r.returncode, 1)
        fallas = self.fallas()
        self.assertEqual(len(fallas), 18, fallas)
        self.assertTrue(all("X-Frame-Options 2 veces" in l for l in fallas), fallas)

    def test_sin_no_store_falla_en_api_pero_no_fuera_de_api(self):
        r = self.correr("cache_sin_no_store")
        self.assertEqual(r.returncode, 1)
        fallas = self.fallas()
        # 12 respuestas de las rutas + pública + 4 de las réplicas; health no está en /api y no lo exige.
        self.assertEqual(len(fallas), 17, fallas)
        self.assertTrue(all("Cache-Control=no-cache" in l for l in fallas), fallas)
        self.assertFalse(any("/actuator/health" in l for l in fallas), fallas)

    def test_una_replica_del_gateway_sin_validar_falla_aunque_la_otra_ande(self):
        r = self.correr("replica_sin_seguridad")
        self.assertEqual(r.returncode, 1)
        self.assertEqual(self.fallas(), ["| réplica 10.0.1.12 GET /api/personas sin token | 401 | 200 | **FALLA** |"])

    def test_sin_replicas_de_swarm_avisa_y_no_falla(self):
        r = self.correr("sin_replicas")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("| aviso |", self.informe_texto())
        self.assertIn("Réplicas del gateway probadas por su IP: 0.", self.informe_texto())

    def test_un_servicio_que_no_valida_el_token_directo_falla_aunque_el_gateway_lo_proteja(self):
        r = self.correr("logistica_sin_validar")
        self.assertEqual(r.returncode, 1)
        fallas = [l for l in self.informe_texto().splitlines() if "**FALLA**" in l]
        self.assertEqual(len(fallas), 2, fallas)
        self.assertTrue(all("logistica-service (directo)" in l for l in fallas), fallas)

    def test_sin_tokens_de_keycloak_corta_antes_de_probar_rutas(self):
        r = self.correr("keycloak_rechaza")
        self.assertEqual(r.returncode, 1)
        self.assertIn("Keycloak respondió 401", r.stderr)
        self.assertNotIn("/api/personas", self.argumentos_de_docker())

    def test_sin_secretos_en_el_env_no_arranca(self):
        with open(self.env_file, "w", encoding="utf-8") as f:
            f.write("PRUEBAS_CLIENT_SECRET=%s\n" % SECRETO_PRUEBAS)
        r = self.correr()
        self.assertEqual(r.returncode, 1)
        self.assertIn("CLIENTE_LIVIANO_SECRET", r.stderr)
        self.assertFalse(os.path.exists(self.log), "no debería llamar a docker")

    def test_secretos_y_tokens_no_aparecen_en_argumentos_salida_ni_informe(self):
        r = self.correr()
        self.assertEqual(r.returncode, 0, r.stderr)
        visibles = {"argumentos de docker": self.argumentos_de_docker(), "salida": r.stdout + r.stderr,
                    "informe": self.informe_texto()}
        for donde, texto in visibles.items():
            for secreto in (SECRETO_PRUEBAS, SECRETO_CLIENTE, "tok-admin-falso", "tok-servicio-falso"):
                self.assertNotIn(secreto, texto, "%s en %s" % (secreto, donde))

    def test_pide_los_tokens_por_la_red_del_stack_y_usa_la_red_pedida(self):
        r = self.correr(RED="otra-red")
        self.assertEqual(r.returncode, 0, r.stderr)
        argumentos = self.argumentos_de_docker()
        self.assertIn('"--network", "otra-red"', argumentos)
        self.assertIn("http://keycloak:8080/realms/donatrack/protocol/openid-connect/token", argumentos)


class RutasContraLaMatrizTests(unittest.TestCase):
    """Las rutas de la prueba tienen que ser solo de ADMIN: si no, el 403 del token SERVICIO no prueba nada."""

    CONTROLLERS = {
        "/api/personas": "donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/PersonasController.java",
        "/api/incentivos/admin/resumen": "incentivos-service/src/main/java/grupo5/incentivos/controllers/MetricasIncentivosController.java",
        "/api/entregas": "logistica-service/src/main/java/grupo5/logistica/controllers/impl/EntregasController.java",
        "/api/notificaciones": "notificaciones-service/src/main/java/grupo5/notificaciones/controllers/NotificacionController.java",
    }

    def test_cada_ruta_esta_en_un_controller_solo_para_admin_y_en_la_matriz(self):
        with open(HUMO, encoding="utf-8") as f:
            humo = f.read()
        with open(os.path.join(RAIZ, "docs", "arquitectura", "seguridad", "matriz-permisos.md"), encoding="utf-8") as f:
            matriz = f.read()
        for ruta, controller in self.CONTROLLERS.items():
            self.assertIn("|%s\"" % ruta, humo, ruta)
            with open(os.path.join(RAIZ, controller), encoding="utf-8") as f:
                java = f.read()
            self.assertRegex(java, r"@PreAuthorize\(ADMIN\)\s*(@\w+(\([^)]*\))?\s*)*public class", controller)
            self.assertRegex(matriz, r"\| `GET` \| `%s`[^|]*\| ADMIN \|" % re.escape(ruta), ruta)

    def test_la_ruta_publica_esta_en_la_allowlist_del_gateway(self):
        with open(os.path.join(RAIZ, "api-gateway", "src", "main", "resources", "application.yml"), encoding="utf-8") as f:
            self.assertIn("- GET /api/necesidades/publicas", f.read())


class EncabezadosContraElGatewayTests(unittest.TestCase):
    """La lista de la prueba de humo es la de EncabezadosDeSeguridad (S1): si cambia un valor, cambian los dos."""

    def test_los_encabezados_y_valores_son_los_del_filtro_del_gateway(self):
        with open(os.path.join(RAIZ, "api-gateway", "src", "main", "java", "grupo5", "gateway", "seguridad",
                               "EncabezadosDeSeguridad.java"), encoding="utf-8") as f:
            java = f.read()
        siempre = java[java.index("SIEMPRE ="):java.index(");", java.index("SIEMPRE ="))]
        del_gateway = dict(re.findall(r'"([A-Za-z-]+)", "([^"]*)"', siempre))
        with open(HUMO, encoding="utf-8") as f:
            humo = f.read()
        bloque = humo[humo.index("ENCABEZADOS=("):humo.index(")\n", humo.index("ENCABEZADOS=("))]
        de_la_prueba = dict(re.findall(r'"([A-Za-z-]+)\|([^"]*)"', bloque))
        self.assertEqual(len(del_gateway), 6, del_gateway)
        self.assertEqual(de_la_prueba, del_gateway)
        self.assertIn('NO_STORE = "no-store"', java)
        self.assertIn('CACHE_API="Cache-Control|no-store"', humo)


@unittest.skipUnless(shutil.which("bash") and shutil.which("node"), "necesita bash y node")
class DesplegarEnciendeLaSeguridadTests(Base):
    def correr(self, **extra):
        # Copia de los scripts en un directorio aparte: desplegar.sh escribe target/swarm/stack.json en su raíz.
        copia = os.path.join(self.tmp, "repo")
        os.makedirs(os.path.join(copia, "scripts", "swarm"))
        for nombre in ("desplegar.sh", "generar-stack.js"):
            shutil.copy(os.path.join(RAIZ, "scripts", "swarm", nombre), os.path.join(copia, "scripts", "swarm"))
        r = subprocess.run(["bash", os.path.join(copia, "scripts", "swarm", "desplegar.sh"), "--solo-generar"],
                           capture_output=True, text=True, timeout=60, env=self.entorno(**extra))
        with open(os.path.join(self.tmp, "env.txt"), encoding="utf-8") as f:
            return r, f.read()

    def test_por_defecto_el_stack_valida_el_jwt(self):
        r, valor = self.correr()
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(valor, "true")

    def test_un_preprod_seguridad_en_el_env_no_la_apaga(self):
        with open(self.env_file, "a", encoding="utf-8") as f:
            f.write("PREPROD_SEGURIDAD=false\n")
        r, valor = self.correr()
        self.assertEqual(valor, "true", r.stderr)

    def test_se_apaga_solo_a_pedido(self):
        r, valor = self.correr(DONATRACK_SEGURIDAD="false")
        self.assertEqual(valor, "false", r.stderr)

    def test_un_valor_invalido_corta_el_despliegue(self):
        env = self.entorno(DONATRACK_SEGURIDAD="si")
        r = subprocess.run(["bash", DESPLEGAR, "--solo-generar"], capture_output=True, text=True, timeout=60, env=env)
        self.assertEqual(r.returncode, 1)
        self.assertIn("DONATRACK_SEGURIDAD", r.stderr)

    def test_el_compose_sigue_apagado_por_defecto_para_el_preprod_y_el_ci(self):
        with open(os.path.join(RAIZ, "docker-compose.preprod.yml"), encoding="utf-8") as f:
            compose = f.read()
        for variable in ("GATEWAY", "DONACIONES", "INCENTIVOS", "LOGISTICA", "NOTIFICACIONES"):
            self.assertIn("%s_SEGURIDAD_HABILITADA=${PREPROD_SEGURIDAD:-false}" % variable, compose)


if __name__ == "__main__":
    unittest.main()
