'use strict';

// Harness de interfaz (ADR 20261115, opción A): las tres skills cortas existen y nadie pasa su tope
// de líneas. Los topes son los de la decisión; se cambian con un ADR, no para que pase el check.

const fs = require('fs');
const path = require('path');
const { passed, failed } = require('../lib/findings');

const TOPES = [
  ['.agents/skills/construir-ui/SKILL.md', 60],
  ['.agents/skills/revisar-ui/SKILL.md', 60],
  ['.agents/skills/explorar-ui/SKILL.md', 60],
  ['.claude/rules/interfaz.md', 10],
  ['cliente-liviano/AGENTS.md', 80],
];

function checkInterfazHarness(repoRoot) {
  return TOPES.map(([rel, tope]) => {
    const full = path.join(repoRoot, rel);
    if (!fs.existsSync(full)) {
      return failed('UI_HARNESS_SIZE', `Missing UI harness file: ${rel}`, rel);
    }
    const lineas = fs.readFileSync(full, 'utf8').replace(/\r?\n$/, '').split(/\r?\n/).length;
    return lineas <= tope
      ? passed('UI_HARNESS_SIZE', `${rel}: ${lineas} lines (cap ${tope})`)
      : failed('UI_HARNESS_SIZE', `${rel}: ${lineas} lines, cap ${tope} (ADR 20261115)`, rel);
  });
}

module.exports = { checkInterfazHarness, TOPES };
