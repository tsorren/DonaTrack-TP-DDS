#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos de SPEC-06 E2-01 en Incentivos: seguridad por token (matriz de permisos, §7).

Corren sin Java ni Docker (stdlib solamente). Verifican que las anotaciones de los controllers
coincidan con la matriz, y atrapan errores que de otro modo solo aparecen al ejecutar (un `#donanteId`
que no coincide con el parámetro, un permiso que no existe en el enum). Lo que NO cubren —que la
cadena de seguridad responda 401 y 403 como corresponde— lo prueba `SeguridadIncentivosTest` con
`mvn clean test -pl incentivos-service -am`, que esta sesión no pudo correr.
"""

import glob
import os
import re
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SERVICIO = os.path.join(RAIZ, "incentivos-service")
CONTROLLERS = os.path.join(SERVICIO, "src", "main", "java", "grupo5", "incentivos", "controllers")
SEGURIDAD = os.path.join(SERVICIO, "src", "main", "java", "grupo5", "incentivos", "seguridad")

# Matriz §7 de docs/arquitectura/seguridad/matriz-permisos.md: (verbo, ruta) -> constante de Acceso.
# "ADMIN" es la regla por defecto de la clase: el endpoint no lleva anotación propia.
ESPERADO = {
    ("POST", "/api/incentivos/donantes/{donanteId}"): "ADMIN_O_SERVICIO",
    ("PATCH", "/api/incentivos/donantes/{donanteId}"): "ADMIN_O_SERVICIO",
    ("DELETE", "/api/incentivos/donantes/{donanteId}"): "ADMIN_O_SERVICIO",
    ("GET", "/api/incentivos/donantes/{donanteId}"): "VER_SUS_INCENTIVOS",
    ("GET", "/api/incentivos/donantes/{donanteId}/ascensos"): "VER_SUS_INCENTIVOS",
    ("GET", "/api/incentivos/donantes/{donanteId}/metricas"): "VER_SUS_INCENTIVOS",
    ("GET", "/api/incentivos/donantes/{donanteId}/misiones"): "VER_SUS_INCENTIVOS",
    ("GET", "/api/incentivos/donantes/{donanteId}/insignias"): "VER_SUS_INCENTIVOS",
    (
        "PUT",
        "/api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad",
    ): "CONFIGURAR_SUS_INSIGNIAS",
    ("GET", "/api/incentivos/ranking/posicion/{donanteId}"): "VER_SUS_INCENTIVOS",
    ("GET", "/api/incentivos/ranking/ultimo"): "VER_RANKING",
    ("GET", "/api/incentivos/ranking/historial"): "VER_RANKING",
    ("GET", "/api/incentivos/ranking/{periodo}"): "VER_RANKING",
    ("POST", "/api/incentivos/ranking/calcular"): "ADMIN_O_SERVICIO",
    ("POST", "/api/incentivos/donaciones"): "ADMIN_O_SERVICIO",
    ("POST", "/api/incentivos/donaciones/exitosa"): "ADMIN_O_SERVICIO",
    ("POST", "/api/incentivos/evaluaciones-inactividad"): "ADMIN_O_SERVICIO",
    ("POST", "/api/incentivos/verificaciones-racha"): "ADMIN_O_SERVICIO",
    ("GET", "/api/incentivos/admin/resumen"): "ADMIN",
}
# Constantes de Acceso que comparan al dueño con el `donanteId` de la ruta.
CON_DUENIO = {"VER_SUS_INCENTIVOS", "CONFIGURAR_SUS_INSIGNIAS"}


def leer(*partes):
    with open(os.path.join(*partes), encoding="utf-8") as f:
        return f.read()


def controllers():
    """Los @RestController de Incentivos (no las interfaces I*Controller)."""
    rutas = []
    for ruta in sorted(glob.glob(os.path.join(CONTROLLERS, "*Controller.java"))):
        if "@RestController" in leer(ruta):
            rutas.append(ruta)
    return rutas


def endpoints(ruta):
    """[(verbo, ruta completa, constante de @PreAuthorize o None, texto de la firma)] de un controller."""
    texto = leer(ruta)
    base = re.search(r'@RequestMapping\("([^"]+)"\)', texto).group(1)
    salida = []
    patron = re.compile(
        r"((?:  @\w+(?:\([^)]*\))?\n)+)"  # bloque de anotaciones del método
        r"  public [\s\S]*?\) \{\n",  # firma: termina en `) {` (un `{4}` de un @Pattern no la corta)
        re.S,
    )
    for m in patron.finditer(texto):
        anotaciones, firma = m.group(1), m.group(0)
        mapeo = re.search(r'@(Get|Post|Put|Patch|Delete)Mapping\("([^"]*)"\)', anotaciones)
        if not mapeo:
            continue
        previa = re.search(r"@PreAuthorize\((\w+)\)", anotaciones)
        salida.append(
            (mapeo.group(1).upper(), base + mapeo.group(2), previa.group(1) if previa else None, firma)
        )
    return salida


class PomYPropiedadesTests(unittest.TestCase):
    def test_el_pom_declara_el_resource_server_sin_version(self):
        pom = leer(SERVICIO, "pom.xml")
        m = re.search(
            r"<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*"
            r"<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>\s*"
            r"(<version>[^<]*</version>\s*)?</dependency>",
            pom,
        )
        self.assertIsNotNone(m, "falta la dependencia del resource server")
        self.assertIsNone(m.group(1), "la versión la administra el BOM de Spring Boot")

    def test_la_seguridad_viene_apagada_por_defecto(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertRegex(
            props, r"(?m)^donatrack\.seguridad\.habilitada=\$\{INCENTIVOS_SEGURIDAD_HABILITADA:false\}$"
        )

    def test_el_token_se_valida_contra_la_audiencia_del_api(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertRegex(
            props,
            r"(?m)^spring\.security\.oauth2\.resourceserver\.jwt\.audiences="
            r"\$\{INCENTIVOS_JWT_AUDIENCIA:donatrack-api\}$",
        )

    def test_el_issuer_y_las_claves_no_se_fijan_en_las_propiedades(self):
        # Con valor vacío Spring igual intenta armar el decoder y falla: llegan por entorno.
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertNotRegex(props, r"(?m)^spring\.security\.oauth2\.resourceserver\.jwt\.(issuer-uri|jwk-set-uri)")

    def test_ninguna_propiedad_enciende_la_seguridad_a_fuego(self):
        for ruta in glob.glob(os.path.join(SERVICIO, "src", "main", "resources", "*.properties")):
            self.assertNotRegex(leer(ruta), r"(?m)^donatrack\.seguridad\.habilitada=true", ruta)


class ComposeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        compose = leer(RAIZ, "docker-compose.yml")
        inicio = compose.index("\n  incentivos-service:\n")
        fin = re.search(r"\n  \S", compose[inicio + 3 :]).start() + inicio + 3
        cls.bloque = compose[inicio:fin]

    def test_el_compose_local_la_deja_apagada_por_defecto(self):
        self.assertIn("INCENTIVOS_SEGURIDAD_HABILITADA=${INCENTIVOS_SEGURIDAD_HABILITADA:-false}", self.bloque)

    def test_el_compose_pasa_el_issuer_y_las_claves_de_keycloak(self):
        self.assertIn("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=", self.bloque)
        self.assertIn(
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/"
            "realms/donatrack/protocol/openid-connect/certs",
            self.bloque,
        )

    def test_el_preprod_no_la_enciende(self):
        # Postman, integration-tests y k6 llaman sin token hasta E2-07 (decisión M-07 de la matriz).
        preprod = leer(RAIZ, "docker-compose.preprod.yml")
        self.assertNotIn("INCENTIVOS_SEGURIDAD_HABILITADA", preprod)


class ControllersTests(unittest.TestCase):
    def test_hay_seis_controllers(self):
        self.assertEqual(len(controllers()), 6)

    def test_cada_controller_deniega_por_defecto_con_admin(self):
        # Matriz, regla general 3: un endpoint que no está en la matriz exige ADMIN.
        for ruta in controllers():
            self.assertRegex(
                leer(ruta),
                r"@RestController\n@PreAuthorize\(ADMIN\)\n@RequestMapping\(",
                os.path.basename(ruta),
            )

    def test_los_endpoints_son_exactamente_los_de_la_matriz(self):
        encontrados = {(v, r) for ruta in controllers() for v, r, _, _ in endpoints(ruta)}
        self.assertEqual(
            sorted(encontrados - set(ESPERADO)), [], "endpoint que la matriz no menciona"
        )
        self.assertEqual(sorted(set(ESPERADO) - encontrados), [], "endpoint de la matriz sin código")

    def test_cada_endpoint_tiene_la_regla_de_la_matriz(self):
        for ruta in controllers():
            for verbo, path, constante, _ in endpoints(ruta):
                esperada = ESPERADO[(verbo, path)]
                if esperada == "ADMIN":
                    self.assertIsNone(constante, f"{verbo} {path}: debe heredar el ADMIN de la clase")
                else:
                    self.assertEqual(constante, esperada, f"{verbo} {path}")

    def test_el_dueno_se_compara_con_un_parametro_que_se_llama_donanteId(self):
        # `principal.puedeOperarSobre(#donanteId)` se resuelve por nombre de parámetro.
        for ruta in controllers():
            for verbo, path, constante, firma in endpoints(ruta):
                if constante in CON_DUENIO:
                    self.assertRegex(firma, r"@PathVariable\s+UUID\s+donanteId", f"{verbo} {path}")
                    self.assertIn("{donanteId}", path)

    def test_los_endpoints_para_el_servicio_no_comparan_dueno(self):
        for ruta in controllers():
            for verbo, path, constante, _ in endpoints(ruta):
                if constante == "ADMIN_O_SERVICIO":
                    self.assertNotIn(constante, CON_DUENIO)

    def test_el_ranking_general_no_pide_pertenencia(self):
        acceso = leer(SEGURIDAD, "Acceso.java")
        m = re.search(r"VER_RANKING\s*=\s*([^;]+);", acceso)
        self.assertNotIn("SU_DONANTE", m.group(1))

    def test_las_anotaciones_estan_importadas(self):
        for ruta in controllers():
            texto = leer(ruta)
            self.assertIn("import org.springframework.security.access.prepost.PreAuthorize;", texto)
            usadas = set(re.findall(r"@PreAuthorize\((\w+)\)", texto))
            for constante in usadas:
                self.assertIn(
                    f"import static grupo5.incentivos.seguridad.Acceso.{constante};",
                    texto,
                    f"{os.path.basename(ruta)}: falta importar {constante}",
                )

    def test_no_se_importan_constantes_que_no_se_usan(self):
        for ruta in controllers():
            texto = leer(ruta)
            for constante in re.findall(r"import static grupo5\.incentivos\.seguridad\.Acceso\.(\w+);", texto):
                self.assertIn(f"@PreAuthorize({constante})", texto, f"{os.path.basename(ruta)}: {constante} sin uso")


class AccesoYPermisosTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.acceso = leer(SEGURIDAD, "Acceso.java")
        enum = re.search(r"enum \w+ implements Permiso \{(.*?)\}", leer(SEGURIDAD, "PermisoIncentivos.java"), re.S)
        cls.permisos = set(re.findall(r"[A-Z_]+", enum.group(1)))

    def test_los_permisos_de_la_matriz_son_los_del_enum(self):
        matriz = leer(RAIZ, "docs", "arquitectura", "seguridad", "matriz-permisos.md")
        fila = re.findall(r"^\| `(\w+)` \| Incentivos \|", matriz, re.M)
        self.assertEqual(set(fila), self.permisos)

    def test_cada_permiso_que_chequea_acceso_esta_en_el_enum(self):
        usados = set(re.findall(r"hasAuthority\('(\w+)'\)", self.acceso))
        self.assertTrue(usados)
        self.assertLessEqual(usados, self.permisos)

    def test_cada_permiso_del_enum_se_chequea_en_algun_endpoint(self):
        usados = set(re.findall(r"hasAuthority\('(\w+)'\)", self.acceso))
        self.assertEqual(usados, self.permisos)

    def test_el_donante_pasa_por_rol_y_el_admin_siempre(self):
        m = re.search(r'DONANTE\s*=\s*ADMIN \+ "([^"]+)"', self.acceso)
        self.assertEqual(m.group(1), " or hasRole('DONANTE') and ")

    def test_las_expresiones_de_dueno_usan_el_actor_como_principal(self):
        self.assertIn('"principal.puedeOperarSobre(#donanteId)"', self.acceso)

    def test_la_clase_no_se_puede_instanciar(self):
        self.assertIn("private Acceso() {}", self.acceso)


if __name__ == "__main__":
    unittest.main()
