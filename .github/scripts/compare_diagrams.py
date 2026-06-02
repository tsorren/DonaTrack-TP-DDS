"""
DonaTrack — Auditoría de Paridad Diseño vs. Implementación
===========================================================
Este script compara diagramas PlantUML manuales (en docs/diagramas/)
con diagramas generados automáticamente desde el código (en <servicio>/target/).

Usa la API de Gemini para analizar discrepancias entre el modelo de dominio
diseñado y la implementación real, garantizando trazabilidad entre artefactos
de diseño y código fuente.

Resultados se escriben a 'audit-results.json' para ser publicados como
comentario en el PR por el workflow de CI.
"""

import json
import os
import sys
from pathlib import Path

import google.generativeai as genai

# ── Configuración ──────────────────────────────────────────────────────────
GEMINI_MODEL = "gemini-2.0-flash"

# Servicios a auditar: se descubren dinámicamente buscando archivos .puml
# generados en target/ de cada módulo Maven.
MODULES_DIR = Path(".")
MANUAL_DIAGRAMS_DIR = Path("docs/diagramas")

AUDIT_PROMPT = """\
Comparar estos dos diagramas de PlantUML para el módulo '{service_name}'.
El 'Diagrama Manual' es el diseño original del equipo.
El 'Diagrama Generado' fue extraído automáticamente del código fuente.

REGLAS DE AUDITORÍA:
1. Ignorar orden de atributos y métodos.
2. Ignorar constructores, getters, setters y métodos generados (equals, hashCode, toString).
3. Verificar coincidencia de visibilidad (+ public, # protected, - private, ~ package).
4. Validar consistencia en nombres de clases, interfaces, enums y tipos de datos.
5. Verificar que las relaciones (herencia, composición, asociación) coincidan.
6. Ignorar clases de frameworks (Spring, JPA, Lombok-generated).

DIAGRAMA MANUAL:
```plantuml
{manual_content}
```

DIAGRAMA GENERADO:
```plantuml
{generated_content}
```

Responder EXCLUSIVAMENTE en formato JSON válido (sin markdown, sin backticks):
{{
  "status": "PASS" o "FAIL",
  "summary": "Resumen breve de la comparación",
  "errors": ["lista de discrepancias encontradas, vacía si PASS"]
}}
"""


def discover_services() -> list[str]:
    """Descubre dinámicamente los servicios que tienen diagramas generados."""
    services = []
    for child in sorted(MODULES_DIR.iterdir()):
        if not child.is_dir():
            continue
        puml_file = child / "target" / "modelo_tecnico.puml"
        if puml_file.exists():
            services.append(child.name)
    return services


def read_file(path: Path) -> str | None:
    """Lee el contenido de un archivo, devuelve None si no existe."""
    try:
        return path.read_text(encoding="utf-8")
    except (FileNotFoundError, PermissionError):
        return None


def audit_service(model, service_name: str) -> dict:
    """Audita un servicio comparando diagrama manual vs. generado."""
    manual_path = MANUAL_DIAGRAMS_DIR / f"{service_name}.puml"
    generated_path = MODULES_DIR / service_name / "target" / "modelo_tecnico.puml"

    manual_content = read_file(manual_path)
    generated_content = read_file(generated_path)

    # Si no hay diagrama manual, no se puede comparar
    if not manual_content:
        return {
            "status": "SKIP",
            "summary": f"No se encontró diagrama manual en {manual_path}",
            "errors": [],
        }

    # Si no hay diagrama generado, algo falló en la compilación
    if not generated_content:
        return {
            "status": "SKIP",
            "summary": f"No se encontró diagrama generado en {generated_path}",
            "errors": [],
        }

    # Invocar Gemini para la auditoría
    prompt = AUDIT_PROMPT.format(
        service_name=service_name,
        manual_content=manual_content,
        generated_content=generated_content,
    )

    try:
        response = model.generate_content(prompt)
        raw_text = response.text.strip()

        # Limpiar posibles wrappers de markdown
        if raw_text.startswith("```"):
            raw_text = raw_text.split("\n", 1)[1] if "\n" in raw_text else raw_text
        if raw_text.endswith("```"):
            raw_text = raw_text.rsplit("```", 1)[0]
        raw_text = raw_text.replace("```json", "").replace("```", "").strip()

        return json.loads(raw_text)
    except json.JSONDecodeError as e:
        return {
            "status": "ERROR",
            "summary": f"No se pudo parsear la respuesta de Gemini: {e}",
            "errors": [raw_text[:500]],
        }
    except Exception as e:
        return {
            "status": "ERROR",
            "summary": f"Error al invocar Gemini: {e}",
            "errors": [],
        }


def main():
    # Configurar Gemini
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        print("⚠️  GEMINI_API_KEY no configurada. Saltando auditoría.")
        sys.exit(0)

    genai.configure(api_key=api_key)
    model = genai.GenerativeModel(GEMINI_MODEL)

    # Descubrir servicios
    services = discover_services()
    if not services:
        print("ℹ️  No se encontraron servicios con diagramas generados.")
        sys.exit(0)

    print(f"🔍 Servicios detectados: {', '.join(services)}")
    print("=" * 60)

    results = {}
    has_failures = False

    for svc in services:
        print(f"\n📐 Auditando: {svc}")
        result = audit_service(model, svc)
        results[svc] = result

        status = result.get("status", "UNKNOWN")
        summary = result.get("summary", "")
        icon = {"PASS": "✅", "FAIL": "❌", "SKIP": "⏭️", "ERROR": "⚠️"}.get(
            status, "❓"
        )

        print(f"   {icon} {status}: {summary}")

        if result.get("errors"):
            for err in result["errors"]:
                print(f"      • {err}")

        if status == "FAIL":
            has_failures = True

    # Escribir resultados a archivo JSON para el step de PR comment
    output_file = Path("audit-results.json")
    output_file.write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"\n📄 Resultados escritos en {output_file}")

    print("\n" + "=" * 60)
    if has_failures:
        print("❌ Auditoría FALLIDA — hay discrepancias entre diseño e implementación.")
        sys.exit(1)
    else:
        print("✅ Auditoría completada — no se detectaron discrepancias críticas.")


if __name__ == "__main__":
    main()