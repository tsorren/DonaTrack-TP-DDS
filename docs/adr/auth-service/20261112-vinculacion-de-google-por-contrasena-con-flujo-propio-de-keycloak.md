# Vinculación de Google por contraseña: un flujo propio de primer ingreso en Keycloak

- Status: proposed
- Date: 2026-11-12
- Deciders: tsorren, al implementar E2-05 de SPEC-06; falta la aprobación del equipo en el PR
- Tags: auth-service, keycloak, google, social-login, vinculacion, contrasena, primer-ingreso, entrega-6

> **Origen:** cierra RF-AUTH-02 y RF-AUTH-03 de los [requerimientos de E6](../../requerimientos/requerimientos-e6.md) y la decisión D4 (pedir la contraseña para vincular). RF-AUTH-04 queda pendiente de D12. Complementa el [ADR del proveedor de identidad](20261104-proveedor-de-identidad-keycloak.md) y el del [correo de cuenta](20261112-correo-de-cuenta-por-rabbitmq-con-spi-de-keycloak.md).

## Contexto y Problema

D4 pide que, si alguien entra con Google y su email ya tiene cuenta, pruebe que es dueño de **la cuenta** con su contraseña. El proveedor Google del realm usaba el flujo de fábrica `first broker login`, que nunca se había probado. Al probarlo con un Keycloak 26.8.1 real y un "Google falso" (otro realm OIDC conectado al mismo flujo) apareció lo siguiente:

1. **Cumplía D4 por casualidad.** El flujo de fábrica ofrece dos caminos para verificar la cuenta existente: por email y por contraseña. Como el realm no tiene SMTP, el del email se omite solo y queda la contraseña. Con un SMTP configurado, Keycloak elige el camino por email y falla con "Falló el envío del email": el correo de cuenta sale por RabbitMQ, y el SPI no cubre este correo.
2. **Una cuenta sin contraseña no puede vincularse:** llega a la pantalla de la contraseña y no tiene con qué pasar.
3. **El botón decía "Sign in with Google"** en una pantalla en español: Keycloak 26.8.1 no traduce `signInWithProvider` ni `orLabel`.
4. **Sin credenciales de Google el botón aparecía igual** y no podía funcionar.

## Alternativas Consideradas

| Alternativa | Resultado |
|---|---|
| **A. Flujo propio que solo ofrece la contraseña** | Elegida |
| B. Dejar el flujo de fábrica | Descartada: depende de que no haya SMTP; con uno, se rompe |
| C. Extender el SPI para que el correo de confirmación de vínculo salga por RabbitMQ | Descartada: probaría que se es dueño del email, no de la cuenta, que es lo que pide D4 |
| D. Vincular sola si Google informa `email_verified=true` | Descartada por ahora: es la excepción de RF-AUTH-04 y depende de D12 (seguridad "Media" en la matriz de D4) |

## Resultado de la Decisión

**A.** `donatrack first broker login`, definido en el realm: revisar el perfil si falta algo, crear la cuenta si el email es nuevo y, si ya existe, confirmar el vínculo y pedir la contraseña de esa cuenta. Sin el camino por email, sin OTP y sin vínculo automático.

- `trustEmail` sigue en `false`: el email de Google no cuenta como prueba de nada. Y la contraseña se pide aunque Google diga que el email está verificado, y también si dice que no.
- **Google arranca apagado.** El proveedor se habilita con `GOOGLE_HABILITADO=true`, junto con las dos credenciales. El placeholder es `"enabled": "${GOOGLE_HABILITADO}"`, entre comillas; Keycloak convierte el texto a booleano.
- El tema agrega `messages_es.properties` con `signInWithProvider=Continuar con {0}` y `orLabel=o`: el botón dice "Continuar con Google", como pide RF-AUTH-02.
- **El sitio no cambia.** El botón de `publico-ingresar.html` es de la maqueta; en el recorrido real `/publico-ingresar` redirige a Keycloak, donde está el botón verdadero.

## Consecuencias Positivas

- D4 deja de depender de un efecto colateral: lo dice el realm y lo comprueba un test.
- Probado con servicios reales: `scripts/e2e_google_vinculacion.py` (20 comprobaciones). Con el flujo de fábrica fallan exactamente las dos que tienen que fallar.
- Un realm sin credenciales de Google deja de mostrar un botón roto.

## Consecuencias Negativas

- **El flujo copia la estructura del de fábrica.** Al actualizar Keycloak hay que revisar si el de fábrica ganó pasos (en la 26 apareció uno para Organizaciones, que acá no se usa).
- **Una cuenta sin contraseña no puede vincularse por este flujo** (RF-AUTH-04). El camino provisional funciona: recupera la clave (RF-AUTH-13) y después vincula.
- La pantalla de la contraseña **vuelve a ofrecer "Continuar con Google"**; es de Keycloak y no se tocó.
- Una cuenta nueva por Google queda con `emailVerified=false`: hoy no molesta (`verifyEmail` está apagado), pero sí si se activa RF-AUTH-16.
- **El Google real no se probó:** sí el flujo, con tokens OIDC verdaderos y `email_verified`, pero no las pantallas ni las credenciales de Google.
- La descripción de un flujo se guarda en una columna de 255 caracteres; una más larga **aborta el arranque** de Keycloak. Pasó, y un test lo vigila.
- Como el resto del realm, estos cambios solo se importan con la base vacía: `docker compose down -v`.

## Validación

- `scripts/e2e_google_vinculacion.py` contra el stack (Google falso). Para el compose hay que poder alcanzar al Google falso desde adentro de Keycloak: `KC_INTERNAL_URL` es `http://localhost:8080` por defecto.
- `scripts/tests/test_google_vinculacion.py`, y `RealmTokenContractIT` (define `GOOGLE_HABILITADO`, espera el flujo propio y comprueba que no hay camino por email).

## Trabajo Futuro / Links

- RF-AUTH-04: vincular cuentas sin contraseña cuando Google informa `email_verified=true`, tras la decisión D12. Sería un autenticador en el SPI de Keycloak; es un cambio sensible en seguridad.
- Decidir si `trustEmail` pasa a `true` (las cuentas de Google nacerían verificadas) sin que eso afecte a la vinculación.
- Probar con un proyecto real de Google Cloud.
