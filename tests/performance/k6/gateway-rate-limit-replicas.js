// R4 (E7-S02c, opción A): un escenario que demuestra el rate limiting del gateway en el stack de
// Swarm con dos réplicas. No calibra los cupos: muestra tres cosas.
//   1. normal:     diez clientes a un pedido por segundo nunca reciben 429.
//   2. rafaga:     un cliente que pide más que su cupo recibe 429.
//   3. compartido: un cliente que alterna entre las dos réplicas (por la IP de cada una) consigue
//                  un cupo, no dos: las cubetas están en Redis. Si cada réplica contara por su lado
//                  pasarían el doble.
// Lo corre scripts/swarm/probar-rate-limit.sh, que le pasa las IPs de las réplicas y el cupo.
//
// Cada cliente se distingue por X-Forwarded-For: k6 corre en la red del stack (10.x), que el gateway
// trata como proxy confiable (GATEWAY_LIMITE_PROXIES_CONFIABLES). Las IPs se sortean en cada corrida
// (198.18.0.0/15, rango de pruebas) para no heredar cubetas de una corrida anterior.
import http from 'k6/http';
import { sleep } from 'k6';
import { Counter, Gauge } from 'k6/metrics';

const VIP = __ENV.GATEWAY_URL || 'http://api-gateway:8086';
const REPLICA_A = __ENV.GATEWAY_A;
const REPLICA_B = __ENV.GATEWAY_B;
const CUPO = Number(__ENV.CUPO_LECTURAS_IP || 120);
const VENTANA_SEGUNDOS = Number(__ENV.VENTANA_SEGUNDOS || 60);
const RUTA = __ENV.RUTA || '/api/categorias';
const CLIENTES_NORMALES = 10;
const SALIDA = __ENV.SALIDA || '/salida/evidencia-rate-limit.md';

if (!REPLICA_A || !REPLICA_B) {
  throw new Error('Faltan GATEWAY_A y GATEWAY_B (las corre scripts/swarm/probar-rate-limit.sh)');
}

// Un tercer octeto por corrida; el cuarto distingue al cliente. 198.19.x es de esta corrida.
const CORRIDA = Number(__ENV.CORRIDA || Math.floor(Math.random() * 250) + 1);
const ipNormal = (vu) => `198.19.${CORRIDA}.${vu}`;
const IP_RAFAGA = `198.19.${CORRIDA}.200`;
const IP_COMPARTIDO = `198.19.${CORRIDA}.201`;

const normalPasaron = new Counter('normal_pasaron');
const normalRechazados = new Counter('normal_rechazados');
const rafagaPasaron = new Counter('rafaga_pasaron');
const rafagaRechazados = new Counter('rafaga_rechazados');
const compartidoPasaronA = new Counter('compartido_pasaron_a');
const compartidoPasaronB = new Counter('compartido_pasaron_b');
const compartidoRechazadosA = new Counter('compartido_rechazados_a');
const compartidoRechazadosB = new Counter('compartido_rechazados_b');
const compartidoSegundos = new Gauge('compartido_segundos');
const compartidoMaximo = new Gauge('compartido_maximo_esperado');
const compartidoOk = new Gauge('compartido_ok');
const errores = new Counter('errores');

export const options = {
  scenarios: {
    normal: {
      executor: 'constant-vus',
      exec: 'normal',
      vus: CLIENTES_NORMALES,
      duration: '30s',
    },
    rafaga: {
      executor: 'per-vu-iterations',
      exec: 'rafaga',
      vus: 1,
      iterations: 1,
      startTime: '32s',
      maxDuration: '60s',
    },
    compartido: {
      executor: 'per-vu-iterations',
      exec: 'compartido',
      vus: 1,
      iterations: 1,
      startTime: '32s',
      maxDuration: '60s',
    },
  },
  thresholds: {
    normal_rechazados: ['count==0'],
    normal_pasaron: ['count>0'],
    rafaga_rechazados: ['count>0'],
    compartido_ok: ['value==1'],
    errores: ['count==0'],
  },
};

// Devuelve el código y cuenta como error lo que no es una respuesta del gateway ni del servicio.
function pedir(base, ip) {
  const respuesta = http.get(`${base}${RUTA}`, {
    headers: { 'X-Forwarded-For': ip },
    tags: { name: RUTA },
    responseCallback: http.expectedStatuses({ min: 200, max: 499 }),
  });
  if (respuesta.status === 0 || respuesta.status >= 500 || respuesta.status === 401) {
    errores.add(1, { codigo: String(respuesta.status) });
  }
  return respuesta.status;
}

export function normal() {
  const codigo = pedir(VIP, ipNormal(__VU));
  (codigo === 429 ? normalRechazados : normalPasaron).add(1);
  sleep(1);
}

export function rafaga() {
  for (let i = 0; i < CUPO + 40; i++) {
    (pedir(VIP, IP_RAFAGA) === 429 ? rafagaRechazados : rafagaPasaron).add(1);
  }
}

