import os
from pathlib import Path

root = Path('.').resolve()
all_md = []
for p in root.rglob('*.md'):
    rel = p.relative_to(root).as_posix()
    if rel.startswith('.agents/') or rel.startswith('.git/') or '/target/' in f'/{rel}' or rel.startswith('target/'):
        continue
    all_md.append((rel, p))

all_md.sort()

triple_odd = []
single_odd = []

for rel, p in all_md:
    with open(p, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    c3 = content.count('`')
    if c3 % 2 != 0:
        triple_odd.append((rel, c3))
    c1 = content.count('')
    if c1 % 2 != 0:
        single_odd.append((rel, c1))

print('Total files checked: ' + str(len(all_md)))
print('Files with ODD count of TRIPLE backticks (`): ' + str(len(triple_odd)))
for r, c in triple_odd:
    print('  TRIPLE: ' + r + ' (' + str(c) + ')')

print('Files with ODD count of SINGLE backticks (): ' + str(len(single_odd)))
for r, c in single_odd:
    print('  SINGLE: ' + r + ' (' + str(c) + ')')
