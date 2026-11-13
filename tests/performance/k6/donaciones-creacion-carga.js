import http from 'k6/http';
import { check, sleep } from 'k6';
import { cabeceras, tokenDePruebas } from './token.js';

export const options = {
  stages: [
    { duration: '5s', target: 20 },
    { duration: '15s', target: 50 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.DONACIONES_URL || 'http://donaciones-service:8080';

export function setup() {
  return { token: tokenDePruebas() };
}

export default function (data) {
  const uniqueId = `${Date.now()}_${Math.floor(Math.random() * 1000000)}`;
  const dni = `${Math.floor(10000000 + Math.random() * 90000000)}`;

  // 1. Crear Persona
  const personaPayload = JSON.stringify({
    tipo: 'HUMANA',
    documento: dni,
    tipoDocumento: 'DNI',
    nombre: `PerfUser_${uniqueId}`,
    apellido: 'Test',
    email: `perf_${uniqueId}@test.com`,
    mediosDeContacto: [
      {
        tipo: 'CORREO',
        direccionCorreo: `perf_${uniqueId}@test.com`,
        esPredeterminado: true,
      },
    ],
  });

  const personaRes = http.post(`${BASE_URL}/api/personas`, personaPayload, {
    headers: cabeceras(data.token),
  });

  const personaCheck = check(personaRes, {
    'persona creada con 201': (r) => r.status === 201,
  });

  if (!personaCheck) {
    sleep(0.5);
    return;
  }

  const personaId = personaRes.json('id');

  // 2. Crear Donante
  const donantePayload = JSON.stringify({ idPersona: personaId });
  const donanteRes = http.post(`${BASE_URL}/api/donantes`, donantePayload, {
    headers: cabeceras(data.token),
  });

  const donanteCheck = check(donanteRes, {
    'donante creado con 201': (r) => r.status === 201,
  });

  if (!donanteCheck) {
    sleep(0.5);
    return;
  }

  const donanteId = donanteRes.json('idDonante');

  // 3. Crear Donacion
  const donacionPayload = JSON.stringify({
    idDonante: donanteId,
    descripcion: `Donacion k6 perf test ${uniqueId}`,
    nombreDeposito: 'Deposito Central',
    items: [
      {
        nombre: 'Arroz 1kg',
        cantidad: 5,
        categoria: 'ALIMENTOS',
      },
    ],
  });

  const donacionRes = http.post(`${BASE_URL}/api/donaciones`, donacionPayload, {
    headers: cabeceras(data.token),
  });

  check(donacionRes, {
    'donacion creada con 201': (r) => r.status === 201,
  });

  sleep(0.1);
}
