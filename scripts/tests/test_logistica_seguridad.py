#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests estáticos de SPEC-06 E2-01 en Logística: seguridad por token (matriz de permisos, §6).

Corren sin Java ni Docker (stdlib solamente). Verifican que las anotaciones de los controllers
coincidan con la matriz, y atrapan errores que de otro modo solo aparecen al ejecutar (un `#id` que no
coincide con el parámetro, un permiso que no existe en el enum, un dueño que delata ids, una ruta
pública sin regla). Lo que NO cubren —que la cadena de seguridad responda 401 y 403— lo prueba
`SeguridadLogisticaTest` con `mvn clean test -pl logistica-service -am`.
"""

import glob
import os
import re
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SERVICIO = os.path.join(RAIZ, "logistica-service")
BASE = os.path.join(SERVICIO, "src", "main", "java", "grupo5", "logistica")
CONTROLLERS = os.path.join(BASE, "controllers", "impl")
SEGURIDAD = os.path.join(BASE, "seguridad")
MATRIZ = os.path.join(RAIZ, "docs", "arquitectura", "seguridad", "matriz-permisos.md")

# Matriz §6 de docs/arquitectura/seguridad/matriz-permisos.md: (verbo, ruta) -> constante de Acceso,
# o None si hereda el ADMIN de la clase. Las filas de las fotos (HB-10) y del seguimiento por donante
# (HB-16) son [PROPOSED] en la matriz: no estaban en ella cuando se escribió.
ESPERADO = {
    ("POST", "/api/entregas"): "ADMIN_O_SERVICIO",
    ("GET", "/api/entregas"): None,
    ("GET", "/api/entregas/mapa"): "PUBLICO",
    ("GET", "/api/entregas/{id}"): "VER_SU_ENTREGA",
    ("GET", "/api/entregas/seguimiento/donaciones/{idDonacion}"): "VER_EL_SEGUIMIENTO_DE_SU_DONACION",
    ("GET", "/api/entregas/seguimiento/beneficiarios/{idBeneficiaria}"): "VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA",
    ("GET", "/api/entregas/seguimiento/donantes/{donanteId}"): "VER_EL_SEGUIMIENTO_DE_SU_DONANTE",
    ("PATCH", "/api/entregas/{id}/estado"): "CONFIRMAR_SU_ENTREGA",
    ("PATCH", "/api/entregas/{id}/fotos"): "CONFIRMAR_SU_ENTREGA",
    ("GET", "/api/entregas/{id}/historial"): "VER_SU_ENTREGA",
    ("POST", "/api/entregas/{id}/fotos"): "CONFIRMAR_SU_ENTREGA",
    ("GET", "/api/entregas/{id}/fotos/{fotoId}"): "VER_SU_ENTREGA",
    ("GET", "/api/rutas"): None,
    ("GET", "/api/rutas/{id}"): "VER_SU_RUTA",
    ("GET", "/api/rutas/{id}/entregas"): None,
    ("POST", "/api/rutas/{id}/entregas"): None,
    ("PATCH", "/api/rutas/{id}/estado"): None,
    ("POST", "/api/camiones"): None,
    ("GET", "/api/camiones"): None,
    ("GET", "/api/camiones/{id}"): None,
    ("PATCH", "/api/camiones/{id}/estado"): None,
    ("DELETE", "/api/camiones/{id}"): None,
    ("POST", "/api/choferes"): None,
    ("GET", "/api/choferes"): None,
    ("GET", "/api/choferes/{id}"): None,
    ("PATCH", "/api/choferes/{id}/estado"): None,
    ("DELETE", "/api/choferes/{id}"): None,
    ("POST", "/api/logistica/resultados"): "ADMIN_O_SERVICIO",
    ("POST", "/api/logistica/callback/rutas"): "ADMIN_O_SERVICIO",
    ("GET", "/api/logistica/planificaciones/{id}"): None,
    ("POST", "/api/logistica/planificaciones/ejecuciones"): None,
}
# Constante de Acceso -> parámetro de la ruta con el que se compara al dueño (nombre que usa el SpEL).
CON_DUENIO = {
    "VER_SU_ENTREGA": "id",
    "CONFIRMAR_SU_ENTREGA": "id",
    "VER_SU_RUTA": "id",
    "VER_EL_SEGUIMIENTO_DE_SU_DONACION": "idDonacion",
    "VER_EL_SEGUIMIENTO_DE_SU_DONANTE": "donanteId",
    "VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA": "idBeneficiaria",
}


def leer(*partes):
    with open(os.path.join(*partes), encoding="utf-8") as f:
        return f.read()


def controllers():
    return sorted(glob.glob(os.path.join(CONTROLLERS, "*Controller.java")))


def nombre(ruta):
    return os.path.basename(ruta)[: -len(".java")]


def regla_de_clase(ruta):
    m = re.search(r"@RestController\n@PreAuthorize\((\w+)\)\n@RequestMapping", leer(ruta))
    return m.group(1) if m else None


def endpoints(ruta):
    """[(verbo, ruta completa, constante de @PreAuthorize o None, firma)]; un método con dos rutas da dos."""
    texto = leer(ruta)
    base = re.search(r'@RequestMapping\("([^"]+)"\)', texto).group(1)
    salida = []
    patron = re.compile(r"((?:  @\w+(?:\([^)]*\))?\n)+)  public [\s\S]*?\) \{\n")
    for m in patron.finditer(texto):
        anotaciones, firma = m.group(1), m.group(0)
        mapeo = re.search(r"@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^)]*)\))?", anotaciones)
        if not mapeo:
            continue
        previa = re.search(r"@PreAuthorize\((\w+)\)", anotaciones)
        rutas = re.findall(r'"([^"]*)"', mapeo.group(2) or "") or [""]
        for sufijo in rutas:
            salida.append((mapeo.group(1).upper(), base + sufijo, previa.group(1) if previa else None, firma))
    return salida


def todos():
    return [(c, e) for c in controllers() for e in endpoints(c)]


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
        self.assertRegex(props, r"(?m)^donatrack\.seguridad\.habilitada=\$\{LOGISTICA_SEGURIDAD_HABILITADA:false\}$")

    def test_el_token_se_valida_contra_la_audiencia_del_api(self):
        props = leer(SERVICIO, "src", "main", "resources", "application.properties")
        self.assertRegex(
            props,
            r"(?m)^spring\.security\.oauth2\.resourceserver\.jwt\.audiences=\$\{LOGISTICA_JWT_AUDIENCIA:donatrack-api\}$",
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
        inicio = compose.index("\n  logistica-service:\n")
        fin = re.search(r"\n  \S", compose[inicio + 3 :]).start() + inicio + 3
        cls.bloque = compose[inicio:fin]

    def test_el_compose_local_la_deja_apagada_por_defecto(self):
        self.assertTrue("LOGISTICA_SEGURIDAD_HABILITADA=${LOGISTICA_SEGURIDAD_HABILITADA:-false}" in self.bloque)

    def test_el_compose_pasa_el_issuer_y_las_claves_de_keycloak(self):
        self.assertTrue("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=" in self.bloque)
        self.assertTrue(
            "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/"
            "realms/donatrack/protocol/openid-connect/certs" in self.bloque
        )

    def test_el_preprod_no_la_enciende(self):
        self.assertFalse("LOGISTICA_SEGURIDAD_HABILITADA" in leer(RAIZ, "docker-compose.preprod.yml"))


class ControllersTests(unittest.TestCase):
    def test_hay_siete_controllers(self):
        self.assertEqual(
            [nombre(c) for c in controllers()],
            [
                "CamionesController",
                "ChoferesController",
                "EntregasController",
                "FotosRecepcionController",
                "PlanificacionController",
                "PlanificacionManualController",
                "RutasController",
            ],
        )

    def test_cada_controller_deniega_por_defecto_con_admin(self):
        # Matriz, regla general 3: un endpoint que no está en la matriz exige ADMIN.
        for ruta in controllers():
            self.assertEqual(regla_de_clase(ruta), "ADMIN", nombre(ruta))

    def test_los_endpoints_son_exactamente_los_esperados(self):
        encontrados = {(v, r) for _, (v, r, _, _) in todos()}
        self.assertEqual(sorted(encontrados - set(ESPERADO)), [], "endpoint sin regla esperada")
        self.assertEqual(sorted(set(ESPERADO) - encontrados), [], "regla esperada sin endpoint")

    def test_cada_endpoint_tiene_la_regla_esperada(self):
        for _, (verbo, path, constante, _) in todos():
            self.assertEqual(constante, ESPERADO[(verbo, path)], f"{verbo} {path}")

    def test_el_dueno_se_compara_con_el_parametro_que_usa_el_spel(self):
        for _, (verbo, path, constante, firma) in todos():
            if constante in CON_DUENIO:
                param = CON_DUENIO[constante]
                self.assertRegex(firma, r'@PathVariable(?:\("%s"\))?\s+UUID\s+%s\b' % (param, param), f"{verbo} {path}")
                self.assertIn("{%s}" % param, path)

    def test_las_anotaciones_estan_importadas_y_las_constantes_se_usan_y_existen(self):
        declaradas = set(re.findall(r"public static final String (\w+)", leer(SEGURIDAD, "Acceso.java")))
        for ruta in controllers():
            texto = leer(ruta)
            self.assertIn("import org.springframework.security.access.prepost.PreAuthorize;", texto)
            usadas = set(re.findall(r"@PreAuthorize\((\w+)\)", texto))
            importadas = set(re.findall(r"import static grupo5\.logistica\.seguridad\.Acceso\.(\w+);", texto))
            self.assertEqual(usadas, importadas, nombre(ruta))
            self.assertLessEqual(usadas, declaradas, nombre(ruta))

    def test_los_endpoints_con_regla_propia_figuran_en_la_matriz(self):
        matriz = leer(MATRIZ)
        for _, (verbo, path, constante, _) in todos():
            if constante is not None:
                self.assertTrue(path in matriz, f"{verbo} {path} no figura en la matriz de permisos")

    def test_la_foto_se_sube_y_se_lee_con_la_misma_regla_que_se_adjunta_y_se_consulta(self):
        e = {(v, r): c for _, (v, r, c, _) in todos()}
        self.assertEqual(e[("POST", "/api/entregas/{id}/fotos")], e[("PATCH", "/api/entregas/{id}/fotos")])
        self.assertEqual(e[("GET", "/api/entregas/{id}/fotos/{fotoId}")], e[("GET", "/api/entregas/{id}/historial")])


class RutasPublicasTests(unittest.TestCase):
    def test_las_rutas_publicas_son_las_de_la_matriz(self):
        reglas = leer(SEGURIDAD, "ReglasDeAccesoLogistica.java")
        publicas = re.findall(r'"(/[^"]+)"', reglas)
        matriz = leer(MATRIZ)
        en_matriz = re.findall(r"^\| Logística \| `GET (/[^`]+)` \|", matriz, re.M)
        self.assertEqual(sorted(publicas), sorted(en_matriz))

    def test_cada_endpoint_publico_esta_en_las_reglas_y_viceversa(self):
        publicas = set(re.findall(r'"(/[^"]+)"', leer(SEGURIDAD, "ReglasDeAccesoLogistica.java")))
        marcados = {r for _, (v, r, c, _) in todos() if c == "PUBLICO"}
        self.assertEqual(publicas, marcados)

    def test_las_reglas_son_un_componente_que_implementa_reglas_de_acceso(self):
        reglas = leer(SEGURIDAD, "ReglasDeAccesoLogistica.java")
        self.assertIn("@Component", reglas)
        self.assertIn("implements ReglasDeAcceso", reglas)


class AccesoPermisosYDuenioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.acceso = leer(SEGURIDAD, "Acceso.java")
        enum = re.search(r"enum \w+ implements Permiso \{(.*?)\}", leer(SEGURIDAD, "PermisoLogistica.java"), re.S)
        cls.permisos = set(re.findall(r"[A-Z_]+", enum.group(1)))
        cls.duenio = leer(SEGURIDAD, "DuenioDe.java")

    def test_los_permisos_de_la_matriz_son_los_del_enum(self):
        matriz = leer(MATRIZ)
        self.assertEqual(set(re.findall(r"^\| `(\w+)` \| Logística \|", matriz, re.M)), self.permisos)

    def test_cada_permiso_que_chequea_acceso_esta_en_el_enum_y_viceversa(self):
        self.assertEqual(set(re.findall(r"hasAuthority\('(\w+)'\)", self.acceso)), self.permisos)

    def test_los_roles_son_los_de_la_matriz(self):
        self.assertIn('DONANTE = ADMIN + " or hasRole(\'DONANTE\') and "', self.acceso)
        self.assertIn('ENTIDAD = ADMIN + " or hasRole(\'ENTIDAD\') and "', self.acceso)
        self.assertIn("hasAnyRole('DONANTE', 'ENTIDAD') and ", self.acceso)

    def test_las_expresiones_de_dueno_llaman_a_metodos_que_existen_en_el_bean(self):
        for llamada in re.findall(r"@duenioDe\.(\w+)\(", self.acceso):
            self.assertRegex(self.duenio, r"public \w+ %s\(" % llamada)
        self.assertIn("@duenioDe.entrega(#id)", self.acceso)
        self.assertIn("@duenioDe.donacion(#idDonacion)", self.acceso)
        self.assertIn("@duenioDe.ruta(#id, principal)", self.acceso)

    def test_el_bean_de_dueno_existe_con_el_nombre_que_usa_el_spel(self):
        self.assertIn('@Component("duenioDe")', self.duenio)

    def test_un_recurso_inexistente_no_tiene_dueno_en_vez_de_dar_404(self):
        # Con 404 quien no es admin podría averiguar qué ids existen: con null recibe 403 igual que con uno ajeno.
        self.assertEqual(self.duenio.count(".orElse(null)"), 2)
        self.assertNotIn("orElseThrow", self.duenio)

    def test_la_ruta_es_de_quien_tiene_una_entrega_en_ella(self):
        self.assertRegex(self.duenio, r"public boolean ruta\(UUID id, Actor actor\)")
        self.assertIn("anyMatch(actor::puedeOperarSobre)", self.duenio)

    def test_los_servicios_exponen_las_consultas_de_dueno(self):
        entregas = leer(BASE, "services", "IEntregasService.java")
        self.assertRegex(entregas, r"Optional<UUID> beneficiariaDe\(UUID id\);")
        self.assertRegex(entregas, r"Optional<UUID> donanteDeDonacion\(UUID idDonacion\);")
        self.assertRegex(leer(BASE, "services", "IRutasService.java"), r"Set<UUID> organizacionesDe\(UUID id\);")

    def test_las_implementaciones_leen_lo_que_dice_la_interfaz(self):
        entregas = leer(BASE, "services", "impl", "EntregasService.java")
        self.assertIn("entregasRepository.findById(id).map(Entrega::getIdBeneficiaria)", entregas)
        self.assertIn("entregasRepository.findByIdDonacion(idDonacion).map(Entrega::getDonanteId)", entregas)
        rutas = leer(BASE, "services", "impl", "RutasService.java")
        self.assertIn("entregasRepository.findByRutaId(id)", rutas)
        self.assertIn("Stream.of(entrega.getIdBeneficiaria(), entrega.getDonanteId())", rutas)
        self.assertIn(".filter(Objects::nonNull)", rutas)

    def test_la_clase_no_se_puede_instanciar(self):
        self.assertIn("private Acceso() {}", self.acceso)


if __name__ == "__main__":
    unittest.main()
