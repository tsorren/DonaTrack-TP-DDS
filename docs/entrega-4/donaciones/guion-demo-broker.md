# Guion de demo — Broker de Integración con Logística

> Demo en vivo del requerimiento «Broker de Integración con Logística» de la Entrega 4: Donaciones elige, para cada donación asignada, a qué proveedor de logística le pide la entrega.
>
> **Material:** [`docker-compose.demo.yml`](../../../docker-compose.demo.yml) · colección [`postman/flujo-9-broker-logistica.json`](../../../postman/flujo-9-broker-logistica.json) · [Bitácora](bitacora-broker-logistica.md) · [ADR](../../adr/20261007-broker-de-integracion-con-logistica.md)

---

## 1. Qué hay en escena

| Pieza | Dónde | Rol |
|---|---|---|
| `donaciones-service` | `localhost:8080` | Tiene el broker: elige el proveedor, manda el pedido y procesa la vuelta |
| `logistica-service` | `localhost:8083` | Proveedor **`donatrack`**: nuestra logística, recibe los pedidos por **RabbitMQ** |
| `logistica-externo` | `localhost:8084` | Proveedor **`externo`**: una segunda instancia de la misma imagen que hace de courier privado; recibe los pedidos por **HTTP** |
| `otra` | — | Proveedor **AMQP configurado sin instancia** (no tiene cola): sirve para mostrar qué pasa cuando RabbitMQ no encuentra destinatario |
| RabbitMQ | `localhost:15672` (consola; usuario y clave del compose) | Se ven `donaciones.exchange` y las colas `logistica.<instancia>.entregas.solicitadas` |

Orden de prueba de los proveedores: el **preferido** primero y después el resto en el orden configurado (`donatrack, externo, otra`). El preferido se cambia en caliente con `PUT /api/logistica/proveedor-preferido`.

En la demo los tiempos están acortados: el relay revisa la bandeja cada 1 s y la espera entre reintentos arranca en 10 s. Los valores reales son 10 s y 60 s.

---

## 2. Preparación

```bash
export LOGISTICA_ADMIN_API_KEY=<clave-admin>
export LOGISTICA_TOKEN_DONATRACK=<token-donatrack> LOGISTICA_TOKEN_EXTERNO=<token-externo>
```

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build rabbitmq donaciones-service logistica-service logistica-externo
```

Para ver al broker trabajando, en otra terminal:

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml logs -f donaciones-service | grep -E "BROKER-LOGISTICA|OUTBOX-LOGISTICA"
```

**Postman:** importar `postman/flujo-9-broker-logistica.json` y completar las variables de colección `adminApiKey` (la misma clave exportada), y, solo para el escenario F, `rabbitUser` y `rabbitPass` (las credenciales de RabbitMQ del compose) y `tokenDonatrack` y `tokenExterno` (los mismos tokens exportados). Correr primero la carpeta **0. Preparación**. Los datos de Donaciones son en memoria: si se reinicia `donaciones-service`, hay que volver a correrla.

**newman** (la misma colección, desde la terminal):

```bash
newman run postman/flujo-9-broker-logistica.json --env-var adminApiKey=$LOGISTICA_ADMIN_API_KEY --env-var rabbitUser=<usuario-rabbit> --env-var rabbitPass=<clave-rabbit> --env-var tokenDonatrack=$LOGISTICA_TOKEN_DONATRACK --env-var tokenExterno=$LOGISTICA_TOKEN_EXTERNO --folder "0. Preparación (una vez por arranque)" --folder "A. Preferido donatrack → AMQP a logística 8083" --folder "B. Preferido externo → HTTP a logística 8084" --folder "C. Preferido otra (sin cola) → devuelto → donatrack" --folder "F. Vuelta del proveedor por mensajería (RabbitMQ)" --folder "G. Administración: negativos y restaurar" --folder "H. Vuelta real de nuestra Logística: flota, planificación y eventos firmados" --delay-request 250
```

---

## 3. Escenarios

Cada escenario asigna una donación nueva (cargar → normalizar → segmentar → necesidad → algoritmo → aprobar) y verifica **en qué logística aparece la entrega**. La aprobación de la propuesta es lo que dispara al broker.

### A. Preferido `donatrack` → RabbitMQ

- **Qué hacer:** carpeta **A**.
- **Qué mirar:**
  - el log `Donación … pendiente de envío al proveedor donatrack` y después `enviada al proveedor donatrack`;
  - la entrega aparece en `8083` y no en `8084`;
  - en la consola de RabbitMQ, el mensaje pasó por la cola `logistica.donatrack.entregas.solicitadas`.
- **Qué explicar:**
  - Donaciones sigue publicando el hecho `donacion.asignada.v1` para Notificaciones, y además le manda a logística un **pedido dirigido**, el comando `entrega.solicitada.donatrack.v1`.
  - El sobre viaja como **carta certificada**: con `mandatory` y publisher confirm, el adapter espera el acuse antes de darlo por enviado.

