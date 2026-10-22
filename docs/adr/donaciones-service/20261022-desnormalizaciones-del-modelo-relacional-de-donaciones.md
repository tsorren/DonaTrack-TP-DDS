# Desnormalizaciones del modelo relacional de Donaciones

- Status: proposed
- Date: 2026-10-22
- Deciders: Decisión Grupal (DER de Donaciones, versión del 19/10/2026)
- Tags: persistencia, desnormalización, der, donaciones, entrega-5

## Contexto y Problema

La Entrega 5 pide persistir el modelo de cada servicio en una base relacional con un ORM y «contemplar estrategias de desnormalización cuando resulte necesario para optimizar consultas de lectura».

Normalizar es guardar cada dato en un solo lugar para que no se contradiga. Desnormalizar es repetir un dato a propósito para que una lectura frecuente no tenga que cruzar tablas (*joins*) ni recalcular agregados (`SUM`, `COUNT`). El costo es que el dato repetido puede desincronizarse.

El DER vigente de Donaciones ya incluye varias desnormalizaciones, decididas por el equipo al diseñarlo. Este ADR las registra y justifica una por una con el mismo criterio: **qué se repite, qué lectura acelera, quién lo mantiene y qué pasa si se desincroniza**. También deja constancia de las alternativas que no se adoptaron.

## Atributos de Calidad y Drivers de Decisión

* **Rendimiento de las lecturas frecuentes:** las búsquedas del algoritmo de asignación (corre todos los días sobre las donaciones en depósito) y las pantallas de donante y de entidad (UI 5 y 11).
* **Trazabilidad:** el historial de estados y lo donado tienen que mostrar lo que pasó en su momento, aunque después cambien los datos de referencia.
* **Integridad:** cada dato repetido tiene un único punto de escritura, para que la desincronización no dependa de que alguien se acuerde de actualizar dos lugares.
* **Simplicidad:** no se agregan mecanismos (triggers, vistas materializadas, jobs) que el equipo no necesite hoy.

## Alternativas Consideradas

* **Modelo totalmente normalizado:** ningún dato repetido. Todo estado actual se deriva del historial, la dirección se arma con cuatro tablas y la entidad asignada se obtiene de la necesidad.
* **Desnormalizaciones del DER (D1 a D6, abajo):** repetir datos solo donde una lectura frecuente lo justifica, con un único punto de escritura por dato.
* **Desnormalizaciones adicionales, no incluidas en el DER:** el donante copiado en `donacion_independiente`, un acumulado asignado en `necesidad` / `periodo_necesidad`, y una vista materializada con las donaciones recibidas por entidad en el último trimestre.

## Resultado de la Decisión

Alternativa elegida: **las desnormalizaciones del DER**.

| # | Qué se repite | Lectura que acelera | Quién lo mantiene | Si se desincroniza | Implementación |
|---|---|---|---|---|---|
| **D1** | Localidad, provincia y país como texto en `direccion`, en vez de tablas `localidad` / `provincia` / `pais` | Leer una dirección (personas, entidades, depósito) sin tres joins | Nadie: la dirección es inmutable y compartida (D-9 de la bitácora) | No aplica: no hay actualización. Un lugar puede quedar escrito de dos formas («CABA» / «Capital Federal») | `[OBSERVED]` `V3__direccion.sql` |
| **D2** | El estado actual en `donacion.estado_donacion` y `donacion_independiente.estado_actual`, además del historial (`cambio_estado_donacion`, `estado_donacion_independiente` + `cambio_estado_donacion_independiente`) | «Donaciones en depósito» (`findEnDeposito`, en cada corrida del algoritmo) y los filtros por estado (UI 5 y 11), sin buscar el último cambio de cada donación | El mismo guardado del agregado: la transición del State Pattern cambia el estado y agrega el cambio al historial, en la misma transacción | El estado actual manda para operar; el historial queda como auditoría. Una inserción manual en el historial por fuera del dominio los desalinea | `[PROPOSED]` Pasos 7 y 8 |
| **D3** | En `donacion_independiente`: `id_necesidad`, `id_periodo` y `id_beneficiario_asignado`, aunque la entidad se pueda deducir de la necesidad y la necesidad del período | «Donaciones asignadas a mi entidad» (UI 11), el conteo por entidad del algoritmo de sub-atendidos y el acumulado de un período, cada uno con una sola columna indexada | La asignación (`PosibleFragmentacion.confirmar` / aprobación de la propuesta), que escribe las tres a la vez | Una donación podría quedar con una entidad distinta de la de su necesidad. Se mitiga con un único punto de escritura y un test que lo verifique. Las tres son opcionales: una donación se puede asignar directo a una entidad, sin necesidad | `[PROPOSED]` Paso 8 |
| **D4** | Texto del bien copiado en los ítems: `item_donacion_normalizado.bien_nombre` / `bien_descripcion` e `item_donacion_independiente.bien_descripcion`, sin tabla `Bien` (P-2) | Mostrar qué se donó sin reconstruir el bien original, y armar la descripción de una donación independiente | La segmentación, que copia el texto una sola vez | No se desincroniza en un sentido que importe: es una foto de lo donado; si se corrige el ítem original, la donación ya segmentada conserva lo que se segmentó | `[PROPOSED]` Pasos 7 y 8 |
| **D5** | Las dos subclases de `Necesidad` en una sola tabla (`SINGLE_TABLE`, con `periodo` nulo en las extraordinarias) | El matching lee todas las necesidades activas de una vez, sin joins por subtipo | Hibernate, con el discriminador `tipo_necesidad` | No aplica: es una decisión de herencia, ver el ADR de mapeo ORM | `[OBSERVED]` `V1` y `V6` |
| **D6** | El mensaje hacia Logística completo en `outbox_logistica.payload` (`JSONB`), además de las tablas de las que sale | El relay reenvía el mensaje sin volver a armarlo desde varias tablas, y reintenta exactamente lo mismo | La transacción que crea la solicitud de entrega (Transactional Outbox) | Es una foto a propósito: el reintento tiene que mandar lo que se decidió en su momento | `[PROPOSED]` Paso 10 |

