#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos de SPEC-06 E2-01 en el api-gateway (ADR-E6-02, etapa 2): validación del JWT.

Corren sin Java ni Docker (stdlib solamente). La garantía principal es que las dos capas coincidan: la
allowlist pública del gateway tiene que ser exactamente el conjunto de endpoints `@PreAuthorize(PUBLICO)`
de los servicios, porque el gateway abre la ruta sin distinguir método y confía en que el servicio
rechace el resto. Lo que NO cubren —que la cadena responda 401 y que el Authorization llegue al
servicio— lo prueban `SeguridadGatewayTest` y `ReglasDeAccesoGatewayTest` con
`mvn clean test -pl api-gateway -am`.
"""

import glob
import os
import re
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
GATEWAY = os.path.join(RAIZ, "api-gateway")
GW_JAVA = os.path.join(GATEWAY, "src", "main", "java", "grupo5", "gateway")
SERVICIOS = ("donaciones-service", "incentivos-service", "logistica-service", "notificaciones-service")
METODOS = ("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")


def leer(*partes):
    with open(os.path.join(*partes), encoding="utf-8") as f:
        return f.read()


def allowlist():
    """[(método, ruta)] de donatrack.gateway.rutas-publicas, tal como están escritas en application.yml."""
    yml = leer(GATEWAY, "src", "main", "resources", "application.yml")
    bloque = re.search(r"^    rutas-publicas:\n((?:      - .*\n)+)", yml, re.M)
    return [tuple(l.strip()[2:].split(" ", 1)) for l in bloque.group(1).splitlines()]


def endpoints_publicos():
    """{(verbo, ruta completa)} de los endpoints con @PreAuthorize(PUBLICO) en los controllers de los servicios."""
    salida = set()
    patron = re.compile(r"((?:  @\w+(?:\([^)]*\))?\n)+)  public [\s\S]*?\) \{\n")
    for servicio in SERVICIOS:
        for ruta in glob.glob(os.path.join(RAIZ, servicio, "src", "main", "java", "**", "*.java"), recursive=True):
            texto = leer(ruta)
            if "@RestController" not in texto:
                continue
            base = re.search(r'@RequestMapping\("([^"]+)"\)', texto)
            base = base.group(1) if base else ""
            for m in patron.finditer(texto):
                anotaciones = m.group(1)
                mapeo = re.search(r"@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?", anotaciones)
                if mapeo and "@PreAuthorize(PUBLICO)" in anotaciones:
                    for sufijo in re.findall(r'"([^"]*)"', mapeo.group(2) or "") or [""]:
                        salida.add((mapeo.group(1).upper(), base + sufijo))
    return salida


def rutas_publicas_de_los_servicios():
    """Rutas que cada servicio abre en su cadena de seguridad (ReglasDeAcceso*.rutasPublicas)."""
    rutas = set()
    for servicio in SERVICIOS:
        for ruta in glob.glob(os.path.join(RAIZ, servicio, "src", "main", "java", "**", "ReglasDeAcceso*.java"), recursive=True):
            rutas.update(re.findall(r'"(/[^"]+)"', leer(ruta)))
    return rutas


def patrones_del_gateway():
    """Patrones de ruta que el gateway enruta (los strings de RutasGateway), como expresiones regulares."""
    texto = leer(GW_JAVA, "RutasGateway.java")
    patrones = re.findall(r'^\s+"(/api/[^"]*)"', texto, re.M)
    return [re.compile("^" + re.escape(p).replace(r"\*\*", ".*") + "$") for p in patrones]


class PomYConfiguracionTests(unittest.TestCase):
    def test_el_pom_declara_el_resource_server_sin_version(self):
        m = re.search(
            r"<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*"
            r"<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>\s*"
            r"(<version>[^<]*</version>\s*)?</dependency>",
            leer(GATEWAY, "pom.xml"),
        )
        self.assertIsNotNone(m, "falta la dependencia del resource server")
        self.assertIsNone(m.group(1), "la versión la administra el BOM de Spring Boot")

    def test_la_seguridad_viene_apagada_por_defecto(self):
        yml = leer(GATEWAY, "src", "main", "resources", "application.yml")
        self.assertRegex(yml, r"(?m)^  seguridad:\n    habilitada: \$\{GATEWAY_SEGURIDAD_HABILITADA:false\}$")

    def test_el_token_se_valida_contra_la_audiencia_del_api(self):
        yml = leer(GATEWAY, "src", "main", "resources", "application.yml")
        self.assertRegex(yml, r"(?m)^          audiences: \$\{GATEWAY_JWT_AUDIENCIA:donatrack-api\}$")

    def test_el_issuer_y_las_claves_no_se_fijan_en_el_yaml(self):
        # Con valor vacío Spring igual intenta armar el decoder y falla: llegan por entorno.
        yml = leer(GATEWAY, "src", "main", "resources", "application.yml")
        self.assertNotRegex(yml, r"(?m)^\s+(issuer-uri|jwk-set-uri):")

    def test_ninguna_configuracion_enciende_la_seguridad_a_fuego(self):
        for ruta in glob.glob(os.path.join(GATEWAY, "src", "main", "resources", "*")):
            self.assertNotRegex(leer(ruta), r"(?m)^\s+habilitada:\s*true", ruta)


class ComposeYRunnerTests(unittest.TestCase):
    @staticmethod
    def bloque(archivo, servicio):
        compose = leer(RAIZ, archivo)
        inicio = compose.index("\n  %s:\n" % servicio)
        fin = re.search(r"\n  \S", compose[inicio + 3 :]).start() + inicio + 3
        return compose[inicio:fin]

    def test_el_compose_local_la_deja_apagada_por_defecto_y_pasa_el_issuer_y_las_claves(self):
        b = self.bloque("docker-compose.yml", "api-gateway")
        self.assertTrue("GATEWAY_SEGURIDAD_HABILITADA=${GATEWAY_SEGURIDAD_HABILITADA:-false}" in b)
        self.assertTrue("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=" in b)
        self.assertTrue(
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/"
            "realms/donatrack/protocol/openid-connect/certs" in b
        )

    def test_el_gateway_esta_en_el_preprod_con_su_imagen_y_su_jar(self):
        b = self.bloque("docker-compose.preprod.yml", "api-gateway")
        self.assertIn("image: donatrack/api-gateway:preprod", b)
        self.assertIn("target: ci", b)
        self.assertIn("JAR_FILE: api-gateway/target/api-gateway-1.0.jar", b)
        self.assertIn('- "8086:8086"', b)
        self.assertIn("preprod-network", b)

    def test_el_preprod_apunta_a_los_cuatro_servicios_por_su_nombre_y_los_espera_sanos(self):
        b = self.bloque("docker-compose.preprod.yml", "api-gateway")
        puertos = {"DONACIONES": ("donaciones-service", 8080), "NOTIFICACIONES": ("notificaciones-service", 8081),
                   "INCENTIVOS": ("incentivos-service", 8082), "LOGISTICA": ("logistica-service", 8083)}
        for var, (host, puerto) in puertos.items():
            self.assertTrue("- %s_SERVICE_URL=http://%s:%d" % (var, host, puerto) in b, var)
            self.assertRegex(b, r"%s:\n        condition: service_healthy" % host)

    def test_el_preprod_tiene_healthcheck_y_la_seguridad_es_una_opcion_apagada_por_defecto(self):
        b = self.bloque("docker-compose.preprod.yml", "api-gateway")
        self.assertIn("http://localhost:8086/actuator/health", b)
        # Solo las líneas de entorno: los comentarios del bloque nombran la variable para explicarla.
        entorno = [l.strip() for l in b.splitlines() if l.strip().startswith("- ")]
        self.assertIn("- GATEWAY_SEGURIDAD_HABILITADA=${PREPROD_SEGURIDAD:-false}", entorno)
        self.assertFalse(any(re.match(r"- GATEWAY_SEGURIDAD_HABILITADA=true", l) for l in entorno))
        self.assertTrue(any(l.startswith("- SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=") for l in entorno))

    def test_el_resolver_ya_no_saltea_el_gateway(self):
        resolver = leer(RAIZ, "scripts", "resolver-imagenes-ghcr.sh")
        excluidos = re.search(r'"\$svc" == "common-lib".*', resolver).group(0)
        self.assertNotIn("api-gateway", excluidos)

    def test_el_runner_local_exige_el_jar_del_gateway(self):
        self.assertIn('"api-gateway/target/api-gateway-1.0.jar"', leer(RAIZ, "run-preprod-tests.sh"))


class AllowlistTests(unittest.TestCase):
    def test_cada_entrada_es_metodo_y_ruta(self):
        entradas = allowlist()
        self.assertTrue(entradas)
        for metodo, ruta in entradas:
            self.assertIn(metodo, METODOS)
            self.assertTrue(ruta.startswith("/api/"), ruta)
            self.assertNotIn("*", ruta, "la allowlist es por ruta exacta, sin comodines")

    def test_la_allowlist_es_exactamente_lo_que_los_servicios_marcan_como_publico(self):
        # Las dos capas coinciden: lo que el gateway abre sin token es lo que el servicio acepta sin token.
        self.assertEqual(set(allowlist()), endpoints_publicos())

    def test_las_rutas_abiertas_por_el_gateway_son_las_que_abren_los_servicios(self):
        self.assertEqual({r for _, r in allowlist()}, rutas_publicas_de_los_servicios())

    def test_cada_ruta_publica_tiene_una_ruta_expuesta_en_el_gateway(self):
        patrones = patrones_del_gateway()
        self.assertTrue(patrones)
        for _, ruta in allowlist():
            self.assertTrue(any(p.match(ruta) for p in patrones), "el gateway no enruta %s" % ruta)

    def test_el_metodo_lo_rechaza_el_servicio_en_las_rutas_que_mezclan_publico_y_privado(self):
        # El gateway abre la ruta para cualquier método: lo seguro depende de que en el servicio solo
        # los endpoints de la allowlist sean PUBLICO. Para cada ruta, cualquier otro verbo que el
        # servicio exponga en ella no debe ser público.
        publicos = endpoints_publicos()
        permitidos = set(allowlist())
        for verbo, ruta in publicos:
            self.assertIn((verbo, ruta), permitidos)
        self.assertEqual(publicos - permitidos, set())

    def test_hay_al_menos_un_metodo_que_no_es_get(self):
        # Si algún día desaparece el POST /api/contacto, este test avisa que el comentario del gateway
        # sobre "rutas que mezclan público y privado" ya no tiene un caso real.
        self.assertTrue(any(m != "GET" for m, _ in allowlist()))


class ReglasDeAccesoGatewayTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.codigo = leer(GW_JAVA, "ReglasDeAccesoGateway.java")

    def test_es_un_componente_que_implementa_reglas_de_acceso(self):
        self.assertIn("@Component", self.codigo)
        self.assertIn("implements ReglasDeAcceso", self.codigo)

    def test_acepta_los_metodos_http_y_corta_el_arranque_con_una_entrada_mala(self):
        for metodo in METODOS:
            self.assertIn('"%s"' % metodo, self.codigo)
        self.assertIn("throw new IllegalStateException", self.codigo)
        self.assertIn("MÉTODO /ruta", self.codigo)

    def test_los_tests_java_del_gateway_existen(self):
        for nombre in ("SeguridadGatewayTest.java", "ReglasDeAccesoGatewayTest.java"):
            self.assertTrue(os.path.isfile(os.path.join(GATEWAY, "src", "test", "java", "grupo5", "gateway", nombre)), nombre)


if __name__ == "__main__":
    unittest.main()
