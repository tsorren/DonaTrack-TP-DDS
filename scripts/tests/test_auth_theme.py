#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tema `donatrack` de las pantallas de login y registro (SPEC-06 A1-04).

Corre sin Docker (stdlib solamente). Lo que NO cubre —cómo se ve de verdad— se mira con capturas
(ver auth-service/README.md): las primeras capturas mostraron el modo oscuro, el botón azul y los
enlaces ilegibles que estos tests no podían ver, y de ahí salen varios de los casos de abajo.
Que Keycloak sirva el tema y el CSS lo comprueba `RealmTokenContractIT`.
"""

import os
import re
import sys
import unittest
import xml.etree.ElementTree as ET

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from test_auth_keycloak_compose import RAIZ, leer  # noqa: E402
from test_auth_realm_contract import cargar_realm  # noqa: E402

TEMA = os.path.join("auth-service", "themes", "donatrack", "login")
CSS = os.path.join(TEMA, "resources", "css", "donatrack.css")
LOGO = os.path.join(TEMA, "resources", "img", "logo.svg")
LOGO_OSCURO = os.path.join(TEMA, "resources", "img", "logo-oscuro.svg")

CLASE_OSCURO = ".pf-v5-theme-dark"  # la que activa Keycloak (keycloak.v2) cuando el sistema está en oscuro
MODOS = [("claro", False), ("oscuro", True)]

PRIMITIVOS = ["--blanco", "--verde-300", "--verde-700", "--verde-900", "--neutro-50", "--neutro-800",
              "--neutro-900", "--neutro-950", "--rojo-300", "--rojo-700"]
SEMANTICOS = [
    "--color-accion-primaria",
    "--color-accion-primaria-hover",
    "--color-texto-sobre-accion",
    "--color-fondo-pagina",
    "--color-superficie",
    "--color-enlace",
    "--color-texto",
    "--color-error",
]
# Los que cambian en modo oscuro (los de la acción primaria se mantienen: verde con texto blanco).
SEMANTICOS_OSCURO = ["--color-fondo-pagina", "--color-superficie", "--color-texto", "--color-enlace",
                     "--color-error"]


def css_texto():
    return leer(*CSS.split(os.sep))


def bloques(css):
    """Tokens por bloque: ':root' (modo claro) y '.pf-v5-theme-dark' (oscuro)."""
    sin_comentarios = re.sub(r"/\*.*?\*/", "", css, flags=re.S)
    resultado = {}
    for selector, cuerpo in re.findall(r"([^{}]+)\{([^{}]*)\}", sin_comentarios):
        selector = selector.strip()
        if selector in (":root", CLASE_OSCURO):
            resultado.setdefault(selector, {}).update(dict(re.findall(r"(--[\w-]+)\s*:\s*([^;]+);", cuerpo)))
    return resultado


def tokens(css, oscuro=False):
    b = bloques(css)
    resultado = dict(b.get(":root", {}))
    if oscuro:
        resultado.update(b.get(CLASE_OSCURO, {}))
    return resultado


def resolver(css, nombre, oscuro=False):
    """Valor final de un token en un modo, siguiendo las cadenas var(--otro)."""
    t = tokens(css, oscuro)
    valor = t[nombre].strip()
    visitados = {nombre}
    while valor.startswith("var("):
        siguiente = re.match(r"var\((--[\w-]+)\)", valor).group(1)
        assert siguiente not in visitados, "ciclo de tokens en %s" % nombre
        visitados.add(siguiente)
        valor = t[siguiente].strip()
    return valor


def luminancia(hex_color):
    h = hex_color.lstrip("#")
    canales = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    lineales = [c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4 for c in canales]
    return 0.2126 * lineales[0] + 0.7152 * lineales[1] + 0.0722 * lineales[2]


def contraste(a, b):
    la, lb = sorted([luminancia(a), luminancia(b)], reverse=True)
    return (la + 0.05) / (lb + 0.05)


class ArchivosDelTemaTests(unittest.TestCase):
    def test_theme_properties_extiende_keycloak_v2_y_suma_el_css_propio(self):
        props = dict(
            l.split("=", 1) for l in leer(*TEMA.split(os.sep), "theme.properties").splitlines()
            if "=" in l and not l.lstrip().startswith("#")
        )
        self.assertEqual(props["parent"].strip(), "keycloak.v2")
        # El css propio va al final: gana a PatternFly y a styles.css del tema base.
        self.assertEqual(props["styles"].split(), ["css/styles.css", "css/donatrack.css"])

    def test_existen_el_css_y_los_dos_logos(self):
        for ruta in (CSS, LOGO, LOGO_OSCURO):
            self.assertTrue(os.path.isfile(os.path.join(RAIZ, ruta)), "falta %s" % ruta)

    def test_los_logos_son_svg_validos_con_titulo(self):
        for ruta in (LOGO, LOGO_OSCURO):
            raiz = ET.parse(os.path.join(RAIZ, ruta)).getroot()
            self.assertTrue(raiz.tag.endswith("}svg") or raiz.tag == "svg", ruta)
            self.assertTrue(raiz.get("viewBox"), "%s necesita viewBox para escalar a 360 px" % ruta)
            self.assertIsNotNone(raiz.find("{http://www.w3.org/2000/svg}title"), "%s: falta <title>" % ruta)

    def test_los_logos_no_traen_scripts_ni_recursos_externos(self):
        for ruta in (LOGO, LOGO_OSCURO):
            texto = leer(*ruta.split(os.sep))
            self.assertNotRegex(texto, r"(?i)<script|href\s*=\s*[\"']https?:|xlink:href|url\(\s*[\"']?https?:")

    def test_el_logo_oscuro_cambia_el_color_del_texto_y_mantiene_la_marca(self):
        """Con el texto oscuro del logo claro, en modo oscuro el nombre desaparecería sobre el fondo."""
        claro = leer(*LOGO.split(os.sep))
        oscuro = leer(*LOGO_OSCURO.split(os.sep))
        self.assertIn('fill="#1c2a1e"', claro)
        self.assertNotIn('fill="#1c2a1e"', oscuro)
        self.assertIn('fill="#f3f6f3"', oscuro)
        self.assertIn('fill="#2e7d32"', oscuro)  # la marca verde es la misma


class TokensTests(unittest.TestCase):
    """D20: dos niveles. Las pantallas usan solo los semánticos."""

    def setUp(self):
        self.css = css_texto()
        self.t = tokens(self.css)

    def test_estan_los_primitivos_y_los_semanticos(self):
        for nombre in PRIMITIVOS + SEMANTICOS:
            self.assertIn(nombre, self.t, "falta el token %s" % nombre)

    def test_los_semanticos_apuntan_a_primitivos(self):
        for nombre in SEMANTICOS:
            self.assertRegex(self.t[nombre].strip(), r"^var\(--(blanco|verde|neutro|rojo)[\w-]*\)$", nombre)

    def test_el_modo_oscuro_redefine_fondo_superficie_texto_enlace_y_error(self):
        oscuro = bloques(self.css).get(CLASE_OSCURO, {})
        for nombre in SEMANTICOS_OSCURO:
            self.assertIn(nombre, oscuro, "el modo oscuro no redefine %s" % nombre)
            self.assertRegex(oscuro[nombre].strip(), r"^var\(--(blanco|verde|neutro|rojo)[\w-]*\)$", nombre)

    def test_el_modo_oscuro_cambia_de_verdad_cada_color(self):
        for nombre in SEMANTICOS_OSCURO:
            self.assertNotEqual(resolver(self.css, nombre, False), resolver(self.css, nombre, True), nombre)

    def test_los_colores_literales_solo_viven_en_los_primitivos(self):
        for numero, linea in enumerate(self.css.splitlines(), 1):
            if re.search(r"#[0-9a-fA-F]{3,8}\b", linea) and not linea.lstrip().startswith("/*"):
                self.assertRegex(linea.strip(), r"^--(blanco|verde|neutro|rojo)[\w-]*\s*:",
                                 "color literal fuera de los primitivos, línea %d: %s" % (numero, linea.strip()))

    def test_las_reglas_usan_solo_tokens_semanticos(self):
        for numero, linea in enumerate(self.css.splitlines(), 1):
            if re.search(r"var\(--(blanco|verde|neutro|rojo)", linea):
                self.assertTrue(linea.lstrip().startswith("--color-"),
                                "usa un primitivo fuera de su definición semántica, línea %d" % numero)


class ContrasteWcagAATests(unittest.TestCase):
    """RQ-E6-08 / RNF-UI-09: WCAG AA en los dos modos. 4.5:1 para texto y 3:1 para componentes."""

    def setUp(self):
        self.css = css_texto()

    def valor(self, nombre, oscuro):
        return resolver(self.css, nombre, oscuro)

    def assertAA(self, primer_plano, fondo, minimo=4.5):
        ratio = contraste(primer_plano, fondo)
        self.assertGreaterEqual(ratio, minimo, "%s sobre %s: %.2f:1" % (primer_plano, fondo, ratio))

    def test_texto_del_boton_sobre_la_accion_primaria(self):
        for modo, oscuro in MODOS:
            with self.subTest(modo=modo):
                texto = self.valor("--color-texto-sobre-accion", oscuro)
                self.assertAA(texto, self.valor("--color-accion-primaria", oscuro))
                self.assertAA(texto, self.valor("--color-accion-primaria-hover", oscuro))

    def test_enlaces_sobre_la_tarjeta_y_sobre_el_fondo(self):
        for modo, oscuro in MODOS:
            with self.subTest(modo=modo):
                enlace = self.valor("--color-enlace", oscuro)
                self.assertAA(enlace, self.valor("--color-superficie", oscuro))
                self.assertAA(enlace, self.valor("--color-fondo-pagina", oscuro))

    def test_texto_sobre_la_tarjeta_y_sobre_el_fondo(self):
        for modo, oscuro in MODOS:
            with self.subTest(modo=modo):
                texto = self.valor("--color-texto", oscuro)
                self.assertAA(texto, self.valor("--color-superficie", oscuro))
                self.assertAA(texto, self.valor("--color-fondo-pagina", oscuro))

    def test_error_sobre_la_tarjeta_y_sobre_el_fondo(self):
        for modo, oscuro in MODOS:
            with self.subTest(modo=modo):
                error = self.valor("--color-error", oscuro)
                self.assertAA(error, self.valor("--color-superficie", oscuro))
                self.assertAA(error, self.valor("--color-fondo-pagina", oscuro))

    def test_el_boton_se_distingue_de_la_tarjeta(self):
        """Componente de interfaz: 3:1 contra lo que tiene alrededor (WCAG 1.4.11)."""
        for modo, oscuro in MODOS:
            with self.subTest(modo=modo):
                self.assertAA(self.valor("--color-accion-primaria", oscuro),
                              self.valor("--color-superficie", oscuro), minimo=3.0)

    def test_la_tarjeta_se_distingue_del_fondo_de_la_pagina_en_oscuro(self):
        self.assertNotEqual(self.valor("--color-superficie", True), self.valor("--color-fondo-pagina", True))


class CalculoDeContrasteTests(unittest.TestCase):
    def test_el_calculo_de_contraste_es_correcto(self):
        """Valores de referencia: negro/blanco = 21:1, gris #767676/blanco = 4.54:1."""
        self.assertAlmostEqual(contraste("#000000", "#ffffff"), 21.0, places=2)
        self.assertAlmostEqual(contraste("#767676", "#ffffff"), 4.54, places=2)


