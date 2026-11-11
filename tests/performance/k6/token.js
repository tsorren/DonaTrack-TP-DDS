import http from 'k6/http';

// Token de la cuenta de pruebas (client credentials, rol ADMIN) para llamar a los servicios con la
// seguridad encendida (SPEC-06 E2-07). Sin PRUEBAS_CLIENT_SECRET devuelve null y los pedidos salen
// sin token, que es lo que pasa con la seguridad apagada.
export function tokenDePruebas() {
  const secreto = __ENV.PRUEBAS_CLIENT_SECRET;
  if (!secreto) {
    return null;
  }
  const url =
    __ENV.KEYCLOAK_TOKEN_URL || 'http://keycloak:8080/realms/donatrack/protocol/openid-connect/token';
  const res = http.post(url, {
    grant_type: 'client_credentials',
    client_id: __ENV.PRUEBAS_CLIENT_ID || 'donatrack-pruebas',
    client_secret: secreto,
  });
  if (res.status !== 200) {
    throw new Error(`No se pudo obtener el token de pruebas: HTTP ${res.status}`);
  }
  return res.json('access_token');
}

// Cabeceras de un pedido JSON, con el token si lo hay.
export function cabeceras(token) {
  const encabezados = { 'Content-Type': 'application/json' };
  if (token) {
    encabezados.Authorization = `Bearer ${token}`;
  }
  return encabezados;
}
