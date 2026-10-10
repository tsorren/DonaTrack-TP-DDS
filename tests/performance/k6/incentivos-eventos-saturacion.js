import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '5s', target: 30 },
    { duration: '15s', target: 100 },
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<300', 'p(99)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.INCENTIVOS_URL || 'http://incentivos-service:8082';

export default function () {
  const res = http.post(`${BASE_URL}/api/incentivos/donaciones`, null, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'evento procesado con 200': (r) => r.status === 200,
  });

  sleep(0.05);
}
