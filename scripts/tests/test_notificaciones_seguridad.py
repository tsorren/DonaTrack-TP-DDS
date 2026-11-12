#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos de SPEC-06 E2-01 en Notificaciones: seguridad por token (matriz de permisos, §8).

Corren sin Java ni Docker (stdlib solamente). Verifican que las anotaciones de los controllers
coincidan con la matriz, y atrapan errores que de otro modo solo aparecen al ejecutar (un `#personaId`
que no coincide con el parámetro, un permiso que no existe en el enum, un dueño que delata ids).
Lo que NO cubren —que la cadena de seguridad responda 401 y 403— lo prueba
`SeguridadNotificacionesTest` con `mvn clean test -pl notificaciones-service -am`.
"""

import glob
import os
import re
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SERVICIO = os.path.join(RAIZ, "notificaciones-service")
BASE = os.path.join(SERVICIO, "src", "main", "java", "grupo5", "notificaciones")
CONTROLLERS = os.path.join(BASE, "controllers")
SEGURIDAD = os.path.join(BASE, "seguridad")

# Regla por defecto de cada controller (la que hereda un endpoint sin @PreAuthorize propio).
POR_DEFECTO = {
    "ContactoController": "ADMIN",
    "NotificacionController": "ADMIN",
    "PersonasController": "ADMIN_O_SERVICIO",
}

# Matriz §8 de docs/arquitectura/seguridad/matriz-permisos.md: (verbo, ruta) -> constante de Acceso,
# o None si hereda la regla de la clase.
ESPERADO = {
    ("POST", "/api/notificaciones/eventos"): "ADMIN_O_SERVICIO",
    ("POST", "/notificaciones"): "ADMIN_O_SERVICIO",
    ("GET", "/api/notificaciones"): None,
    ("GET", "/api/notificaciones/{id}"): None,
    ("GET", "/api/notificaciones/persona/{personaId}"): "VER_SUS_NOTIFICACIONES",
    ("GET", "/notificaciones/persona/{personaId}"): None,  # alias QA: solo ADMIN
    ("GET", "/api/notificaciones/persona/{personaId}/no-leidas/conteo"): "VER_SUS_NOTIFICACIONES",
    ("PATCH", "/api/notificaciones/{id}/leida"): "MARCAR_SU_NOTIFICACION",
    ("PATCH", "/api/notificaciones/persona/{personaId}/marcar-todas-leidas"): "VER_SUS_NOTIFICACIONES",
    ("PUT", "/api/notificaciones/personas"): None,
    ("GET", "/api/notificaciones/personas/{id}"): None,
    ("DELETE", "/api/notificaciones/personas/{id}"): None,
    # Contacto (HB-21): el envío es público (ReglasDeAccesoNotificaciones); leer y responder, del ADMIN.
    ("POST", "/api/contacto"): "PUBLICO",
    ("GET", "/api/contacto"): None,
    ("GET", "/api/contacto/{numero}"): None,
    ("POST", "/api/contacto/{numero}/respuesta"): None,
}
# Constante de Acceso -> parámetro del que sale el dueño (nombre que usa el SpEL).
CON_DUENIO = {"VER_SUS_NOTIFICACIONES": "personaId", "MARCAR_SU_NOTIFICACION": "id"}


def leer(*partes):
    with open(os.path.join(*partes), encoding="utf-8") as f:
        return f.read()


def controllers():
    """Los @RestController (no las interfaces I*Controller)."""
    return [r for r in sorted(glob.glob(os.path.join(CONTROLLERS, "*Controller.java"))) if "@RestController" in leer(r)]


def nombre(ruta):
    return os.path.basename(ruta)[: -len(".java")]


def notificacion_controller():
    return os.path.join(CONTROLLERS, "NotificacionController.java")


def regla_de_clase(ruta):
    m = re.search(r"@RestController\n@PreAuthorize\((\w+)\)\n", leer(ruta))
    return m.group(1) if m else None


def endpoints(ruta):
    """[(verbo, ruta completa, constante de @PreAuthorize o None, firma)] de un controller."""
    texto = leer(ruta)
    base = re.search(r'@RequestMapping\("([^"]+)"\)', texto)
    base = base.group(1) if base else ""
    salida = []
    patron = re.compile(
        r"((?:  @\w+(?:\([^)]*\))?\n)+)"  # bloque de anotaciones del método
        r"  public [\s\S]*?\) \{\n"  # firma: termina en `) {`
    )
    for m in patron.finditer(texto):
        anotaciones, firma = m.group(1), m.group(0)
        mapeo = re.search(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', anotaciones)
        if not mapeo:
            continue
        previa = re.search(r"@PreAuthorize\((\w+)\)", anotaciones)
        salida.append(
            (mapeo.group(1).upper(), base + (mapeo.group(2) or ""), previa.group(1) if previa else None, firma)
        )
    return salida


class PomYPropiedadesTests(unittest.TestCase):
    def test_el_pom_declara_el_resource_server_sin_version(self):
        m = re.search(
            r"<dependency>\s*<groupId>org\.springframework\.boot</groupId>\s*"
            r"<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>\s*"
            r"(<version>[^<]*</version>\s*)?</dependency>",
            leer(SERVICIO, "pom.xml"),
        )
        self.assertIsNotNone(m, "falta la dependencia del resource server")
        self.assertIsNone(m.group(1), "la versión la administra el BOM de Spring Boot")

    def test_la_seguridad_viene_apagada_por_defecto(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertRegex(
            props, r"(?m)^donatrack\.seguridad\.habilitada=\$\{NOTIFICACIONES_SEGURIDAD_HABILITADA:false\}$"
        )

    def test_el_token_se_valida_contra_la_audiencia_del_api(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertRegex(
            props,
            r"(?m)^spring\.security\.oauth2\.resourceserver\.jwt\.audiences="
            r"\$\{NOTIFICACIONES_JWT_AUDIENCIA:donatrack-api\}$",
        )

    def test_el_issuer_y_las_claves_no_se_fijan_en_las_propiedades(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertNotRegex(props, r"(?m)^spring\.security\.oauth2\.resourceserver\.jwt\.(issuer-uri|jwk-set-uri)")

    def test_ninguna_propiedad_enciende_la_seguridad_a_fuego(self):
        for ruta in glob.glob(os.path.join(SERVICIO, "src", "main", "resources", "*.properties")):
            self.assertNotRegex(leer(ruta), r"(?m)^donatrack\.seguridad\.habilitada=true", ruta)


class ComposeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        compose = leer(RAIZ, "docker-compose.yml")
        inicio = compose.index("\n  notificaciones-service:\n")
        fin = re.search(r"\n  \S", compose[inicio + 3 :]).start() + inicio + 3
        cls.bloque = compose[inicio:fin]

    def test_el_compose_local_la_deja_apagada_por_defecto(self):
        self.assertIn("NOTIFICACIONES_SEGURIDAD_HABILITADA=${NOTIFICACIONES_SEGURIDAD_HABILITADA:-false}", self.bloque)

    def test_el_compose_pasa_el_issuer_y_las_claves_de_keycloak(self):
        self.assertIn("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=", self.bloque)
        self.assertIn(
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/"
            "realms/donatrack/protocol/openid-connect/certs",
            self.bloque,
        )

    def test_el_preprod_no_la_enciende(self):
        self.assertNotIn("NOTIFICACIONES_SEGURIDAD_HABILITADA", leer(RAIZ, "docker-compose.preprod.yml"))


class ControllersTests(unittest.TestCase):
    def test_estan_los_tres_controllers(self):
        self.assertEqual(
            [nombre(c) for c in controllers()], ["ContactoController", "NotificacionController", "PersonasController"]
        )

    def test_cada_controller_deniega_por_defecto_con_su_regla(self):
        for ruta in controllers():
            self.assertEqual(regla_de_clase(ruta), POR_DEFECTO[nombre(ruta)], nombre(ruta))

    def test_los_endpoints_son_exactamente_los_de_la_matriz(self):
        encontrados = {(v, r) for ruta in controllers() for v, r, _, _ in endpoints(ruta)}
        self.assertEqual(sorted(encontrados - set(ESPERADO)), [], "endpoint que la matriz no menciona")
        self.assertEqual(sorted(set(ESPERADO) - encontrados), [], "endpoint de la matriz sin código")

    def test_cada_endpoint_tiene_la_regla_de_la_matriz(self):
        for ruta in controllers():
            for verbo, path, constante, _ in endpoints(ruta):
                self.assertEqual(constante, ESPERADO[(verbo, path)], f"{verbo} {path}")

    def test_ningun_endpoint_mezcla_dos_rutas(self):
        # Un método con dos rutas no puede tener reglas distintas por ruta: el alias va aparte.
        for ruta in controllers():
            self.assertNotRegex(leer(ruta), r"@(Get|Post|Put|Patch|Delete)Mapping\(\{", nombre(ruta))

    def test_el_alias_legado_es_un_metodo_propio_que_hereda_el_admin(self):
        alias = [e for e in endpoints(notificacion_controller()) if e[1] == "/notificaciones/persona/{personaId}"]
        self.assertEqual(len(alias), 1)
        self.assertIsNone(alias[0][2], "el alias es solo del ADMIN (hereda la regla de la clase)")

    def test_el_dueno_se_compara_con_el_parametro_que_usa_el_spel(self):
        for ruta in controllers():
            for verbo, path, constante, firma in endpoints(ruta):
                if constante in CON_DUENIO:
                    param = CON_DUENIO[constante]
                    self.assertRegex(firma, r"@PathVariable\s+UUID\s+%s\b" % param, f"{verbo} {path}")
                    self.assertIn("{%s}" % param, path)

    def test_las_anotaciones_estan_importadas_y_las_constantes_se_usan(self):
        for ruta in controllers():
            texto = leer(ruta)
            self.assertIn("import org.springframework.security.access.prepost.PreAuthorize;", texto)
            usadas = set(re.findall(r"@PreAuthorize\((\w+)\)", texto))
            importadas = set(re.findall(r"import static grupo5\.notificaciones\.seguridad\.Acceso\.(\w+);", texto))
            self.assertEqual(usadas, importadas, nombre(ruta))

    def test_la_interfaz_y_la_implementacion_tienen_los_mismos_endpoints(self):
        interfaz = leer(CONTROLLERS, "INotificacionController.java")
        de_interfaz = set(re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping\("([^"]*)"\)', interfaz))
        de_impl = {(v.capitalize(), r) for v, r, _, _ in endpoints(notificacion_controller())}
        self.assertEqual({(v, r) for v, r in de_interfaz}, de_impl)


class AccesoPermisosYDuenioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.acceso = leer(SEGURIDAD, "Acceso.java")
        enum = re.search(r"enum \w+ implements Permiso \{(.*?)\}", leer(SEGURIDAD, "PermisoNotificaciones.java"), re.S)
        cls.permisos = set(re.findall(r"[A-Z_]+", enum.group(1)))
        cls.duenio = leer(SEGURIDAD, "DuenioDe.java")

    def test_los_permisos_de_la_matriz_son_los_del_enum(self):
        matriz = leer(RAIZ, "docs", "arquitectura", "seguridad", "matriz-permisos.md")
        self.assertEqual(set(re.findall(r"^\| `(\w+)` \| Notificaciones \|", matriz, re.M)), self.permisos)

    def test_cada_permiso_que_chequea_acceso_esta_en_el_enum_y_viceversa(self):
        self.assertEqual(set(re.findall(r"hasAuthority\('(\w+)'\)", self.acceso)), self.permisos)

    def test_donante_y_entidad_pasan_por_rol_y_el_admin_siempre(self):
        m = re.search(r'DONANTE_O_ENTIDAD\s*=\s*ADMIN \+ "([^"]+)"', self.acceso)
        self.assertEqual(m.group(1), " or hasAnyRole('DONANTE', 'ENTIDAD') and ")

    def test_las_expresiones_de_dueno_usan_el_actor_y_el_bean_de_dueno(self):
        self.assertIn('"principal.puedeOperarSobre(#personaId)"', self.acceso)
        self.assertIn('"principal.puedeOperarSobre(@duenioDe.notificacion(#id))"', self.acceso)

    def test_el_bean_de_dueno_existe_con_el_nombre_y_el_metodo_que_usa_el_spel(self):
        self.assertIn('@Component("duenioDe")', self.duenio)
        self.assertRegex(self.duenio, r"public UUID notificacion\(UUID id\)")

    def test_una_notificacion_inexistente_no_tiene_dueno_en_vez_de_dar_404(self):
        # Con 404 quien no es admin podría averiguar qué ids existen: con null recibe 403 igual que con una ajena.
        self.assertIn(".orElse(null)", self.duenio)
        self.assertNotIn("orElseThrow", self.duenio)

    def test_el_servicio_expone_la_persona_destinataria(self):
        servicio = leer(BASE, "services", "impl", "NotificacionService.java")
        self.assertRegex(servicio, r"public Optional<UUID> personaDe\(UUID id\)")
        self.assertIn("repository.findById(id).map(Notificacion::getPersonaId)", servicio)

    def test_el_formulario_de_contacto_es_la_unica_ruta_publica(self):
        reglas = leer(SEGURIDAD, "ReglasDeAccesoNotificaciones.java")
        self.assertIn('List.of("/api/contacto")', reglas)
        self.assertIn('PUBLICO = "permitAll()"', self.acceso)

    def test_la_clase_no_se_puede_instanciar(self):
        self.assertIn("private Acceso() {}", self.acceso)


if __name__ == "__main__":
    unittest.main()
