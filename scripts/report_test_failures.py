#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
report_test_failures.py — Reporteador Detallado de Fallos de Tests (DonaTrack)
=============================================================================
Analiza reportes XML de Maven (Surefire y Failsafe) y genera un diagnóstico
estructurado tanto en consola como en GitHub Step Summary ($GITHUB_STEP_SUMMARY).

Códigos de salida semánticos:
  0: Éxito (tests ejecutados > 0, 0 fallos, 0 errores).
  1: Fallo funcional (fallos > 0, errores > 0, o 0 tests con --fail-on-zero-tests).
  2: Fallo de infraestructura / tooling (excepciones no controladas).
"""

import sys
import os
import glob
import argparse
import xml.etree.ElementTree as ET
from datetime import datetime

# Asegurar codificación UTF-8 en salida
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


class Colors:
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    BOLD = '\033[1m'
    RESET = '\033[0m'

    @classmethod
    def disable(cls):
        cls.RED = ''
        cls.GREEN = ''
        cls.YELLOW = ''
        cls.BLUE = ''
        cls.CYAN = ''
        cls.BOLD = ''
        cls.RESET = ''


if not sys.stdout.isatty():
    Colors.disable()


def parse_reports_in_directory(reports_dir):
    """Parsea todos los archivos TEST-*.xml dentro de un directorio dado."""
    xml_files = glob.glob(os.path.join(reports_dir, "TEST-*.xml"))
    total_tests = 0
    total_failures = 0
    total_errors = 0
    total_skipped = 0
    total_time = 0.0
    failed_cases = []

    for xml_path in xml_files:
        try:
            tree = ET.parse(xml_path)
            root = tree.getroot()

            suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
            for suite in suites:
                total_tests += int(suite.attrib.get("tests", 0))
                total_failures += int(suite.attrib.get("failures", 0))
                total_errors += int(suite.attrib.get("errors", 0))
                total_skipped += int(suite.attrib.get("skipped", 0))
                total_time += float(suite.attrib.get("time", 0.0))

                suite_name = suite.attrib.get("name", "UnknownSuite")

                for testcase in suite.findall("testcase"):
                    name = testcase.attrib.get("name", "unknown")
                    classname = testcase.attrib.get("classname", suite_name)
                    time_taken = testcase.attrib.get("time", "0")

                    failure = testcase.find("failure")
                    error = testcase.find("error")

                    if failure is not None or error is not None:
                        elem = failure if failure is not None else error
                        kind = "FAILURE" if failure is not None else "ERROR"
                        msg = elem.attrib.get("message", "Sin mensaje")
                        error_type = elem.attrib.get("type", kind)
                        stacktrace = elem.text.strip() if elem.text else ""

                        failed_cases.append({
                            "class": classname,
                            "method": name,
                            "kind": kind,
                            "type": error_type,
                            "message": msg,
                            "time": time_taken,
                            "stacktrace": stacktrace,
                            "source_dir": os.path.basename(reports_dir)
                        })
        except Exception as e:
            print(f"{Colors.YELLOW}⚠️ Error al parsear {xml_path}: {e}{Colors.RESET}")

    return {
        "files_count": len(xml_files),
        "total": total_tests,
        "failures": total_failures,
        "errors": total_errors,
        "skipped": total_skipped,
        "time": total_time,
        "failed_cases": failed_cases
    }


def parse_all_directories(directories):
    """Parsea y consolida los resultados de múltiples directorios de reportes."""
    consolidated = {
        "directories_scanned": [],
        "total": 0,
        "failures": 0,
        "errors": 0,
        "skipped": 0,
        "time": 0.0,
        "failed_cases": []
    }

    for d in directories:
        if not os.path.exists(d):
            continue
        data = parse_reports_in_directory(d)
        if data["files_count"] > 0:
            consolidated["directories_scanned"].append(d)
            consolidated["total"] += data["total"]
            consolidated["failures"] += data["failures"]
            consolidated["errors"] += data["errors"]
            consolidated["skipped"] += data["skipped"]
            consolidated["time"] += data["time"]
            consolidated["failed_cases"].extend(data["failed_cases"])

    return consolidated


def print_terminal_report(data):
    total = data["total"]
    failures = data["failures"]
    errors = data["errors"]
    skipped = data["skipped"]
    passed = total - (failures + errors + skipped)

    print("\n" + "=" * 80)
    print(f"{Colors.BOLD}🔍 REPORTE DE RESULTADOS DE PRUEBAS AUTOMATIZADAS{Colors.RESET}")
    print("=" * 80)

    if data["directories_scanned"]:
        print(f"  {Colors.BOLD}Directorios procesados:{Colors.RESET}")
        for d in data["directories_scanned"]:
            print(f"    • {d}")
        print("-" * 80)

    if total == 0:
        print(f"{Colors.YELLOW}⚠️ No se encontraron reportes de tests en los directorios examinados.{Colors.RESET}\n")
        return

    if failures == 0 and errors == 0:
        print(f"{Colors.GREEN}✅ TODOS LOS TESTS PASARON EXITOSAMENTE ({passed}/{total}) en {data['time']:.2f}s{Colors.RESET}\n")
        return

    print(f"  {Colors.BOLD}Total Ejecutados:{Colors.RESET} {total}")
    print(f"  {Colors.GREEN}Exitosos:{Colors.RESET}         {passed}")
    print(f"  {Colors.RED}Fallos (Assertion):{Colors.RESET} {failures}")
    print(f"  {Colors.RED}Errores (Exceptions):{Colors.RESET} {errors}")
    print(f"  {Colors.YELLOW}Omitidos:{Colors.RESET}        {skipped}")
    print(f"  {Colors.CYAN}Tiempo Total:{Colors.RESET}    {data['time']:.2f}s")
    print("-" * 80)

    print(f"\n{Colors.BOLD}{Colors.RED}🚨 DETALLE DE TEST CASES FALLIDOS ({len(data['failed_cases'])}):{Colors.RESET}")

    for i, fc in enumerate(data["failed_cases"], 1):
        full_name = f"{fc['class']}.{fc['method']}"
        source = f"[{fc.get('source_dir', '')}] " if fc.get('source_dir') else ""
        print(f"\n{Colors.BOLD}{Colors.RED}#{i} {source}[{fc['kind']}] {full_name}{Colors.RESET} ({fc['time']}s)")
        print(f"   • {Colors.BOLD}Tipo de Error:{Colors.RESET} {fc['type']}")
        print(f"   • {Colors.BOLD}Motivo:{Colors.RESET}        {fc['message']}")

        if fc["stacktrace"]:
            print(f"   • {Colors.BOLD}Traza de la Excepción:{Colors.RESET}")
            lines = fc["stacktrace"].split("\n")
            for line in lines[:15]:
                print(f"       {line.rstrip()}")
            if len(lines) > 15:
                print(f"       {Colors.YELLOW}... ({len(lines) - 15} líneas más en el reporte){Colors.RESET}")

    print("\n" + "=" * 80 + "\n")


def write_github_summary(data, workspace_root, output_dir=None):
    total = data["total"]
    failures = data["failures"]
    errors = data["errors"]
    skipped = data["skipped"]
    passed = total - (failures + errors + skipped)

    lines = []

    if failures == 0 and errors == 0 and total > 0:
        lines.append("## ✅ Pruebas de Integración y E2E Aprobadas")
        lines.append(f"Se ejecutaron **{total}** pruebas satisfactoriamente en `{data['time']:.2f}s`.")
    elif total == 0:
        lines.append("## ⚠️ Sin Resultados de Pruebas de Integración")
        lines.append("No se registraron ejecuciones de pruebas en esta fase.")
    else:
        lines.append("## ❌ Reporte de Fallos en Pruebas de Integración y E2E")
        lines.append("")
        lines.append("| Métrica | Valor |")
        lines.append("| :--- | :---: |")
        lines.append(f"| **Total Tests** | {total} |")
        lines.append(f"| **✅ Exitosos** | {passed} |")
        lines.append(f"| **❌ Fallos (Assertion)** | {failures} |")
        lines.append(f"| **💥 Errores (Exception)** | {errors} |")
        lines.append(f"| **⏱️ Tiempo** | {data['time']:.2f}s |")
        lines.append("")
        lines.append("### 🔍 Detalle de Pruebas Fallidas")
        lines.append("")

        for i, fc in enumerate(data["failed_cases"], 1):
            badge = "❌ FALLO" if fc["kind"] == "FAILURE" else "💥 ERROR"
            lines.append("<details open>")
            lines.append(f"<summary><b>{i}. {badge} — <code>{fc['class']}.{fc['method']}</code></b></summary>")
            lines.append("")
            lines.append(f"* **Tipo:** `{fc['type']}`")
            lines.append(f"* **Mensaje:** {fc['message']}")
            lines.append(f"* **Duración:** `{fc['time']}s`")
            lines.append("")
            lines.append("```java")
            lines.append(fc["stacktrace"])
            lines.append("```")
            lines.append("</details>")
            lines.append("")

    try:
        report_md_pattern = os.path.join(workspace_root, "logs", "registro", "*", "reporte-analisis.md")
        report_md_files = glob.glob(report_md_pattern)
        if report_md_files:
            latest_report = sorted(report_md_files, key=os.path.getmtime, reverse=True)[0]
            with open(latest_report, "r", encoding="utf-8", errors="replace") as rf:
                report_content = rf.read()
                lines.append("\n---\n")
                lines.append(report_content)
    except Exception as e:
        print(f"No se pudo adjuntar reporte-analisis.md: {e}", file=sys.stderr)

    content = "\n" + "\n".join(lines) + "\n"

    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        try:
            with open(summary_file, "a", encoding="utf-8") as f:
                f.write(content)
        except Exception as e:
            print(f"Error escribiendo en GITHUB_STEP_SUMMARY: {e}", file=sys.stderr)

    if output_dir:
        os.makedirs(output_dir, exist_ok=True)
        out_path = os.path.join(output_dir, "test-failures-summary.md")
        try:
            with open(out_path, "w", encoding="utf-8") as f:
                f.write(content)
        except Exception as e:
            print(f"Error escribiendo en {out_path}: {e}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description="Reporteador detallado de fallos de pruebas Maven (Surefire / Failsafe).")
    parser.add_argument(
        "--dir",
        dest="directories",
        action="append",
        default=None,
        help="Directorio de reportes XML (puede repetirse). Por defecto busca en failsafe-reports y surefire-reports."
    )
    parser.add_argument(
        "--fail-on-zero-tests",
        "--fail-on-zero",
        dest="fail_on_zero",
        action="store_true",
        help="Retornar código de salida 1 si no se ejecutó ninguna prueba (total == 0)."
    )
    parser.add_argument(
        "--output-dir",
        dest="output_dir",
        default=None,
        help="Directorio opcional donde exportar test-failures-summary.md."
    )

    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.abspath(os.path.join(script_dir, ".."))

    # Si no se pasaron directorios explícitos, usar convenciones estándar
    if not args.directories:
        default_dirs = [
            os.path.join(workspace_root, "integration-tests", "target", "failsafe-reports"),
            os.path.join(workspace_root, "integration-tests", "target", "surefire-reports"),
        ]
        dirs_to_check = default_dirs
    else:
        dirs_to_check = [os.path.abspath(d) for d in args.directories]

    try:
        data = parse_all_directories(dirs_to_check)
        print_terminal_report(data)
        write_github_summary(data, workspace_root, output_dir=args.output_dir)

        total = data["total"]
        failures = data["failures"]
        errors = data["errors"]

        # Política semántica de códigos de salida
        if total == 0:
            if args.fail_on_zero:
                print(f"{Colors.RED}::error::No se ejecutó ninguna prueba automatizada (total_tests == 0).{Colors.RESET}", file=sys.stderr)
                return 1
            return 0

        if failures > 0 or errors > 0:
            print(f"{Colors.RED}::error::Se detectaron {failures} fallos y {errors} errores en la suite de pruebas.{Colors.RESET}", file=sys.stderr)
            return 1

        return 0

    except Exception as ex:
        print(f"{Colors.RED}❌ Error catastrófico en report_test_failures: {ex}{Colors.RESET}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