### B. Preferido `externo` → HTTP

- **Qué hacer:** carpeta **B**. El primer request cambia el preferido en caliente, sin reiniciar nada.
- **Qué mirar:** la entrega aparece en `8084` y no en `8083`.
- **Qué explicar:**
  - Es otro transporte y otro contrato: el adapter HTTP traduce el formato canónico de Donaciones (`DatosEntregaLogistica`) al contrato REST del proveedor.
  - **El broker no cambió**: sumar un proveedor es configuración o un adapter nuevo (Strategy + Adapter).

### C. Preferido `otra` (AMQP sin cola) → devuelto → `donatrack`

- **Qué hacer:** carpeta **C**.
- **Qué mirar:**
  - en el log: `Envío rechazado: Proveedor otra: mensaje devuelto, no hay cola para entrega.solicitada.otra.v1`, y después `El proveedor otra rechazó la donación …; se prueba con el siguiente`;
  - la entrega termina en `8083`.
- **Qué explicar:**
  - Sin protección, RabbitMQ habría **tirado el sobre en silencio**. Con `mandatory`, lo devuelve.
  - Como la devolución llega dentro del acuse, el broker tiene la **certeza** de que no llegó a nadie, así que es seguro probar con el siguiente.

### D. [Manual] `externo` caído → rechazo → `donatrack`

1. Bajar la instancia:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.demo.yml stop logistica-externo
   ```
2. Correr la carpeta **D**.
3. Volver a levantarla:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.demo.yml start logistica-externo
   ```

- **Qué mirar:** no se puede conectar con `externo` y el pedido termina en `8083`.
- **Qué explicar:**
  - «No me pude conectar» es un rechazo seguro: el pedido no salió.
  - Si **todos** rechazaran (por ejemplo, RabbitMQ caído además), el broker no lo da por perdido: repite la ronda completa más tarde, con espera creciente, hasta 5 veces.

### E. [Manual] Timeout con `externo` → incierto → mismo proveedor

1. Recrear donaciones con un timeout de lectura mínimo:
   ```bash
   LOGISTICA_EXTERNA_READ_TIMEOUT_MS=1 docker-compose -f docker-compose.yml -f docker-compose.demo.yml up -d donaciones-service
   ```
