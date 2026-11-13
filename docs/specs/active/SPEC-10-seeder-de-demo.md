# Spec: SPEC-10 — Seeder de demo

> **Estado:** PROPOSED (2026-11-11). El usuario aprobó el plan y las decisiones ese día; la aprobación de esta spec es el merge de su PR. Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL (AGENTS.md §7.0): una imagen Docker nueva con dependencias de Python, un overlay de compose y escrituras directas en las bases de los cuatro servicios. No cambia código ni contratos de ningún servicio.
> **Fecha:** 2026-11-11
> **Base:** `ENTREGA_6` (`60de489b`). Rama: `E6_seeder`.
> **Relación:** sirve a la demo de [SPEC-09](SPEC-09-cierre-e6-demo-punta-a-punta.md) (F6). Convive con [SPEC-06](SPEC-06-auth-mvp-keycloak.md): el seeder funciona con la seguridad encendida o apagada y no toca el realm, el gateway ni el compose base.

---

## 1. Objetivo Funcional (Goal)

Un comando que deja el sistema como si tuviera usuarios activos desde hace seis meses: donantes con
historia, misiones e insignias, entidades con necesidades de todos los estados, entregas recibidas
con fotos, avisos, rankings y, en el momento, un camión en camino, una entrega en la puerta y
pendientes para el admin. Las cuentas de la demo entran con una clave común.

**Criterio de hecho:** con las bases vacías, `docker compose -f docker-compose.yml -f
docker-compose.seed.yml up -d` termina con el seeder en `exit 0`, la verificación final pasa para
cada protagonista y cada pantalla del cliente liviano tiene algo que mostrar.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope

- `demo/seeder/`: el seeder en Python (`python:3.12-slim` con `psycopg`, `pymongo`, `minio`,
  `Pillow` y `requests` en versiones fijas, aprobadas por el usuario), su Dockerfile y sus tests.
- `docker-compose.seed.yml`: un overlay que agrega solo el servicio `seeder` (corre una vez y
  termina).
- Lo que el seeder crea en el sistema que está corriendo:
  - el cliente `demo-seeder` de Keycloak, con login por usuario y clave y los mismos mappers que
    `cliente-liviano`;
  - las cuentas de la demo;
  - cinco categorías más con sus subcategorías y alias (en total, ocho categorías).
- La documentación: `docs/testing/demo-seeder.md` (cómo correrlo, cuentas y guion por rol) y la
  sincronía de los índices.

### Out-of-Scope

- El cliente liviano en el compose y el emisor único de Keycloak (SPEC-06, iteración 6). El cliente
  se levanta como hoy, con `mvn spring-boot:run`.
- Encender la seguridad. Quien presenta elige las variables `*_SEGURIDAD_HABILITADA`, y el seeder
  anda con cualquiera de las dos.
- El gateway, el realm del repo, el compose base, el código de los servicios y `DEUDA_TECNICA.md`.
- Los huecos del sistema que aparecieron en el relevamiento (sección 8): se reportan y no se
  arreglan acá.

## 3. Decisiones Acordadas y Trade-offs

### 3.1 Cómo se cargan los datos (Trade-off)

| Alternativa | A favor | En contra |
|---|---|---|
| a. Todo por API | Respeta todas las reglas | No se pueden poner fechas pasadas: todo pasaría "hoy" |
| b. Todo por SQL y Mongo | Rápido y con fechas libres | Saltea los eventos: Incentivos y Notificaciones no se enteran de las personas |
| **c. Híbrido** | Las cuentas y lo de hoy por API (con sus eventos); la historia por base, con fechas de los últimos seis meses | Hay que respetar a mano las reglas de cada tabla (sección 4) |

**Elegida: c**, por el usuario.

### 3.2 Cómo consigue tokens el seeder

| Alternativa | A favor | En contra |
|---|---|---|
| **a. Cliente `demo-seeder` con login directo** | Actúa como cada cuenta, igual que el sitio | Una puerta extra, solo en la base de la demo |
| b. Simular el navegador | No agrega nada a Keycloak | Frágil ante cambios del tema o del flujo de login |
| c. Sembrar con la seguridad apagada | No necesita tokens | Para presentar con login hay que reiniciar los servicios |

**Elegida: a**, por el usuario. El cliente se crea con el admin de la consola
(`KC_BOOTSTRAP_ADMIN_*`), con un secreto aleatorio en cada corrida. No queda en
`auth-service/realm/`.

### 3.3 Otras decisiones

- **Catálogo de ocho categorías**, para variar los filtros y que todas las misiones se puedan
  cumplir.
- **Clave común `DEMO_PASSWORD`** para las cuentas de la demo, sin valor por defecto. El admin
  sigue con `AUTH_ADMIN_PASSWORD`.
- **Semilla fija y fechas relativas al día de la corrida**: la historia son siempre los últimos seis
  meses.
- **Arranca de cero**: si encuentra datos de demo, se detiene sin tocar nada. Para volver a
  sembrar: `docker compose down -v`.

## 4. Invariantes

