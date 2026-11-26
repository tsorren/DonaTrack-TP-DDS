// Arma guia-y-pantallas.html a partir de partes/ y comprueba que no haya colores literales
// fuera de los dos bloques de tokens. Uso: node armar.js
const fs = require('fs');
const path = require('path');
const aqui = __dirname;
const P = n => fs.readFileSync(path.join(aqui, 'partes', n), 'utf8');
const SALIDA = path.join(aqui, 'guia-y-pantallas.html');
const TOKENS_REPO = path.join(aqui, '..', '..', '..', 'cliente-liviano', 'src', 'main', 'resources', 'static', 'css', 'tokens.css');

const tokens = P('05-tokens.css');
const lamTokens = P('01-lamina-tokens.css');
const cssProducto = P('08-dibujo.css') + '\n' + P('10-componentes.css') + '\n' + P('15-pantallas.css');
const cssLamina = P('02-lamina.css');
const htmls = ['20-simbolos.html', '30-inicio.html', '40-guia.html', '50-pantallas.html', '90-script.html'].map(n => [n, P(n)]);

// ---- 1. Colores literales fuera de los bloques de tokens ----
const NOMBRES = 'white|black|red|green|blue|yellow|orange|gray|grey|silver|purple|pink|brown|beige|ivory|tan|olive|teal|navy|maroon|gold';
function literales(nombre, texto, esHtml) {
  let t = texto.replace(/\/\*[\s\S]*?\*\//g, '').replace(/<!--[\s\S]*?-->/g, '');
  if (esHtml) { t = t.replace(/href="#[^"]*"/g, '').replace(/url\(#[^)]*\)/g, ''); }
  const hallados = [];
  const re = [/#[0-9a-fA-F]{3,8}\b/g, /\b(?:rgb|rgba|hsl|hsla|oklch|oklab|lab|lch)\(/g];
  re.forEach(r => (t.match(r) || []).forEach(m => hallados.push(m)));
  if (esHtml) {
    (t.match(/\sstyle="[^"]*"/g) || []).forEach(m => hallados.push(m.trim()));
    (t.match(/\s(?:fill|stroke|color|bgcolor|stop-color)="(?!none|currentColor)[^"]*"/g) || []).forEach(m => hallados.push(m.trim()));
  } else {
    (t.match(new RegExp('(?:color|background|border|outline|fill|stroke|shadow)[a-z-]*\\s*:[^;{}]*\\b(?:' + NOMBRES + ')\\b', 'g')) || []).forEach(m => hallados.push(m));
  }
  return hallados.map(h => nombre + ': ' + h);
}
let fallas = [];
fallas = fallas.concat(literales('08+10+15 (producto)', cssProducto, false), literales('02-lamina.css', cssLamina, false));
htmls.forEach(([n, h]) => { fallas = fallas.concat(literales(n, h.replace(/<script>[\s\S]*?<\/script>/g, ''), true)); });
// en el script: ni hex ni funciones de color
fallas = fallas.concat(literales('90-script (js)', (P('90-script.html').match(/<script>[\s\S]*?<\/script>/) || [''])[0].replace(/'#[a-z][a-z0-9-]*'/g, ''), false));

// ---- 2. Un solo bloque de tokens del producto: una sola regla :root ----
const reglasRoot = (tokens.replace(/\/\*[\s\S]*?\*\//g, '').match(/[^{}]+\{/g) || []).map(s => s.trim());
if (reglasRoot.length !== 1 || reglasRoot[0] !== ':root {') { fallas.push('05-tokens.css: tiene que ser una sola regla :root (hay: ' + reglasRoot.join(' | ') + ')'); }

// ---- 3. Comparación con tokens.css del repo (solo lectura) ----
function leer(css) {
  const m = {}; const re = /(--[a-z0-9-]+)\s*:\s*([^;]+);/g; let x;
  const limpio = css.replace(/\/\*[\s\S]*?\*\//g, '');
  while ((x = re.exec(limpio))) { m[x[1]] = x[2].trim().replace(/\s+/g, ' '); }
  return m;
}
function resolver(mapa, valor, hondo) {
  if ((hondo || 0) > 12) { return valor; }
  return valor.replace(/var\((--[a-z0-9-]+)\)/g, (todo, n) => (n in mapa ? resolver(mapa, mapa[n], (hondo || 0) + 1) : todo));
}
const mios = leer(tokens); const repo = leer(fs.readFileSync(TOKENS_REPO, 'utf8'));
const comparacion = {}; const cuenta = { igual: 0, cambia: 0, nuevo: 0 };
Object.keys(mios).forEach(n => {
  const mio = resolver(mios, mios[n]).toLowerCase();
  if (!(n in repo)) { comparacion[n] = { m: 'nuevo' }; cuenta.nuevo++; return; }
  const suyo = resolver(repo, repo[n]).toLowerCase();
  if (mio === suyo) { comparacion[n] = { m: 'igual' }; cuenta.igual++; } else { comparacion[n] = { m: 'cambia', antes: resolver(repo, repo[n]) }; cuenta.cambia++; }
});

// ---- 4. Armado ----
const escapar = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
const comprobacionPath = path.join(aqui, 'comprobacion.html');
const comprobacion = fs.existsSync(comprobacionPath) ? fs.readFileSync(comprobacionPath, 'utf8') : '<tr><td colspan="3">La comprobación se completa al cerrar las pantallas.</td></tr>';
let cuerpo = htmls.map(([, h]) => h).join('\n');
const usoRepo = Object.keys(repo).filter(n => !n.startsWith('--p-'));
const ausentes = usoRepo.filter(n => !(n in mios));
const usoMios = Object.keys(mios).filter(n => !n.startsWith('--p-'));
const htmlAusentes = '          <li>Hoy <code>tokens.css</code> tiene ' + usoRepo.length + ' tokens de uso. Este bloque trae ' + (usoRepo.length - ausentes.length) + ' de ellos y suma ' + usoMios.filter(n => !(n in repo)).length + ' nuevos. Los otros ' + ausentes.length + ' no están acá porque estas pantallas no los usan. Ninguno se renombra: siguen en <code>tokens.css</code> mientras alguna pantalla del producto los use, y se retiran recién cuando esa pantalla migre. Por ejemplo <code>--color-acento</code>, que hoy tiene el mismo valor que <code>--color-accion-primaria</code> y se usa en muchas hojas. Tampoco están ' + Object.keys(repo).filter(n => n.startsWith('--p-') && !(n in mios)).length + ' valores base (<code>--p-</code>) que solo alimentan a esos tokens.<details><summary class="boton-lam">Ver los ' + ausentes.length + ' que no están en el bloque</summary><p class="lam-chico">' + ausentes.map(n => '<code>' + n + '</code>').join(', ') + '</p></details></li>';
cuerpo = cuerpo.replace('<!--TOKENS-->', escapar(tokens)).replace('<!--COMPROBACION-->', comprobacion).replace('<!--AUSENTES-->', htmlAusentes);
cuerpo = cuerpo.replace('<script>\n(function () {', '<script>\nwindow.DT_COMPARACION = ' + JSON.stringify(comparacion) + ';\n(function () {');
const salida = [
  '<title>Guía de estilo y pantallas</title>',
  '<meta name="claude:viewport-fit-sides" content="cover">',
  '<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Archivo:ital,wdth,wght@0,62..125,100..900&family=DM+Sans:opsz,wght@9..40,300..900&display=swap">',
  '<style id="lamina-tokens">\n' + lamTokens + '</style>',
  '<style id="tokens">\n' + tokens + '</style>',
  '<style id="producto">\n' + cssProducto + '</style>',
  '<style id="lamina">\n' + cssLamina + '</style>',
  cuerpo
].join('\n');
fs.mkdirSync(path.dirname(SALIDA), { recursive: true });
fs.writeFileSync(SALIDA, salida);

// ---- 5. Informe ----
const ids = (salida.match(/\sid="([^"]+)"/g) || []).map(s => s.slice(5, -1));
const repetidos = ids.filter((x, i) => ids.indexOf(x) !== i);
if (repetidos.length) { fallas.push('ids repetidos: ' + [...new Set(repetidos)].join(', ')); }
const anclas = [...new Set((salida.match(/href="#([^"]+)"/g) || []).map(s => s.slice(7, -1)))];
const rotas = anclas.filter(a => !ids.includes(a));
if (rotas.length) { fallas.push('anclas sin destino: ' + rotas.join(', ')); }
console.log('Salida:', SALIDA, '(' + Math.round(salida.length / 1024) + ' KB)');
console.log('Tokens del producto:', Object.keys(mios).length, JSON.stringify(cuenta));
console.log('Tokens de uso del repo:', usoRepo.length, '· presentes en el bloque:', usoRepo.length - ausentes.length, '· ausentes:', ausentes.length, '· de uso propios:', usoMios.length, '· de uso nuevos:', usoMios.filter(n => !(n in repo)).length);
if (repo['--color-acento'] === undefined || resolver(repo, repo['--color-acento']) !== resolver(repo, repo['--color-accion-primaria'])) { fallas.push('el ejemplo de --color-acento ya no vale'); }
console.log('Cambian:', Object.keys(comparacion).filter(n => comparacion[n].m === 'cambia').map(n => n + ' (antes ' + comparacion[n].antes + ')').join('; ') || 'ninguno');
console.log(fallas.length ? 'FALLAS:\n  ' + fallas.join('\n  ') : 'Sin colores literales fuera de los bloques de tokens; ids únicos; anclas con destino.');
process.exit(fallas.length ? 1 : 0);
