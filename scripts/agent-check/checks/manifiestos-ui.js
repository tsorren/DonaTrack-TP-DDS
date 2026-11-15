'use strict';

// Manifiestos de oleada de interfaz (ADR 20261115, paso 4): todo JSON de docs/ con
// formato "oleada-ui/1" cumple la misma regla con la que arranca el workflow oleada-ui. Con una
// decisión del usuario sin asignar a un lote ni diferida, falla (UI_WAVE_MANIFEST).

const fs = require('fs');
const path = require('path');
const { passed, failed } = require('../lib/findings');
const { SKIP_DIRS, HISTORY_PREFIXES } = require('../config');

const FORMATO = 'oleada-ui/1';

// ── Regla oleada-ui/1 ── idéntica en .claude/workflows/oleada-ui.js y en
// scripts/agent-check/checks/manifiestos-ui.js: el sandbox del workflow no hace require, así que la
// regla va copiada, y scripts/tests/run-tests.js falla si las dos copias difieren.
function problemasDelManifiesto(M) {
  const p = []
  const txt = (v) => typeof v === 'string' && v.trim() !== ''
  if (!M || typeof M !== 'object' || Array.isArray(M)) return ['el manifiesto no es un objeto']
  if (M.formato !== 'oleada-ui/1') p.push(`formato tiene que ser "oleada-ui/1" (vino ${JSON.stringify(M.formato)})`)
  if (!txt(M.nombre) || !/^[a-z0-9-]+$/.test(M.nombre)) p.push('nombre: solo minúsculas, números y guiones')
  for (const k of ['backlog', 'prefijoCommit']) if (!txt(M[k])) p.push(`falta ${k}`)
  if (!M.ramas || !txt(M.ramas.base) || !txt(M.ramas.destino) || !txt(M.ramas.prefijoOla)) p.push('ramas necesita base, destino y prefijoOla')
  const olas = Array.isArray(M.olas) && M.olas.length && M.olas.every(txt) ? M.olas : (p.push('olas: una lista de textos, con al menos una'), [])
  if (!Array.isArray(M.reglas) || !M.reglas.every(txt)) p.push('reglas: una lista de textos (puede estar vacía)')
  const lotes = Array.isArray(M.lotes) ? M.lotes : (p.push('lotes: una lista'), [])
  const ids = new Set()
  for (const l of lotes) {
    if (!l || !txt(l.id)) { p.push('hay un lote sin id'); continue }
    const n = `lote ${l.id}`
    if (ids.has(l.id)) p.push(`${n}: id repetido`)
    ids.add(l.id)
    if (!olas.includes(l.ola)) p.push(`${n}: su ola no está en olas`)
    if (!Number.isInteger(l.tanda)) p.push(`${n}: tanda tiene que ser un entero`)
    for (const k of ['items', 'modulos', 'rutasVisuales']) if (!Array.isArray(l[k])) p.push(`${n}: ${k} tiene que ser una lista`)
    for (const k of ['titulo', 'que', 'archivos']) if (!txt(l[k])) p.push(`${n}: falta ${k}`)
    if (!['QUICK', 'STANDARD', 'CONTRATO'].includes(l.ruta)) p.push(`${n}: ruta tiene que ser QUICK, STANDARD o CONTRATO`)
    if (!['ninguno', 'ficha', 'spec', 'ficha+spec'].includes(l.disenio)) p.push(`${n}: disenio tiene que ser ninguno, ficha, spec o ficha+spec`)
    if (!['juicio', 'mecanico'].includes(l.rol)) p.push(`${n}: rol tiene que ser juicio o mecanico`)
  }
  if (M.ensayo !== undefined && (!M.ensayo || !txt(M.ensayo.guia) || !Array.isArray(M.ensayo.lotes) || !M.ensayo.lotes.every((id) => ids.has(id)))) {
    p.push('ensayo necesita guia y lotes que existan')
  }
  const dids = new Set()
  for (const d of Array.isArray(M.decisiones) ? M.decisiones : (p.push('decisiones: una lista (puede estar vacía)'), [])) {
    if (!d || !txt(d.id)) { p.push('hay una decisión sin id'); continue }
    const n = `decisión ${d.id}`
    if (dids.has(d.id)) p.push(`${n}: id repetido`)
    dids.add(d.id)
    for (const k of ['texto', 'quien', 'ok']) if (!txt(d[k])) p.push(`${n}: falta ${k}`)
    const enLote = d.lote !== undefined
    const diferida = d.diferida !== undefined
    if (enLote === diferida) {
      p.push(`${n}: ${enLote ? 'tiene lote y diferida a la vez' : 'sin asignar: va a un lote con su criterio comprobable, o queda diferida con quién la difirió y dónde consta el OK'}`)
    } else if (enLote) {
      if (!ids.has(d.lote)) p.push(`${n}: el lote ${d.lote} no existe`)
      if (!txt(d.criterio)) p.push(`${n}: falta el criterio comprobable`)
    } else if (!d.diferida || !txt(d.diferida.quien) || !txt(d.diferida.ok)) {
      p.push(`${n}: diferida necesita quien y ok`)
    }
  }
  return p
}
// ── fin de la regla oleada-ui/1 ──

function jsonDeDocs(repoRoot) {
  const salida = [];
  const recorrer = (dir) => {
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      const full = path.join(dir, e.name);
      const rel = path.relative(repoRoot, full).split(path.sep).join('/');
      if (e.isDirectory()) {
        if (!SKIP_DIRS.has(e.name) && !HISTORY_PREFIXES.some((p) => rel.startsWith(p))) recorrer(full);
      } else if (e.name.endsWith('.json')) {
        salida.push([rel, full]);
      }
    }
  };
  const docs = path.join(repoRoot, 'docs');
  if (fs.existsSync(docs)) recorrer(docs);
  return salida;
}

function checkManifiestosUi(repoRoot) {
  const findings = [];
  for (const [rel, full] of jsonDeDocs(repoRoot)) {
    const contenido = fs.readFileSync(full, 'utf8');
    if (!contenido.includes(FORMATO)) continue;
    let manifiesto;
    try {
      manifiesto = JSON.parse(contenido);
    } catch (e) {
      findings.push(failed('UI_WAVE_MANIFEST', `${rel}: invalid JSON (${e.message})`, rel));
      continue;
    }
    if (!manifiesto || manifiesto.formato !== FORMATO) continue;
    const problemas = problemasDelManifiesto(manifiesto);
    findings.push(problemas.length
      ? failed('UI_WAVE_MANIFEST', `${rel}: ${problemas.join('; ')}`, rel)
      : passed('UI_WAVE_MANIFEST', `${rel}: ${FORMATO} OK`));
  }
  return findings.length ? findings : [passed('UI_WAVE_MANIFEST', `No ${FORMATO} manifests under docs/`)];
}

module.exports = { checkManifiestosUi, problemasDelManifiesto };
