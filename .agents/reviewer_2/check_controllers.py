import os
import re

workspace_root = r"c:\IdeaProjects\DonaTrack-TP-DDS"

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

endpoints = []

for full_path, content in sorted(controllers):
    rel_path = os.path.relpath(full_path, workspace_root)
    file_name = os.path.basename(full_path)
    service = rel_path.split(os.sep)[0]

    # Split into class header (before 'public class ...') and class body
    class_match = re.search(r'public\s+(?:class|record)\s+(\w+)', content)
    if not class_match:
        continue
    
    header = content[:class_match.start()]
    body = content[class_match.end():]

    # Base path from header @RequestMapping
    base_match = re.search(r'@RequestMapping\s*\(\s*(?:value\s*=\s*|path\s*=\s*)?["\']([^"\']*)["\']', header)
    base_path = base_match.group(1).rstrip('/') if base_match else ""

    # Parse methods in body
    # Regex to find annotations and following method signature
    method_pattern = re.compile(
        r'(@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\s*(?:\((.*?)\))?)\s*(?:@[A-Za-z0-9_]+(?:\(.*?\))?\s*)*public\s+[^\(\)]+\s+(\w+)\s*\((.*?)\)',
        re.DOTALL
    )

    for m in method_pattern.finditer(body):
        anno_full = m.group(1)
        anno_type = m.group(2)
        anno_args = m.group(3) or ""
        method_name = m.group(4)
        params = m.group(5)

        # Determine HTTP method
        http_method = anno_type.replace("Mapping", "").upper()
        if http_method == "REQUEST":
            method_match = re.search(r'method\s*=\s*RequestMethod\.([A-Z]+)', anno_args)
            http_method = method_match.group(1) if method_match else "ALL"

        # Determine path(s)
        # Could be path = "/foo", value = "/foo", or just "/foo", or {"/foo", "/bar"}
        paths = re.findall(r'["\']([^"\']+)["\']', anno_args)
        # Filter out anything that is an attribute name or non-path if any
        # Actually in Spring annotations, string literals in Mapping are almost always paths
        paths = [p for p in paths if p.startswith('/') or p == ""]
        if not paths:
            paths = [""]

        for p in paths:
            sub = p.strip()
            if sub:
                full_ep = (base_path + "/" + sub.lstrip("/")).replace("//", "/")
            else:
                full_ep = base_path if base_path else "/"
            endpoints.append({
                "service": service,
                "file": file_name,
                "method": http_method,
                "path": full_ep,
                "java_method": method_name
            })

print(f"Total precise endpoints parsed: {len(endpoints)}")
print(f"{'SERVICE':22} | {'HTTP':6} | {'PATH':45} | {'METHOD':25} | {'FILE'}")
print("-" * 120)
for ep in sorted(endpoints, key=lambda x: (x["service"], x["path"], x["method"])):
    print(f"{ep['service']:22} | {ep['method']:6} | {ep['path']:45} | {ep['java_method']:25} | {ep['file']}")
