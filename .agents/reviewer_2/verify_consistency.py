import os
import re

workspace_root = r"c:\IdeaProjects\DonaTrack-TP-DDS"
auditoria_path = os.path.join(workspace_root, "docs", "arquitectura", "diseno", "auditoria-final-proyecto.md")
with open(auditoria_path, "r", encoding="utf-8") as f:
    auditoria_content = f.read()

contratos_path = os.path.join(workspace_root, "docs", "arquitectura", "contratos-rest.md")
with open(contratos_path, "r", encoding="utf-8") as f:
    contratos_content = f.read()

# Check every endpoint mentioned in auditoria-final-proyecto.md
# Search for patterns like `(GET|POST|PUT|DELETE|PATCH)\s+([/a-zA-Z0-9_\-{}]+)`
# or `| (/api/[^ |]+) |`
found_paths = set(re.findall(r'(/api/[a-zA-Z0-9_\-{}/]+|/donaciones-independientes[a-zA-Z0-9_\-{}/]*|/notificaciones[a-zA-Z0-9_\-{}/]*)', auditoria_content))

# Clean up path variables formatting: e.g. {id} vs {donanteId} vs {personaId}
def normalize_path(p):
    p = p.rstrip("/")
    # normalize path params like {id}, {donanteId}, {personaId}, {alias}, {nombreInsignia} -> {*}
    return re.sub(r'\{[^}]+\}', '{*}', p)

from check_controllers import endpoints as java_eps

java_normalized = set((ep["method"], normalize_path(ep["path"])) for ep in java_eps)
doc_normalized = set()

for line in contratos_content.splitlines():
    line = line.strip()
    if line.startswith("|") and "`" in line:
        parts = [p.strip() for p in line.split("|")[1:-1]]
        if len(parts) >= 2:
            m_method = re.search(r'`([A-Z]+)`', parts[0])
            m_path = re.search(r'`([^`]+)`', parts[1])
            if m_method and m_path:
                method = m_method.group(1)
                path = normalize_path(m_path.group(1))
                doc_normalized.add((method, path))

print(f"Java unique normalized (method, path): {len(java_normalized)}")
print(f"Doc (contratos-rest.md) unique normalized (method, path): {len(doc_normalized)}")

diff_java_doc = java_normalized - doc_normalized
diff_doc_java = doc_normalized - java_normalized

print("Diff Java - Doc:", diff_java_doc)
print("Diff Doc - Java:", diff_doc_java)

assert len(diff_java_doc) == 0
assert len(diff_doc_java) == 0
print("PERFECT MATCH: 0 discrepancies between Java and contratos-rest.md!")
