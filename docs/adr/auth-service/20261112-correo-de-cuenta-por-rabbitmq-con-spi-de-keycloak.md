# Correo de cuenta por RabbitMQ: un SPI de Keycloak que publica el evento

- Status: proposed
- Date: 2026-11-12
- Deciders: tsorren, al implementar E2-06 de SPEC-06; falta la aprobación del equipo en el PR
- Tags: auth-service, keycloak, spi, rabbitmq, notificaciones, correo, recuperar-clave, verificar-email, seguridad, entrega-6

> **Origen:** cierra la decisión D8 de los [requerimientos de E6](../../requerimientos/requerimientos-e6.md) (cómo sale el email de recuperación y de verificación) y los requerimientos RF-AUTH-13 y RF-AUTH-16. Complementa el [ADR del proveedor de identidad](20261104-proveedor-de-identidad-keycloak.md). Evento definido en el [catálogo de eventos](../../arquitectura/eventos-amqp.md).

## Contexto y Problema

Keycloak genera los enlaces de un solo uso para recuperar la clave y verificar el email, y por defecto los manda por SMTP. Ya estaba decidido que el email sale por `notificaciones-service`, para reusar su mensajería y su canal de correo. Faltaba el cómo (D8).

Hay tres hechos que condicionan la decisión:

1. **El enlace es un secreto.** Mientras vale, da acceso a la cuenta. No puede quedar en el historial de notificaciones, que un admin puede consultar, ni en un log.
2. **El destinatario es un dato personal** y la cuenta puede no tener todavía una `Persona` (la verificación del email ocurre en el registro, antes de completar el perfil), así que Notificaciones no puede resolver el email desde un `personaId`.
3. **Keycloak solo tiene el enlace en un lugar:** el `EmailTemplateProvider`, antes de renderizar el correo.

## Alternativas Consideradas

| Alternativa | Resultado |
|---|---|
| **A. SPI que publica un evento estructurado en RabbitMQ** (`EmailTemplateProvider`) | Elegida |
| B. SMTP propio de Keycloak | Descartada: el mail no pasa por Notificaciones, incumple lo decidido |
| C. Flujo propio en un servicio (token + Admin API) | Descartada: duplica lo que Keycloak ya trae y pierde sus protecciones |
| D. Un `EmailSenderProvider` que reenvía el correo ya renderizado | Descartada: habría que sacar el enlace de un HTML, y el contenido quedaría atado al tema de Keycloak |
| E. Un `EventListenerProvider` | Descartada: es un SPI público y estable, pero sus eventos no traen el enlace |

## Resultado de la Decisión

**A.** El proveedor `donatrack-amqp` sobrescribe `sendPasswordReset` y `sendVerifyEmail` y publica `cuenta.enlace-solicitado.v1` en `auth.exchange`. `notificaciones-service` lo consume de su cola `notificaciones.auth` y manda el correo.

- **El evento lleva el destinatario y el enlace**, no un `personaId` (hecho 2). El mensaje vence cuando vence el enlace (propiedad AMQP `expiration`), para que un enlace muerto no quede en la cola.
- **Notificaciones no persiste nada.** `EnlaceDeCuentaService` no tiene repositorio: manda el correo y listo. Es una desviación consciente de D8 ("queda en el historial de notificaciones"), por el hecho 1. El evento tampoco se muestra en ningún log: su `toString` oculta el email y el enlace.
- **Solo se manda un enlace que apunte a nuestro Keycloak** (`notificaciones.cuentas.prefijo-de-enlaces`). Un publicador comprometido no puede usar el correo de DonaTrack para mandar un enlace ajeno. Un enlace que no cumple va a la cola de mensajes muertos.
- **Publicación confiable pero simple:** una conexión por mensaje, confirmación del broker y `mandatory`. Si ninguna cola recibe el evento, falla en vez de perderse.
- **Trazabilidad:** el SPI genera un `traceId` (UUID sin guiones) y lo manda en `X-Trace-Id`; el listener lo restablece en el MDC mientras procesa.
- **La verificación del email queda soportada pero apagada.** El realm no activa `verifyEmail`: cambiaría el registro y el recorrido de Playwright. Es una decisión de producto.

## Consecuencias Positivas

- El correo de cuenta usa el mismo canal que el resto, con inbox por `message_id` y cola de mensajes muertos.
- Verificado contra un Keycloak 26.8.1 real y un RabbitMQ real: recuperar la clave de punta a punta, el enlace de un solo uso, una cuenta inexistente sin mensaje (RNF-AUTH-10) y la verificación del email.
- Keycloak sigue sano si RabbitMQ cae, y publica de nuevo apenas vuelve, sin reiniciar.

## Consecuencias Negativas

- **`emailTemplate` es un SPI interno de Keycloak.** Su propio log avisa que "puede cambiar sin aviso". Depende de la versión, que está fijada por digest en el `Dockerfile`; al actualizar Keycloak hay que reconstruir y volver a correr `scripts/e2e_correo_de_cuenta.py`.
- **Elegir el proveedor es una opción de construcción** de Keycloak. Con `start --optimized` un valor puesto solo al arrancar no se usa y el servidor se niega a iniciar. Por eso `KC_SPI_EMAIL_TEMPLATE__PROVIDER` está en el `Dockerfile` y no en el compose. Una prueba hecha con `start` normal no lo habría mostrado.
- **Si RabbitMQ no está, la persona no ve un error:** Keycloak muestra la misma página de siempre (protección deliberada de RNF-AUTH-10) y la única señal es el log (`KC-SERVICES0026`). Hace falta monitorearlo.
- **El log de eventos de Keycloak imprime el `username`** (en este realm, el email) cuando falla el envío. Es de Keycloak, no del SPI.
- **El enlace viaja en un mensaje.** RabbitMQ del compose no usa TLS. Vida corta y mensaje con vencimiento lo mitigan, pero en producción corresponde TLS.
- El correo está solo en español, que es el único idioma del realm.
- Cada correo abre su propia conexión a RabbitMQ: no sirve para alto volumen, y no es el caso.

## Validación

- `scripts/e2e_correo_de_cuenta.py`: 23 comprobaciones contra el stack (recuperar la clave, enlace de un solo uso, cuenta inexistente, verificar el email).
- Tests del SPI (`auth-service/spi`), de Notificaciones (servicio, listener, evento, configuración y contrato con el JSON real del SPI) y `scripts/tests/test_correo_de_cuenta.py`.

## Trabajo Futuro / Links

- TLS entre Keycloak, RabbitMQ y los servicios.
- Alertar sobre `KC-SERVICES0026` en el log de Keycloak.
- Decidir si se activa `verifyEmail` en el realm (RF-AUTH-16).
- Más idiomas en el correo, junto con las locales del realm.
