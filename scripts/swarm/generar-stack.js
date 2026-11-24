// Pasa la salida de `docker compose config --format json` a un archivo que acepta `docker stack deploy`
// (ADR 20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas, prueba de Swarm del 2026-11-18).
// Uso: docker compose -f docker-compose.preprod.yml config --format json | node scripts/swarm/generar-stack.js > stack.json
// Lo que borra va a la salida de error. Ver scripts/swarm/README.md.

// [PROPOSED] Puertos que quedan publicados en Swarm. Swarm rechaza 127.0.0.1 (prueba, [VERIFIED]); que
// queden en todas las interfaces es [INFERRED]. El resto se despublica: el gateway porque, según el ADR,
// los compose del PR 986 confían en X-Forwarded-For desde 10.* y el mesh llega desde 10.0.0.2 ([INFERRED]:
// cualquiera elegiría su cubeta de rate limiting), los servicios para que nadie esquive el gateway y la
// infraestructura porque nadie de afuera la usa.
// RabbitMQ (5672), solo con DONATRACK_PUBLICAR_RABBITMQ=true (pruebas desde el host, RM-08).
const PUERTOS_PUBLICADOS = { 'cliente-liviano': [8090], keycloak: [8180], 'otel-lgtm': [3000] };
const PUERTOS_RABBITMQ = { rabbitmq: [5672] };

const CAMPOS_QUE_SWARM_RECHAZA = ['build', 'depends_on', 'container_name', 'pull_policy'];

function generarStack(compose, { publicarRabbitmq = false } = {}) {
  const c = structuredClone(compose);
  const quitados = [];
  const publicados = { ...PUERTOS_PUBLICADOS, ...(publicarRabbitmq ? PUERTOS_RABBITMQ : {}) };
  delete c.name;
  for (const [n, s] of Object.entries(c.services)) {
    if (s.profiles) { delete c.services[n]; quitados.push(`${n}: profiles`); continue; }
    for (const k of Object.keys(s)) if (s[k] === null) { delete s[k]; quitados.push(`${n}: ${k}=null`); }
    for (const k of CAMPOS_QUE_SWARM_RECHAZA) if (k in s) { delete s[k]; quitados.push(`${n}: ${k}`); }
    if (s.healthcheck && 'start_interval' in s.healthcheck) {
      delete s.healthcheck.start_interval;
      quitados.push(`${n}: healthcheck.start_interval`);
    }
    if (s.ports) {
      const quedan = [];
      for (const { host_ip, ...p } of s.ports) {
        p.published = Number(p.published);
        if ((publicados[n] || []).includes(p.published)) quedan.push(p);
        else quitados.push(`${n}: puerto ${p.published}`);
      }
      if (quedan.length) s.ports = quedan; else delete s.ports;
    }
  }
  for (const red of Object.values(c.networks || {})) { red.driver = 'overlay'; red.attachable = true; }
  c.version = '3.9';
  return { stack: c, quitados };
}

module.exports = { generarStack, PUERTOS_PUBLICADOS };

if (require.main === module) {
  const entrada = JSON.parse(require('node:fs').readFileSync(0, 'utf8'));
  const { stack, quitados } = generarStack(entrada, {
    publicarRabbitmq: process.env.DONATRACK_PUBLICAR_RABBITMQ === 'true',
  });
  process.stderr.write(`quitado: ${quitados.join(', ')}\n`);
  process.stdout.write(JSON.stringify(stack, null, 1));
}
