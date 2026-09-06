import os
import re
import sys
from pathlib import Path
import urllib.parse

def normalize_file_uri(uri_str, workspace_root):
    # Handle file:///c:/... or file:///C:/...
    if uri_str.startswith('file:///'):
        path_part = uri_str[len('file:///'):]
        # Remove any anchor (#...)
        if '#' in path_part:
            path_part = path_part.split('#')[0]
        # unquote
        path_part = urllib.parse.unquote(path_part)
        return Path(path_part)
    return None

def check_markdown_links(docs_root, workspace_root="."):
    docs_path = Path(docs_root).resolve()
    ws_path = Path(workspace_root).resolve()
    print(f"Docs Root: {docs_path}")
    print(f"Workspace Root: {ws_path}")
    
    md_files = list(docs_path.rglob("*.md"))
    print(f"Found {len(md_files)} markdown files in docs/.")
    
    # Matches [text](link) and ![alt](img)
    link_pattern = re.compile(r'(?<!!)\[([^\]]+)\]\(([^)]+)\)')
    img_pattern = re.compile(r'!\[([^\]]*)\]\(([^)]+)\)')
    
    total_links = 0
    broken_links = []
    
    for md_file in md_files:
        try:
            with open(md_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
        except Exception as e:
            print(f"Error reading {md_file}: {e}")
            continue
            
        links = link_pattern.findall(content)
        imgs = img_pattern.findall(content)
        
        all_targets = [(text, target, 'link') for text, target in links] + [(alt, target, 'img') for alt, target in imgs]
        
        for text, raw_target, link_type in all_targets:
            target = raw_target.strip()
            
            if not target:
                continue
            if target.startswith('#'):
                continue
            if target.startswith('http://') or target.startswith('https://') or target.startswith('mailto:'):
                continue
            if target.startswith('javascript:'):
                continue
                
            total_links += 1
            
            # Handle file:/// URIs
            if target.startswith('file:'):
                target_file = normalize_file_uri(target, ws_path)
            else:
                # Remove anchor
                target_without_anchor = target.split('#')[0].split('?')[0]
                if not target_without_anchor:
                    # Target was only anchor/query
                    continue
                target_path_str = urllib.parse.unquote(target_without_anchor)
                
                if target_path_str.startswith('/'):
                    # Relative to workspace root or docs root
                    p_ws = (ws_path / target_path_str.lstrip('/')).resolve()
                    p_docs = (docs_path / target_path_str.lstrip('/')).resolve()
                    if p_ws.exists():
                        target_file = p_ws
                    elif p_docs.exists():
                        target_file = p_docs
                    else:
                        target_file = p_ws
                else:
                    target_file = (md_file.parent / target_path_str).resolve()
                    
            if not target_file.exists():
                broken_links.append({
                    'source': str(md_file.relative_to(ws_path)),
                    'type': link_type,
                    'text': text,
                    'raw_target': raw_target,
                    'resolved_target': str(target_file)
                })
                
    print(f"\nTotal relative/local links checked: {total_links}")
    print(f"Broken links found: {len(broken_links)}")
    
    if broken_links:
        print("\n=== BROKEN LINKS LIST ===")
        for i, b in enumerate(broken_links, 1):
            print(f"[{i}] File: {b['source']}")
            print(f"    Text: {b['text']}")
            print(f"    Target: {b['raw_target']}")
            print(f"    Resolved: {b['resolved_target']}")
        return False, broken_links
    else:
        print("\nAll relative markdown links resolved successfully! (0 broken links)")
        return True, []

if __name__ == '__main__':
    docs_dir = sys.argv[1] if len(sys.argv) > 1 else 'docs'
    ws_dir = sys.argv[2] if len(sys.argv) > 2 else '.'
    success, _ = check_markdown_links(docs_dir, ws_dir)
    sys.exit(0 if success else 1)
