#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de SPEC-06 E2-02 (iteración 7): la sesión del cliente liviano en PostgreSQL (Spring Session JDBC, D5).

Corren sin Java ni Docker (stdlib; `sh` para ejecutar el script de contraseñas con un `psql` falso). Fijan lo
que cruza archivos: que la base `cliente`, su rol y su esquema se llamen igual en el script de Postgres, en el
compose y en la URL; que la sesión se active solo por propiedad (los tests del cliente no tienen base); que el
pom traiga solo las librerías y no el starter, que arrastraría un DataSource a todos los contextos; y que el
script de inicialización siga siendo opcional para los compose que no levantan el cliente. Que el script de
Postgres funcione de verdad se comprobó contra un PostgreSQL 16 real; que la sesión se serialice lo prueba
`SesionSerializableTest`, y el resto se prueba levantando el stack (docs/testing/demo-punta-a-punta.md).
"""

import base64
import os
import re
import shutil
import stat
import subprocess
import tempfile
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
COMPOSES = ("docker-compose.yml", "docker-compose.preprod.yml")
CLIENTE = os.path.join("cliente-liviano", "src")


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8", errors="replace") as f:
        return f.read()


def bloque(compose, servicio):
    texto = leer(compose)
    inicio = texto.index("\n  %s:\n" % servicio)
    fin = re.search(r"\n  \S", texto[inicio + 3:]).start() + inicio + 3
    return texto[inicio:fin]


def entorno(b):
    return [l.strip()[2:] for l in b.splitlines() if l.strip().startswith("- ")]


def variables_env(texto):
    return dict(re.findall(r"(?m)^([A-Z][A-Z0-9_]*)=(.*)$", texto))


class BaseDeDatosTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.sql = leer("persistencia", "init-db", "01-init-schemas-roles.sql")

    def test_hay_un_rol_una_base_y_un_esquema_del_cliente(self):
        self.assertRegex(self.sql, r"ARRAY\[[^\]]*'cliente_user'[^\]]*\]")
        self.assertRegex(self.sql, r"unnest\(ARRAY\[[^\]]*'cliente'[^\]]*\]\)")
        self.assertIn("\\connect cliente\nCREATE SCHEMA IF NOT EXISTS cliente AUTHORIZATION cliente_user;", self.sql)
        self.assertIn("ALTER ROLE cliente_user SET search_path = cliente;", self.sql)

    def test_solo_el_rol_dueno_se_conecta_a_la_base_del_cliente(self):
        revoke = re.search(r"REVOKE CONNECT ON DATABASE ([^;]+) FROM PUBLIC;", self.sql).group(1)
        self.assertIn("cliente", [b.strip() for b in revoke.split(",")])

    def test_el_sql_no_lleva_ninguna_contrasena(self):
        self.assertNotRegex(self.sql, r"(?i)cliente_user[^;\n]*password\s*'")

    def test_el_orden_de_las_bases_es_el_mismo_en_las_tres_listas(self):
        roles = re.findall(r"'(\w+)_user'", re.search(r"FOREACH rol IN ARRAY ARRAY\[(.*?)\] LOOP", self.sql, re.S).group(1))
        bases = re.findall(r"'(\w+)'", re.search(r"unnest\(ARRAY\[(.*?)\]\)", self.sql, re.S).group(1))
        self.assertEqual(roles, bases)
        self.assertEqual(bases[-1], "cliente")


class ScriptDeContrasenasTests(unittest.TestCase):
    """Corre 02-set-role-passwords.sh con un `psql` falso que guarda lo que recibe."""

    def _correr(self, extra_env):
        with tempfile.TemporaryDirectory() as tmp:
            salida = os.path.join(tmp, "stdin.sql")
            fake = os.path.join(tmp, "psql")
            with open(fake, "w", encoding="utf-8") as f:
                f.write('#!/bin/sh\ncat >> "%s"\nprintf "%%s\\n" "$@" >> "%s.args"\n' % (salida, salida))
            os.chmod(fake, os.stat(fake).st_mode | stat.S_IEXEC)
            env = {"PATH": tmp + os.pathsep + os.environ.get("PATH", ""), "POSTGRES_USER": "admin", "POSTGRES_DB": "donatrack",
                   "NOTIFICACIONES_DB_PASSWORD": "n", "INCENTIVOS_DB_PASSWORD": "i", "LOGISTICA_DB_PASSWORD": "l",
                   "DONACIONES_DB_PASSWORD": "d"}
            env.update(extra_env)
            r = subprocess.run(["sh", os.path.join(RAIZ, "persistencia", "init-db", "02-set-role-passwords.sh")],
                               env=env, capture_output=True, text=True)
            def leer_o_vacio(ruta):
                if not os.path.exists(ruta):
                    return ""
                with open(ruta, encoding="utf-8") as f:
                    return f.read()

            return r, leer_o_vacio(salida), leer_o_vacio(salida + ".args")

    @unittest.skipIf(shutil.which("sh") is None, "sh no disponible")
    def test_fija_la_clave_del_cliente_si_la_variable_esta(self):
        r, sql, args = self._correr({"CLIENTE_DB_PASSWORD": "cl-secreta"})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("ALTER ROLE cliente_user WITH PASSWORD :'cliente';", sql)   # la variable de psql, no un literal
        self.assertIn("cliente=cl-secreta", args)
        self.assertNotIn("cl-secreta", sql, "la clave no debe ir escrita dentro del SQL")

    @unittest.skipIf(shutil.which("sh") is None, "sh no disponible")
    def test_no_rompe_si_falta_la_variable_del_cliente(self):
        # Los compose de demo e híbrido usan este mismo script y no levantan el cliente.
        r, sql, args = self._correr({})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertNotIn("cliente", sql + args)
        self.assertIn("ALTER ROLE donaciones_user", sql)

    @unittest.skipIf(shutil.which("sh") is None, "sh no disponible")
    def test_la_clave_del_cliente_vacia_cuenta_como_ausente(self):
        r, sql, args = self._correr({"CLIENTE_DB_PASSWORD": ""})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertNotIn("cliente", sql + args)

    @unittest.skipIf(shutil.which("sh") is None, "sh no disponible")
    def test_keycloak_y_el_cliente_conviven(self):
        r, sql, args = self._correr({"KEYCLOAK_DB_PASSWORD": "kc", "CLIENTE_DB_PASSWORD": "cl"})
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("ALTER ROLE keycloak_user", sql)
        self.assertIn("ALTER ROLE cliente_user", sql)


class VariablesYComposeTests(unittest.TestCase):
    def test_la_contrasena_esta_en_env_env_example_y_generador(self):
        for archivo in (".env", ".env.example"):
            env = variables_env(leer(archivo))
            self.assertTrue(env.get("CLIENTE_DB_PASSWORD", "").startswith("cambiar-"), archivo)
        self.assertIn('echo "CLIENTE_DB_PASSWORD=$(pw)"', leer("scripts", "generar-env-db.sh"))

    def test_los_dos_compose_le_pasan_la_clave_a_postgres_y_es_obligatoria(self):
        for compose in COMPOSES:
            self.assertIn("CLIENTE_DB_PASSWORD: ${CLIENTE_DB_PASSWORD:?ver .env.example}", bloque(compose, "postgres"), compose)

    def test_los_compose_de_demo_e_hibrido_no_la_necesitan(self):
        for compose in ("docker-compose.demo.yml", "docker-compose.hybrid.yml"):
            self.assertNotIn("CLIENTE_DB_PASSWORD", leer(compose), compose)

    def test_el_cliente_enciende_la_sesion_y_apunta_a_su_base(self):
        for compose in COMPOSES:
            e = entorno(bloque(compose, "cliente-liviano"))
            self.assertIn("CLIENTE_SESIONES_JDBC_HABILITADO=true", e, compose)
            self.assertIn("CLIENTE_SESIONES_JDBC_URL=jdbc:postgresql://postgres:5432/cliente?currentSchema=cliente", e, compose)
            self.assertIn("CLIENTE_SESIONES_JDBC_USUARIO=cliente_user", e, compose)
            self.assertIn("CLIENTE_SESIONES_JDBC_CLAVE=${CLIENTE_DB_PASSWORD:?ver .env.example}", e, compose)

    def test_la_clave_de_cifrado_de_la_sesion_llega_al_cliente_y_es_obligatoria(self):
        # CifradoDeLaSesion: con la sesión en la base, todas las instancias necesitan la misma clave.
        for archivo in (".env", ".env.example"):
            clave = variables_env(leer(archivo)).get("CLIENTE_SESIONES_CIFRADO_CLAVE", "")
            self.assertEqual(len(base64.b64decode(clave, validate=True)), 32, archivo)
        self.assertIn('echo "CLIENTE_SESIONES_CIFRADO_CLAVE=$(openssl rand -base64 32)"', leer("scripts", "generar-env-db.sh"))
        for compose in COMPOSES:
            e = entorno(bloque(compose, "cliente-liviano"))
            self.assertIn("CLIENTE_SESIONES_CIFRADO_CLAVE=${CLIENTE_SESIONES_CIFRADO_CLAVE:?ver .env.example}", e, compose)
            for servicio in ("donaciones-service", "incentivos-service", "notificaciones-service", "logistica-service", "api-gateway", "keycloak"):
                self.assertNotIn("CLIENTE_SESIONES_CIFRADO_CLAVE", bloque(compose, servicio), "%s %s" % (compose, servicio))
        props = leer("cliente-liviano", "src", "main", "resources", "application.properties")
        self.assertIn("cliente.sesiones.cifrado-clave=${CLIENTE_SESIONES_CIFRADO_CLAVE:}", props)

    def test_la_base_el_rol_y_el_esquema_se_llaman_igual_en_el_script_y_en_la_url(self):
        sql = leer("persistencia", "init-db", "01-init-schemas-roles.sql")
        url = re.search(r"CLIENTE_SESIONES_JDBC_URL=jdbc:postgresql://postgres:5432/(\w+)\?currentSchema=(\w+)", leer("docker-compose.yml"))
        base, esquema = url.groups()
        self.assertEqual(base, "cliente")
        self.assertEqual(esquema, "cliente")
        self.assertIn("CREATE SCHEMA IF NOT EXISTS %s AUTHORIZATION %s_user;" % (esquema, base), sql)

    def test_el_cliente_espera_a_postgres_sano(self):
        for compose in COMPOSES:
            dep = re.search(r"    depends_on:\n((?:      .*\n)+)", bloque(compose, "cliente-liviano") + "\n").group(1)
            self.assertRegex(dep, r"postgres:\n        condition: service_healthy", compose)

    def test_el_host_de_la_url_es_el_servicio_de_postgres_de_cada_compose(self):
        for compose in COMPOSES:
            self.assertIn("\n  postgres:\n", leer(compose), compose)

    def test_ningun_otro_servicio_recibe_la_clave_del_cliente(self):
        for compose in COMPOSES:
            for servicio in ("donaciones-service", "incentivos-service", "notificaciones-service", "logistica-service", "api-gateway", "keycloak"):
                self.assertNotIn("CLIENTE_DB_PASSWORD", bloque(compose, servicio), "%s %s" % (compose, servicio))


class JavaYPomTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.pom = leer("cliente-liviano", "pom.xml")
        cls.config = leer(CLIENTE, "main", "java", "grupo5", "cliente", "sesion", "SesionesJdbcConfig.java")
        cls.props = leer(CLIENTE, "main", "resources", "application.properties")

    def _dependencia(self, grupo, artefacto):
        return re.search(r"<dependency>\s*<groupId>%s</groupId>\s*<artifactId>%s</artifactId>(.*?)</dependency>" % (re.escape(grupo), re.escape(artefacto)),
                         self.pom, re.S)

    def test_el_pom_trae_las_librerias_de_la_sesion_sin_version(self):
        for grupo, artefacto in (("org.springframework.session", "spring-session-jdbc"), ("com.zaxxer", "HikariCP"),
                                 ("org.postgresql", "postgresql")):
            m = self._dependencia(grupo, artefacto)
            self.assertIsNotNone(m, artefacto)
            self.assertNotIn("<version>", m.group(1), "%s: la versión la administra el BOM de Spring Boot" % artefacto)
        self.assertIn("<scope>runtime</scope>", self._dependencia("org.postgresql", "postgresql").group(1))

    def test_el_pom_no_trae_los_starters_que_arrastrarian_un_datasource(self):
        for starter in ("spring-boot-starter-session-jdbc", "spring-boot-starter-jdbc", "spring-boot-starter-data-jpa",
                        "spring-boot-starter-data-jdbc", "spring-boot-starter-flyway"):
            self.assertNotIn("<artifactId>%s</artifactId>" % starter, self.pom, starter)

    def test_la_configuracion_solo_se_activa_por_propiedad_y_viene_apagada(self):
        self.assertIn('@ConditionalOnProperty(name = "cliente.sesiones.jdbc.habilitado", havingValue = "true")', self.config)
        self.assertIn("cliente.sesiones.jdbc.habilitado=${CLIENTE_SESIONES_JDBC_HABILITADO:false}", self.props)

    def test_el_enable_de_la_sesion_esta_solo_en_esa_clase(self):
        usos = []
        for ruta, _, archivos in os.walk(os.path.join(RAIZ, "cliente-liviano", "src", "main", "java")):
            for a in archivos:
                if a.endswith(".java") and "@EnableJdbcHttpSession" in open(os.path.join(ruta, a), encoding="utf-8").read():
                    usos.append(a)
        self.assertEqual(usos, ["SesionesJdbcConfig.java"])

    def test_cada_propiedad_que_lee_el_java_tiene_su_variable_en_el_compose(self):
        e = entorno(bloque("docker-compose.yml", "cliente-liviano"))
        for propiedad in re.findall(r'\$\{(cliente\.sesiones\.jdbc\.(?:url|usuario|clave))\}', self.config):
            variable = propiedad.upper().replace(".", "_")
            self.assertTrue(any(x.startswith(variable + "=") for x in e), "%s -> falta %s en el compose" % (propiedad, variable))

    def test_las_tablas_se_crean_tolerando_el_error_y_se_verifica_que_existan(self):
        self.assertIn("setContinueOnError(true)", self.config)
        self.assertIn("select count(*) from spring_session_attributes", self.config)
        self.assertIn("throw new IllegalStateException", self.config)

    def test_usa_el_script_de_tablas_de_la_libreria_y_no_uno_propio(self):
        self.assertIn('"org/springframework/session/jdbc/schema-postgresql.sql"', self.config)

    def test_el_pool_y_el_datasource_son_los_de_la_sesion(self):
        self.assertIn("@SpringSessionDataSource", self.config)
        self.assertIn("setPoolName(\"sesiones\")", self.config)

    def test_el_cliente_no_guarda_tipos_propios_en_la_sesion(self):
        # Todo lo que va a la sesión se serializa: un tipo propio nuevo tendría que ser Serializable.
        for ruta, _, archivos in os.walk(os.path.join(RAIZ, "cliente-liviano", "src", "main", "java")):
            for a in archivos:
                if a.endswith(".java"):
                    texto = open(os.path.join(ruta, a), encoding="utf-8").read()
                    self.assertNotIn("@SessionAttributes", texto, a)
                    self.assertNotIn("@SessionScope", texto, a)

    def test_los_flash_attributes_son_solo_los_conocidos(self):
        # Los flash attributes van a la sesión y se serializan. Hoy: el aviso (texto, Envios), el número del
        # mensaje de contacto (long), "guardado" (boolean, AccesoController) y la contraseña provisoria, que
        # viaja cifrada como texto (CifradoDeLaSesion). Uno nuevo, o de otro tipo, tiene que revisarse a
        # propósito (y sumarse a SesionSerializableTest).
        esperadas = {
            "Envios.java": 1,
            "PublicoContactoEnvioController.java": 1,
            "AccesoController.java": 1,
            "AdminAdministradoresController.java": 2,
        }
        llamadas = {}
        for ruta, _, archivos in os.walk(os.path.join(RAIZ, "cliente-liviano", "src", "main", "java")):
            for a in archivos:
                if a.endswith(".java"):
                    n = len(re.findall(r"\.addFlashAttribute\(", open(os.path.join(ruta, a), encoding="utf-8").read()))
                    if n:
                        llamadas[a] = n
        self.assertEqual(llamadas, esperadas, "cambiaron los addFlashAttribute: ¿lo que guardan es Serializable?")
        controller = leer(CLIENTE, "main", "java", "grupo5", "cliente", "web", "AdminAdministradoresController.java")
        self.assertEqual(len(re.findall(r"PROVISORIA,\s*cifrada\(", controller)), 2,
                         "la contraseña provisoria tiene que ir al flash cifrada")

    def test_existen_los_tests_java_de_la_sesion(self):
        for nombre in ("SesionSerializableTest.java", "SesionesJdbcConfigTest.java"):
            self.assertTrue(os.path.isfile(os.path.join(RAIZ, CLIENTE, "test", "java", "grupo5", "cliente", "sesion", nombre)), nombre)

    def test_la_exclusion_de_autoconfiguraciones_lista_solo_datasource_y_sesion(self):
        m = re.search(r"spring\.autoconfigure\.exclude=((?:\\\n|[^\n])*)", self.props)
        clases = [c.strip() for c in re.split(r",\\?\s*", m.group(1).replace("\\\n", "")) if c.strip()]
        self.assertTrue(clases)
        for c in clases:
            self.assertRegex(c.rsplit(".", 1)[1], r"^(DataSource|Session)AutoConfiguration$", c)


class DocumentacionTests(unittest.TestCase):
    def test_la_guia_ya_no_dice_que_las_sesiones_viven_en_memoria(self):
        guia = leer("docs", "testing", "demo-punta-a-punta.md")
        self.assertIsNone(re.search(r"(?i)sesiones del sitio viven en memoria", guia), "la guía sigue diciendo que viven en memoria")
        self.assertIn("base `cliente`", guia)

    def test_el_readme_de_persistencia_nombra_la_base_del_cliente(self):
        readme = leer("persistencia", "README.md")
        self.assertTrue("cliente_user" in readme, "el README de persistencia no nombra a cliente_user")
        self.assertTrue("CLIENTE_DB_PASSWORD" in readme, "el README de persistencia no nombra CLIENTE_DB_PASSWORD")


if __name__ == "__main__":
    unittest.main()
