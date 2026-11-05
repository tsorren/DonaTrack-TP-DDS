#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos y de scripts de SPEC-06 A1-01 (Keycloak en el docker-compose).

Corren sin Docker (stdlib solamente). Lo que NO cubren —que `docker compose up` deje
`keycloak` en `healthy`— se verifica a mano; ver auth-service/README.md.
"""

import json
import os
import re
import stat
import subprocess
import tempfile
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))

DIGEST = "sha256:3f3c5f8e03c614774b4bd9173b88391fefa4153bfc010d8012d59ad825377332"
IMAGEN = "quay.io/keycloak/keycloak:26.8.1@" + DIGEST

VARIABLES_KEYCLOAK = [
    "KEYCLOAK_DB_PASSWORD",
    "KC_BOOTSTRAP_ADMIN_USERNAME",
    "KC_BOOTSTRAP_ADMIN_PASSWORD",
    "KC_HOSTNAME",
    "KC_HTTP_ENABLED",
    "KC_HEALTH_ENABLED",
]
# Las que son contraseñas: en .env.example llevan un placeholder y el generador las hace aleatorias.
CONTRASENAS = ["KEYCLOAK_DB_PASSWORD", "KC_BOOTSTRAP_ADMIN_PASSWORD"]


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8") as f:
        return f.read()


def leer_si_existe(ruta):
    if not os.path.exists(ruta):
        return ""
    with open(ruta, encoding="utf-8") as f:
        return f.read()


def bloque_servicio(compose, nombre):
    """Devuelve el texto del servicio `nombre` (indentado 2 espacios bajo `services:`)."""
    lineas = compose.splitlines()
    inicio = None
    for i, linea in enumerate(lineas):
        if linea == "  %s:" % nombre:
            inicio = i
            break
    if inicio is None:
        return None
    fin = len(lineas)
    for j in range(inicio + 1, len(lineas)):
        if re.match(r"^  \S", lineas[j]) or re.match(r"^\S", lineas[j]):
            fin = j
            break
    return "\n".join(lineas[inicio:fin])


def variables_env(archivo_texto):
    """Pares CLAVE=valor de un .env (ignora comentarios y vacías)."""
    pares = {}
    for linea in archivo_texto.splitlines():
        if not linea.strip() or linea.lstrip().startswith("#"):
            continue
        clave, _, valor = linea.partition("=")
        pares[clave.strip()] = valor.strip()
    return pares


class DockerfileTests(unittest.TestCase):
    def setUp(self):
        ruta = os.path.join(RAIZ, "auth-service", "Dockerfile")
        self.assertTrue(os.path.isfile(ruta), "falta auth-service/Dockerfile")
        self.texto = leer("auth-service", "Dockerfile")

    def test_imagen_fijada_por_digest(self):
        self.assertRegex(self.texto, r"(?m)^FROM %s\b" % re.escape(IMAGEN))

    def test_corre_kc_build_con_opciones_de_build(self):
        self.assertIn("kc.sh build", self.texto)
        self.assertRegex(self.texto, r"(?m)^ENV KC_DB=postgres\b")
        self.assertRegex(self.texto, r"(?m)^ENV KC_HEALTH_ENABLED=true\b")

    def test_copia_realm_y_tema(self):
        self.assertRegex(self.texto, r"(?m)^COPY realm/donatrack-realm\.json /opt/keycloak/data/import/")
        self.assertRegex(self.texto, r"(?m)^COPY themes/donatrack /opt/keycloak/themes/donatrack")

    def test_arranca_optimizado_e_importa_el_realm(self):
        self.assertIn("--optimized", self.texto)
        self.assertIn("--import-realm", self.texto)

    def test_no_trae_secretos(self):
        self.assertNotRegex(self.texto, r"(?i)(password|secret)\s*=\s*[^\s$]")

    def test_el_realm_stub_es_json_valido_y_se_llama_donatrack(self):
        ruta = os.path.join(RAIZ, "auth-service", "realm", "donatrack-realm.json")
        self.assertTrue(os.path.isfile(ruta), "falta auth-service/realm/donatrack-realm.json")
        with open(ruta, encoding="utf-8") as f:
            realm = json.load(f)
        self.assertEqual(realm.get("realm"), "donatrack")
        self.assertIs(realm.get("enabled"), True)

    def test_existe_el_directorio_del_tema(self):
        self.assertTrue(os.path.isdir(os.path.join(RAIZ, "auth-service", "themes", "donatrack")))


class ComposeTests(unittest.TestCase):
    def setUp(self):
        self.compose = leer("docker-compose.yml")
        self.kc = bloque_servicio(self.compose, "keycloak")

    def test_existe_el_servicio_keycloak(self):
        self.assertIsNotNone(self.kc, "falta el servicio keycloak en docker-compose.yml")

    def test_se_construye_desde_auth_service(self):
        self.assertRegex(self.kc, r"context:\s*\./auth-service")
        self.assertRegex(self.kc, r"dockerfile:\s*Dockerfile")

    def test_publica_solo_el_8180_y_no_el_9000(self):
        puertos = re.findall(r'(?m)^\s+- "(\d+:\d+)"', self.kc)
        self.assertEqual(puertos, ["8180:8080"])

    def test_variables_obligatorias_sin_valor_por_defecto(self):
        for var in ["KC_DB_PASSWORD=${KEYCLOAK_DB_PASSWORD:?",
                    "KC_BOOTSTRAP_ADMIN_USERNAME=${KC_BOOTSTRAP_ADMIN_USERNAME:?",
                    "KC_BOOTSTRAP_ADMIN_PASSWORD=${KC_BOOTSTRAP_ADMIN_PASSWORD:?",
                    "KC_HOSTNAME=${KC_HOSTNAME:?"]:
            self.assertIn(var, self.kc)

    def test_flags_de_http_y_health(self):
        self.assertIn("KC_HTTP_ENABLED=${KC_HTTP_ENABLED:-true}", self.kc)
        self.assertIn("KC_HEALTH_ENABLED=${KC_HEALTH_ENABLED:-true}", self.kc)

    def test_usa_su_propia_base_y_su_propio_rol(self):
        self.assertIn("KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak", self.kc)
        self.assertIn("KC_DB_USERNAME=keycloak_user", self.kc)

    def test_espera_a_postgres_sano(self):
        self.assertRegex(self.kc, r"postgres:\s*\n\s+condition: service_healthy")

    def test_healthcheck_por_el_puerto_de_gestion_sin_curl(self):
        hc = self.kc.split("healthcheck:", 1)[-1]
        self.assertIn("/dev/tcp/", hc)
        self.assertIn("9000", hc)
        self.assertIn("/health/ready", hc)
        self.assertNotIn("curl", hc)

    def test_sin_contrasenas_literales(self):
        for linea in self.kc.splitlines():
            if re.search(r"(?i)(PASSWORD|SECRET)=", linea):
                self.assertIn("${", linea, "contraseña literal: %s" % linea.strip())

    def test_postgres_recibe_la_clave_de_keycloak(self):
        pg = bloque_servicio(self.compose, "postgres")
        self.assertIn("KEYCLOAK_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD:?ver .env.example}", pg)


class InitDbTests(unittest.TestCase):
    def test_sql_crea_rol_y_base_keycloak_y_cierra_el_acceso(self):
        sql = leer("persistencia", "init-db", "01-init-schemas-roles.sql")
        self.assertRegex(sql, r"ARRAY\[[^\]]*'keycloak_user'[^\]]*\]")
        self.assertRegex(sql, r"unnest\(ARRAY\[[^\]]*'keycloak'[^\]]*\]\)")
        self.assertRegex(sql, r"REVOKE CONNECT ON DATABASE [^;]*\bkeycloak\b[^;]* FROM PUBLIC;")
        self.assertNotRegex(sql, r"(?i)keycloak_user[^;\n]*password\s*'")

    def _correr_02(self, extra_env):
        """Corre 02-set-role-passwords.sh con un `psql` falso que guarda lo que recibe por stdin."""
        with tempfile.TemporaryDirectory() as tmp:
            salida = os.path.join(tmp, "stdin.sql")
            fake = os.path.join(tmp, "psql")
            with open(fake, "w", encoding="utf-8") as f:
                f.write('#!/bin/sh\ncat >> "%s"\nprintf "%%s\\n" "$@" >> "%s.args"\n' % (salida, salida))
            os.chmod(fake, os.stat(fake).st_mode | stat.S_IEXEC)
            env = {
                "PATH": tmp + os.pathsep + os.environ.get("PATH", ""),
                "POSTGRES_USER": "admin",
                "POSTGRES_DB": "donatrack",
                "NOTIFICACIONES_DB_PASSWORD": "n",
                "INCENTIVOS_DB_PASSWORD": "i",
                "LOGISTICA_DB_PASSWORD": "l",
                "DONACIONES_DB_PASSWORD": "d",
            }
            env.update(extra_env)
            r = subprocess.run(["sh", os.path.join(RAIZ, "persistencia", "init-db", "02-set-role-passwords.sh")],
                               env=env, capture_output=True, text=True)
            return r, leer_si_existe(salida), leer_si_existe(salida + ".args")

    def test_02_fija_la_clave_de_keycloak_si_la_variable_esta(self):
        r, sql, args = self._correr_02({"KEYCLOAK_DB_PASSWORD": "kc-secreta"})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("ALTER ROLE keycloak_user WITH PASSWORD", sql + args)
        self.assertIn("kc-secreta", args)
        self.assertNotIn("kc-secreta", sql, "la clave no debe ir escrita dentro del SQL")

    def test_02_no_rompe_si_falta_la_variable_de_keycloak(self):
        """docker-compose.preprod.yml usa este mismo script y todavía no pasa KEYCLOAK_DB_PASSWORD (E2-12)."""
        r, sql, args = self._correr_02({})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertNotIn("keycloak", sql + args)
        self.assertIn("ALTER ROLE notificaciones_user", sql)

    def test_02_sigue_exigiendo_las_claves_de_los_servicios(self):
        with tempfile.TemporaryDirectory() as tmp:
            fake = os.path.join(tmp, "psql")
            with open(fake, "w", encoding="utf-8") as f:
                f.write("#!/bin/sh\ncat >/dev/null\n")
            os.chmod(fake, os.stat(fake).st_mode | stat.S_IEXEC)
            env = {"PATH": tmp + os.pathsep + os.environ.get("PATH", ""), "POSTGRES_USER": "admin",
                   "POSTGRES_DB": "donatrack", "KEYCLOAK_DB_PASSWORD": "x"}
            r = subprocess.run(["sh", os.path.join(RAIZ, "persistencia", "init-db", "02-set-role-passwords.sh")],
                               env=env, capture_output=True, text=True)
            self.assertNotEqual(r.returncode, 0)


class VariablesDeEntornoTests(unittest.TestCase):
    def test_env_example_define_las_variables_de_keycloak(self):
        env = variables_env(leer(".env.example"))
        for var in VARIABLES_KEYCLOAK:
            self.assertIn(var, env, "falta %s en .env.example" % var)
        for var in CONTRASENAS:
            self.assertTrue(env[var].startswith("cambiar-"), "%s debe ser un placeholder 'cambiar-…'" % var)
        self.assertRegex(env["KC_HOSTNAME"], r"^http://localhost:8180$")
        self.assertEqual(env["KC_HTTP_ENABLED"], "true")
        self.assertEqual(env["KC_HEALTH_ENABLED"], "true")

    def test_todas_las_variables_obligatorias_del_servicio_estan_en_env_example(self):
        kc = bloque_servicio(leer("docker-compose.yml"), "keycloak") or ""
        env = variables_env(leer(".env.example"))
        for var in set(re.findall(r"\$\{(\w+):\?", kc)):
            self.assertIn(var, env, "%s es obligatoria en el compose pero no está en .env.example" % var)

    def _generar(self):
        with tempfile.TemporaryDirectory() as tmp:
            destino = os.path.join(tmp, ".env.generado")
            r = subprocess.run(["bash", os.path.join(RAIZ, "scripts", "generar-env-db.sh"), destino],
                               capture_output=True, text=True)
            self.assertEqual(r.returncode, 0, r.stderr)
            with open(destino, encoding="utf-8") as f:
                return variables_env(f.read())

    def test_generador_produce_las_variables_de_keycloak(self):
        env = self._generar()
        for var in VARIABLES_KEYCLOAK:
            self.assertIn(var, env, "scripts/generar-env-db.sh no genera %s" % var)
        self.assertEqual(env["KC_HOSTNAME"], "http://localhost:8180")

    def test_generador_hace_aleatorias_las_contrasenas_de_keycloak(self):
        a, b = self._generar(), self._generar()
        for var in CONTRASENAS:
            self.assertNotEqual(a[var], b[var], "%s no es aleatoria" % var)
            self.assertGreaterEqual(len(a[var]), 16)

    def test_generador_y_env_example_tienen_las_mismas_variables_de_keycloak(self):
        ejemplo = {k for k in variables_env(leer(".env.example")) if k in VARIABLES_KEYCLOAK}
        generado = {k for k in self._generar() if k in VARIABLES_KEYCLOAK}
        self.assertEqual(ejemplo, generado)


class ReadmeTests(unittest.TestCase):
    def test_avisa_que_con_un_volumen_previo_hace_falta_down_v(self):
        texto = leer("auth-service", "README.md")
        self.assertIn("docker compose down -v", texto)
        self.assertIn("8180", texto)


if __name__ == "__main__":
    unittest.main()
