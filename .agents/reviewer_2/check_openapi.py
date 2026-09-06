import os
import re

workspace_root = r"c:\IdeaProjects\DonaTrack-TP-DDS"
contratos_dir = os.path.join(workspace_root, "docs", "arquitectura", "contratos")

oas_files = [
    "openapi-donaciones.yaml",
    "openapi-logistica.yaml",
    "openapi-incentivos.yaml",
    "openapi-notificaciones.yaml"
]

def normalize_path(p):
    p = p.rstrip("/")
    return re.sub(r'\{[^}]+\}', '{*}', p)

oas_endpoints = set()

for f in oas_files:
    full_p = os.path.join(contratos_dir, f)
    with open(full_p, "r", encoding="utf-8") as yf:
        lines = yf.readlines()
    
    current_path = None
    in_paths = False
    for line in lines:
        if line.startswith("paths:"):
            in_paths = True
            continue
        if in_paths and re.match(r'^[a-zA-Z]', line):
            # Out of paths section
            in_paths = False
            continue
        if in_paths:
            m_path = re.match(r'^  (["\']?/[^:"\']+["\']?):', line)
            if m_path:
                current_path = m_path.group(1).strip("\"'")
                continue
            m_method = re.match(r'^    (get|post|put|delete|patch):', line)
            if m_method and current_path:
                method = m_method.group(1).upper()
                oas_endpoints.add((method, normalize_path(current_path)))

print(f"Total endpoints in OpenAPI YAMLs: {len(oas_endpoints)}")

from check_controllers import endpoints as java_eps
java_normalized = set((ep["method"], normalize_path(ep["path"])) for ep in java_eps)

diff_java_oas = java_normalized - oas_endpoints
diff_oas_java = oas_endpoints - java_normalized

print(f"In Java but not in OpenAPI: {len(diff_java_oas)}")
for m, p in sorted(diff_java_oas):
    print(f"  + Java only: {m} {p}")

print(f"In OpenAPI but not in Java: {len(diff_oas_java)}")
for m, p in sorted(diff_oas_java):
    print(f"  - OAS only: {m} {p}")
