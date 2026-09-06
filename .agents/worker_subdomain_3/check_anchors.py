import glob
import os
import re

adr_files = glob.glob('docs/adr/**/*.md', recursive=True)
anchor_issues = []

def slugify(header):
    # CommonMark / GitHub slugify: lowercase, strip punctuation, spaces to hyphens
    h = header.strip().lower()
    # remove chars that are not alphanumeric or hyphen or space
    h = re.sub(r'[^\w\s-]', '', h)
    # replace whitespace with single hyphens
    h = re.sub(r'[-\s]+', '-', h).strip('-')
    return h

for f in sorted(adr_files):
    with open(f, 'r', encoding='utf-8') as fp:
        content = fp.read()

    links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
    for text, target in links:
        if target.startswith('http://') or target.startswith('https://') or target.startswith('mailto:'):
            continue
        if '#' not in target:
            continue
        parts = target.split('#')
        target_file_rel = parts[0]
        anchor = parts[1]
        if not target_file_rel:
            target_file_path = f
        else:
            target_file_path = os.path.normpath(os.path.join(os.path.dirname(f), target_file_rel))

        if not os.path.exists(target_file_path):
            anchor_issues.append((f, target, 'Target file does not exist', target_file_path))
            continue

        if target_file_path.endswith('.md'):
            with open(target_file_path, 'r', encoding='utf-8') as tf:
                target_lines = tf.readlines()
            # find all headers
            headers = [line.strip().lstrip('#').strip() for line in target_lines if line.strip().startswith('#')]
            slugs = [slugify(h) for h in headers]
            if anchor not in slugs:
                # check if line number anchor e.g. L18-L46
                if re.match(r'^L\d+(-L\d+)?$', anchor):
                    continue
                anchor_issues.append((f, target, f'Anchor #{anchor} not found in {target_file_path}', slugs))

print(f'Anchor issues found: {len(anchor_issues)}')
for iss in anchor_issues:
    print(iss[0], iss[1], iss[2])
