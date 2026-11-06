#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tema `donatrack` de las pantallas de login y registro (SPEC-06 A1-04).

Corre sin Docker (stdlib solamente). Lo que NO cubre —cómo se ve de verdad— se mira con las capturas
en 360 y 1280 px (ver auth-service/README.md). Que Keycloak sirva el tema y el CSS lo comprueba
`RealmTokenContractIT`.
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

PRIMITIVOS = ["--blanco", "--verde-700", "--verde-900", "--neutro-50", "--neutro-900", "--rojo-700"]
SEMANTICOS = [
    "--color-accion-primaria",
    "--color-accion-primaria-hover",
    "--color-texto-sobre-accion",
    "--color-fondo-pagina",
    "--color-enlace",
    "--color-texto",
    "--color-error",
]


def css_texto():
    return leer(*CSS.split(os.sep))


def tokens(css):
    """Todas las declaraciones `--nombre: valor;` del CSS."""
    return dict(re.findall(r"(--[\w-]+)\s*:\s*([^;]+);", css))


def resolver(css, nombre):
    """Valor final de un token, siguiendo las cadenas var(--otro)."""
    t = tokens(css)
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

    def test_existen_el_css_y_el_logo(self):
        for ruta in (CSS, LOGO):
            self.assertTrue(os.path.isfile(os.path.join(RAIZ, ruta)), "falta %s" % ruta)

    def test_el_logo_es_un_svg_valido_con_titulo(self):
        raiz = ET.parse(os.path.join(RAIZ, LOGO)).getroot()
        self.assertTrue(raiz.tag.endswith("}svg") or raiz.tag == "svg")
        self.assertTrue(raiz.get("viewBox"), "el SVG necesita viewBox para escalar a 360 px")
        self.assertIsNotNone(raiz.find("{http://www.w3.org/2000/svg}title"), "falta <title> (accesibilidad)")

    def test_el_logo_no_trae_scripts_ni_recursos_externos(self):
        texto = leer(*LOGO.split(os.sep))
        self.assertNotRegex(texto, r"(?i)<script|href\s*=\s*[\"']https?:|xlink:href|url\(\s*[\"']?https?:")


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
    """RQ-E6-08 / RNF-UI-09: WCAG AA, contraste mínimo de 4.5:1 para texto normal."""

    def setUp(self):
        self.css = css_texto()

    def valor(self, nombre):
        return resolver(self.css, nombre)

    def assertAA(self, primer_plano, fondo, minimo=4.5):
        ratio = contraste(primer_plano, fondo)
        self.assertGreaterEqual(ratio, minimo, "%s sobre %s: %.2f:1" % (primer_plano, fondo, ratio))

    def test_texto_del_boton_sobre_la_accion_primaria(self):
        self.assertAA(self.valor("--color-texto-sobre-accion"), self.valor("--color-accion-primaria"))
        self.assertAA(self.valor("--color-texto-sobre-accion"), self.valor("--color-accion-primaria-hover"))

    def test_enlaces_sobre_la_tarjeta_blanca_y_sobre_el_fondo(self):
        self.assertAA(self.valor("--color-enlace"), self.valor("--blanco"))
        self.assertAA(self.valor("--color-enlace"), self.valor("--color-fondo-pagina"))

    def test_texto_sobre_la_tarjeta_y_sobre_el_fondo(self):
        self.assertAA(self.valor("--color-texto"), self.valor("--blanco"))
        self.assertAA(self.valor("--color-texto"), self.valor("--color-fondo-pagina"))

    def test_error_sobre_la_tarjeta_blanca_y_sobre_el_fondo(self):
        self.assertAA(self.valor("--color-error"), self.valor("--blanco"))
        self.assertAA(self.valor("--color-error"), self.valor("--color-fondo-pagina"))


class CalculoDeContrasteTests(unittest.TestCase):
    def test_el_calculo_de_contraste_es_correcto(self):
        """Valores de referencia: negro/blanco = 21:1, gris #767676/blanco = 4.54:1."""
        self.assertAlmostEqual(contraste("#000000", "#ffffff"), 21.0, places=2)
        self.assertAlmostEqual(contraste("#767676", "#ffffff"), 4.54, places=2)


class EstilosDelTemaTests(unittest.TestCase):
    def setUp(self):
        self.css = css_texto()

    def test_los_tokens_gobiernan_a_patternfly(self):
        for pf, token in [
            ("--pf-v5-global--primary-color--100", "--color-accion-primaria"),
            ("--pf-v5-global--primary-color--200", "--color-accion-primaria-hover"),
            ("--pf-v5-global--link--Color", "--color-enlace"),
            ("--pf-v5-global--danger-color--100", "--color-error"),
        ]:
            self.assertRegex(self.css, r"%s\s*:\s*var\(%s\)" % (re.escape(pf), re.escape(token)))

    def test_el_logo_reemplaza_el_nombre_del_realm_sin_quitarlo_del_dom(self):
        """El texto sigue en el HTML (lectores de pantalla): se oculta con font-size, no con display:none."""
        regla = re.search(r"#kc-header-wrapper\s*\{([^}]*)\}", self.css)
        self.assertIsNotNone(regla, "falta la regla de #kc-header-wrapper")
        cuerpo = regla.group(1)
        self.assertIn("../img/logo.svg", cuerpo)
        self.assertRegex(cuerpo, r"font-size\s*:\s*0")
        self.assertNotIn("display: none", cuerpo)

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
    def test_el_it_verifica_que_keycloak_sirve_el_tema(self):
        texto = leer("auth-service", "src", "test", "java", "grupo5", "auth", "RealmTokenContractIT.java")
        for fragmento in ["/login/donatrack/css/donatrack.css", "lang=\\\"es\\\"", "img/logo.svg",
                          "--color-accion-primaria"]:
            self.assertIn(fragmento, texto)

    def test_el_readme_explica_como_ver_el_tema_y_sacar_las_capturas(self):
        texto = leer("auth-service", "README.md")
        for fragmento in ["360", "1280", "prompt=create", "down -v", "provisori"]:
            self.assertIn(fragmento, texto)


if __name__ == "__main__":
    unittest.main()
