#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests del tema de Keycloak con la identidad Raíces (SPEC-06 A1-04 y E2-09, D20).

Corren sin Java, Docker, Keycloak ni navegador (stdlib). Fijan lo que se decidió: el tema es una copia
sincronizada de `tokens.css` del sitio (D20), no tiene modo oscuro, usa solo tokens de uso, cumple WCAG AA en cada
par de colores que usa y cubre los componentes de PatternFly que muestra Keycloak.

Que se vea bien lo comprobó Firefox 160 sobre un Keycloak 26.8.1 real: capturas de 17 pantallas, colores y
tipografías calculados por el navegador y sin desborde horizontal a 360 y 320 px. Que Keycloak sirva los archivos
lo prueba `RealmTokenContractIT`.
"""

import json
import os
import re
import sys
import unittest

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
sys.path.insert(0, os.path.join(RAIZ, "scripts"))
import sincronizar_tema  # noqa: E402

TEMA = os.path.join(RAIZ, "auth-service", "themes", "donatrack", "login")
RECURSOS = os.path.join(TEMA, "resources")
IT = ("auth-service", "src", "test", "java", "grupo5", "auth", "RealmTokenContractIT.java")
FUENTES = ("archivo-latin.woff2", "archivo-cursiva-latin.woff2", "dm-sans-latin.woff2")


def leer(*partes):
    with open(os.path.join(RAIZ, *partes), encoding="utf-8", errors="replace") as f:
        return f.read()


def css_propio():
    return leer("auth-service", "themes", "donatrack", "login", "resources", "css", "donatrack.css")


def tokens_css():
    return leer("auth-service", "themes", "donatrack", "login", "resources", "css", "tokens.css")


def declaraciones():
    return {m.group(1): m.group(2).strip() for m in re.finditer(r"(--[\w-]+)\s*:\s*([^;]+);", tokens_css())}


def resolver(nombre):
    valor = declaraciones()[nombre]
    m = re.fullmatch(r"var\((--[\w-]+)\)", valor)
    return resolver(m.group(1)) if m else valor


def luminancia(color):
    color = color.lstrip("#")
    r, g, b = (int(color[i:i + 2], 16) / 255 for i in (0, 2, 4))

    def canal(x):
        return x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4

    return 0.2126 * canal(r) + 0.7152 * canal(g) + 0.0722 * canal(b)


def contraste(a, b):
    alto, bajo = sorted((luminancia(a), luminancia(b)), reverse=True)
    return (alto + 0.05) / (bajo + 0.05)


def sin_comentarios(css):
    return re.sub(r"/\*.*?\*/", "", css, flags=re.S)


def reglas(css):
    """[(lista de selectores, cuerpo)] de cada regla, sin comentarios y con las de dentro de un @media."""
    resultado = []
    for m in re.finditer(r"([^{}]+)\{([^{}]*)\}", sin_comentarios(css)):
        selectores = [" ".join(x.split()) for x in m.group(1).split(",")]
        resultado.append((selectores, m.group(2)))
    return resultado


def regla(css, selector):
    """El cuerpo de la primera regla que tiene exactamente ese selector en su lista."""
    for selectores, cuerpo in reglas(css):
        if selector in selectores:
            return cuerpo
    return None


def regla_con(css, fragmento):
    """El cuerpo de la primera regla con un selector que contiene ese fragmento."""
    for selectores, cuerpo in reglas(css):
        if any(fragmento in x for x in selectores):
            return cuerpo
    return None


class ArchivosDelTemaTests(unittest.TestCase):
    def test_theme_properties_extiende_keycloak_v2_sin_modo_oscuro_y_carga_los_tokens_antes_del_css_propio(self):
        props = leer("auth-service", "themes", "donatrack", "login", "theme.properties")
        self.assertRegex(props, r"(?m)^parent=keycloak\.v2$")
        self.assertRegex(props, r"(?m)^darkMode=false$")
        self.assertRegex(props, r"(?m)^styles=css/styles\.css css/tokens\.css css/donatrack\.css$")

    def test_estan_todos_los_archivos(self):
        for ruta in ("css/tokens.css", "css/donatrack.css", "img/logo.svg", "fuentes/OFL-archivo.txt", "fuentes/OFL-dm-sans.txt") + tuple("fuentes/" + f for f in FUENTES):
            self.assertTrue(os.path.isfile(os.path.join(RECURSOS, ruta)), ruta)
        self.assertTrue(os.path.isfile(os.path.join(TEMA, "messages", "messages_es.properties")))

    def test_no_queda_el_logo_oscuro_del_tema_provisorio(self):
        self.assertFalse(os.path.exists(os.path.join(RECURSOS, "img", "logo-oscuro.svg")))

    def test_el_logo_es_un_svg_valido_sin_scripts_ni_recursos_externos(self):
        svg = leer("auth-service", "themes", "donatrack", "login", "resources", "img", "logo.svg")
        self.assertIn("<svg", svg)
        self.assertIn("viewBox", svg)
        self.assertNotRegex(svg, r"(?i)<script|href=\"https?:|url\(https?:|<image")

    def test_las_fuentes_son_woff2_de_verdad(self):
        # Una copia que Git o un editor tocó (saltos de línea) deja de abrirse: la cabecera lo delata.
        for f in FUENTES:
            ruta = os.path.join(RECURSOS, "fuentes", f)
            with open(ruta, "rb") as h:
                self.assertEqual(h.read(4), b"wOF2", f)
            self.assertGreater(os.path.getsize(ruta), 10_000, f)

    def test_git_trata_las_fuentes_como_binarios(self):
        self.assertRegex(leer(".gitattributes"), r"(?m)^\*\.woff2 binary$")


class SincronizacionConElSitioTests(unittest.TestCase):
    """D20: tokens.css del sitio es la fuente de verdad. La copia del tema no puede quedar vieja."""

    def test_el_tema_es_identico_al_sitio(self):
        self.assertEqual(sincronizar_tema.distintos(), [], "volvé a copiar: python scripts/sincronizar_tema.py")

    def test_se_sincronizan_los_tokens_las_tres_fuentes_sus_licencias_y_el_logo(self):
        destinos = [d for _, d in sincronizar_tema.ARCHIVOS]
        self.assertEqual(destinos[0], "css/tokens.css")
        for f in FUENTES:
            self.assertIn("fuentes/" + f, destinos)
        self.assertIn("img/logo.svg", destinos)
        self.assertEqual(sum(1 for d in destinos if d.endswith(".txt")), 2)
        for origen, _ in sincronizar_tema.ARCHIVOS:
            self.assertTrue(os.path.isfile(os.path.join(sincronizar_tema.ORIGEN, origen)), origen)

    def test_las_subcarpetas_son_las_del_sitio_asi_las_rutas_de_los_font_face_valen_igual(self):
        for origen, destino in sincronizar_tema.ARCHIVOS:
            self.assertEqual(origen, destino)

    def test_verificar_sale_con_cero_si_esta_al_dia(self):
        self.assertEqual(sincronizar_tema.main(["--verificar"]), 0)

    def test_verificar_detecta_una_copia_vieja_y_no_toca_nada(self):
        ruta = os.path.join(sincronizar_tema.DESTINO, "css", "tokens.css")
        with open(ruta, "rb") as f:
            original = f.read()
        try:
            with open(ruta, "wb") as f:
                f.write(original + b"\n/* copia vieja */\n")
            self.assertEqual(sincronizar_tema.distintos(), ["css/tokens.css"])
            self.assertEqual(sincronizar_tema.main(["--verificar"]), 1)
            with open(ruta, "rb") as f:
                self.assertIn(b"copia vieja", f.read(), "--verificar no debe copiar")
        finally:
            with open(ruta, "wb") as f:
                f.write(original)
        self.assertEqual(sincronizar_tema.distintos(), [])

    def test_los_font_face_apuntan_a_fuentes_que_existen_en_el_tema(self):
        urls = re.findall(r'src:\s*url\("([^"]+)"\)', tokens_css())
        self.assertEqual(len(urls), 3)
        for url in urls:
            self.assertTrue(os.path.isfile(os.path.normpath(os.path.join(RECURSOS, "css", url))), url)


class TokensTests(unittest.TestCase):
    """D20: las pantallas usan solo los tokens de uso, nunca los valores base."""

    def test_no_usa_los_valores_base(self):
        self.assertNotIn("--p-", sin_comentarios(css_propio()))

    def test_toda_variable_que_usa_esta_definida_en_tokens_css_o_es_de_patternfly(self):
        definidas = set(declaraciones())
        usadas = set(re.findall(r"var\((--[\w-]+)", css_propio()))
        self.assertTrue(usadas)
        for variable in usadas:
            self.assertTrue(variable in definidas or variable.startswith("--pf-"), variable)

    def test_no_escribe_colores_a_mano(self):
        css = sin_comentarios(css_propio())
        self.assertNotRegex(css, r"#[0-9a-fA-F]{3,8}\b")
        self.assertNotRegex(css, r"\b(?:rgb|rgba|hsl|hsla)\(")

    def test_las_variables_de_patternfly_se_apuntan_a_tokens(self):
        cuerpo = regla(css_propio(), ":root")
        for variable in ("--pf-v5-global--FontFamily--text", "--pf-v5-global--FontFamily--heading", "--pf-v5-global--primary-color--100",
                         "--pf-v5-global--link--Color", "--pf-v5-global--danger-color--100"):
            self.assertRegex(cuerpo, re.escape(variable) + r":\s*var\(--(?:color|fuente)-")

    def test_estan_los_tokens_de_uso_que_el_tema_necesita(self):
        for variable in ("--color-fondo-pagina", "--color-superficie", "--color-texto", "--color-titulo", "--color-enlace", "--color-campo",
                         "--color-borde-control", "--color-foco", "--color-accion-primaria", "--color-accion-primaria-activa",
                         "--color-texto-sobre-accion", "--color-sombra", "--radio-lamina", "--radio-boton", "--sombra-papel",
                         "--fuente-titulo", "--fuente-texto"):
            self.assertIn(variable, declaraciones())


class ContrasteWcagAATests(unittest.TestCase):
    """RNF-UI-09: cada par de colores que usa el tema, calculado a partir de tokens.css."""

    TEXTO = 4.5
    NO_TEXTO = 3.0
    PARES = (
        ("--color-texto", "--color-superficie", TEXTO), ("--color-texto", "--color-fondo-pagina", TEXTO), ("--color-texto", "--color-campo", TEXTO),
        ("--color-texto-suave", "--color-superficie", TEXTO), ("--color-texto-suave", "--color-fondo-pagina", TEXTO),
        ("--color-titulo", "--color-superficie", TEXTO), ("--color-enlace", "--color-superficie", TEXTO),
        ("--color-enlace", "--color-fondo-pagina", TEXTO), ("--color-acento-hondo", "--color-superficie", TEXTO),
        ("--color-texto-sobre-accion", "--color-accion-primaria", TEXTO), ("--color-texto-sobre-accion", "--color-accion-primaria-activa", TEXTO),
        ("--color-tinta", "--color-papel", TEXTO), ("--color-tinta", "--color-acento-claro", TEXTO),
        ("--color-error-texto", "--color-error-fondo", TEXTO), ("--color-exito-texto", "--color-exito-fondo", TEXTO),
        ("--color-info-texto", "--color-info-fondo", TEXTO), ("--color-advertencia-texto", "--color-advertencia-fondo", TEXTO),
        ("--color-error-texto", "--color-superficie", TEXTO), ("--color-error-texto", "--color-campo", TEXTO),
        ("--color-foco", "--color-superficie", NO_TEXTO), ("--color-foco", "--color-fondo-pagina", NO_TEXTO),
        ("--color-borde-control", "--color-campo", NO_TEXTO),
    )

    def test_cada_par_cumple_el_minimo(self):
        for primero, segundo, minimo in self.PARES:
            with self.subTest(par="%s sobre %s" % (primero, segundo)):
                self.assertGreaterEqual(contraste(resolver(primero), resolver(segundo)), minimo, "%s %s" % (resolver(primero), resolver(segundo)))

    def test_los_pares_que_se_calculan_son_los_que_usa_el_css(self):
        # Si el CSS empieza a usar un token de color que ningún par cubre, este test lo avisa.
        usados = {v for v in re.findall(r"var\((--color-[\w-]+)", css_propio())}
        cubiertos = {t for par in self.PARES for t in par[:2]}
        cubiertos |= {"--color-fondo-pagina", "--color-sombra", "--color-filete", "--color-papel", "--color-acento", "--color-exito-fondo",
                      "--color-exito-texto", "--color-info-fondo", "--color-info-texto", "--color-advertencia-fondo", "--color-advertencia-texto"}
        self.assertEqual(sorted(usados - cubiertos), [], "tokens de color usados sin un par de contraste")

    def test_el_calculo_de_contraste_es_correcto(self):
        self.assertAlmostEqual(contraste("#000000", "#ffffff"), 21.0, places=1)
        self.assertAlmostEqual(contraste("#ffffff", "#ffffff"), 1.0, places=2)
        self.assertAlmostEqual(contraste("#767676", "#ffffff"), 4.54, places=1)


class EstilosDelTemaTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.css = css_propio()

    def test_el_tema_declara_que_es_claro(self):
        self.assertIn("color-scheme: light;", regla(self.css, ":root"))

    def test_la_marca_gana_al_blanco_con_important_del_tema_base_y_va_centrada(self):
        cuerpo = regla(self.css, "#kc-header-wrapper")
        self.assertIn("color: var(--color-tinta) !important;", cuerpo)
        self.assertIn("text-align: center;", cuerpo)
        self.assertIn("display: block;", cuerpo)

    def test_la_marca_lleva_el_icono_del_sitio_y_track_en_el_color_de_acento(self):
        self.assertIn('background: url("../img/logo.svg")', regla(self.css, "#kc-header-wrapper::before"))
        self.assertIn("color: var(--color-acento);", regla(self.css, "#kc-header-wrapper em"))

    def test_se_saca_la_franja_de_color_del_tema_base(self):
        self.assertIn("border-top: 0;", regla(self.css, ".pf-v5-c-login__main-header"))

    def test_la_tarjeta_tiene_el_papel_las_esquinas_y_la_sombra_del_sitio(self):
        cuerpo = regla(self.css, ".pf-v5-c-login__main")
        for dato in ("background-color: var(--color-superficie);", "border-radius: var(--radio-lamina);", "box-shadow: var(--sombra-papel);"):
            self.assertIn(dato, cuerpo)

    def test_los_titulos_usan_la_letra_de_los_titulos_y_el_color_de_titulo(self):
        cuerpo = regla(self.css, "#kc-page-title")
        self.assertIn("font-family: var(--fuente-titulo);", cuerpo)
        self.assertIn("color: var(--color-titulo);", cuerpo)

    def test_el_cuerpo_usa_la_letra_del_texto(self):
        self.assertIn("font-family: var(--fuente-texto);", regla(self.css, "html"))

    def test_el_boton_principal_es_verde_con_la_sombra_plana_de_hoja(self):
        cuerpo = regla(self.css, ".pf-v5-c-button.pf-m-primary")
        for dato in ("background-color: var(--color-accion-primaria);", "color: var(--color-texto-sobre-accion);", "box-shadow: 0.25rem 0.25rem 0 var(--color-sombra);"):
            self.assertIn(dato, cuerpo)

    def test_los_demas_botones_van_en_contorno_de_tinta(self):
        cuerpo = regla(self.css, ".pf-v5-c-button.pf-m-secondary")
        self.assertIn("box-shadow: inset 0 0 0 2px var(--color-tinta);", cuerpo)
        self.assertIn(".pf-v5-c-button.pf-m-tertiary", self.css)

    def test_los_botones_miden_48_px_y_tienen_las_esquinas_del_sitio(self):
        cuerpo = regla(self.css, ".pf-v5-c-button")
        self.assertIn("min-height: 3rem;", cuerpo)
        self.assertIn("border-radius: var(--radio-boton);", cuerpo)

    def test_los_campos_se_distinguen_del_papel_y_al_enfocar_tienen_borde_y_anillo(self):
        campo = regla(self.css, ".pf-v5-c-form-control")
        for dato in ("background-color: var(--color-campo);", "border: 1.5px solid var(--color-borde-control);", "border-radius: var(--radio-boton);"):
            self.assertIn(dato, campo)
        foco = regla(self.css, ".pf-v5-c-form-control:focus-within")
        self.assertIn("border-color: var(--color-acento);", foco)
        self.assertIn("outline: 3px solid var(--color-foco);", foco)

    def test_un_campo_con_error_se_marca_con_el_color_del_error(self):
        self.assertIn("border-color: var(--color-error-texto);", regla_con(self.css, ':has(input[aria-invalid="true"])'))
        self.assertIn("font-weight: 700;", regla(self.css, ".pf-v5-c-helper-text__item.pf-m-error"))

    def test_hay_un_anillo_de_foco_visible_para_todo(self):
        self.assertIn("outline: 3px solid var(--color-foco);", regla(self.css, ":focus-visible"))

    def test_los_cuatro_tipos_de_aviso_usan_los_colores_de_estado_del_sitio(self):
        for tipo, token in (("danger", "error"), ("success", "exito"), ("info", "info"), ("warning", "advertencia")):
            cuerpo = regla(self.css, ".pf-v5-c-alert.pf-m-%s" % tipo)
            self.assertIn("background-color: var(--color-%s-fondo);" % token, cuerpo, tipo)
            self.assertIn("color: var(--color-%s-texto);" % token, cuerpo, tipo)

    def test_la_banda_de_abajo_y_el_separador_no_traen_el_gris_de_patternfly(self):
        self.assertIn("background-color: transparent;", regla(self.css, ".pf-v5-c-login__main-footer-band"))
        self.assertIn("var(--color-texto-suave)", regla(self.css, ".kc-login-divider"))

    def test_en_el_celular_la_tarjeta_tiene_un_margen_lateral(self):
        self.assertRegex(self.css, r"@media \(max-width: 36rem\)\s*\{\s*\.pf-v5-c-login__container\s*\{\s*padding-inline: 0\.75rem;")

    def test_no_pisa_el_fondo_con_imagenes_externas(self):
        self.assertNotRegex(self.css, r"url\(\s*[\"']?https?:")
        self.assertIn("background-image: none;", regla(self.css, "body#keycloak-bg"))


class RealmDelTemaTests(unittest.TestCase):
    def setUp(self):
        self.realm = json.loads(leer("auth-service", "realm", "donatrack-realm.json"))

    def test_el_realm_usa_el_tema_en_las_pantallas_de_login(self):
        self.assertEqual(self.realm.get("loginTheme"), "donatrack")

    def test_el_nombre_visible_es_donatrack_y_track_va_resaltado(self):
        self.assertEqual(self.realm.get("displayName"), "DonaTrack")
        self.assertEqual(self.realm.get("displayNameHtml"), "Dona<em>Track</em>")

    def test_el_css_da_color_al_em_de_la_marca(self):
        self.assertIn("#kc-header-wrapper em", css_propio())

    def test_espanol_por_defecto_y_unico_idioma(self):
        self.assertIs(self.realm.get("internationalizationEnabled"), True)
        self.assertEqual(self.realm.get("supportedLocales"), ["es"])
        self.assertEqual(self.realm.get("defaultLocale"), "es")


class DocumentacionYTestDeRuntimeTests(unittest.TestCase):
    def test_el_it_verifica_que_keycloak_sirve_las_hojas_las_fuentes_y_el_logo(self):
        texto = leer(*IT)
        for fragmento in ("/login/donatrack/css/donatrack.css", "/login/donatrack/css/tokens.css", 'lang=\\"es\\"', "img/logo.svg",
                          "archivo-latin.woff2", "archivo-cursiva-latin.woff2", "dm-sans-latin.woff2", "var(--color-accion-primaria)",
                          "Dona<em>Track</em>", "pf-v5-theme-dark"):
            self.assertIn(fragmento, texto)
        self.assertNotIn("logo-oscuro", texto)

    def test_el_readme_explica_como_mantener_y_ver_el_tema(self):
        texto = leer("auth-service", "README.md")
        for fragmento in ("360", "1280", "prompt=create", "down -v", "scripts/sincronizar_tema.py", "darkMode=false", "17 pantallas",
                          "Raíces", "Sin modo oscuro"):
            self.assertIn(fragmento, texto)
        self.assertNotIn("logo-oscuro", texto)
        self.assertNotIn("Provisorio", texto)

    def test_el_dockerfile_copia_el_tema_entero(self):
        self.assertIn("COPY themes/donatrack /opt/keycloak/themes/donatrack", leer("auth-service", "Dockerfile"))

    def test_el_contexto_de_build_no_deja_afuera_ni_el_tema_ni_las_fuentes(self):
        patrones = [l.strip() for l in leer("auth-service", ".dockerignore").splitlines() if l.strip()]
        for ruta in ("themes/donatrack/login/resources/fuentes/archivo-latin.woff2", "themes/donatrack/login/resources/css/tokens.css"):
            self.assertFalse(any(ruta == p or ruta.startswith(p + "/") for p in patrones), ruta)


if __name__ == "__main__":
    unittest.main()
