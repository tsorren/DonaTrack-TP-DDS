#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de humo para report_test_failures.py (decide el resultado del pipeline)."""

import os
import sys
import shutil
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import report_test_failures as rtf

SUREFIRE_XML_OK = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="grupo5.donaciones.DonacionTest" tests="2" failures="0" errors="0" skipped="0" time="0.123">
    <testcase name="creaDonacionValida" classname="grupo5.donaciones.DonacionTest" time="0.05"/>
    <testcase name="rechazaDonacionInvalida" classname="grupo5.donaciones.DonacionTest" time="0.07"/>
</testsuite>
"""

SUREFIRE_XML_WITH_FAILURE = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="grupo5.donaciones.DonacionTest" tests="2" failures="1" errors="1" skipped="0" time="0.5">
    <testcase name="creaDonacionValida" classname="grupo5.donaciones.DonacionTest" time="0.1">
        <failure message="expected true" type="AssertionError">stacktrace aqui</failure>
    </testcase>
    <testcase name="rechazaDonacionInvalida" classname="grupo5.donaciones.DonacionTest" time="0.1">
        <error message="NPE" type="NullPointerException">stacktrace aqui</error>
    </testcase>
</testsuite>
"""


FAILSAFE_XML_CONTRACT_SKIPPED = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="grupo5.tests.contract.ContractIT" tests="2" failures="0" errors="0" skipped="1" time="0.2">
    <testcase name="testAllOpenApiSpecsLoadSuccessfully" classname="grupo5.tests.contract.ContractIT" time="0.1"/>
    <testcase name="testLogisticaEntregasContract" classname="grupo5.tests.contract.ContractIT" time="0.0">
        <skipped message="Assumption failed: logistica-service no disponible en http://localhost:8083"/>
    </testcase>
</testsuite>
"""

FAILSAFE_XML_LOGISTICA_FAILURE = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="grupo5.tests.integration.LogisticaIntegrationIT" tests="1" failures="1" errors="0" skipped="0" time="0.3">
    <testcase name="testCrearCamionYVerificarPersistencia" classname="grupo5.tests.integration.LogisticaIntegrationIT" time="0.3">
        <failure message="expected 201 but was 500" type="AssertionError">stacktrace aqui</failure>
    </testcase>
</testsuite>
"""

FAILSAFE_XML_E2E_FAILURE = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="grupo5.tests.e2e.FullDistributedDonationE2EIT" tests="1" failures="1" errors="0" skipped="0" time="0.3">
    <testcase name="testFlujoCompletoDistribuidoConLogisticaYRabbitMQ" classname="grupo5.tests.e2e.FullDistributedDonationE2EIT" time="0.3">
        <failure message="timeout" type="AssertionError">stacktrace aqui</failure>
    </testcase>
