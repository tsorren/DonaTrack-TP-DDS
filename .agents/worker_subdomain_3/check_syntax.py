import glob
import os
import re

adr_files = glob.glob('docs/adr/**/*.md', recursive=True)
issues = []
fence_re = re.compile(r'^[ \t]{0,3}(`{3,}|~{3,})')

for f in sorted(adr_files):
    with open(f, 'r', encoding='utf-8') as fp:
        lines = fp.readlines()

    in_block = False
    fence_char = None
    fence_len = 0

    for idx, line in enumerate(lines):
        m = fence_re.match(line)
        if m:
            chars = m.group(1)
            char = chars[0]
            flen = len(chars)
            if not in_block:
                in_block = True
                fence_char = char
                fence_len = flen
            else:
                if char == fence_char and flen >= fence_len:
                    in_block = False
        else:
            if not in_block:
                if re.match(r'^[ \t]{0,3}#{1,6}[^ \t\n#]', line):
                    issues.append((f, idx + 1, 'Malformed header missing space', line.strip()))

    if in_block:
        issues.append((f, len(lines), 'Unclosed code block', ''))

print(f'Total ADR files checked: {len(adr_files)}')
print(f'Total issues: {len(issues)}')
for iss in issues:
    print(iss)
