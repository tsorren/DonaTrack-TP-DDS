// node --test scripts/swarm/*.test.js   (Node 24 no acepta un directorio)
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { generarStack } = require('./generar-stack.js');

const ejemplo = JSON.parse(fs.readFileSync(path.join(__dirname, 'ejemplo-compose.json'), 'utf8'));
const { stack } = generarStack(ejemplo);
const s = stack.services;

test('borra name y pone version 3.9', () => {
  assert.equal(stack.name, undefined);
  assert.equal(stack.version, '3.9');
});

test('borra build, depends_on, container_name, pull_policy y los campos en null', () => {
  assert.equal(s['cliente-liviano'].build, undefined);
  assert.equal(s['cliente-liviano'].depends_on, undefined);
  assert.equal(s.mongodb.container_name, undefined);
  assert.equal(s.mongodb.pull_policy, undefined);
  assert.equal('command' in s.mongodb, false);
});

test('borra healthcheck.start_interval y deja el resto del healthcheck', () => {
  const h = s['cliente-liviano'].healthcheck;
  assert.equal(h.start_interval, undefined);
  assert.equal(h.interval, '5s');
  assert.ok(h.test.includes('http://localhost:8090/actuator/health'));
});

test('el $$ de los healthcheck llega intacto (lo interpola docker stack)', () => {
  assert.match(s.keycloak.healthcheck.test[3], /\$\$estado/);
});

test('deja afuera los servicios con profiles', () => {
  assert.equal(s.k6, undefined);
});

test('redes overlay y attachable', () => {
  assert.equal(stack.networks['preprod-network'].driver, 'overlay');
  assert.equal(stack.networks['preprod-network'].attachable, true);
});

test('published como número y sin host_ip', () => {
  assert.deepEqual(s.keycloak.ports, [{ mode: 'ingress', target: 8080, published: 8180, protocol: 'tcp' }]);
  assert.strictEqual(s['cliente-liviano'].ports[0].published, 8090);
});

test('despublica el gateway y RabbitMQ por defecto', () => {
  assert.equal(s['api-gateway'].ports, undefined);
  assert.equal(s.rabbitmq.ports, undefined);
});

test('RabbitMQ se publica solo si se pide', () => {
  const { stack: conRabbit } = generarStack(ejemplo, { publicarRabbitmq: true });
  assert.equal(conRabbit.services.rabbitmq.ports[0].published, 5672);
  assert.equal(conRabbit.services.rabbitmq.ports[0].host_ip, undefined);
  assert.equal(conRabbit.services['api-gateway'].ports, undefined);
});

test('el cliente liviano conserva la sesión en la base (E7-S01c)', () => {
  assert.equal(s['cliente-liviano'].environment.CLIENTE_SESIONES_JDBC_HABILITADO, 'true');
});

test('no cambia la entrada', () => {
  assert.equal(ejemplo.name, 'donatrack-tp-dds');
  assert.ok(ejemplo.services.k6);
});

test('el override fija hostname de mongodb y la restart_policy de minio-init', () => {
  const override = fs.readFileSync(path.join(__dirname, '..', '..', 'docker-stack.override.yml'), 'utf8');
  assert.match(override, /\n {2}mongodb:\n {4}hostname: mongodb\n/);
  assert.match(override, /\n {2}minio-init:\n {4}deploy:\n {6}restart_policy:\n {8}condition: on-failure\n/);
});
