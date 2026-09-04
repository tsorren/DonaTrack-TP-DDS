#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tests de humo para analyze_preprod_logs.py (decide el resultado del pipeline)."""

import io
import json
import os
import sys
import shutil
import tempfile
import unittest
from contextlib import redirect_stdout

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import analyze_preprod_logs as apl

PIPE_LINE_HTTP_IN = (
    "2026-09-03 12:00:00.100 | INFO  | donaciones-service | inst-1 | trace-abc | "
    "grupo5.common.logging.ControllerLoggingInterceptor | "
    "[CONTROLLER] [VERB: GET] [PATH: /api/donaciones] [PACKAGE: p] [CLASS: c] [METHOD: m] - Request received"
)

PIPE_LINE_SERVICE_ERROR = (
    "2026-09-03 12:00:00.200 | ERROR | donaciones-service | inst-1 | trace-abc | "
    "grupo5.common.logging.ServiceLoggingAspect | "
    "[SERVICE-ERROR] [PACKAGE: p] [CLASS: c] [METHOD: m] - Failed with exception: boom"
)


class ParseSingleLineTests(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()
        self.analyzer = apl.RunAnalyzer(self.tmp_dir, log_file=os.path.join(self.tmp_dir, "dummy.log"))

    def tearDown(self):
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def test_formato_clasico_pipe_se_parsea_correctamente(self):
        entry = self.analyzer._parse_single_line(PIPE_LINE_HTTP_IN, "app.log", 1)
        self.assertIsNotNone(entry)
        self.assertEqual(entry.level, "INFO")
        self.assertEqual(entry.app_name, "donaciones-service")
        self.assertEqual(entry.trace_id, "trace-abc")
        self.assertIn("[CONTROLLER]", entry.message)

    def test_ndjson_se_parsea_y_preserva_event_type(self):
        line = json.dumps({
            "@timestamp": "2026-09-03T12:00:00.123Z",
            "level": "INFO",
            "service": "donaciones-service",
            "instanceId": "inst-1",
            "traceId": "trace-xyz",
            "logger_name": "grupo5.common.logging.ControllerLoggingInterceptor",
            "message": "[CONTROLLER] request received",
            "eventType": "HTTP_IN",
        })
        entry = self.analyzer._parse_single_line(line, "app.log", 1)
        self.assertIsNotNone(entry)
        self.assertEqual(entry.event_type, "HTTP_IN")
        self.assertEqual(entry.trace_id, "trace-xyz")

    def test_linea_no_reconocible_devuelve_none(self):
        entry = self.analyzer._parse_single_line("esto no es un log valido", "app.log", 1)
        self.assertIsNone(entry)


class AnalyzeFlowsTests(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def _write_log(self, lines):
        path = os.path.join(self.tmp_dir, "docker-preprod-full.log")
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        return path

    def test_service_error_genera_anomalia_critica(self):
        log_path = self._write_log([PIPE_LINE_HTTP_IN, PIPE_LINE_SERVICE_ERROR])
        analyzer = apl.RunAnalyzer(self.tmp_dir, log_file=log_path)
        analyzer.load_and_parse()
        analyzer.analyze_flows()

        self.assertEqual(len(analyzer.controller_invocations), 1)
        self.assertEqual(len(analyzer.service_errors), 1)
        self.assertTrue(
            any(a["severity"] == "CRITICAL" and a["category"] == "SERVICE_FAILURE"
                for a in analyzer.detected_anomalies)
        )

    def test_sin_errores_no_hay_anomalias(self):
        log_path = self._write_log([PIPE_LINE_HTTP_IN])
        analyzer = apl.RunAnalyzer(self.tmp_dir, log_file=log_path)
        analyzer.load_and_parse()
        analyzer.analyze_flows()

        self.assertEqual(analyzer.detected_anomalies, [])


class MainExitCodeTests(unittest.TestCase):
    """Reproduce la invocación real de CI (.github/workflows/main.yml, sin --json)."""

    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()

    def tearDown(self):
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def _write_log(self, lines):
        path = os.path.join(self.tmp_dir, "docker-preprod-full.log")
        with open(path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        return path

    def _run_main(self, log_path, extra_args=None):
        argv_backup = sys.argv
        sys.argv = ["analyze_preprod_logs.py", "--file", log_path] + (extra_args or [])
        buf = io.StringIO()
        try:
            with redirect_stdout(buf):
                code = apl.main()
            return code, buf.getvalue()
        finally:
            sys.argv = argv_backup

    def test_corrida_sin_anomalias_devuelve_codigo_0(self):
        log_path = self._write_log([PIPE_LINE_HTTP_IN])
        code, _ = self._run_main(log_path)
        self.assertEqual(code, 0)

    def test_corrida_con_service_error_devuelve_codigo_1(self):
        log_path = self._write_log([PIPE_LINE_HTTP_IN, PIPE_LINE_SERVICE_ERROR])
        code, _ = self._run_main(log_path)
        self.assertEqual(code, 1)

    def test_archivo_inexistente_devuelve_codigo_2(self):
        code, _ = self._run_main(os.path.join(self.tmp_dir, "no-existe.log"))
        self.assertEqual(code, 2)

    def test_modo_json_no_refleja_anomalias_en_el_codigo_de_salida(self):
        # Caracteriza un comportamiento real y actual del script, no un ideal:
        # con --json, main() hace `return 0` incondicional (ver analyze_preprod_logs.py
        # rama `if args.json`) sin pasar por el chequeo de severidad CRITICAL que sí
        # aplica en el modo de salida humana. Hoy no afecta a CI porque el workflow
        # invoca el script sin --json (ver arriba). Si algún día se usa --json para
        # gatear un pipeline, este test debe actualizarse junto con el fix real.
        log_path = self._write_log([PIPE_LINE_HTTP_IN, PIPE_LINE_SERVICE_ERROR])
        code, output = self._run_main(log_path, extra_args=["--json"])
        parsed = json.loads(output)
        self.assertTrue(any(a["severity"] == "CRITICAL" for a in parsed["anomalies"]))
        self.assertEqual(code, 0)


if __name__ == "__main__":
    unittest.main()
