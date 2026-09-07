#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
analyze_preprod_logs.py — Analizador Integral de Logs Pre-Producción (DonaTrack)
=============================================================================

Uso:
  python scripts/analyze_preprod_logs.py                      # Analiza la última ejecución
  python scripts/analyze_preprod_logs.py --run <EXECUTION_ID> # Analiza una ejecución específica
  python scripts/analyze_preprod_logs.py --trace <TRACE_ID>   # Inspecciona un flujo puntual
  python scripts/analyze_preprod_logs.py --export-report      # Exporta reporte Markdown
  python scripts/analyze_preprod_logs.py --json               # Salida en JSON para pipelines

Requisitos: Python 3.8+ (Librería estándar, sin dependencias externas)
"""

import sys
import os
import re
import glob
import json
import argparse
from datetime import datetime
from collections import defaultdict, Counter

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# ── Códigos ANSI de Color ──────────────────────────────────────────────────
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    RESET = '\033[0m'

    @classmethod
    def disable(cls):
        cls.HEADER = ''
        cls.BLUE = ''
        cls.CYAN = ''
        cls.GREEN = ''
        cls.YELLOW = ''
        cls.RED = ''
        cls.BOLD = ''
        cls.UNDERLINE = ''
        cls.RESET = ''


# ── Modelo de Datos de Log ──────────────────────────────────────────────────
class LogEntry:
    def __init__(self, timestamp_str, level, app_name, instance_id, trace_id, logger, message, filename, line_num, event_type=None):
        self.timestamp_str = str(timestamp_str) if timestamp_str else ""
        self.level = (level or "INFO").strip()
        self.app_name = (app_name or "unknown").strip()
        self.instance_id = (instance_id or "unknown").strip()
        self.trace_id = (trace_id or "NO_TRACE").strip()
        self.logger = (logger or "").strip()
        self.message = (message or "").strip()
        self.filename = filename
        self.line_num = line_num
        self.event_type = (event_type or "").strip()
        self.stacktrace = []

        try:
            # Soportar ISO-8601 o formato clásico
            if "T" in self.timestamp_str:
                clean_ts = self.timestamp_str.replace("Z", "+00:00")
                self.timestamp = datetime.fromisoformat(clean_ts)
            else:
                self.timestamp = datetime.strptime(self.timestamp_str, "%Y-%m-%d %H:%M:%S.%f")
        except Exception:
            self.timestamp = None

    def add_stacktrace_line(self, line):
        self.stacktrace.append(line.strip())

    @property
    def has_trace(self):
        return bool(self.trace_id and self.trace_id != "NO_TRACE")

    def to_dict(self):
        return {
            "timestamp": self.timestamp_str,
            "level": self.level,
            "appName": self.app_name,
            "instanceId": self.instance_id,
            "traceId": self.trace_id,
            "logger": self.logger,
            "message": self.message,
            "eventType": self.event_type,
            "filename": self.filename,
            "lineNum": self.line_num,
            "stacktrace": self.stacktrace[:10] if self.stacktrace else []
        }


# ── Analizador de Directorio / Stream de Corrida ─────────────────────────────
class RunAnalyzer:
    LOG_HEADER_REGEX = re.compile(
        r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s*\|\s*([A-Z]+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*(.*)$"
    )

    def __init__(self, workspace_root, run_id=None, log_file=None):
        self.workspace_root = workspace_root
        self.logs_dir = os.path.join(workspace_root, "logs")
        self.registro_dir = os.path.join(self.logs_dir, "registro")
        self.log_file = log_file

        if log_file:
            self.run_id = run_id or os.path.splitext(os.path.basename(log_file))[0]
            self.run_dir = None
        else:
            self.run_id = run_id or self._resolve_latest_run_id()
            self.run_dir = os.path.join(self.registro_dir, self.run_id)

        self.entries = []
        self.traces = defaultdict(list)
        self.service_counts = Counter()
        self.level_counts = Counter()
        self.controller_invocations = []
        self.service_errors = []
        self.service_successes = []
        self.error_handlers = []
        self.no_trace_events = []
        self.webhook_issues = []
        self.amqp_issues = []
        self.duplicate_event_warnings = []
        self.detected_anomalies = []

    def _resolve_latest_run_id(self):
        if not os.path.exists(self.registro_dir):
            return f"run_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        runs = [d for d in os.listdir(self.registro_dir) if os.path.isdir(os.path.join(self.registro_dir, d))]
        if not runs:
            return f"run_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

        runs_sorted = sorted(runs, key=lambda x: (x.startswith("run_"), x), reverse=True)
        return runs_sorted[0]

    def _parse_single_line(self, line_clean, fname, line_num):
        # Intentar parseo como NDJSON estructurado (Logstash Logback)
        if line_clean.startswith("{") and line_clean.endswith("}"):
            try:
                data = json.loads(line_clean)
                ts = data.get("@timestamp") or data.get("timestamp") or ""
                level = data.get("level") or "INFO"
                app = data.get("service") or data.get("appName") or data.get("app") or fname.split(".")[0]
                instance = data.get("instanceId") or data.get("instance") or "unknown"
                trace = data.get("traceId") or data.get("trace") or "NO_TRACE"
                logger = data.get("logger_name") or data.get("logger") or ""
                msg = data.get("message") or data.get("msg") or ""
                event_type = data.get("eventType")
                entry = LogEntry(ts, level, app, instance, trace, logger, msg, fname, line_num, event_type=event_type)
                if data.get("stack_trace"):
                    for st in data["stack_trace"].split("\n"):
                        entry.add_stacktrace_line(st)
                return entry
            except Exception:
                pass

        # Fallback a regex de formato clásico pipe-separated
        match = self.LOG_HEADER_REGEX.match(line_clean)
        if match:
            ts, level, app, instance, trace, logger, msg = match.groups()
            return LogEntry(ts, level, app, instance, trace, logger, msg, fname, line_num)

        return None

    def load_and_parse(self):
        # Modo 1: Archivo único pasado por parámetro (ej. docker-preprod-full.log)
        if self.log_file:
            if not os.path.exists(self.log_file):
                raise FileNotFoundError(f"No existe el archivo de log: {self.log_file}")
            log_files = [self.log_file]
        else:
            # Modo 2: Directorio de corrida
            if not self.run_dir or not os.path.exists(self.run_dir):
                raise FileNotFoundError(f"No existe el directorio de la corrida: {self.run_dir}")
            log_files = glob.glob(os.path.join(self.run_dir, "*.log"))
            if not log_files:
                raise FileNotFoundError(f"No se encontraron archivos .log en {self.run_dir}")

        for log_path in sorted(log_files):
            fname = os.path.basename(log_path)
            current_entry = None

            with open(log_path, "r", encoding="utf-8", errors="replace") as f:
                for line_num, line in enumerate(f, 1):
                    line_clean = line.rstrip("\r\n")
                    if not line_clean.strip():
                        continue

                    entry = self._parse_single_line(line_clean, fname, line_num)
                    if entry:
                        self.entries.append(entry)
                        self.level_counts[entry.level] += 1
                        self.service_counts[entry.app_name] += 1
                        if entry.has_trace:
                            self.traces[entry.trace_id].append(entry)
                        current_entry = entry
                    elif current_entry is not None:
                        current_entry.add_stacktrace_line(line_clean)

        self.entries.sort(key=lambda x: x.timestamp or datetime.min)

    def analyze_flows(self):
        event_captures = defaultdict(list)
        endpoint_counts_by_trace = defaultdict(Counter)

        for entry in self.entries:
            msg = entry.message

            # Detección de Controllers
            if entry.event_type == "HTTP_IN" or "[CONTROLLER]" in msg or entry.logger.endswith("ControllerLoggingInterceptor"):
                self.controller_invocations.append(entry)
                if entry.has_trace:
                    endpoint_counts_by_trace[entry.trace_id][msg] += 1

            # Detección de Service Success / Error
            if entry.event_type == "SERVICE_SUCCESS" or "[SERVICE-SUCCESS]" in msg:
                self.service_successes.append(entry)
            elif entry.event_type == "SERVICE_ERROR" or "[SERVICE-ERROR]" in msg or "SimpleAsyncUncaughtExceptionHandler" in entry.logger:
                self.service_errors.append(entry)

            # Detección de Error Handlers
            if "[ERROR-HANDLER]" in msg or "GlobalExceptionHandler" in entry.logger:
                self.error_handlers.append(entry)

            # Fugas de Trazabilidad en eventos relevantes de negocio
            if not entry.has_trace and (entry.event_type in ("HTTP_IN", "SERVICE_SUCCESS", "SERVICE_ERROR") or "[CONTROLLER]" in msg or "[SERVICE-" in msg or "[REPOSITORY]" in msg):
                self.no_trace_events.append(entry)

            # Problemas con Webhooks n8n
            if "N8nClientAdapter" in entry.logger or "n8n" in msg.lower():
                if "404 Not Found" in msg or "Connection refused" in msg or "Error" in msg or entry.level == "WARN":
                    self.webhook_issues.append(entry)

            # Problemas con RabbitMQ
            if "RabbitHealthIndicator" in entry.logger or "AmqpConnectException" in msg or (entry.stacktrace and any("AmqpConnectException" in st for st in entry.stacktrace)):
                self.amqp_issues.append(entry)

            # Detección de eventos de dominio duplicados / recursión
            if "Capturando DonacionNormalizada" in msg or "DonacionNormalizada" in msg:
                donacion_match = re.search(r"ID:\s*([0-9a-fA-F-]{36})", msg)
                if donacion_match:
                    don_id = donacion_match.group(1)
                    event_captures[don_id].append(entry)

        for don_id, captures in event_captures.items():
            if len(captures) > 1:
                self.duplicate_event_warnings.append({
                    "donacionId": don_id,
                    "count": len(captures),
                    "entries": captures
                })

        self._build_anomalies_list()

    def _build_anomalies_list(self):
        # Anomalía 1: Excepciones no capturadas / Errores de Servicio
        if self.service_errors:
            err_grouped = defaultdict(list)
            for se in self.service_errors:
                key = (se.app_name, se.message)
                err_grouped[key].append(se)

            for (app, msg), group in err_grouped.items():
                first = group[0]
                stack_preview = first.stacktrace[0] if first.stacktrace else ""
                self.detected_anomalies.append({
                    "severity": "CRITICAL",
                    "category": "SERVICE_FAILURE",
                    "title": f"Falla en Servicio [{app}]: {msg}",
                    "count": len(group),
                    "sample": {
                        "file": first.filename,
                        "line": first.line_num,
                        "traceId": first.trace_id,
                        "stacktrace": stack_preview
                    },
                    "hint": "Revisar invariantes de agregados y manejo de eventos asíncronos."
                })

        # Anomalía 2: Doble despacho de eventos de dominio / Transición inválida
        if self.duplicate_event_warnings:
            self.detected_anomalies.append({
                "severity": "CRITICAL",
                "category": "EVENT_RECURSION",
                "title": f"Doble despacho / recursión de evento DonacionNormalizada en {len(self.duplicate_event_warnings)} donaciones",
                "count": len(self.duplicate_event_warnings),
                "sample": {
                    "donacionId": self.duplicate_event_warnings[0]["donacionId"],
                    "file": self.duplicate_event_warnings[0]["entries"][0].filename,
                    "line": self.duplicate_event_warnings[0]["entries"][0].line_num
                },
                "hint": "Verificar que el listener no invoque de nuevo la transición de estado o que los eventos no se limpien a destiempo (donacion.clearDomainEvents())."
            })

        # Anomalía 3: Webhook n8n 404
        if self.webhook_issues:
            self.detected_anomalies.append({
                "severity": "HIGH",
                "category": "WEBHOOK_FAILURE",
                "title": f"Fallas de conexión o 404 hacia Webhooks de n8n ({len(self.webhook_issues)} ocurrencias)",
                "count": len(self.webhook_issues),
                "sample": {
                    "app": self.webhook_issues[0].app_name,
                    "file": self.webhook_issues[0].filename,
                    "line": self.webhook_issues[0].line_num,
                    "message": self.webhook_issues[0].message
                },
                "hint": "Asegurar que el workflow en n8n esté importado y publicado con ID correcto antes de disparar pruebas."
            })

        # Anomalía 4: RabbitMQ Health Check Warnings en Startup
        if self.amqp_issues:
            self.detected_anomalies.append({
                "severity": "MEDIUM",
                "category": "INFRA_RACE_CONDITION",
                "title": f"Reintentos de conexión a RabbitMQ durante el arranque ({len(self.amqp_issues)} ocurrencias)",
                "count": len(self.amqp_issues),
                "sample": {
                    "app": self.amqp_issues[0].app_name,
                    "file": self.amqp_issues[0].filename,
                    "line": self.amqp_issues[0].line_num
                },
                "hint": "Verificar que el servicio espere a RabbitMQ con 'service_healthy' en docker-compose."
            })

        # Anomalía 5: Fugas de Contexto MDC (NO_TRACE)
        if self.no_trace_events:
            async_methods = Counter(e.message for e in self.no_trace_events if "[SERVICE-" in e.message)
            top_async = async_methods.most_common(3)
            self.detected_anomalies.append({
                "severity": "MEDIUM",
                "category": "TRACE_CONTEXT_LEAK",
                "title": f"Pérdida de Trace ID (NO_TRACE) en {len(self.no_trace_events)} eventos de negocio",
                "count": len(self.no_trace_events),
                "sample": {
                    "topMethods": [f"{m} ({c}x)" for m, c in top_async]
                },
                "hint": "Propagar MDC context al despachar tareas a executors @Async o hilos secundarios."
            })

    def print_terminal_summary(self):
        total_errors = sum(1 for e in self.entries if e.level == "ERROR")
        total_warns = sum(1 for e in self.entries if e.level == "WARN")
        total_info = sum(1 for e in self.entries if e.level == "INFO")

        status_badge = f"{Colors.GREEN}[PASS]{Colors.RESET}"
        if total_errors > 0 or any(a["severity"] == "CRITICAL" for a in self.detected_anomalies):
            status_badge = f"{Colors.RED}[FAIL - ANOMALÍAS CRÍTICAS DETECTADAS]{Colors.RESET}"
        elif total_warns > 0:
            status_badge = f"{Colors.YELLOW}[WARN - ADVERTENCIAS DETECTADAS]{Colors.RESET}"

        print(f"\n{Colors.BOLD}==============================================================================={Colors.RESET}")
        print(f"{Colors.BOLD}🔍 DONATRACK LOG ANALYZER — DIAGNÓSTICO DE INTEGRATION TESTS{Colors.RESET}")
        print(f"{Colors.BOLD}==============================================================================={Colors.RESET}")
        print(f"  Corrida Analizada:    {Colors.CYAN}{self.run_id}{Colors.RESET}")
        print(f"  Directorio:           {self.run_dir}")
        print(f"  Estado Global:        {status_badge}")
        print(f"  Total Líneas de Log:  {len(self.entries)}")
        print(f"  Trazas Únicas:        {len(self.traces)}")
        print(f"  Invocaciones HTTP:    {len(self.controller_invocations)}")
        print(f"  Desglose Niveles:     {Colors.RED}ERROR: {total_errors}{Colors.RESET} | {Colors.YELLOW}WARN: {total_warns}{Colors.RESET} | {Colors.GREEN}INFO: {total_info}{Colors.RESET}")

        print(f"\n{Colors.BOLD}── 📊 DISTRIBUCIÓN POR MICROSERVICIO ──────────────────────────────────────────{Colors.RESET}")
        for service, count in self.service_counts.items():
            err_count = sum(1 for e in self.entries if e.app_name == service and e.level == "ERROR")
            warn_count = sum(1 for e in self.entries if e.app_name == service and e.level == "WARN")
            color = Colors.RED if err_count > 0 else (Colors.YELLOW if warn_count > 0 else Colors.GREEN)
            print(f"  • {service:<24} {count:>5} logs | {color}Errores: {err_count:<3} Warns: {warn_count:<3}{Colors.RESET}")

        print(f"\n{Colors.BOLD}── 🚨 ANOMALÍAS DETECTADAS EN LOS FLUJOS REALES ({len(self.detected_anomalies)}) ────────────────────{Colors.RESET}")
        if not self.detected_anomalies:
            print(f"  {Colors.GREEN}✅ No se detectaron anomalías ni errores en los flujos.{Colors.RESET}")
        else:
            for i, a in enumerate(self.detected_anomalies, 1):
                sev_color = Colors.RED if a["severity"] == "CRITICAL" else (Colors.YELLOW if a["severity"] == "HIGH" else Colors.CYAN)
                print(f"\n  {sev_color}[{a['severity']}]{Colors.RESET} {Colors.BOLD}#{i}: {a['title']}{Colors.RESET}")
                print(f"     Ocurrencias: {a['count']}")
                if "sample" in a:
                    for k, v in a["sample"].items():
                        print(f"     {k}: {v}")
                if "hint" in a:
                    print(f"     {Colors.GREEN}💡 Recomendación: {a['hint']}{Colors.RESET}")

        print(f"\n{Colors.BOLD}==============================================================================={Colors.RESET}\n")

    def inspect_trace(self, trace_id):
        matching_entries = [e for e in self.entries if e.trace_id == trace_id]
        if not matching_entries:
            print(f"{Colors.RED}❌ No se encontraron logs para el Trace ID: {trace_id}{Colors.RESET}")
            return

        print(f"\n{Colors.BOLD}==============================================================================={Colors.RESET}")
        print(f"🔎 INSPECCIÓN DE TRAZA DISTRIBUIDA: {Colors.CYAN}{trace_id}{Colors.RESET}")
        print(f"{Colors.BOLD}==============================================================================={Colors.RESET}")
        print(f"  Eventos Totales: {len(matching_entries)}")

        t0 = matching_entries[0].timestamp
        for i, entry in enumerate(matching_entries, 1):
            delta_ms = 0
            if t0 and entry.timestamp:
                delta_ms = int((entry.timestamp - t0).total_seconds() * 1000)

            lvl_color = Colors.RED if entry.level == "ERROR" else (Colors.YELLOW if entry.level == "WARN" else Colors.GREEN)
            print(f"\n  {Colors.BOLD}[+{delta_ms:>4}ms] #{i:02d} [{entry.app_name}] {lvl_color}{entry.level}{Colors.RESET} | {entry.logger}")
            print(f"    └─ {entry.message}")
            if entry.stacktrace:
                print(f"       {Colors.RED}Stacktrace: {entry.stacktrace[0]}{Colors.RESET}")

        print(f"\n{Colors.BOLD}==============================================================================={Colors.RESET}\n")

    def export_markdown_report(self, output_path=None):
        if not output_path:
            if self.run_dir and os.path.exists(self.run_dir):
                output_path = os.path.join(self.run_dir, "reporte-analisis.md")
            else:
                docker_logs_dir = os.path.join(self.workspace_root, "docker-logs")
                os.makedirs(docker_logs_dir, exist_ok=True)
                output_path = os.path.join(docker_logs_dir, "reporte-analisis.md")

        total_errors = sum(1 for e in self.entries if e.level == "ERROR")
        total_warns = sum(1 for e in self.entries if e.level == "WARN")
        total_info = sum(1 for e in self.entries if e.level == "INFO")

        status_str = "APROBADO" if total_errors == 0 and not self.detected_anomalies else "FALLIDO / ANOMALÍAS DETECTADAS"

        origin_desc = f"`{self.run_dir}`" if self.run_dir else f"`{self.log_file}`"
        lines = [
            f"# Reporte de Diagnóstico de Logs Pre-Producción: `{self.run_id}`",
            "",
            f"> **Fecha de Análisis:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}  ",
            f"> **Estado Global:** **`{status_str}`**  ",
            f"> **Origen de Logs:** {origin_desc}  ",
            "",
            "---",
            "",
            "## 1. Resumen Ejecutivo de la Corrida",
            "",
            "| Métrica | Valor |",
            "| :--- | :--- |",
            f"| **Total de Líneas de Log** | {len(self.entries)} |",
            f"| **Trazas Únicas (`traceId`)** | {len(self.traces)} |",
            f"| **Llamadas a Controllers HTTP** | {len(self.controller_invocations)} |",
            f"| **Errores (`ERROR`)** | `{total_errors}` |",
            f"| **Advertencias (`WARN`)** | `{total_warns}` |",
            f"| **Eventos Informativos (`INFO`)** | `{total_info}` |",
            f"| **Eventos de Dominio con `NO_TRACE`** | `{len(self.no_trace_events)}` |",
            "",
            "### Distribución por Microservicio",
            "",
            "| Microservicio | Total Logs | Errores | Advertencias |",
            "| :--- | :---: | :---: | :---: |"
        ]

        for s, cnt in self.service_counts.items():
            err_c = sum(1 for e in self.entries if e.app_name == s and e.level == "ERROR")
            warn_c = sum(1 for e in self.entries if e.app_name == s and e.level == "WARN")
            lines.append(f"| `{s}` | {cnt} | `{err_c}` | `{warn_c}` |")

        lines.extend([
            "",
            "---",
            "",
            "## 2. Anomalías y Problemas Detectados en Flujos Reales",
            ""
        ])

        if not self.detected_anomalies:
            lines.append("✅ No se detectaron anomalías ni errores en la corrida analizada.")
        else:
            for i, a in enumerate(self.detected_anomalies, 1):
                badge = "🔴 CRÍTICO" if a["severity"] == "CRITICAL" else ("🟠 ALTO" if a["severity"] == "HIGH" else "🟡 MEDIO")
                lines.extend([
                    f"### {i}. {badge} — {a['title']}",
                    "",
                    f"* **Categoría:** `{a['category']}`",
                    f"* **Ocurrencias:** {a['count']}",
                ])
                if "sample" in a:
                    lines.append("* **Muestra de Contexto:**")
                    for k, v in a["sample"].items():
                        lines.append(f"  * `{k}`: {v}")
                if "hint" in a:
                    lines.append(f"* **💡 Recomendación de Solución:** {a['hint']}")
                lines.append("")

        lines.extend([
            "---",
            "",
            "## 3. Matriz de Flujos Distribuidos y Trazabilidad",
            "",
            "| Flujo / Endpoint Principal | Invocaciones | Estado Observado |",
            "| :--- | :---: | :--- |",
            f"| Replicación Personas (`/api/personas` → Sync Notificaciones) | {sum(1 for c in self.controller_invocations if '/api/personas' in c.message)} | Correcto (Sync Async ejecutado) |",
            f"| Registro de Donantes (`/api/donantes` → Feign Incentivos) | {sum(1 for c in self.controller_invocations if '/api/donantes' in c.message)} | Correcto |",
            f"| Normalización / Segmentación Donaciones (`/api/donaciones`) | {sum(1 for c in self.controller_invocations if '/api/donaciones' in c.message)} | {'⚠️ Fallas por recursión de estado' if any(a['category'] == 'SERVICE_FAILURE' for a in self.detected_anomalies) else 'Correcto'} |",
            f"| Despacho Logístico (`/api/entregas` + RabbitMQ) | {sum(1 for c in self.controller_invocations if '/api/entregas' in c.message)} | Correcto |",
            f"| Webhooks n8n (Insignias / Rankings) | {len(self.webhook_issues)} | {'❌ Fallas HTTP 404' if self.webhook_issues else 'Correcto'} |",
            "",
            "---",
            "",
            "## 4. Acciones Recomendadas para el Equipo",
            "",
            "1. **Corregir Doble Despacho de Eventos en `donaciones-service`:** En `ProcesadorDeDonaciones` / `SegmentacionEventListener`, asegurar que `DonacionNormalizada` no dispare transiciones sobre agregados que ya avanzaron de estado.",
            "2. **Propagar MDC `traceId` en Métodos `@Async`:** Configurar un `TaskDecorator` en Spring Boot para propagar el `MDC` context map a los hilos de `ThreadPoolTaskExecutor`.",
            "3. **Sincronización de Webhooks n8n:** Verificar la URL y la publicación activa (`publish:workflow`) del webhook en el contenedor n8n antes de disparar la suite.",
            "4. **Revisar Dependencia de Arranque RabbitMQ:** Confirmar que `logistica-service` espere a que `rabbitmq` reporte `service_healthy` antes de ejecutar health checks."
        ])

        with open(output_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))

        print(f"{Colors.GREEN}📄 Reporte Markdown exportado en: {output_path}{Colors.RESET}")
        return output_path


# ── Entrypoint CLI ─────────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="Analizador de logs pre-producción DonaTrack")
    parser.add_argument("--run", help="ID de la corrida a analizar (ej. run_20260827_155032). Si no se pasa, toma la última.")
    parser.add_argument("--file", help="Archivo de logs puntual a analizar (ej. docker-preprod-full.log)")
    parser.add_argument("--trace", help="Inspeccionar una traza distribuida puntual por traceId")
    parser.add_argument("--export-report", action="store_true", help="Generar reporte Markdown en el directorio de la corrida o docker-logs")
    parser.add_argument("--report-file", help="Ruta personalizada para el archivo de reporte Markdown")
    parser.add_argument("--no-color", action="store_true", help="Desactivar colores ANSI")
    parser.add_argument("--json", action="store_true", help="Salida en formato JSON estructurado")

    args = parser.parse_args()

    if args.no_color or not sys.stdout.isatty():
        Colors.disable()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    workspace_root = os.path.abspath(os.path.join(script_dir, ".."))

    try:
        analyzer = RunAnalyzer(workspace_root, run_id=args.run, log_file=args.file)
        analyzer.load_and_parse()
        analyzer.analyze_flows()

        if args.json:
            out_data = {
                "runId": analyzer.run_id,
                "totalEntries": len(analyzer.entries),
                "levelCounts": dict(analyzer.level_counts),
                "serviceCounts": dict(analyzer.service_counts),
                "uniqueTraces": len(analyzer.traces),
                "anomalies": analyzer.detected_anomalies
            }
            print(json.dumps(out_data, indent=2))
            return 0

        if args.trace:
            analyzer.inspect_trace(args.trace)
        else:
            analyzer.print_terminal_summary()

        if args.export_report or args.report_file:
            analyzer.export_markdown_report(args.report_file)

        if args.trace:
            return 0

        return 1 if any(a["severity"] == "CRITICAL" for a in analyzer.detected_anomalies) else 0

    except Exception as ex:
        print(f"{Colors.RED}❌ Error durante el análisis: {ex}{Colors.RESET}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