</testsuite>
"""


class ParseReportsInDirectoryTests(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def _write(self, name, content):
        path = os.path.join(self.tmp_dir, name)
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        return path

    def test_directorio_sin_reportes_da_totales_en_cero(self):
        data = rtf.parse_reports_in_directory(self.tmp_dir)
        self.assertEqual(data["total"], 0)
        self.assertEqual(data["files_count"], 0)

    def test_reporte_todo_exitoso_no_registra_fallos(self):
        self._write("TEST-DonacionTest.xml", SUREFIRE_XML_OK)
        data = rtf.parse_reports_in_directory(self.tmp_dir)
        self.assertEqual(data["total"], 2)
        self.assertEqual(data["failures"], 0)
        self.assertEqual(data["errors"], 0)
        self.assertEqual(data["failed_cases"], [])

    def test_reporte_con_falla_y_error_se_capturan_los_dos_casos(self):
        self._write("TEST-DonacionTest.xml", SUREFIRE_XML_WITH_FAILURE)
        data = rtf.parse_reports_in_directory(self.tmp_dir)
        self.assertEqual(data["total"], 2)
        self.assertEqual(data["failures"], 1)
        self.assertEqual(data["errors"], 1)
        self.assertEqual(len(data["failed_cases"]), 2)
        kinds = {fc["kind"] for fc in data["failed_cases"]}
        self.assertEqual(kinds, {"FAILURE", "ERROR"})

    def test_xml_corrupto_no_interrumpe_el_parseo(self):
        self._write("TEST-Corrupto.xml", "<testsuite tests=")
        data = rtf.parse_reports_in_directory(self.tmp_dir)
        self.assertEqual(data["total"], 0)
        self.assertEqual(data["files_count"], 1)


class MainExitCodeTests(unittest.TestCase):
    """Reproduce los 3 exit codes semánticos documentados en el módulo (0/1/2)."""

    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def _run_main(self, extra_args):
        argv_backup = sys.argv
        sys.argv = ["report_test_failures.py", "--dir", self.tmp_dir] + extra_args
        try:
            return rtf.main()
        finally:
            sys.argv = argv_backup

    def test_sin_tests_y_sin_fail_on_zero_devuelve_exito(self):
        self.assertEqual(self._run_main([]), 0)

    def test_sin_tests_y_con_fail_on_zero_devuelve_fallo(self):
        # Modo real usado en CI: .github/workflows/main.yml pasa --fail-on-zero-tests.
        self.assertEqual(self._run_main(["--fail-on-zero-tests"]), 1)

    def test_con_fallos_reales_devuelve_codigo_1(self):
        path = os.path.join(self.tmp_dir, "TEST-DonacionTest.xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(SUREFIRE_XML_WITH_FAILURE)
        self.assertEqual(self._run_main([]), 1)

    def test_con_todo_exitoso_devuelve_codigo_0(self):
        path = os.path.join(self.tmp_dir, "TEST-DonacionTest.xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(SUREFIRE_XML_OK)
        self.assertEqual(self._run_main([]), 0)

    def _write(self, content):
        with open(os.path.join(self.tmp_dir, "TEST-Caso.xml"), "w", encoding="utf-8") as f:
            f.write(content)

    def test_salteado_en_grupo_requerido_devuelve_codigo_1(self):
        # RM-02: un test salteado de un grupo requerido no puede dar verde.
        self._write(FAILSAFE_XML_CONTRACT_SKIPPED)
        self.assertEqual(self._run_main(["--required-groups", "smoke,contract,integration,e2e"]), 1)

    def test_salteado_fuera_de_los_grupos_requeridos_no_falla(self):
        self._write(FAILSAFE_XML_CONTRACT_SKIPPED)
        self.assertEqual(self._run_main(["--required-groups", "smoke"]), 0)
        self.assertEqual(self._run_main([]), 0)

    def test_falla_heredada_se_marca_y_sigue_fallando(self):
        # D5: la falla de un servicio que el PR no modificó se marca, pero no se perdona.
        self._write(FAILSAFE_XML_LOGISTICA_FAILURE)
        self.assertEqual(self._run_main(["--changed-services", "donaciones-service"]), 1)
        data = rtf.parse_all_directories([self.tmp_dir])
        rtf.mark_inherited(data["failed_cases"], ["donaciones-service"])
        self.assertTrue(data["failed_cases"][0]["inherited"])
        self.assertEqual(data["failed_cases"][0]["services"], ["logistica-service"])

    def test_falla_del_servicio_modificado_no_es_heredada(self):
        self._write(FAILSAFE_XML_LOGISTICA_FAILURE)
        data = rtf.parse_all_directories([self.tmp_dir])
        rtf.mark_inherited(data["failed_cases"], ["logistica-service"])
        self.assertFalse(data["failed_cases"][0]["inherited"])

    def test_falla_sin_servicio_reconocible_no_es_heredada(self):
        # Ante la duda (un E2E que cruza servicios), la falla es del PR.
        self._write(FAILSAFE_XML_E2E_FAILURE)
        data = rtf.parse_all_directories([self.tmp_dir])
        rtf.mark_inherited(data["failed_cases"], ["donaciones-service"])
        self.assertFalse(data["failed_cases"][0]["inherited"])


if __name__ == "__main__":
    unittest.main()
