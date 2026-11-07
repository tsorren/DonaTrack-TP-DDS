#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de manifiesto_version.py (unidad de promoción y despliegue, RM-06)."""

import io
import json
import os
import shutil
import sys
import tempfile
import unittest
from contextlib import redirect_stdout

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import manifiesto_version as mv

D1 = "sha256:" + "a" * 64
D2 = "sha256:" + "b" * 64
REGISTRO = "ghcr.io/tsorren/donatrack-tp-dds"


class ManifiestoTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.digests = os.path.join(self.tmp, "digests")
        os.makedirs(self.digests)
        self.salida = os.path.join(self.tmp, "manifiesto-version.json")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def _digest(self, servicio, valor):
        with open(os.path.join(self.digests, f"{servicio}.digest"), "w", encoding="utf-8") as f:
            f.write(valor + "\n")

    def _generar(self):
        return mv.main(["generar", "--rama", "ENTREGA_5", "--commit", "abc1234", "--corrida", "42",
                        "--registro", REGISTRO, "--digests", self.digests, "-o", self.salida])

    def _leer(self, *extra):
        out = io.StringIO()
        with redirect_stdout(out):
            code = mv.main(["leer", self.salida, *extra])
        return code, out.getvalue().strip()

    def test_generar_y_leer_devuelve_referencias_por_digest(self):
        self._digest("logistica-service", D1)
        self._digest("donaciones-service", D2)
        self.assertEqual(self._generar(), 0)
        with open(self.salida, encoding="utf-8") as f:
            m = json.load(f)
        self.assertEqual(m["commit"], "abc1234")
        self.assertEqual(m["servicios"]["logistica-service"]["digest"], D1)
        code, out = self._leer("--servicio", "logistica-service")
        self.assertEqual(code, 0)
        self.assertEqual(out, f"{REGISTRO}/logistica-service@{D1}")
        code, out = self._leer()
        self.assertEqual(out.splitlines(), [
            f"donaciones-service {REGISTRO}/donaciones-service@{D2}",
            f"logistica-service {REGISTRO}/logistica-service@{D1}",
        ])

    def test_digest_invalido_no_genera_manifiesto(self):
        self._digest("logistica-service", "latest")
        self.assertEqual(self._generar(), 1)
        self.assertFalse(os.path.exists(self.salida))

    def test_sin_digests_falla(self):
        self.assertEqual(self._generar(), 1)

    def test_servicio_ausente_sale_con_1(self):
        self._digest("logistica-service", D1)
        self._generar()
        code, _ = self._leer("--servicio", "auth-service")
        self.assertEqual(code, 1)


if __name__ == "__main__":
    unittest.main()
