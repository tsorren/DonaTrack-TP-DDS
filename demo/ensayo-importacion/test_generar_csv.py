"""El generador del CSV de ensayo: reproducible, con la forma que el importador espera."""

import tempfile
import unittest
from collections import Counter
from pathlib import Path

import generar_csv as g

CABECERA = "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono"


def _filas(texto: str):
    """Las filas de datos como las lee Donaciones: una línea, columnas separadas por coma."""
    lineas = texto.lstrip("﻿").split("\n")
    assert lineas[0] == CABECERA
    return [l.split(",") for l in lineas[1:] if l]


class GeneradorTest(unittest.TestCase):
    def test_con_la_misma_semilla_sale_el_mismo_archivo(self):
        a = g.generar(filas=2000, duplicados=40, con_error=20, semilla=7)
        b = g.generar(filas=2000, duplicados=40, con_error=20, semilla=7)
        self.assertEqual(a.contenido, b.contenido)

    def test_con_otra_semilla_sale_otro_archivo(self):
        a = g.generar(filas=2000, duplicados=40, con_error=20, semilla=7)
        b = g.generar(filas=2000, duplicados=40, con_error=20, semilla=8)
        self.assertNotEqual(a.contenido, b.contenido)

    def test_por_defecto_son_veinte_mil_filas_de_datos(self):
        r = g.generar()
        self.assertEqual(20000, len(_filas(r.contenido)))
        self.assertEqual(20000, r.filas)

    def test_la_cabecera_es_la_que_lee_el_importador(self):
        r = g.generar(filas=500, duplicados=5, con_error=5)
        self.assertTrue(r.contenido.startswith("﻿" + CABECERA + "\n"))

    def test_sin_bom_la_cabecera_va_al_principio(self):
        r = g.generar(filas=500, duplicados=5, con_error=5, con_bom=False)
        self.assertTrue(r.contenido.startswith(CABECERA + "\n"))

    def test_los_esperados_suman_las_filas(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        self.assertEqual(30, r.con_error)
        self.assertEqual(60, r.actualizadas)
        self.assertEqual(3000 - 60 - 30, r.creadas)
        self.assertEqual(r.filas, r.creadas + r.actualizadas + r.con_error)

    def test_las_filas_validas_tienen_seis_columnas_y_las_de_error_de_formato_no(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        columnas = Counter(len(f) for f in _filas(r.contenido))
        self.assertEqual(r.filas - r.con_error_de_formato, columnas[6])
        self.assertEqual(r.con_error_de_formato, columnas[5] + columnas[7])
        self.assertGreater(r.con_error_de_formato, 0)

    def test_hay_errores_de_las_dos_clases(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        self.assertGreater(r.con_error - r.con_error_de_formato, 0)  # correo de otra persona

    def test_los_duplicados_repiten_documento_y_correo_de_una_fila_anterior(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        vistos = {}
        repetidos = 0
        for f in _filas(r.contenido):
            if len(f) != 6:
                continue
            if f[2] in vistos:
                repetidos += 1
                self.assertEqual(vistos[f[2]][4], f[4])  # mismo correo
            else:
                vistos[f[2]] = f
        self.assertEqual(60, repetidos)

    def test_los_correos_de_otra_persona_chocan_con_un_correo_anterior_y_otro_documento(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        por_correo = {}
        conflictos = 0
        for f in _filas(r.contenido):
            if len(f) != 6:
                continue
            previo = por_correo.get(f[4])
            if previo is None:
                por_correo[f[4]] = f
            elif previo[2] != f[2]:
                conflictos += 1
        self.assertEqual(r.con_error - r.con_error_de_formato, conflictos)

    def test_ninguna_fila_usa_la_serie_de_documentos_del_seeder(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        for f in _filas(r.contenido):
            if len(f) == 6:
                self.assertFalse(f[2].startswith("90"), f[2])
                self.assertTrue(f[4].endswith("@ensayo.donatrack.test"), f[4])

    def test_hay_personas_humanas_y_juridicas(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        tipos = Counter(f[0] for f in _filas(r.contenido))
        self.assertGreater(tipos["HUMANA"], tipos["JURIDICA"])
        self.assertGreater(tipos["JURIDICA"], 0)

    def test_los_valores_no_llevan_comas_ni_saltos_de_linea(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        for f in _filas(r.contenido):
            for valor in f:
                self.assertNotIn("\n", valor)
                self.assertNotIn("\r", valor)

    def test_no_hay_filas_vacias_ni_la_ultima_columna_vacia(self):
        # Donaciones separa con split(","): una columna final vacía se pierde y la fila se rechazaría
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        for linea in r.contenido.lstrip("﻿").split("\n")[1:-1]:
            self.assertTrue(linea)
            self.assertFalse(linea.endswith(","), linea)

    def test_los_documentos_nuevos_no_se_repiten(self):
        r = g.generar(filas=3000, duplicados=60, con_error=30)
        nuevos = [f[2] for f in _filas(r.contenido) if len(f) == 6]
        # los 60 duplicados repiten documento a propósito; los conflictos traen documento propio
        self.assertEqual(len(nuevos) - 60, len(set(nuevos)))

    def test_otra_serie_cambia_los_documentos_y_los_correos(self):
        a = g.generar(filas=500, duplicados=5, con_error=5, serie=88)
        b = g.generar(filas=500, duplicados=5, con_error=5, serie=77)

        def docs(r):
            return {f[2] for f in _filas(r.contenido) if len(f) == 6}

        def correos(r):
            return {f[4] for f in _filas(r.contenido) if len(f) == 6}

        self.assertFalse(docs(a) & docs(b))
        self.assertFalse(correos(a) & correos(b))
        self.assertTrue(all(d.startswith(("77", "3077")) for d in docs(b)))

    def test_la_serie_del_seeder_y_las_fuera_de_rango_se_rechazan(self):
        for serie in (90, 9, 100):
            with self.assertRaises(ValueError):
                g.generar(filas=100, duplicados=1, con_error=1, serie=serie)

    def test_pide_valores_que_tengan_sentido(self):
        with self.assertRaises(ValueError):
            g.generar(filas=100, duplicados=80, con_error=40)
        with self.assertRaises(ValueError):
            g.generar(filas=0)
        with self.assertRaises(ValueError):
            g.generar(filas=100, duplicados=-1)

    def test_escribe_el_archivo_en_utf8(self):
        r = g.generar(filas=500, duplicados=5, con_error=5)
        with tempfile.TemporaryDirectory() as d:
            destino = Path(d) / "ensayo.csv"
            g.escribir(r, destino)
            crudo = destino.read_bytes()
        self.assertTrue(crudo.startswith(b"\xef\xbb\xbf"))
        self.assertEqual(r.contenido.encode("utf-8"), crudo)


if __name__ == "__main__":
    unittest.main()