export function compartido() {
  // El doble del cupo, alternando réplicas: con cubetas por réplica pasarían todos.
  const total = 2 * CUPO;
  const inicio = Date.now();
  let pasaron = 0;
  let rechazadosA = 0;
  let rechazadosB = 0;
  for (let i = 0; i < total; i++) {
    const enA = i % 2 === 0;
    const codigo = pedir(enA ? REPLICA_A : REPLICA_B, IP_COMPARTIDO);
    if (codigo === 429) {
      if (enA) {
        rechazadosA++;
        compartidoRechazadosA.add(1);
      } else {
        rechazadosB++;
        compartidoRechazadosB.add(1);
      }
    } else {
      pasaron++;
      (enA ? compartidoPasaronA : compartidoPasaronB).add(1);
    }
  }
  const segundos = (Date.now() - inicio) / 1000;
  // Un cupo más lo que se recargó mientras duró la tanda (recarga gradual), con margen de 5.
  const maximo = CUPO + Math.ceil((segundos * CUPO) / VENTANA_SEGUNDOS) + 5;
  compartidoSegundos.add(segundos);
  compartidoMaximo.add(maximo);
  compartidoOk.add(pasaron <= maximo && rechazadosA > 0 && rechazadosB > 0 ? 1 : 0);
}

function valor(data, nombre, campo) {
  const metrica = data.metrics[nombre];
  return metrica ? metrica.values[campo] : 0;
}

function cumple(data, nombre) {
  const metrica = data.metrics[nombre];
  if (!metrica || !metrica.thresholds) return false;
  return Object.values(metrica.thresholds).every((t) => t.ok);
}

export function handleSummary(data) {
  const n = (nombre) => valor(data, nombre, 'count') || 0;
  const pasaronCompartido = n('compartido_pasaron_a') + n('compartido_pasaron_b');
  const segundos = valor(data, 'compartido_segundos', 'value') || 0;
  const maximo = valor(data, 'compartido_maximo_esperado', 'value') || 0;
  const marca = (ok) => (ok ? 'OK' : '**FALLA**');
  const normalOk = cumple(data, 'normal_rechazados') && cumple(data, 'normal_pasaron');
  const rafagaOk = cumple(data, 'rafaga_rechazados');
  const compartidoBien = cumple(data, 'compartido_ok');
  const erroresOk = cumple(data, 'errores');

  let diagnostico = '';
  if (!compartidoBien && pasaronCompartido >= 2 * CUPO - 5) {
    diagnostico =
      '\nPasaron casi todos: o cada réplica cuenta por su lado, o Redis no contesta y el gateway ' +
      'está en *fail-open* (buscar `[REDIS-NO-DISPONIBLE]` en sus logs).\n';
  }

  const md = `# Evidencia de R4: rate limiting con dos réplicas del gateway

Generado por \`tests/performance/k6/gateway-rate-limit-replicas.js\` el ${new Date().toISOString()}.
Cupo de lecturas por IP: ${CUPO} por ${VENTANA_SEGUNDOS} s, con recarga gradual. Ruta: \`GET ${RUTA}\`.
Réplicas: A = \`${REPLICA_A}\`, B = \`${REPLICA_B}\`. Por nombre de servicio: \`${VIP}\`.

| Qué se demuestra | Resultado | Datos |
|---|---|---|
| 1. El tráfico normal pasa | ${marca(normalOk)} | ${CLIENTES_NORMALES} clientes, 1 pedido/s durante 30 s: ${n('normal_pasaron')} pasaron, ${n('normal_rechazados')} con 429 |
| 2. Una ráfaga recibe 429 | ${marca(rafagaOk)} | 1 cliente, ${CUPO + 40} pedidos seguidos: ${n('rafaga_pasaron')} pasaron, ${n('rafaga_rechazados')} con 429 |
| 3. El cupo se comparte entre las réplicas | ${marca(compartidoBien)} | 1 cliente, ${2 * CUPO} pedidos alternando A y B en ${segundos.toFixed(1)} s: pasaron ${pasaronCompartido} (A ${n('compartido_pasaron_a')}, B ${n('compartido_pasaron_b')}; máximo esperado ${maximo}), 429 en A ${n('compartido_rechazados_a')} y en B ${n('compartido_rechazados_b')}. Con cubetas por réplica habrían pasado ${2 * CUPO} |
| Sin errores (5xx, 401, sin respuesta) | ${marca(erroresOk)} | ${n('errores')} |
${diagnostico}`;

  const resumen = [
    `1. tráfico normal:   ${marca(normalOk)} (${n('normal_pasaron')} pasaron, ${n('normal_rechazados')} con 429)`,
    `2. ráfaga:           ${marca(rafagaOk)} (${n('rafaga_rechazados')} con 429 de ${CUPO + 40})`,
    `3. cupo compartido:  ${marca(compartidoBien)} (pasaron ${pasaronCompartido} de ${2 * CUPO}, máximo ${maximo})`,
    `   errores:          ${marca(erroresOk)} (${n('errores')})`,
    diagnostico.trim(),
    `Informe en ${SALIDA}`,
    '',
  ].join('\n');

  return { stdout: resumen, [SALIDA]: md };
}
