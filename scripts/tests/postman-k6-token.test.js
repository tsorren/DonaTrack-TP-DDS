'use strict';

// Tests de la lógica de token de Postman y k6 (SPEC-06 E2-07). Corre con Node, sin dependencias:
//   node scripts/tests/postman-k6-token.test.js
// Lee los archivos reales del repo: el script de pre-request de cada colección de postman/ y
// tests/performance/k6/token.js. Postman (`pm`) y k6 (`k6/http`) se reemplazan por dobles.

const fs = require('fs');
const os = require('os');
const path = require('path');

const RAIZ = path.resolve(__dirname, '..', '..');
let pasaron = 0;
let fallaron = 0;
function check(nombre, ok) {
  if (ok) { console.log(`  [PASS] ${nombre}`); pasaron++; } else { console.error(`  [FAIL] ${nombre}`); fallaron++; }
}

// ─── Postman ────────────────────────────────────────────────────────────────

const colecciones = fs.readdirSync(path.join(RAIZ, 'postman')).filter((f) => /^flujo-.*\.json$/.test(f)).sort();

function scriptDe(archivo) {
  const d = JSON.parse(fs.readFileSync(path.join(RAIZ, 'postman', archivo), 'utf8'));
  const evento = (d.event || []).find((e) => e.listen === 'prerequest');
  return evento ? evento.script.exec.join('\n') : null;
}

function correr(codigo, { vars, url, respuesta, error = null, colVars = {} }) {
  const cabeceras = {};
  const pedidos = [];
  const res = respuesta || { code: 200, json: () => ({ access_token: 'tok-1', expires_in: 900 }) };
  const pm = {
    variables: { get: (k) => vars[k], replaceIn: (s) => s.replace(/\{\{(\w+)\}\}/g, (_, k) => vars[k] ?? '') },
    collectionVariables: { get: (k) => colVars[k], set: (k, v) => { colVars[k] = v; } },
    request: { url: { toString: () => url }, headers: { upsert: (h) => { cabeceras[h.key] = h.value; } } },
    sendRequest: (opciones, cb) => { pedidos.push(opciones); cb(error, res); },
  };
  let lanzo = null;
  try { new Function('pm', codigo)(pm); } catch (e) { lanzo = e; }
  return { cabeceras, pedidos, colVars, lanzo };
}

const servicios = {
  baseUrl: 'http://localhost:8080', donacionesUrl: 'http://localhost:8080', notificacionesUrl: 'http://localhost:8081',
  incentivosUrl: 'http://localhost:8082', logisticaUrl: 'http://localhost:8083', logisticaExternoUrl: 'http://localhost:8084',
};
const vigente = () => ({ pruebasToken: 'tok-guardado', pruebasTokenVence: String(Date.now() + 600000) });

console.log('\n── Postman: una colección por vez ──');
check('hay 9 colecciones', colecciones.length === 9);
for (const archivo of colecciones) {
  const codigo = scriptDe(archivo);
  const t = (n) => `${archivo}: ${n}`;
  if (!codigo) { check(t('tiene script de pre-request'), false); continue; }

  let r = correr(codigo, { vars: { ...servicios }, url: '{{donacionesUrl}}/api/personas' });
  check(t('sin pruebasClientSecret no hace nada'), !Object.keys(r.cabeceras).length && !r.pedidos.length && !r.lanzo);

  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: '{{donacionesUrl}}/api/personas' });
  check(t('pide client_credentials al cliente de pruebas y manda el Bearer'),
    r.pedidos.length === 1 && r.cabeceras.Authorization === 'Bearer tok-1' &&
    JSON.stringify(r.pedidos[0].body.urlencoded.map((c) => c.key + '=' + c.value)) ===
      JSON.stringify(['grant_type=client_credentials', 'client_id=donatrack-pruebas', 'client_secret=s3cr']));

  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: '{{logisticaUrl}}/api/entregas', colVars: vigente() });
  check(t('reusa el token vigente sin volver a pedirlo'), !r.pedidos.length && r.cabeceras.Authorization === 'Bearer tok-guardado');

  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: '{{logisticaUrl}}/api/entregas',
    colVars: { pruebasToken: 'viejo', pruebasTokenVence: String(Date.now() + 10000) } });
  check(t('a menos de 30 s de vencer renueva'), r.pedidos.length === 1 && r.cabeceras.Authorization === 'Bearer tok-1');

  // El token ADMIN nunca sale hacia el proveedor externo ni hacia un host parecido (límite de frontera).
  for (const ajena of ['{{logisticaExternoUrl}}/api/proveedor/envios', 'https://httpbin.org/post', 'http://localhost:80801/x',
    'http://localhost:8080.evil.com/x', 'http://localhost:8080@evil.com/x']) {
    r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: ajena, colVars: vigente() });
    check(t(`no manda el token a ${ajena}`), !('Authorization' in r.cabeceras) && !r.pedidos.length);
  }
  for (const propia of ['http://localhost:8080', 'http://localhost:8080/api/x', 'http://localhost:8080?a=1']) {
    r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: propia, colVars: vigente() });
    check(t(`sí lo manda a ${propia}`), r.cabeceras.Authorization === 'Bearer tok-guardado');
  }

  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: '{{donacionesUrl}}/api/x', respuesta: { code: 401, json: () => ({}) } });
  check(t('un 401 de Keycloak corta, con mensaje claro y sin el secreto'),
    r.lanzo && /HTTP 401/.test(r.lanzo.message) && !r.lanzo.message.includes('s3cr'));
  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr' }, url: '{{donacionesUrl}}/api/x', error: new Error('ECONNREFUSED') });
  check(t('Keycloak caído corta con la causa'), r.lanzo && /ECONNREFUSED/.test(r.lanzo.message));
  r = correr(codigo, { vars: { ...servicios, pruebasClientSecret: 's3cr', keycloakTokenUrl: 'http://keycloak:8080/t', pruebasClientId: 'otro' }, url: '{{donacionesUrl}}/api/x' });
  check(t('respeta keycloakTokenUrl y pruebasClientId'), r.pedidos[0].url === 'http://keycloak:8080/t' && r.pedidos[0].body.urlencoded[1].value === 'otro');
}

