import os
import re

workspace_root = r"c:\IdeaProjects\DonaTrack-TP-DDS"

# 1. Parse Java endpoints
controllers = []
for root, dirs, files in os.walk(workspace_root):
    if "target" in root or ".git" in root or ".agents" in root:
        continue
    for file in files:
        if file.endswith(".java"):
            full_path = os.path.join(root, file)
            with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            if "@RestController" in content:
                controllers.append((full_path, content))

java_endpoints = set()
java_list = []

for full_path, content in sorted(controllers):
    rel_path = os.path.relpath(full_path, workspace_root)
    file_name = os.path.basename(full_path)
    service = rel_path.split(os.sep)[0]

    class_match = re.search(r'public\s+(?:class|record)\s+(\w+)', content)
    if not class_match:
        continue
    header = content[:class_match.start()]
    body = content[class_match.end():]

    base_match = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*|path\s*=\s*)?["\']([^"\']*)["\']', header)
    base_path = base_match.group(1).rstrip('/') if base_match else ""

    method_pattern = re.compile(
        r'(@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\s*(?:\((.*?)\))?)\s*(?:@[A-Za-z0-9_]+(?:\(.*?\))?\s*)*public\s+[^\(\)]+\s+(\w+)\s*\((.*?)\)',
        re.DOTALL
    )

    for m in method_pattern.finditer(body):
        anno_full = m.group(1)
        anno_type = m.group(2)
        anno_args = m.group(3) or ""
        method_name = m.group(4)

        http_method = anno_type.replace("Mapping", "").upper()
        if http_method == "REQUEST":
            method_match = re.search(r'method\s*=\s*RequestMethod\.([A-Z]+)', anno_args)
            http_method = method_match.group(1) if method_match else "ALL"

        paths = re.findall(r'["\']([^"\']+)["\']', anno_args)
        paths = [p for p in paths if p.startswith('/') or p == ""]
        if not paths:
            paths = [""]

        for p in paths:
            sub = p.strip()
            if sub:
                full_ep = (base_path + "/" + sub.lstrip("/")).replace("//", "/")
            else:
                full_ep = base_path if base_path else "/"
            key = (http_method, full_ep)
            java_endpoints.add(key)
            java_list.append((http_method, full_ep, service, file_name, method_name))

print(f"Total Java endpoints: {len(java_endpoints)}")

# 2. Parse docs/arquitectura/contratos-rest.md table rows
# Format: | `POST` | `/api/donaciones` | ...
contratos_path = os.path.join(workspace_root, "docs", "arquitectura", "contratos-rest.md")
with open(contratos_path, "r", encoding="utf-8") as f:
    contratos_content = f.read()

doc_endpoints = set()
for line in contratos_content.splitlines():
    line = line.strip()
    if line.startswith("|") and "`" in line:
        parts = [p.strip() for p in line.split("|")[1:-1]]
        if len(parts) >= 2:
            # Check if part 0 has method and part 1 has path
            m_method = re.search(r'`([A-Z]+)`', parts[0])
            m_path = re.search(r'`([^`]+)`', parts[1])
            if m_method and m_path:
                method = m_method.group(1)
                path = m_path.group(1)
                doc_endpoints.add((method, path))

print(f"Total doc endpoints in contratos-rest.md: {len(doc_endpoints)}")

# Discrepancies
in_java_not_doc = java_endpoints - doc_endpoints
in_doc_not_java = doc_endpoints - java_endpoints

print(f"\nDiscrepancies:")
print(f"In Java but NOT in contratos-rest.md: {len(in_java_not_doc)}")
for m, p in sorted(in_java_not_doc):
    print(f"  + Java only: {m} {p}")

print(f"In contratos-rest.md but NOT in Java: {len(in_doc_not_java)}")
for m, p in sorted(in_doc_not_java):
    print(f"  - Doc only: {m} {p}")