2. Correr las carpetas **0** y **E** (al recrearse, Donaciones perdió los datos en memoria).
3. Restaurar:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.demo.yml up -d donaciones-service
   ```

- **Qué mirar:**
  - en el log: `Envío incierto … (intento 1/5); se reintenta con el mismo proveedor` y, unos segundos después, el intento 2;
  - **nunca** aparece una entrega en `8083`, ni siquiera tras varios reintentos;
  - en `8084` hay **cero o una** entrega para la donación: depende de si `externo` llegó a procesar algún pedido antes de que el adapter cortara por timeout. Con 1 ms casi siempre corta antes, así que suele no haber ninguna. Nunca hay más de una: desde #886, `POST /api/entregas` deduplica por donación y responde 409 (`ERR-EST-816`) a los reintentos (D55).
- **Qué explicar:**
  - Un timeout no significa que el pedido no llegó; significa que **no sabemos**. Por eso el broker **nunca cambia de proveedor** ante un timeout: si lo hiciera, la donación podría terminar con dos entregas en dos empresas distintas.
  - Reintenta con el mismo proveedor, que tiene que deduplicar por donación. Es un requisito del contrato con cualquier proveedor.
  - Que `externo` no cree una segunda entrega es justamente lo que el contrato le exige a cualquier proveedor. Con 1 ms el broker no llega a leer ese 409, así que registra cada intento como incierto. Con un timeout normal, el 409 se toma como éxito (`ProveedorLogisticaHttp`). Nuestra logística real se alcanza por AMQP, donde también deduplica.

### F. Vuelta de un proveedor por mensajería

- **Qué hacer:** carpeta **F**. Requiere haber corrido A, B y C, y completar `rabbitUser`, `rabbitPass`, `tokenDonatrack` y `tokenExterno`.
- **Qué mirar:**
  - F.1 y F.2: RabbitMQ responde `routed: true` (el broker de mensajes no sabe de identidades), pero el log de Donaciones muestra `[ORIGEN-EVENTO] ... se descarta`: el primero no traía token y el segundo era de `externo` sobre una donación de `donatrack`;
  - F.3 y F.4: el evento de `externo`, con su id y su token, se acepta y su donación pasa a `ListaParaEntregar` (así lo devuelve la API);
  - F.5 y F.6: las donaciones de los eventos descartados no cambiaron. Como la cola es FIFO, cuando F.4 ve el cambio los eventos anteriores ya se procesaron.
- **Qué explicar:**
  - Un proveedor informa **publicando en `logistica.exchange`** los mismos eventos que publica nuestra Logística (`ruta.asignada`, `ruta.iniciada`, `entrega.exitosa`, `entrega.fallida`). No llama a Donaciones por HTTP: el enunciado de la Entrega 4 (requerimiento de implementación 3) dice que el servicio de logística no debe invocar a Donaciones sino dejar disponible la información.
  - HTTP se usa solo de ida (Donaciones → proveedor). Es un **requisito de contrato** para cualquier proveedor.
  - **Cómo sabe Donaciones quién mandó el evento:** cada evento lleva en headers el id del proveedor (`X-Proveedor-Id`) y un token propio (`X-Proveedor-Token`). Donaciones compara el token con el configurado para ese proveedor y después comprueba que la donación le pertenezca según el registro del broker. Nuestra Logística firma así todos sus eventos.
  - **Falla cerrado:** sin id, sin token, con un token incorrecto o sobre una donación ajena, el evento se descarta con un aviso en el log.
  - El evento aceptado pasa por el **mismo procesador idempotente**: repetirlo no cambia el estado dos veces.
  - Límite conocido: el token viaja en un header del mensaje, sin rotación ni firma del cuerpo; para un entorno real, la recomendación es un usuario de RabbitMQ por proveedor con permisos de publicación acotados (DTI-14, ítem 7).
  - Evolución posible: que Donaciones consulte el estado al proveedor (polling), para un proveedor que no pueda publicar en nuestro RabbitMQ.

### G. Administración

- **Qué hacer:** carpeta **G**.
- **Qué mirar:** `401` sin clave de administración; `400` con un proveedor no configurado; el preferido vuelve a `donatrack`.

### H. Vuelta real de nuestra Logística (flota, planificación y eventos firmados)

- **Qué hacer:** carpeta **H**. Requiere haber corrido 0 y A (la entrega de la donación A tiene que estar en `donatrack`, 8083). `docker-compose.demo.yml` habilita el disparador manual de la planificación (`LOGISTICA_PLANIFICACION_MANUAL_ENABLED=true`). Con newman conviene `--delay-request 250`, porque las esperas reintentan sin pausa.
- **Qué hacer, paso a paso:** se carga un camión y un chofer en 8083, se dispara la planificación y se avanza la ruta y la entrega. En cada paso se consulta la donación en Donaciones.
- **Qué mirar:**
  - H.6: la donación pasa a `ListaParaEntregar` porque Logística publicó `ruta.asignada`;
  - H.8: pasa a `EnTraslado` por `ruta.iniciada`;
  - H.10: pasa a `Entregada` por `entrega.exitosa`;
  - en el log de Donaciones aparecen `Evento RutaAsignada recibido`, `Evento RutaIniciada recibido` y `Evento EntregaExitosa recibido`, y ningún `[ORIGEN-EVENTO]` ni `Fatal message conversion error`.
- **Qué explicar:**
  - Estos eventos **no los simula Postman**: los genera de verdad nuestra Logística y los firma con su id y su token en los headers (`X-Proveedor-Id`, `X-Proveedor-Token`). Es el mismo contrato que se le exige a cualquier proveedor.
  - El tipo del mensaje (`__TypeId__`) viaja como la routing key (`ruta.asignada`, etc.), no como el nombre de la clase. Un proveedor tiene que respetarlo: si manda el nombre de su clase, Donaciones no puede convertir el mensaje.
  - Falta cubrir el camino de `entrega.fallida` de punta a punta.

---

## 4. Si algo no sale

| Síntoma | Causa probable |
|---|---|
| El compose no arranca y pide `LOGISTICA_ADMIN_API_KEY` | Falta exportar la clave de administración o los tokens de vuelta (`LOGISTICA_TOKEN_DONATRACK`, `LOGISTICA_TOKEN_EXTERNO`) |
| `401` en los requests de administración | La variable `adminApiKey` de Postman no coincide con `LOGISTICA_ADMIN_API_KEY` |
| `401` en el escenario F.1 | `rabbitUser` o `rabbitPass` no coinciden con las credenciales del RabbitMQ del compose |
| En H.6 la donación no pasa a `ListaParaEntregar` | Revisar el log de Donaciones: `Fatal message conversion error` indica un `__TypeId__` que no es el alias de la routing key; `[ORIGEN-EVENTO]` indica un token o un id que no coinciden |
| En F.4 la donación no pasa a `ListaParaEntregar` | `tokenExterno` no coincide con `LOGISTICA_TOKEN_EXTERNO` (en el log de Donaciones aparece `Token ausente o incorrecto`) |
| La carpeta A no encuentra la propuesta | No se corrió la carpeta 0 desde el último reinicio de Donaciones |
| La entrega no aparece | Revisar el log del broker: rechazos, inciertos y rondas quedan con el prefijo `[BROKER-LOGISTICA]` |
