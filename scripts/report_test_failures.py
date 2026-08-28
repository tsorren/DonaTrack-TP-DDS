#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
report_test_failures.py — Reporteador Detallado de Fallos de Tests (DonaTrack)
=============================================================================
Analiza los reportes XML/TXT de Maven Surefire en integration-tests y genera
un diagnóstico detallado tanto en consola como en GitHub Step Summary ($GITHUB_STEP_SUMMARY).
"""

import sys
import os
import glob
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

def parse_surefire_reports(reports_dir):
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
                            "stacktrace": stacktrace
                        })
        except Exception as e:
            print(f"{Colors.YELLOW}⚠️ Error al parsear {xml_path}: {e}{Colors.RESET}")

    return {
        "total": total_tests,
        "failures": total_failures,
        "errors": total_errors,
        "skipped": total_skipped,
        "time": total_time,
        "failed_cases": failed_cases
    }

def print_terminal_report(data):
    total = data["total"]
    failures = data["failures"]
    errors = data["errors"]
    skipped = data["skipped"]
    passed = total - (failures + errors + skipped)

    print("\n" + "=" * 80)
    print(f"{Colors.BOLD}🔍 REPORTE DE RESULTADOS DE PRUEBAS DE INTEGRACIÓN{Colors.RESET}")
    print("=" * 80)
    
    if failures == 0 and errors == 0 and total > 0:
        print(f"{Colors.GREEN}✅ TODOS LOS TESTS PASARON EXITOSAMENTE ({passed}/{total}) en {data['time']:.2f}s{Colors.RESET}\n")
        return

    if total == 0:
        print(f"{Colors.YELLOW}⚠️ No se encontraron reportes de tests de integración.{Colors.RESET}\n")
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
        print(f"\n{Colors.BOLD}{Colors.RED}#{i} [{fc['kind']}] {full_name}{Colors.RESET} ({fc['time']}s)")
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

def write_github_summary(data, workspace_root):
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary_file:
        return

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
            lines.append(f"<details open>")
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

    with open(summary_file, "a", encoding="utf-8") as f:
        f.write("\n" + "\n".join(lines) + "\n")

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.abspath(os.path.join(script_dir, ".."))
    reports_dir = os.path.join(workspace_root, "integration-tests", "target", "surefire-reports")

    if not os.path.exists(reports_dir):
        print(f"{Colors.YELLOW}Directorio de reportes Surefire no encontrado: {reports_dir}{Colors.RESET}")
        return 0

    data = parse_surefire_reports(reports_dir)
    print_terminal_report(data)
    write_github_summary(data, workspace_root)

    return 0

if __name__ == "__main__":
    sys.exit(main())
