# El mapa público pide las entregas de un período y con tope (`desde`, `hasta`, `limite`), no por páginas

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (decisión "Mapa público" de la revisión del 2026-11-11 y aprobación de la ampliación R11-19 de la ficha pública, 2026-11-12, con las opciones recomendadas en las tres dudas); los detalles que la ficha dejaba abiertos, el constructor del lote `p1-mapa` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: logistica, contrato, rest, paginado, persistencia, mapa, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su ficha aprobada
> ([ampliación R11-19](../../interfaz/flujos/raices-publico-navegable.md#ampliación-r11-19-mapa-más-corto-lote-p1-mapa),
> sección 10, que hace de spec del contrato) y dejó los ADRs para el cierre documental de la
> oleada. La ficha decía "sin ADR nuevo"; el cierre lo escribe porque el lote elige para el mapa
> una forma de acotar distinta de la del resto de los listados y cambia lo que devuelve un pedido
> sin parámetros. `[OBSERVED]` El código ya implementa esta decisión (commits `c99cefe8d` y
> `0210cb5b4`, migración `V10__entrega_mapa_por_periodo.sql`, OpenAPI de Logística 1.10.0). Código
> existente no es aprobación: si el revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-19, `GET /api/entregas/mapa` leía todas las entregas `ENTREGADA` de la
base, sin período ni tope (`EntregasService.java:133-139`), y el cliente liviano se quedaba con las
de los últimos 30 días. La lista de la pantalla mostraba cada lugar con todas sus entregas, y los
botones de salida quedaban al final. El usuario decidió: botones debajo del mapa y lista plegada y
paginada (backlog, sección 3).

El mapa no es una lista que se recorre: tiene que dibujar todos los puntos del período a la vez
para contestar "adónde llegaron las donaciones". La lista de abajo es la alternativa accesible del
mapa (RF-CL-06) y se pagina.

Problema: ¿cómo se acota lo que devuelve Logística sin partir el mapa en páginas?

**Gate A:** decisión nueva: los listados de R11-01 acotan con `page` y `size`; el mapa acota por
período y tope, y la lista se pagina en el cliente sobre lo que ya trajo. **Gate B:** cambia el
comportamiento de un contrato público entre servicios (sin parámetros, ya no toda la historia) y la
consulta de los dos adaptadores de persistencia de Logística (PostgreSQL y MongoDB).

## Alternativas Consideradas

1. **Período y tope** (elegida): `desde`, `hasta` y `limite` opcionales, el arreglo de siempre y el
   total del período en `X-Total-Count`; el cliente agrupa por lugar y pagina la lista de a 10.
2. **`page` y `size`**, como los listados de Donaciones.
3. **Lugares ya agrupados y paginados en Logística** (un endpoint nuevo).
4. **Dejarlo como estaba** y filtrar en el cliente.

## Resultado de la Decisión

Alternativa 1:

| Regla | Valor |
|---|---|
| Parámetros | `desde` y `hasta` (`date`, inclusivos, en hora de Argentina); sin `desde`, `hasta` menos 30 días; sin `hasta`, hoy. `limite` de 1 a 500, por defecto 200 |
| Cuerpo | El arreglo de `EntregaMapaPublicoResponseDTO` de siempre, sin campos nuevos: las `ENTREGADA` con coordenadas cuya `hora_arribo` cae en el período, de la más reciente a la más vieja; a igual hora, por `idEntrega` |
| Total | `X-Total-Count`: las del período con coordenadas, sin el tope, con un conteo en la base. El servicio devuelve `max(total, recibidas)` para que la cifra nunca sea menor que lo que se muestra |
| Errores | 400 `ERR-CSR-003` (`ErrorCatalog.ARGUMENTO_INVALIDO`, sin código nuevo en `common-lib`) con `hasta` anterior a `desde`, una fecha que no se entiende o `limite` fuera de rango |
| Consulta | Período, orden y tope resueltos en la consulta de los dos adaptadores. PostgreSQL: `V10`, índice por `(estado, hora_arribo)`; MongoDB: `@CompoundIndex` equivalente en `EntregaDocument`. Reloj inyectable (`Clock`) para la fecha de hoy, con `[desde, hasta + 1)` como `Instant` |
| Cliente | Pide el último mes con `limite=200` y pasa las fechas como texto ISO (con `LocalDate` en `@HttpExchange` salían con el formato del idioma). Agrupa por lugar, el mapa recibe todos los puntos y la lista se pagina de a 10 con el paginador común; una página fuera de rango muestra la última |

**Por qué:**

- El mapa no tiene página 2: con `page` y `size`, cada página movería todos los puntos y el mapa
  dejaría de contestar "adónde llegaron".
- Agrupar en Logística sería un segundo contrato y el mapa igual necesitaría todos los puntos.
- La regla 1 del backlog pide que ningún pedido traiga la historia entera: el período y el tope lo
  cumplen, y el total sigue siendo un número.

## Consecuencias

**Positivas**

- Lo que llega del mapa público está acotado (200 por defecto, 500 como mucho), aunque la historia
  crezca.
- La cifra con tope dice el total y lo que se muestra ("El mapa muestra las 200 más recientes").

**Negativas**

- Un período con más entregas que el tope muestra solo las más recientes en el mapa; la lista
  también.
- Sin parámetros, el endpoint devuelve 30 días y no toda la historia: oasdiff no lo ve. Se anotó a
  mano en `docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt` (bloque R11-19). El único
  consumidor es el cliente liviano, que ya se quedaba con 30 días.
- La paginación de la lista vive en el cliente liviano: es una página sobre un conjunto acotado, no
  una lista completa de un servicio, pero no es el patrón de los otros listados.
- `[INFERRED]` Los dos adaptadores filtran "con coordenadas" con reglas que se escriben distinto
  (en MongoDB `destino.ubicacion` presente; en JPA, latitud y longitud no nulas); son equivalentes
  mientras el subdocumento guarde `ubicacion` solo con coordenadas ([DTI-42](../DEUDA_TECNICA.md#dti-42)).
- Con datos reales, la fila dice el lugar y no la organización: Logística no sabe quién recibió y
  cruzarlo toca la privacidad de D24 (duda 1 de la ficha, al backlog).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. Período y tope** | El mapa ve todo el período; un pedido por visita; consulta con índice | Un criterio de acotar distinto de `page`/`size`; la lista se pagina en el cliente |
| 2. `page` y `size` | El mismo patrón que Donaciones | El mapa mostraría solo una página de puntos |
| 3. Lugares agrupados en Logística | La lista llega paginada del servicio | Segundo contrato; el mapa igual pide todos los puntos |
| 4. Como estaba | Sin cambios | La historia entera en cada visita (regla 1) |

## Relaciones

- **Se aparta, a propósito, de:** el [ADR de listados paginados de Donaciones](../donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  en cómo se acota; comparte el arreglo en el cuerpo y el total en `X-Total-Count`.
- **Respeta:** la [posición del camión: fuente, privacidad y actualización](../20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)
  (D24, el mapa público sin dirección exacta en texto) y la
  [persistencia documental de Logística en MongoDB](./20261025-persistencia-documental-de-logistica-en-mongodb.md).
- **Ficha:** [ampliación R11-19 de la ficha pública](../../interfaz/flujos/raices-publico-navegable.md#ampliación-r11-19-mapa-más-corto-lote-p1-mapa).
- **Deuda:** [DTI-42](../DEUDA_TECNICA.md#dti-42).
