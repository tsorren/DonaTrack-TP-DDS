import os
import sys
import google.generativeai as genai

genai.configure(api_key=os.environ["GEMINI_API_KEY"])
model = genai.GenerativeModel('gemini-1.5-flash')

def get_file_content(path):
    try:
        with open(path, 'r') as f:
            return f.read()
    except FileNotFoundError:
        return None

def audit_service(service_name):
    manual_path = f"docs/diagramas/{service_name}.puml"
    generated_path = f"{service_name}/target/modelo_tecnico.puml"

    manual_content = get_file_content(manual_path)
    generated_content = get_file_content(generated_path)

    if not manual_content or not generated_content:
        print(f"Info: Saltando {service_name} por falta de archivos.")
        return True

    prompt = f"""
    Compare estos dos diagramas de PlantUML.
    El 'Diagrama Manual' es el diseno original. El 'Diagrama Generado' es la implementacion.

    REGLAS DE AUDITORIA:
    1. Ignorar orden de atributos y metodos.
    2. Ignorar constructores, getters y setters.
    3. Verificar coincidencia de privacidad (+ public, # protected, - private).
    4. Validar consistencia en nombres de clases, metodos y tipos de datos.

    DIAGRAMA MANUAL:
    {manual_content}

    DIAGRAMA GENERADO:
    {generated_content}

    Responder exclusivamente en formato JSON:
    {{
      "status": "PASS" o "FAIL",
      "errors": ["lista de discrepancias"]
    }}
    """

    response = model.generate_content(prompt)
    return response.text.replace("```json", "").replace("```", "").strip()

services = ["donaciones-service", "notificaciones-service"]
failed = False

for svc in services:
    print(f"Auditando servicio: {svc}")
    result = audit_service(svc)
    print(result)
    if '"status": "FAIL"' in result:
        failed = True

if failed:
    sys.exit(1)