class EstilosDelTemaTests(unittest.TestCase):
    def setUp(self):
        self.css = css_texto()

    def regla(self, selector):
        sin_comentarios = re.sub(r"/\*.*?\*/", "", self.css, flags=re.S)
        m = re.search(r"(?m)^%s\s*\{([^}]*)\}" % re.escape(selector), sin_comentarios)
        self.assertIsNotNone(m, "falta la regla %s" % selector)
        return m.group(1)

    def test_los_tokens_gobiernan_a_patternfly(self):
        for pf, token in [
            ("--pf-v5-global--primary-color--100", "--color-accion-primaria"),
            ("--pf-v5-global--primary-color--200", "--color-accion-primaria-hover"),
            ("--pf-v5-global--link--Color", "--color-enlace"),
            ("--pf-v5-global--active-color--100", "--color-enlace"),
            ("--pf-v5-global--danger-color--100", "--color-error"),
        ]:
            self.assertRegex(self.css, r"%s\s*:\s*var\(%s\)" % (re.escape(pf), re.escape(token)))

    def test_el_boton_principal_tiene_regla_directa_y_no_depende_de_variables_de_patternfly(self):
        """En las primeras capturas el botón seguía azul: las variables globales no le llegaban."""
        reposo = self.regla(".pf-v5-c-button.pf-m-primary")
        self.assertIn("background-color: var(--color-accion-primaria)", reposo)
        self.assertIn("color: var(--color-texto-sobre-accion)", reposo)
        hover = self.regla(".pf-v5-c-button.pf-m-primary:hover,\n.pf-v5-c-button.pf-m-primary:focus,\n"
                           ".pf-v5-c-button.pf-m-primary:active")
        self.assertIn("background-color: var(--color-accion-primaria-hover)", hover)
        self.assertIn("color: var(--color-texto-sobre-accion)", hover)

    def test_la_tarjeta_tiene_superficie_propia_y_acento(self):
        cuerpo = self.regla(".pf-v5-c-login__main")
        self.assertIn("background-color: var(--color-superficie)", cuerpo)
        self.assertIn("color: var(--color-texto)", cuerpo)
        self.assertIn("border-top: 4px solid var(--color-accion-primaria)", cuerpo)

    def test_el_fondo_de_la_pagina_usa_el_token(self):
        cuerpo = self.regla("body#keycloak-bg,\n.pf-v5-c-login")
        self.assertIn("background-color: var(--color-fondo-pagina)", cuerpo)
        self.assertIn("background-image: none", cuerpo)

    def test_el_logo_reemplaza_el_nombre_del_realm_sin_quitarlo_del_dom(self):
        """El texto sigue en el HTML (lectores de pantalla): se oculta con font-size, no con display:none."""
        cuerpo = self.regla("#kc-header-wrapper")
        self.assertIn("../img/logo.svg", cuerpo)
        self.assertRegex(cuerpo, r"font-size\s*:\s*0")
        self.assertNotIn("display: none", cuerpo)

    def test_en_modo_oscuro_el_encabezado_usa_el_logo_oscuro(self):
        cuerpo = self.regla("%s #kc-header-wrapper" % CLASE_OSCURO)
        self.assertIn("../img/logo-oscuro.svg", cuerpo)

    def test_no_pisa_el_fondo_con_imagenes_externas(self):
        self.assertNotRegex(self.css, r"url\(\s*[\"']?https?:")
        self.assertNotIn("@import", self.css)


