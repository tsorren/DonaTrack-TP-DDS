# Seguimientos de Logística e historial del ranking de Incentivos, paginados por defecto

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación automática de la spec `R11-p2-listas-restantes` con las opciones recomendadas, 2026-11-12); el detalle de la construcción, el constructor del lote `p2-listas-restantes` (agente, sin consulta); este registro, el cierre documental de la oleada P2 (agente). Pendiente de aprobación del equipo en el PR
- Tags: logistica, incentivos, contrato, rest, rendimiento, paginado, cliente-liviano, r11, entrega-6

> **Escrito después del código**, en el cierre documental de la oleada P2 (2026-11-12), como desvío
> acordado del ciclo ARCHITECTURAL (la guía de orquestación del backlog R11 deja los ADR de las rutas
> CONTRATO para el cierre de cada oleada). `[OBSERVED]` El código actual ya implementa esta decisión
> (lote `p2-listas-restantes`, `dbd73deb4` y arreglos `a1b90f19e`). Código existente no es
> aprobación: si el revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes del lote, `GET /api/entregas/seguimiento/beneficiarios/{id}` devolvía todas las
entregas de la organización en cualquier estado, `GET /api/entregas/seguimiento/donantes/{id}`
todas las activas del donante, y `GET /api/incentivos/ranking/historial` todos los meses
calculados, sin orden fijo. Las pantallas de la organización (Recibidas, Seguir entregas,
Confirmar recepción, Necesidades y Notificaciones) leían además "una página de 100 asignadas" de
Donaciones para cruzar y contar en memoria. Eso choca con la regla general 1 del
[backlog de la revisión del 2026-11-11](../interfaz/backlog-revision-2026-11-11.md#2-reglas-generales-nuevas)
("nunca se traen todos los resultados"), y con 20.000 donantes no escala.

`[DOCUMENTED]` La spec del lote decía "ADR: ninguno nuevo", porque aplica a otros servicios el
patrón ya decidido en el [ADR de listados paginados de Donaciones](./donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
(`page`, `size`, total en `X-Total-Count`). El cierre lo registra igual porque hay una parte nueva:
**dos endpoints públicos que ya existían cambian lo que devuelven sin parámetros** (antes, todo;
ahora, la primera página). Es un cambio de comportamiento de un contrato vivo (AGENTS.md §8.2), del
mismo tipo que el que llevó al [ADR del mapa por período](./logistica-service/20261112-mapa-publico-por-periodo-y-con-tope.md).

Problema: ¿cómo dejan de devolver listas enteras dos servicios cuyos endpoints ya tienen clientes,
sin romperlos y sin una lectura a la base por entrega?

**Gate A:** decisión nueva para Logística e Incentivos (el patrón existía solo en Donaciones y
Notificaciones) y cambio del valor por defecto de dos contratos. **Gate B:** contrato entre
servicios y rendimiento (trade-off deliberado entre compatibilidad y regla 1).

## Alternativas Consideradas

1. **Paginar por defecto** los dos seguimientos y el historial: sin `page` ni `size`, la primera
   página (20 entregas; 12 meses en el historial); `size` hasta 100; total en `X-Total-Count`;
   filtros `estado` e `idDonacion` repetibles en Logística (elegida).
2. **Paginar solo si se piden `page` o `size`**, y sin ellos seguir devolviendo todo.
3. **Endpoints nuevos paginados** (`/seguimiento/.../pagina`) y los viejos sin tocar, marcados como
   obsoletos.
4. **Status quo:** que el cliente siga trayendo todo y recorte en memoria.

## Decisión

Elegida la alternativa 1.

- **Logística.** Los dos seguimientos aceptan `page`, `size`, `estado` (repetible, `EstadoEntrega`)
  e `idDonacion` (repetible, hasta 100) y paginan en la base (`EntregasRepositoryJpaAdapter`, con
  `ConsultaSeguimiento`). Cada página se arma con una lectura de las rutas, una de los camiones y
  una de las entregas de esas rutas, no una por entrega. Orden fijo (`CriterioSeguimiento.ORDEN`):
  las que no llegaron primero, después por llegada y salida, de la más reciente a la más vieja,
  con desempate por id. Sin `estado`, el seguimiento del donante sigue trayendo solo las tres
  activas de antes. Permisos sin cambios. Un `estado` que no existe da 400.
- **Incentivos.** `GET /api/incentivos/ranking/historial` pagina en la base del mes más reciente al
  más viejo; sin parámetros, la página 0 de 12 meses (`HistorialDeRankings.TAMANIO_POR_DEFECTO`),
  porque el menú "Mes" de la pantalla muestra los de la página. `/ranking/{periodo}` y `/ultimo`
  no cambian; la pantalla pide `/{periodo}` cuando el mes de `?mes=` está en otra página.
- **Clientes.** El cliente liviano cambia todas sus llamadas en el mismo lote. El seguimiento del
  donante pide `size=100` (`LogisticaApi.TOPE`), el mismo tope que ya tenía su lista de
  Donaciones, hasta que su lote lo pagine. Las pantallas de la organización piden en Logística solo
  las entregas de las donaciones de la página (`idDonacion` repetido), y Notificaciones de la
  organización deja de leer las asignadas de Donaciones.
- **Contratos escritos:** `openapi-logistica.yaml`, `openapi-incentivos.yaml` y
  `openapi-donaciones.yaml`, con los parámetros nuevos y el encabezado.

## Consecuencias

- Bueno: ninguna de esas pantallas trae una lista entera de Logística ni de Incentivos; las cifras
  salen de un conteo (ver el [ADR de las cifras de la organización](./donaciones-service/20261112-cifras-de-la-organizacion-como-conteos-y-orden-de-lo-que-hay-que-atender.md)).
- Bueno: el orden es fijo y estable entre páginas, lo que hace previsible el paginador.
- Malo: un cliente que llame sin parámetros esperando todo recibe la primera página sin aviso.
  `[OBSERVED]` Hoy el único cliente es el cliente liviano, que cambió en el mismo lote; el seeder,
  `integration-tests` y el gateway no leen esas rutas esperando todo (spec §7). Si oasdiff lo
  marca, va a `cambios-incompatibles-aceptados.txt`.
- Malo: el seguimiento del donante sigue con un tope fijo de 100 (con más, se queda corto sin
  aviso), y los puestos de un mes del ranking llegan todos dentro de cada mes
  ([DTI-43](./DEUDA_TECNICA.md#dti-43)).
- Neutro: `[VERIFIED]` `run-preprod-tests.sh` en el cierre de la oleada (smoke 4/4; contrato, integración y E2E 26/26, stack propio) y el recorrido con
  login real de cada rol pasaron con los seguimientos paginados.

## Análisis de las alternativas descartadas

- **2. Paginar solo si se pide:** conserva la compatibilidad, pero deja abierta la puerta a la
  lista entera, que es justo lo que la regla 1 prohíbe; cualquier cliente nuevo que se olvide los
  parámetros vuelve al problema.
- **3. Endpoints nuevos:** duplica rutas, permisos y tests, y el viejo seguiría sirviendo listas
  enteras hasta que alguien lo borre; para un solo cliente que cambia en el mismo lote no paga.
- **4. Status quo:** el cliente recortando en memoria no baja lo que viaja ni lo que lee la base,
  y una cifra sacada del largo de una página engaña.

## Enlaces

- Spec: [R11-p2-listas-restantes](../specs/completed/R11-p2-listas-restantes-spec.md) (§6.1, §6.2 y §7)
- Patrón de origen: [listados paginados de Donaciones](./donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
- Contratos: [openapi-logistica.yaml](../arquitectura/contratos/openapi-logistica.yaml),
  [openapi-incentivos.yaml](../arquitectura/contratos/openapi-incentivos.yaml)
- Deuda: [DTI-39](./DEUDA_TECNICA.md#dti-39), [DTI-43](./DEUDA_TECNICA.md#dti-43)
