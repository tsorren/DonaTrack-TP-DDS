#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de SPEC-06 E2-12b / SPEC-09 F6: el cliente liviano en el compose, con "emisor único".

Corren sin Docker ni Java (stdlib; `sh` para ejecutar el entrypoint con un `socat` y un `java` falsos).
Fijan que el cliente hable con los servicios solo por el gateway, que use la misma URL de Keycloak que los
tokens (el issuer) y que el reenvío dentro del contenedor haga lo que dice y corte ante una configuración
peligrosa. Que Keycloak responda de verdad por ese reenvío lo prueba levantar el stack: ver
`docs/testing/demo-punta-a-punta.md` y `ingresoConKeycloakReal` en el módulo del cliente.
"""

import os
import re
import shutil
import stat
import subprocess
import tempfile
import time
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
ENTRYPOINT = os.path.join(RAIZ, "cliente-liviano", "docker-entrypoint.sh")
COMPOSES = ("docker-compose.yml", "docker-compose.preprod.yml")


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


class DockerfileTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.d = leer("cliente-liviano", "Dockerfile")

    def test_tiene_las_tres_etapas_como_los_demas_servicios(self):
        for etapa in ("AS builder", "AS ci", "AS local"):
            self.assertIn(etapa, self.d)
        self.assertIn("mvn clean package -pl ${SERVICE_NAME} -am -DskipTests", self.d)
        self.assertIn("ARG JAR_FILE", self.d)

    def test_expone_el_puerto_del_cliente_y_corre_sin_root(self):
        self.assertEqual(self.d.count("EXPOSE 8090"), 2)
        self.assertEqual(self.d.count("USER 1001:1001"), 2)

    def test_instala_curl_para_el_healthcheck_y_socat_para_el_reenvio(self):
        self.assertEqual(self.d.count("apk add --no-cache curl socat"), 2)

    def test_el_entrypoint_se_corre_con_sh_y_sin_cr(self):
        # Un checkout de Windows con autocrlf no puede romperlo: ni bit de ejecución ni CRLF.
        self.assertEqual(self.d.count('ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]'), 2)
        self.assertEqual(self.d.count("sed -i 's/\\r$//' /app/docker-entrypoint.sh"), 2)

    def test_el_script_no_tiene_cr_en_el_repo(self):
        with open(ENTRYPOINT, "rb") as f:
            self.assertNotIn(b"\r", f.read())


class ComposeTests(unittest.TestCase):
    def test_el_cliente_esta_en_los_dos_compose_con_su_puerto(self):
        for compose in COMPOSES:
            b = bloque(compose, "cliente-liviano")
            self.assertIn('- "8090:8090"', b, compose)
            self.assertIn("PORT=8090", entorno(b), compose)

    def test_habla_con_los_cuatro_servicios_solo_por_el_gateway(self):
        for compose in COMPOSES:
            e = entorno(bloque(compose, "cliente-liviano"))
            for var in ("DONACIONES_URL", "NOTIFICACIONES_URL", "INCENTIVOS_URL", "LOGISTICA_URL"):
                self.assertIn("%s=http://api-gateway:8086" % var, e, "%s %s" % (compose, var))
            self.assertFalse(any(re.search(r"=http://(donaciones|notificaciones|incentivos|logistica)-service", x) for x in e), compose)

    def test_la_url_de_keycloak_es_la_misma_que_el_issuer_de_los_servicios(self):
        # Emisor único: el issuer de los tokens, el que validan los servicios y el que el navegador abre.
        for compose in COMPOSES:
            issuer = "KEYCLOAK_ISSUER_URI=${KC_HOSTNAME:-http://localhost:8180}/realms/donatrack"
            self.assertIn(issuer, entorno(bloque(compose, "cliente-liviano")), compose)
            servicio = "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=${KC_HOSTNAME:-http://localhost:8180}/realms/donatrack"
            self.assertIn(servicio, entorno(bloque(compose, "donaciones-service")), compose)

    def test_el_reenvio_apunta_a_keycloak_por_la_red_del_compose(self):
        for compose in COMPOSES:
            self.assertIn("KEYCLOAK_LOCAL_FORWARD_TO=keycloak:8080", entorno(bloque(compose, "cliente-liviano")), compose)

    def test_el_secreto_es_obligatorio_y_no_es_un_literal(self):
        for compose in COMPOSES:
            self.assertIn("CLIENTE_LIVIANO_SECRET=${CLIENTE_LIVIANO_SECRET:?ver .env.example}", entorno(bloque(compose, "cliente-liviano")), compose)

    def test_espera_a_keycloak_y_al_gateway_sanos(self):
        for compose in COMPOSES:
            b = bloque(compose, "cliente-liviano")
            dep = re.search(r"    depends_on:\n((?:      .*\n)+)", b + "\n").group(1)
            self.assertRegex(dep, r"keycloak:\n        condition: service_healthy")
            self.assertRegex(dep, r"api-gateway:\n        condition: service_healthy")

    def test_el_healthcheck_pega_a_una_ruta_publica_del_cliente(self):
        # Sin esto el contenedor nunca quedaría healthy: el cliente exige ingreso en todo lo demás.
        seguridad = leer("cliente-liviano", "src", "main", "java", "grupo5", "cliente", "seguridad", "SeguridadConfig.java")
        publicas = re.search(r"PUBLICAS = \{(.*?)\};", seguridad, re.S).group(1)
        self.assertIn('"/actuator/health"', publicas)
        for compose in COMPOSES:
            self.assertIn("http://localhost:8090/actuator/health", bloque(compose, "cliente-liviano"), compose)

    def test_el_preprod_usa_su_imagen_y_su_jar_y_el_local_construye(self):
        pre = bloque("docker-compose.preprod.yml", "cliente-liviano")
        self.assertIn("image: donatrack/cliente-liviano:preprod", pre)
        self.assertIn("JAR_FILE: cliente-liviano/target/cliente-liviano-1.0.jar", pre)
        self.assertIn("target: ci", pre)
        self.assertIn("preprod-network", pre)
        self.assertIn("target: local", bloque("docker-compose.yml", "cliente-liviano"))

    def test_el_puerto_publicado_coincide_con_el_de_los_redirect_del_realm(self):
        # El realm arma los redirect con CLIENTE_LIVIANO_BASE_URL: si no coincide con el puerto, el ingreso falla.
        base = re.search(r"(?m)^CLIENTE_LIVIANO_BASE_URL=(\S+)$", leer(".env.example")).group(1)
        self.assertEqual(base, "http://localhost:8090")

    def test_todos_los_servicios_de_cada_compose_estan_en_la_misma_red(self):
        # Un servicio sin `networks` cae en la red `default` y no ve a los demás por su nombre: el cliente
        # no encontraba a `keycloak` y el reenvío cerraba la conexión ("Unexpected end of file").
        for compose in COMPOSES:
            texto = leer(compose)
            seccion = re.search(r"(?ms)^services:\s*\n(.*?)(?=^[a-z]+:|\Z)", texto).group(1)
            servicios = re.findall(r"(?m)^  ([a-z0-9-]+):\s*$", seccion)
            self.assertIn("cliente-liviano", servicios, compose)
            redes = {}
            for servicio in servicios:
                m = re.search(r"    networks:\n((?:      - .*\n)+)", bloque(compose, servicio) + "\n")
                redes[servicio] = tuple(l.strip()[2:] for l in m.group(1).splitlines()) if m else ()
            self.assertTrue(redes["cliente-liviano"], "%s: el cliente no declara red" % compose)
            sin_red = [x for x, r in redes.items() if not r]
            self.assertEqual(sin_red, [], "%s: servicios sin red (caen en `default`): %s" % (compose, sin_red))
            self.assertEqual(len(set(redes.values())), 1, "%s: servicios en redes distintas: %s" % (compose, redes))

    def test_el_runner_local_exige_el_jar_del_cliente(self):
        self.assertIn('"cliente-liviano/target/cliente-liviano-1.0.jar"', leer("run-preprod-tests.sh"))

    def test_la_url_publica_de_keycloak_del_cliente_coincide_con_la_de_su_application_properties(self):
        props = leer("cliente-liviano", "src", "main", "resources", "application.properties")
        self.assertIn("issuer-uri=${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/donatrack}", props)


class IngresoRealTests(unittest.TestCase):
    """B1-05: el recorrido con Playwright. No se puede ejecutar sin un stack, así que se cruzan sus
    selectores y rutas contra las plantillas y controllers reales del cliente."""

    @classmethod
    def setUpClass(cls):
        cls.ruta = os.path.join("cliente-liviano", "src", "test", "java", "grupo5", "cliente", "visual",
                                "IngresoConKeycloakRealTest.java")
        cls.test = leer(cls.ruta)
        cls.templates = os.path.join("cliente-liviano", "src", "main", "resources", "templates")
        cls.acceso = leer("cliente-liviano", "src", "main", "java", "grupo5", "cliente", "web", "AccesoController.java")

    def test_vive_en_visual_que_el_build_normal_no_compila(self):
        self.assertIn("/visual/", self.ruta.replace(os.sep, "/"))
        self.assertIn("<testExclude>**/visual/**</testExclude>", leer("cliente-liviano", "pom.xml"))

    def test_solo_corre_con_el_stack_y_no_rompe_al_cargar_la_clase_sin_la_variable(self):
        self.assertIn('@EnabledIfEnvironmentVariable(named = "INGRESO_REAL_URL", matches = ".+")', self.test)
        # Un campo estático que lea la variable lanzaría NullPointerException al cargar la clase.
        self.assertNotRegex(self.test, r"static final [^;]*getenv\(")

    def test_el_boton_de_cerrar_sesion_existe_donde_el_recorrido_lo_busca(self):
        self.assertIn("form[action$='/logout'] [type=submit]", self.test)
        self.assertIn('action="/logout"', leer(self.templates, "cerrar-sesion.html"))
        # La cuenta recién registrada (sin perfil) ve ese botón suelto. En las pantallas privadas vive en el
        # menú de la foto (cuenta-menu.html, que incluye layout-privado), y el recorrido lo abre antes del clic.
        self.assertIn("cerrar-sesion", leer(self.templates, "publico-registro-donante.html"))
        menu = leer(self.templates, "cuenta-menu.html")
        self.assertIn("cerrar-sesion", menu)
        self.assertIn('class="navegacion-cuenta"', menu)
        self.assertIn("cuenta-menu :: menu", leer(self.templates, "layout-privado.html"))
        self.assertIn("details.navegacion-cuenta > summary", self.test)

    def test_el_formulario_de_completar_perfil_tiene_la_clase_que_busca(self):
        self.assertIn("form.registro-formulario", self.test)
        self.assertIn("registro-formulario", leer(self.templates, "publico-registro-donante.html"))

    def test_las_rutas_del_recorrido_existen_en_el_cliente(self):
        self.assertIn('"/publico-ingresar"', self.acceso)
        self.assertIn("/publico-registro{tipo", self.acceso)
        self.assertIn("?prompt=create", self.acceso)
        for ruta in ("/publico-registro-donante", "/publico-ingresar"):
            self.assertIn('sitio() + "%s"' % ruta, self.test)

    def test_una_cuenta_sin_perfil_cae_en_la_pantalla_de_elegir_el_tipo_y_la_tarjeta_existe(self):
        # El recorrido asumió primero que, al registrarse, se llegaba directo al formulario del donante. No:
        # PerfilPendiente manda a la cuenta sin perfil al destino de EstadoDeCuenta.SIN_PERFIL, que es esta pantalla.
        estado = leer("cliente-liviano", "src", "main", "java", "grupo5", "cliente", "seguridad", "EstadoDeCuenta.java")
        primera = re.search(r'SIN_PERFIL\("([^"]+)"', estado).group(1)
        self.assertEqual(primera, "/publico-registro")
        self.assertIn('url.equals(sitio() + "%s")' % primera, self.test)
        self.assertIn("~{componentes/registro :: tipos(''", leer(self.templates, "publico-registro.html"))
        tarjeta = re.search(r'<a href="/publico-registro-donante\?tipo=persona"[^>]*class="tarjeta registro-tipo"', leer(self.templates, "componentes", "registro.html"))
        self.assertIsNotNone(tarjeta, "falta la tarjeta de persona donante en el fragmento de tipos")
        self.assertIn("a.registro-tipo[href*='tipo=persona']", self.test)

    def test_cuando_algo_falla_el_recorrido_dice_donde_se_quedo_el_navegador(self):
        self.assertEqual(self.test.count("diagnostico(pagina,"), 2)
        self.assertIn('diagnostico(pagina, "donante", PISTA_DEL_DONANTE)', self.test)
        self.assertIn('diagnostico(pagina, "admin", PISTA_DEL_ADMIN)', self.test)
        cuerpo = re.search(r"private static String diagnostico\(.*?\n  \}\n", self.test, re.S).group(0)
        for dato in ("pagina.url()", "pagina.title()", "innerText", "screenshot"):
            self.assertIn(dato, cuerpo)
        # El parámetro también aparece en la firma: lo que importa es que la pista se muestre, y solo si el
        # navegador sigue en Keycloak.
        self.assertRegex(cuerpo, r"if \(enKeycloak\(pagina\.url\(\)\)\) \{\s*datos\.append\(\"\\nPista: \"\)\.append\(pistaSiSigueEnKeycloak\);")

    def test_si_el_admin_sigue_en_el_login_la_pista_apunta_a_la_clave_y_al_env(self):
        # Fue lo que pasó en la primera corrida real: Keycloak dijo "Usuario o contraseña incorrectos".
        pista = re.search(r"PISTA_DEL_ADMIN =(.*?);", self.test, re.S).group(1)
        for dato in ("INGRESO_REAL_ADMIN_EMAIL", "INGRESO_REAL_ADMIN_PASSWORD", "AUTH_ADMIN_PASSWORD", "del .env", "docker compose down -v"):
            self.assertIn(dato, pista)

    def test_el_admin_espera_la_url_de_su_area_sin_esperar_a_que_la_pagina_termine_de_cargar(self):
        # Esperar el evento load hacía vencer el test si el inicio del admin tarda en pintarse.
        self.assertIn("setWaitUntil(WaitUntilState.COMMIT)", self.test)
        self.assertIn('enElSitio(url) && url.contains("/admin-")', self.test)

    def test_las_pantallas_protegidas_que_usa_son_de_areas_que_existen(self):
        area = leer("cliente-liviano", "src", "main", "java", "grupo5", "cliente", "seguridad", "Area.java")
        for prefijo in ("donante", "admin"):
            self.assertIn('"%s"' % prefijo, area)
            self.assertIn('"/%s-inicio"' % prefijo, self.test)

    def test_comprueba_que_volver_a_entrar_pide_la_clave(self):
        # Dentro del método que cierra sesión: el selector de la clave también se usa al rellenar el login.
        metodo = re.search(r"private static void cerrarSesionYVerificarQuePideLaClave\(.*?\n  \}\n", self.test, re.S)
        self.assertIsNotNone(metodo)
        cuerpo = metodo.group(0)
        self.assertIn('url.equals(sitio() + "/")', cuerpo)
        self.assertIn("pagina.waitForURL(IngresoConKeycloakRealTest::enKeycloak)", cuerpo)
        self.assertRegex(cuerpo, r'assertTrue\(\s*pagina\.locator\("input\[name=password\]"\)\.count\(\) > 0')
        # Y los dos recorridos lo usan, cada uno con su pantalla protegida.
        self.assertEqual(self.test.count("cerrarSesionYVerificarQuePideLaClave(pagina,"), 2)

    def test_graba_video_y_no_lleva_credenciales_en_el_codigo(self):
        self.assertIn("setRecordVideoDir(VIDEOS)", self.test)
        self.assertIn('Path.of("target/ingreso-real")', self.test)
        self.assertNotRegex(self.test, r'"[^"]*(password|clave)[^"]*"\s*,\s*"[^"]{6,}"')
        self.assertIn('System.getenv("INGRESO_REAL_ADMIN_PASSWORD")', self.test)

    def test_el_admin_falla_ruidosamente_si_falta_la_configuracion(self):
        self.assertIn('assertNotNull(email, "Falta INGRESO_REAL_ADMIN_EMAIL', self.test)
        self.assertIn('assertNotNull(clave, "Falta INGRESO_REAL_ADMIN_PASSWORD', self.test)


@unittest.skipIf(shutil.which("sh") is None, "sh no disponible")
class EntrypointTests(unittest.TestCase):
    """Ejecuta el entrypoint real con un `socat` y un `java` falsos que registran cómo los invocan."""

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.log = os.path.join(self.tmp, "llamadas.log")
        bin_dir = os.path.join(self.tmp, "bin")
        os.makedirs(bin_dir)
        for nombre in ("socat", "java"):
            ruta = os.path.join(bin_dir, nombre)
            with open(ruta, "w") as f:
                f.write('#!/bin/sh\necho "%s $*" >> "%s"\n' % (nombre, self.log))
            os.chmod(ruta, os.stat(ruta).st_mode | stat.S_IEXEC)
        self.bin = bin_dir

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _leer_log(self):
        if not os.path.exists(self.log):
            return []
        with open(self.log) as f:
            return f.read().splitlines()

    def _correr(self, esperadas=0, **env):
        entorno = {"PATH": self.bin + os.pathsep + "/usr/bin:/bin"}
        entorno.update(env)
        r = subprocess.run(["sh", ENTRYPOINT, "arg1"], env=entorno, capture_output=True, text=True)
        # El socat real corre en segundo plano (`&`): su falso escribe al log sin orden garantizado ni
        # antes de que termine el proceso principal. Se espera a que lleguen las llamadas esperadas.
        limite = time.time() + 5
        llamadas = []
        while time.time() < limite:
            llamadas = self._leer_log()
            if len(llamadas) >= esperadas:
                break
            time.sleep(0.02)
        return r, llamadas

    def test_sin_la_variable_solo_arranca_java_y_no_abre_nada(self):
        r, llamadas = self._correr(1)
        self.assertEqual(r.returncode, 0, r.stderr)
        time.sleep(0.2)   # por si algo más corriera en segundo plano: no tiene que haber nada
        llamadas = self._leer_log()
        self.assertEqual(len(llamadas), 1)
        self.assertTrue(llamadas[0].startswith("java "))
        self.assertNotIn("preferIPv4Stack", llamadas[0])
        self.assertIn("-jar /app/app.jar arg1", llamadas[0])

    def test_con_la_variable_abre_el_puerto_del_issuer_solo_en_loopback(self):
        r, llamadas = self._correr(2, KEYCLOAK_LOCAL_FORWARD_TO="keycloak:8080",
                                   KEYCLOAK_ISSUER_URI="http://localhost:8180/realms/donatrack")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(len(llamadas), 2)
        self.assertIn("socat TCP-LISTEN:8180,bind=127.0.0.1,fork,reuseaddr TCP:keycloak:8080", llamadas)
        java = [l for l in llamadas if l.startswith("java ")]
        self.assertEqual(len(java), 1)
        self.assertTrue(java[0].startswith("java -Djava.net.preferIPv4Stack=true "))
        self.assertIn("-jar /app/app.jar arg1", java[0])

    def test_el_puerto_sale_del_issuer_y_no_esta_fijo(self):
        _, llamadas = self._correr(2, KEYCLOAK_LOCAL_FORWARD_TO="keycloak:8080", KEYCLOAK_ISSUER_URI="http://127.0.0.1:9180/realms/x")
        self.assertTrue(any("TCP-LISTEN:9180," in l for l in llamadas), llamadas)

    def test_corta_ante_una_configuracion_peligrosa_y_no_arranca_java(self):
        casos = {
            "issuer que no es local": "http://keycloak:8080/realms/x",
            "host que solo contiene localhost": "http://localhost.evil.com:8180/realms/x",
            "https": "https://localhost:8443/realms/x",
            "sin puerto (80, privilegiado)": "http://localhost/realms/x",
            "puerto privilegiado": "http://localhost:443/realms/x",
            "puerto no numérico": "http://localhost:abc/realms/x",
            "sin issuer": "",
        }
        for nombre, issuer in casos.items():
            r, llamadas = self._correr(KEYCLOAK_LOCAL_FORWARD_TO="keycloak:8080", KEYCLOAK_ISSUER_URI=issuer)
            self.assertEqual(r.returncode, 1, nombre)
            self.assertEqual(llamadas, [], "no debe abrir un puerto ni arrancar java: %s" % nombre)
            self.assertTrue(r.stderr.strip(), nombre)


if __name__ == "__main__":
    unittest.main()