class RealmDelTemaTests(unittest.TestCase):
    def setUp(self):
        self.realm = cargar_realm()

    def test_el_realm_usa_el_tema_en_las_pantallas_de_login(self):
        self.assertEqual(self.realm.get("loginTheme"), "donatrack")

    def test_el_nombre_visible_es_donatrack(self):
        self.assertEqual(self.realm.get("displayName"), "DonaTrack")

    def test_espanol_por_defecto_y_unico_idioma(self):
        self.assertIs(self.realm.get("internationalizationEnabled"), True)
        self.assertEqual(self.realm.get("supportedLocales"), ["es"])
        self.assertEqual(self.realm.get("defaultLocale"), "es")


class DocumentacionYTestDeRuntimeTests(unittest.TestCase):
    def test_el_it_verifica_que_keycloak_sirve_el_tema_y_los_dos_logos(self):
        texto = leer("auth-service", "src", "test", "java", "grupo5", "auth", "RealmTokenContractIT.java")
        for fragmento in ["/login/donatrack/css/donatrack.css", "lang=\\\"es\\\"", "img/logo.svg",
                          "img/logo-oscuro.svg", "--color-accion-primaria"]:
            self.assertIn(fragmento, texto)

    def test_el_readme_explica_como_ver_el_tema_y_sacar_las_capturas(self):
        texto = leer("auth-service", "README.md")
        for fragmento in ["360", "1280", "prompt=create", "down -v", "provisori", "modo oscuro"]:
            self.assertIn(fragmento, texto)


if __name__ == "__main__":
    unittest.main()
