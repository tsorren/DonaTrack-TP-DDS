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
export LOGISTICA_ADMIN_API_KEY=<clave-admin> LOGISTICA_EXTERNA_CALLBACK_API_KEY=<clave-externo>
```

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml up -d --build rabbitmq donaciones-service logistica-service logistica-externo
```

Para ver al broker trabajando, en otra terminal:

```bash
docker-compose -f docker-compose.yml -f docker-compose.demo.yml logs -f donaciones-service | grep -E "BROKER-LOGISTICA|OUTBOX-LOGISTICA"
```

**Postman:** importar `postman/flujo-9-broker-logistica.json` y completar las variables de colección `adminApiKey` y `callbackApiKeyExterno` con las mismas claves. Correr primero la carpeta **0. Preparación**. Los datos de Donaciones son en memoria: si se reinicia `donaciones-service`, hay que volver a correrla.

**newman** (la misma colección, desde la terminal):

```bash
newman run postman/flujo-9-broker-logistica.json --env-var adminApiKey=$LOGISTICA_ADMIN_API_KEY --env-var callbackApiKeyExterno=$LOGISTICA_EXTERNA_CALLBACK_API_KEY --folder "0. Preparación (una vez por arranque)" --folder "A. Preferido donatrack → AMQP a logística 8083" --folder "B. Preferido externo → HTTP a logística 8084" --folder "C. Preferido otra (sin cola) → devuelto → donatrack" --folder "F. Callback del proveedor HTTP (vuelta)" --folder "G. Administración: negativos y restaurar"
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
  - en `8084` puede haber **ninguna o varias** entregas para la misma donación: depende de si `externo` llegó a procesar el pedido antes de que el adapter cortara por timeout. Con 1 ms casi siempre corta antes, así que suele no haber ninguna.
- **Qué explicar:**
  - Un timeout no significa que el pedido no llegó; significa que **no sabemos**. Por eso el broker **nunca cambia de proveedor** ante un timeout: si lo hiciera, la donación podría terminar con dos entregas en dos empresas distintas.
  - Reintenta con el mismo proveedor, que tiene que deduplicar por donación. Es un requisito del contrato con cualquier proveedor.
  - Si en `8084` aparecen varias entregas, es porque la instancia de prueba `externo` **no deduplica** su `POST /api/entregas`, y eso muestra justamente por qué el contrato lo exige. Nuestra logística real se alcanza por AMQP, donde sí deduplica.

### F. Vuelta de un proveedor HTTP (callback)

- **Qué hacer:** carpeta **F**. Requiere haber corrido A y B.
- **Qué mirar:**
  - `401` sin clave;
  - `404` si `externo` avisa por una donación que le tocó a `donatrack`;
  - `202` por su propia donación, que pasa a `ListaParaEntregar` (así lo devuelve la API).
- **Qué explicar:**
  - Un proveedor HTTP avisa por el callback `POST /api/logistica/proveedores/{id}/avisos`, protegido con **su** clave.
  - Solo puede avisar por donaciones que el broker le asignó a él.
  - El aviso pasa por el **mismo procesador idempotente** que los eventos que llegan por RabbitMQ.

### G. Administración

- **Qué hacer:** carpeta **G**.
- **Qué mirar:** `401` sin clave de administración; `400` con un proveedor no configurado; el preferido vuelve a `donatrack`.

---

## 4. Si algo no sale

| Síntoma | Causa probable |
|---|---|
| El compose no arranca y pide `LOGISTICA_ADMIN_API_KEY` | Faltan las variables de entorno con las claves |
| `401` en los requests de administración o del callback | Las variables de Postman no coinciden con las claves exportadas |
| La carpeta A no encuentra la propuesta | No se corrió la carpeta 0 desde el último reinicio de Donaciones |
| La entrega no aparece | Revisar el log del broker: rechazos, inciertos y rondas quedan con el prefijo `[BROKER-LOGISTICA]` |
