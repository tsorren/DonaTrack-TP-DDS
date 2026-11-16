# Bandeja por persona paginada en Notificaciones, con la búsqueda sin tildes por `translate` y las categorías como tipos

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p1-notificaciones`, 2026-11-12, con las opciones recomendadas; el punto 2 pide este ADR corto al cerrar la oleada); los detalles que la spec dejaba abiertos, el constructor del lote `p1-notificaciones` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: notificaciones, contrato, rest, paginado, persistencia, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-notificaciones-spec.md) y dejó los ADRs para el
> cierre documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión (commits
> `2e56394d1` y `702bcfe88`, migración `V6__indice_bandeja_por_persona.sql`, OpenAPI de
> Notificaciones 1.3.0). Código existente no es aprobación: si el revisor rechaza el ADR, puede
> hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-14, `GET /api/notificaciones/persona/{personaId}` devolvía todas las
notificaciones de la persona, sin orden definido; el cliente las traía enteras para filtrarlas,
contarlas y marcarlas (en el recorrido del cierre de P0, unos 149 KB para una organización;
[DTI-40](../DEUDA_TECNICA.md#dti-40), ítem 3). La regla general 1 del backlog pide la página, la
búsqueda, los filtros y el total resueltos en el servidor.

El formato del listado (arreglo y `X-Total-Count`, página 0 de 20 sin parámetros, tope 100) ya lo
decidió el [ADR de listados paginados de Donaciones](../donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md):
aplicarlo acá no es una decisión nueva. Lo nuevo es cómo busca sin tildes un servicio que no tiene
`unaccent`, y cómo filtra por las categorías que ve la pantalla sin que el servicio las conozca.

**Gate A:** decisión nueva en dos puntos (la búsqueda con `translate` en lugar de la extensión que
usa Donaciones, y `excluirTipo` para "Otras novedades"). **Gate B:** cambia el comportamiento de un
contrato público entre servicios (sin página, ya no todas) y la estrategia de consulta del servicio.

## Alternativas Consideradas

Para buscar sin tildes:

1. **`translate(lower(mensaje), 'áéíóúüñ', 'aeiouun') LIKE … ESCAPE`** (elegida), en SQL nativo, con
   `q` normalizado igual en Java.
2. **`unaccent`**, como Donaciones.
3. **JPQL o Criteria** con `lower` y `LIKE`.
4. **`ILIKE`** solo.

Para filtrar por categoría:

- a. **`tipo` y `excluirTipo` repetibles** (elegida); el cliente traduce cada categoría del rol a sus
  tipos, y `excluirTipo` también trae las notificaciones sin tipo.
- b. **Un valor especial `tipo=NINGUNO`** para las que no tienen tipo.
- c. **Las categorías dentro del servicio.**

## Resultado de la Decisión

Alternativas 1 y a:

| Regla | Valor |
|---|---|
| Ruta | La misma (`/api/notificaciones/persona/{personaId}` y su alias legado), el mismo permiso (`VER_SUS_NOTIFICACIONES`) y el mismo cuerpo: arreglo de `NotificacionDTO` |
| Parámetros | `leida` (como antes), `q` (hasta 100), `tipo` y `excluirTipo` (repetibles, combinados con "y"), `orden` (`recientes` o `antiguas`, desempata por `id`), `page`, `size` (por defecto 0 y 20, tope 100). En el controller se juntan en el record `FiltroBandeja` (`@ParameterObject`), no en siete `@RequestParam` |
| Total | `X-Total-Count`, siempre, también con `[]`. Página fuera de rango: `[]` con el total real |
| Errores | 400 `ERR-CSR-003` con `page` negativo, `size` fuera de 1..100, `q` largo, `orden` o tipo desconocido |
| Consulta | SQL nativo con el esquema `notificaciones` escrito: una consulta para la página y otra para el total, sin cargar el historial de estados (la bandeja no lo muestra); el repositorio en memoria aplica exactamente la misma regla |
| Índice | `V6`: `idx_notificacion_persona_fecha (persona_id, fecha_creacion DESC, id DESC)`. El conteo de sin leer sigue en `idx_notificacion_persona_leida` |
| Sin leer | El cliente las cuenta con `GET …/no-leidas/conteo` (de la cuenta, no de la página); 0 si no responde |
| Marcar | Un `PATCH /{id}/leida` por id, sin leer la bandeja; `403` y `404` se saltean: el dueño lo controla el servicio (`MARCAR_SU_NOTIFICACION`) |

**Por qué:**

- `translate` anda en PostgreSQL y en H2, no pide una extensión ni una migración que la cree en el
  esquema de Notificaciones, y como primero filtra por `persona_id` (índice) recorre solo las de
  una persona. `unaccent` habría sumado una extensión con su propio ADR todavía `proposed` en
  Donaciones; `ILIKE` no encuentra "camion" en "camión"; JPQL no tiene `translate`.
- Las categorías cambian por rol y agrupan tipos ("Problemas" son la entrega fallida y la
  vencida): son de la pantalla. Notificaciones solo conoce tipos de evento. Con `excluirTipo`, el
  cliente arma "Otras novedades" sin listar todos los tipos que existen.

## Consecuencias

**Positivas**

- Ninguna bandeja trae más de 20 notificaciones por pedido; las sin leer son un número.
- Un solo contrato de listado en Donaciones y en Notificaciones.

**Negativas**

- `translate` solo cubre las vocales con tilde, la diéresis y la eñe en minúscula: otra letra
  acentuada (una cita en otro idioma) no se normaliza. Para los textos del sitio alcanza.
- Sin página, el endpoint devuelve 20, no todas: un consumidor que no se migró pierde avisos sin
  error. El cliente liviano es el único y se migró en el mismo lote.
- `GET /api/notificaciones` (el listado general del admin, con `personaId` y `estado`) sigue sin
  página: [DTI-40](../DEUDA_TECNICA.md#dti-40), ítem 3, lote `p2-listas-restantes`.
- "Marcar todas como leídas" alcanza a toda la cuenta, no solo a la página: la nota bajo el botón
  lo dice, el texto del botón todavía no ([DTI-42](../DEUDA_TECNICA.md#dti-42)).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. `translate`** | Sin extensión; PostgreSQL y H2; el repositorio en memoria la copia | Solo las letras que se listan |
| 2. `unaccent` | Cubre todas las tildes | Extensión y migración nuevas en el esquema |
| 3. JPQL o Criteria | Sin SQL nativo | No hay `translate`: no ignora tildes |
| 4. `ILIKE` | Simple | Distingue tildes |
| **a. `tipo` y `excluirTipo`** | El servicio no sabe de pantallas; "Otras" sin listar todo | Dos parámetros |
| b. `tipo=NINGUNO` | Un parámetro | El cliente igual tiene que listar el resto de los tipos |
| c. Categorías en el servicio | El cliente no traduce | Presentación dentro del dominio de Notificaciones |

## Relaciones

- **Aplica:** el [ADR de listados paginados de Donaciones](../donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  (`proposed`), con el mismo contrato.
- **Se aparta de:** la [búsqueda sin tildes con `unaccent`](../donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md),
  por los motivos de arriba.
- **Respeta:** el [estado de las notificaciones](./20260520-estado-de-notificaciones.md) y la
  [estrategia de mapeo ORM](./20260901-estrategia-de-mapeo-orm-en-notificaciones.md).
- **Spec y ficha:** [R11-p1-notificaciones](../../specs/completed/R11-p1-notificaciones-spec.md) ·
  [ficha de flujo](../../interfaz/flujos/r11-p1-notificaciones.md).
- **Deuda:** [DTI-40](../DEUDA_TECNICA.md#dti-40) (ítem 3) y [DTI-42](../DEUDA_TECNICA.md#dti-42).