// ─── k6 ─────────────────────────────────────────────────────────────────────

async function k6() {
  console.log('\n── k6: token.js ──');
  const fuente = fs.readFileSync(path.join(RAIZ, 'tests', 'performance', 'k6', 'token.js'), 'utf8');
  const tmp = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'k6-token-')), 'token.mjs');
  fs.writeFileSync(tmp, fuente.replace("import http from 'k6/http';", 'const http = globalThis.__http;'));
  const llamadas = [];
  let respuesta = { status: 200, json: () => 'tok-k6' };
  globalThis.__http = { post: (url, cuerpo) => { llamadas.push({ url, cuerpo }); return respuesta; } };
  globalThis.__ENV = {};
  const { tokenDePruebas, cabeceras } = await import('file://' + tmp);

  check('k6: sin PRUEBAS_CLIENT_SECRET devuelve null y no llama a Keycloak', tokenDePruebas() === null && llamadas.length === 0);
  globalThis.__ENV = { PRUEBAS_CLIENT_SECRET: 's3cr' };
  check('k6: con secreto devuelve el access_token', tokenDePruebas() === 'tok-k6');
  check('k6: pide client_credentials con el cliente por defecto',
    JSON.stringify(llamadas[0].cuerpo) === JSON.stringify({ grant_type: 'client_credentials', client_id: 'donatrack-pruebas', client_secret: 's3cr' }));
  check('k6: usa Keycloak de la red de compose por defecto', llamadas[0].url === 'http://keycloak:8080/realms/donatrack/protocol/openid-connect/token');
  globalThis.__ENV = { PRUEBAS_CLIENT_SECRET: 's3cr', KEYCLOAK_TOKEN_URL: 'http://x/t', PRUEBAS_CLIENT_ID: 'otro' };
  tokenDePruebas();
  check('k6: respeta KEYCLOAK_TOKEN_URL y PRUEBAS_CLIENT_ID', llamadas[1].url === 'http://x/t' && llamadas[1].cuerpo.client_id === 'otro');
  respuesta = { status: 401, json: () => ({}) };
  let mensaje = '';
  try { tokenDePruebas(); } catch (e) { mensaje = e.message; }
  check('k6: un 401 corta con mensaje claro y sin el secreto', mensaje.includes('HTTP 401') && !mensaje.includes('s3cr'));
  check('k6: cabeceras sin token trae solo Content-Type', JSON.stringify(cabeceras(null)) === JSON.stringify({ 'Content-Type': 'application/json' }));
  check('k6: cabeceras con token agrega el Bearer', cabeceras('abc').Authorization === 'Bearer abc');
}

k6().then(() => {
  console.log(`\nPASS: ${pasaron}  │  FAIL: ${fallaron}`);
  process.exit(fallaron ? 1 : 0);
});