**Alternativas adicionales que no se adoptan** (no están en el DER):

* **Donante copiado en `donacion_independiente`:** el filtro «mis donaciones» (UI 5) se resuelve con un join a `donacion` por `id_donacion`, indexado. Se puede agregar más adelante si la medición lo justifica, porque es una columna inmutable y de riesgo casi nulo.
* **Acumulado asignado en `necesidad` / `periodo_necesidad`:** se calcula con `SUM` sobre las donaciones asignadas, apoyado en D3 (`id_necesidad` e `id_periodo`). Un contador puede desviarse sin aviso y obligaría a reconciliarlo. Mientras tanto, «no satisfechas» se filtra en memoria después de leer las activas (Paso 5). Es una decisión distinta del «puente» temporal descartado en P-4.
* **Vista materializada de donaciones recibidas por entidad en el trimestre:** con D3, el conteo del algoritmo de sub-atendidos sale de una consulta agregada sobre `id_beneficiario_asignado`. La vista agregaría un refresco programado y una lectura por fuera del ORM, que hoy no hacen falta.

## Consecuencias

### Positivas

* Las lecturas más frecuentes (algoritmo de asignación, pantallas de donante y entidad) se resuelven con una tabla y un índice.
* El historial y lo donado quedan fijos: muestran lo que pasó, no lo que dicen hoy los datos de referencia (D1, D4, D6).
* Cada dato repetido tiene un único punto de escritura, dentro de la misma transacción que el dato original.

### Negativas

* Hay datos repetidos que la base no controla. Su consistencia depende del dominio y de tests que la verifiquen (D2, D3).
* D1 permite que un mismo lugar aparezca escrito de varias formas, y agrupar por zona queda librado a cómo se cargó (Logística tomó el camino opuesto: ver su ADR de direcciones normalizadas).
* Las consultas agregadas (acumulado de un período, conteo por entidad) siguen siendo `SUM` / `COUNT` en cada lectura.

### Validación

Se valida mediante:

1. Tests de persistencia con Postgres real (Testcontainers) por cada desnormalización implementada: D1 ya los tiene (`DireccionesPersistenciaTest`); D2, D3 y D4 los suman en los Pasos 7 y 8; D6 en el Paso 10.
2. Para D2, un test que verifique que el estado actual coincide con el último cambio del historial después de cada transición.
3. Para D3, un test que verifique que, después de una asignación a una necesidad, `id_beneficiario_asignado` es la entidad de esa necesidad e `id_periodo` pertenece a ella.

## Análisis de Alternativas

### Modelo totalmente normalizado

#### Pros

* Una sola fuente de verdad para cada dato; nada que sincronizar.

#### Contras

* El estado actual de cada donación sale de buscar el último cambio del historial, en la consulta más frecuente del sistema.
* Leer una dirección requiere tres joins.
* No cumple el objetivo de la entrega de aplicar desnormalización donde mejora las lecturas.

### Desnormalizaciones del DER

#### Pros

* Cada una responde a una lectura concreta del dominio, y todas tienen un único punto de escritura.
* Ya están en el DER que el equipo usa como referencia.

#### Contras

* Requieren tests que verifiquen la consistencia que la base no garantiza.

### Desnormalizaciones adicionales

#### Pros

* Bajan todavía más el costo de algunas lecturas (filtro por donante, «no satisfechas», ranking de sub-atendidos).

#### Contras

* Suman puntos de escritura (contador) o mecanismos nuevos (vista materializada y su refresco) sin una medición que muestre que hacen falta.
