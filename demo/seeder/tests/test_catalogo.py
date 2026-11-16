"""Cada bien de ejemplo se normaliza solo, con la misma comparación que Donaciones
(NormalizadorBasicoTexto + ComparadorTexto + NormalizadorSemanticoBien)."""

import re
import unicodedata
import unittest

from seeder import catalogo


def _palabras(texto: str) -> set[str]:
    t = unicodedata.normalize("NFD", texto.strip().lower())
    t = "".join(c for c in t if not unicodedata.combining(c))
    t = re.sub(r"[^a-z0-9\s]", "", t)
    return set(re.sub(r"\s+", " ", t).strip().split(" "))


def _confianza(descripcion: str, alias: str) -> float:
    del_alias = _palabras(alias)
    return len(_palabras(descripcion) & del_alias) / len(del_alias)


class CatalogoTest(unittest.TestCase):
    def test_hay_ocho_categorias_con_nombres_unicos(self):
        nombres = [c.nombre for c in catalogo.CATALOGO]
        self.assertEqual(8, len(set(nombres)))
        self.assertEqual(len(catalogo.todas_las_subcategorias()),
                         len(set(catalogo.todas_las_subcategorias())))

    def test_cada_bien_se_normaliza_solo_y_en_su_subcategoria(self):
        for categoria in catalogo.CATALOGO:
            for sub in categoria.subcategorias:
                for bien in sub.bienes:
                    with self.subTest(bien=bien.descripcion):
                        propia = max(_confianza(bien.descripcion, a) for a in sub.alias)
                        self.assertEqual(1.0, propia)
                        for otra in catalogo.CATALOGO:
                            for ajena in otra.subcategorias:
                                if ajena is sub:
                                    continue
                                for alias in ajena.alias:
                                    self.assertLess(_confianza(bien.descripcion, alias), 1.0,
                                                    f"también calza con {ajena.nombre}")

    def test_los_rangos_de_cantidad_son_validos(self):
        for categoria in catalogo.CATALOGO:
            for sub in categoria.subcategorias:
                for bien in sub.bienes:
                    self.assertTrue(0 < bien.minimo <= bien.maximo)
                    self.assertGreater(bien.peso_kg, 0)
                    self.assertGreater(bien.volumen_m3, 0)


if __name__ == "__main__":
    unittest.main()