- Solo datos sintéticos: nombres inventados, documentos fuera de rango y correos
  `@demo.donatrack.test`. No hay credenciales en el repo.
- Lo que va por base respeta las reglas de cada servicio:
  - `version` en 0;
  - los historiales encadenados;
  - las listas ordenadas contiguas desde 0;
  - UUID binarios estándar y GeoJSON `[lon, lat]` en Mongo;
  - horas en UTC en Donaciones y Logística, y en hora de Buenos Aires en Notificaciones.
- Nada de lo histórico dispara trabajos del sistema:
  - outbox en `PUBLICADO`;
  - vencimientos en el futuro;
  - períodos vigentes que cubren hoy;
  - un solo donante inactivo a propósito.
- Los ids coinciden entre servicios: el donante, la entidad y la donación independiente son el
  mismo id en Donaciones, Incentivos, Notificaciones y Logística.

## 5. Criterios de Aceptación

```gherkin
Escenario: siembra completa
  Dado el sistema levantado con las bases vacías
  Cuando corre el seeder
  Entonces termina sin error e imprime la tabla de cuentas
  Y la verificación encuentra datos en cada pantalla de cada protagonista

Escenario: segunda corrida
  Dado un sistema ya sembrado
  Cuando corre el seeder otra vez
  Entonces se detiene sin tocar nada y explica cómo empezar de cero

Escenario: la donante estrella
  Dado el sistema sembrado
  Cuando entra la donante estrella
  Entonces ve seis meses de donaciones, misiones cumplidas con insignias, su categoría y su puesto en el ranking
  Y ve un camión en camino con su donación

Escenario: la entidad completa
  Dado el sistema sembrado
  Cuando entra la entidad completa
  Entonces ve necesidades vigentes, cumplidas, terminadas y dadas de baja
  Y una entrega en la puerta que espera su confirmación

Escenario: pendientes del admin
  Dado el sistema sembrado
  Cuando entra el admin
  Entonces ve una entidad por aprobar, propuestas sin decidir, un CSV con filas con error y mensajes de contacto sin responder
```

## 6. Validación (Validation)

- Tests unitarios del guion (`python -m unittest` en `demo/seeder`): fechas coherentes, ids
  cruzados y misiones acordes a su categoría.
- `docker compose -f docker-compose.yml -f docker-compose.seed.yml config`.
- Corrida completa con Docker, con la seguridad encendida, y el recorrido de las cuentas en el
  navegador.
- `node scripts/agent-check.js`.

## 7. Especificación Técnica de Diseño (Technical Design)

- **Two-Gate (AGENTS.md §9.1):**
  - Gate A: no hay una decisión nueva del sistema; es una herramienta de demo que usa las APIs y
    las bases como están.
  - Gate B: no aplica, porque no cambia la comunicación, la persistencia ni la seguridad de ningún
    servicio.
  - **Sin ADR.**
- **Plan TDD:** antes de los escritores van los tests del guion, que es la parte pura:
  1. cada donación tiene una fecha dentro de la ventana y estados en orden;
  2. el avance de cada misión no pasa su objetivo y sigue el orden de su categoría;
  3. cada entrega histórica de Logística tiene su donación independiente y su entidad.
- **Etapas del seeder:**
  1. esperar la salud de los servicios;
  2. Keycloak;
  3. catálogo;
  4. cuentas por API;
  5. historia por base;
  6. fotos en MinIO;
  7. lo de hoy por API;
  8. verificación.

## 8. Riesgos

- **Esquema que cambia:** si una migración nueva cambia una tabla, el SQL del seeder falla. El
  seeder valida las columnas que usa antes de escribir y, si falta alguna, se detiene con el nombre
  de la tabla.
- **Huecos del sistema** que aparecieron en el relevamiento y quedan para sus dueños:
  - Logística no geocodifica, así que el seeder les pone coordenadas a mano a las entregas.
  - Con la seguridad encendida, la foto de una destacada da 403 a un visitante.
  - Incentivos llama a Notificaciones sin token.
  - El gateway no tiene la ruta `/donaciones-independientes`.
  - Donaciones consume `ruta.asignada` y `ruta.iniciada` por colas distintas. Si la ruta arranca
    antes de que llegue la asignación, la donación no pasa a `EN_TRASLADO` y después rechaza
    `ENTREGADA` (`BusinessStateException`): Logística la da por entregada y Donaciones no. El seeder
    espera a `ListaParaEntregar` antes de arrancar la ruta.
  - Cada corrida del algoritmo vuelve a proponer el stock que ya está en propuestas pendientes:
    el admin ve la misma propuesta repetida. El seeder descarta las sobrantes de su propia corrida.
  - El aviso de bienvenida (`DONANTE_REGISTRADO`) muestra un "Usuario / Password" que no es el de
    la cuenta.
  - `.env` está versionado aunque figura en `.gitignore`: el seeder no lo toca y `DEMO_PASSWORD`
    se pasa por el entorno.
- **Tiempo del simulador:** el camión llega a cada parada a los cinco minutos de la anterior. Para
  presentar más tarde, `seeder vivo` arma otra ruta en el momento.
