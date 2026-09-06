import os
import re

workspace_root = r"c:\IdeaProjects\DonaTrack-TP-DDS"
auditoria_path = os.path.join(workspace_root, "docs", "arquitectura", "diseno", "auditoria-final-proyecto.md")

with open(auditoria_path, "r", encoding="utf-8") as f:
    auditoria_content = f.read()

# Find section 10: "## 10. Matriz de Endpoints y Cobertura Postman"
sec10_match = re.search(r'## 10\..*?(?=\n## 11|\Z)', auditoria_content, re.DOTALL)
if sec10_match:
    sec10_text = sec10_match.group(0)
    print("Found Section 10 in auditoria-final-proyecto.md")
    
    # Table format in Section 10:
    # | Endpoint | Método | Controller | En Postman | Discrepancias |
    # | /api/donaciones | POST | DonacionesController | ...
    rows = []
    for line in sec10_text.splitlines():
        line = line.strip()
        if line.startswith("| /") and "|" in line:
            parts = [p.strip() for p in line.split("|")[1:-1]]
            if len(parts) >= 3:
                endpoint_path = parts[0].strip()
                methods_raw = parts[1].strip()
                controller = parts[2].strip()
                # methods can be "POST, GET" or "POST" or "PUT, DELETE"
                methods = [m.strip() for m in methods_raw.split(",")]
                for m in methods:
                    rows.append((m, endpoint_path, controller))
                    
    print(f"Parsed {len(rows)} endpoint methods from Section 10 tables.")
    for m, p, c in rows:
        print(f"  {m:6} | {p:45} | {c}")
else:
    print("Section 10 not found!")
