# Plan E6 — Trabajo que avanza sin el servicio de autenticación

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Fecha:** 2026-11-06 · **Entrega E6:** 2026-11-13 · **Estado:** propuesta para priorizar con el equipo
> **Insumo de:** sesiones de trabajo en paralelo a "Raíces cálida" (identidad visual) y a la parte A de SPEC-06 (Keycloak).

## 1. Para qué sirve

Mientras la sesión "Raíces cálida: etapa 2" reescribe la identidad visual y el equipo monta Keycloak
(SPEC-06), hay trabajo de E6 que no depende de ninguno de los dos. Este plan lo lista, con su valor
para la entrega, su tamaño contado, sus dependencias y si choca con los archivos de esas dos líneas.
No implementa nada.

## 2. Punto de partida `[OBSERVED]`

Contrastado el 2026-11-06 contra `origin/ENTREGA_6` (08d88cee), `origin/E6_raices` (PR 926) y
`origin/E6_auth-service` (PR 927).

| Pieza | Estado |
|---|---|
| Cliente liviano | Es una maqueta: en `ENTREGA_6`, `grupo5.cliente.maqueta` (`Escenario`, `MaquetaConfig`, `VariantesInterceptor`), `web/EstilosController` y `vistas/*`; `SitioController` y el resto de la maqueta de pantallas están en `E6_raices`. Ninguna llama a un servicio. Los bloques de componentes (fases 0 a 2 y B3-01 del backlog) también están en `E6_raices`, no en `ENTREGA_6` |
| Gateway (RI-GW-01..04) | No existe |
| Huecos de backend HB-01 a HB-10, HB-14 (solo el progreso por fila), HB-15 y HB-16 | Ninguno está en `ENTREGA_6`. La subida multipart del CSV (`POST /api/donantes/archivos`, con `MinioStorageAdapter`) ya está integrada desde `E4_donaciones_minio`; `ArchivoOutputDTO` no trae progreso. `GET /donaciones-independientes` filtra por `estado`, `subcategoriaId` y `donanteId`; falta `categoriaId`, entidad asignada y fechas. `EntregasController` tiene `/mapa` y los dos `/seguimiento/...`, sin posición del camión |
| Subida multipart | `DonantesController` ya recibe `MultipartFile` (base de HB-14, integrada); la subida de fotos de entrega (HB-10) no existe |
| PRs abiertos | 924 (corrida creativa → `ENTREGA_6`), 926 (Raíces → `E6_corrida_creativa`), 927 (ADR + SPEC-06 → `ENTREGA_6`) |
| Decisiones pendientes | D1 (⏸️ en `requerimientos-e6.md`; SPEC-06 del PR 927 la cierra con Keycloak, aún sin integrar), D8, D12 🎓, D30 (D22 se cerró el 2026-11-06 con ADR-E6-07, ver tarea 20) |

## 3. Criterios de elección

- **Valor para E6:** cubre un RF o RNF que la cátedra pide `[DOCUMENTED]` o desbloquea uno.
- **Independencia:** no necesita token, Keycloak ni la sesión del cliente.
- **Choque:** toca archivos que reescribe Raíces cálida (`cliente-liviano/src/main/resources/static/css/`, `tokens.css`, plantillas con identidad) o la parte A de SPEC-06 (`auth-service/`, compose, `persistencia/init-db`).
- **Tamaño:** archivos y tests contados, no horas.

## 4. Inventario priorizado

### 4.1 Backend: huecos que la UI necesita (no tocan el cliente ni auth)

Son cambios aditivos (AGENTS.md §8.2, *Feature*). Cada uno es una tanda propia de STANDARD, con
`mvn clean test -pl <modulo> -am`. `[INFERRED]` Solo HB-05 podría exigir ADR (decisión nueva de dominio, ver nota).

| # | Tarea | RF | Tamaño `[INFERRED]` | Depende de | Choque |
|:-:|---|---|---|---|---|
| 1 | ~~**HB-07** coordenadas aproximadas en `GET /api/entregas/mapa` (y corregir el Javadoc de `EntregaMapaPublicoResponseDTO`)~~ Con la D24 revisada (ADR-E6-07) las coordenadas quedan exactas: la tarea se reduce al Javadoc y la absorbe la tarea 22 | RF-02, RNF-PRIV-01 | ~3 archivos · 1 servicio | — | No |
| 2 | **HB-03 y HB-04** filtros por `categoriaId`, entidad asignada y rango de fechas en `GET /donaciones-independientes` | RF-05, RF-11 | ~5 archivos · 1 servicio | — | No |
| 3 | **HB-15** paginado y búsqueda por nombre en `GET /api/entidades` | RF-06, RF-22 | ~4 archivos · 1 servicio | — | No |
| 4 | **HB-08** DTOs públicos de entidades y necesidades (D26: nunca el DTO interno con campos ocultos) | RF-22, RF-06 | ~6 archivos · 1 servicio | #3 | No |
| 5 | **HB-16** listado de entregas activas de un donante o una entidad en una llamada. **Decidido:** `donanteId` opcional en el comando y en `Entrega` ([ADR](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md), `proposed`) | RF-09, RF-14 | ~5 archivos · 1 servicio | — | No |
| 6 | **HB-05 y HB-06** `PosicionCamion`, puerto `FuentePosicionCamion` con adaptador simulado, campo aditivo en `SeguimientoEntregaResponseDTO` | RF-09, RF-14, RF-CL-07 | ~8 archivos · 1 servicio | D22, cerrada (ADR-E6-07) | No |
| 7 | **HB-01 y HB-02** `GET /donaciones/destacadas` (parámetro `periodo`) y contadores públicos de la landing. **Decidido:** Donaciones replica las fotos de `entrega.exitosa` ([ADR](../adr/20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md), `proposed`) | RF-01 | ~8 archivos · 2 servicios | Fotos por `entrega.exitosa` (ADR de réplicas) y su URL estable (ADR-E6-08, tarea 8) | No |
| 8 | **HB-10** `POST /api/entregas/{id}/fotos` multipart → MinIO, sin EXIF, bucket privado, URL firmada | RF-12, RNF-PRIV-03/04 | ~8 archivos · 1 servicio + compose | MinIO ya está en el compose | Toca `docker-compose.yml`, que A1-01 también edita: coordinar |
| 9 | **HB-14** progreso con resultado por fila del CSV (la subida multipart → MinIO ya está en `ENTREGA_6`; falta el progreso en `ArchivoOutputDTO`) | RF-20 | ~6 archivos · 1 servicio | — | No |
| 10 | **HB-09** verificar que los 4 eventos del donante y los 2 de la entidad generan notificación | RF-08, RF-13 | ~1 test de integración · 1 servicio | — | No |

Nota HB-05 `[INFERRED]`: es la única con decisión nueva (un puerto de dominio y una fuente simulada). Si
Gate A + Gate B de AGENTS.md §9.1 aplican, va un ADR `proposed`; la D23 ya la decidió en los
requerimientos, así que probablemente alcance con citarla.

### 4.2 Cliente liviano sin tocar estilos

| # | Tarea | RF | Tamaño `[INFERRED]` | Depende de | Choque |
|:-:|---|---|---|---|---|
| 11 | **Contratos del cliente (RNF-CL-01):** DTOs propios del cliente y un puerto por API con dos adaptadores: el de la maqueta (hoy) y uno REST. El enunciado exige que las vistas rendericen datos de las APIs (RQ-E6-02, RQ-E6-03) | RQ-E6-02/03 | ~10 archivos Java · 20 tests | Ninguna: se puede construir contra los HB de 4.1 o contra WireMock | **Medio:** convive con `grupo5.cliente.maqueta` en `E6_raices`; conviene arrancar desde `E6_raices` (o esperar a que 926 y 924 se integren) |
| 12 | **Páginas de error propias (RF-CL-08):** 403, 404 y 5xx con `traceId` visible; 401 queda para cuando haya login | RF-CL-08 | ~4 archivos · 4 plantillas | — | Bajo: usa las clases del catálogo, no escribe CSS nuevo |
| 13 | **Cierre de menores:** literales 6000/1000 ms en `interfaz.js` (líneas 15 y 30) y `role="status"` en los `<p class="cargando">` del donante (`donante-inicio`, `-donaciones`, `-donaciones-detalle`, `-entidades` y `-notificaciones`, 5 plantillas). El código está en `E6_raices`, no en `ENTREGA_6` | RNF-UI-05/06 | ~6 archivos | PR 926 | Bajo |
| 14 | **Pasos de los flujos críticos con checklist de microcopy (RNF-UI-04)** y mapa del sitio con la profundidad de cada pantalla (RNF-UI-02): verificar que ninguna función clave pasa de 3 niveles | RNF-UI-02/04 | 1 documento · revisión de plantillas | — | No (solo lee) |

### 4.3 Backlog de componentes (migración CSS, B3-02 en adelante)

Es el trabajo natural del cliente liviano, pero **choca con Raíces cálida**: B3-02 a B3-06 reescriben
las hojas de pantalla (`entidad-necesidades.css`, `publico-registro.css`, `entidad-seguimiento-confirmar.css`,
`privado.css` y 23 más) y Raíces cálida cambia tokens y la identidad de esas mismas pantallas. Hacerlo en
paralelo duplica el trabajo o produce conflictos en cada hoja.

| # | Tarea | Líneas | Choque |
|:-:|---|:-:|---|
| 15 | B3-02 `entidad-necesidades.css` | 575 | **Alto** |
| 16 | B3-05 `publico-registro.css` | 453 | **Alto** (y el registro lo reemplaza Keycloak: B1-02 deja `publico-registro-donante` solo en la maqueta) |
| 17 | B3-04 `entidad-seguimiento-confirmar.css` | 387 | **Alto** |
| 18 | B3-03 `privado.css` | 239 | **Alto** |
| 19 | B3-06 las 23 hojas restantes | 4.070 | **Alto** |

Recomendación `[PROPOSED]`: esperar a que Raíces cálida cierre la etapa 2 (o fije sus tokens) y
retomar B3 desde la rama que la integre. Si hay que avanzar ya, B3-03 (`privado.css`, la más corta) es
la de menor riesgo, y solo con la rama de Raíces cálida como base.

### 4.4 Decisiones y documentación

| # | Tarea | Tamaño | Choque |
|:-:|---|---|---|
| 20 | **D22 (polling vs SSE) para el camión:** matriz de alternativas y ADR `proposed` si aplican los dos gates. Desbloquea la tarea 6. **Redactada (2026-11-06):** [ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md), `proposed`, con la D24 revisada | 1 documento · 1 ADR | No |
| 21 | **Integrar los PR 924 y 926 a `ENTREGA_6`** (revisión humana): sin eso el cliente real no tiene base común y las tareas 11 y 12 parten de una rama distinta de `ENTREGA_6` | Revisión, no código | Es la dependencia que más pesa |
| 22 | **Deuda documental de §13 de requerimientos-e6:** `contratos-rest.md` sin `/mapa` ni `/seguimiento/...`; Javadoc de `EntregaMapaPublicoResponseDTO`; DTI-09 y DTI-14 con fecha sin respaldo | 3 documentos · 1 Javadoc | No |
| 23 | **Script de demo de E6 punta a punta sin login** (recorrido de las pantallas públicas contra el backend real en el compose), para la defensa del 13/11 | 1 script · 1 doc | Bajo |

## 5. Orden recomendado

La entrega es el 13/11. Con el camino crítico de los requerimientos (R1: IdP + gateway + login + una
pantalla por rol), esto es lo que más acerca a un cliente que muestre datos reales:

1. **Ahora, en paralelo y sin choque:** tareas 1, 2, 3, 5 y 10 (backend aditivo, una por servicio y por sesión, sin tocar los mismos archivos).
2. **Con D22 cerrada (tarea 20, ADR-E6-07):** tarea 6, y las 4, 7, 8 y 9.
3. **En cuanto se integre 926:** tareas 11, 12 y 13 sobre esa base.
4. **Cuando Raíces cálida fije tokens:** B3-03 en adelante.
5. **Cierre:** tareas 22 y 23.

Dos tareas no pueden correr juntas si tocan los mismos archivos; la columna "Choque" y los servicios
indicados bastan para repartirlas.

## 6. Lo que este plan no cubre

- Keycloak, el gateway con validación de JWT, la sesión del cliente, el login y la autorización por rol y pertenencia (SPEC-06, A1 a E2-12).
- HB-11, HB-12 y HB-13, que dependen de `sub` y del resource server.
- Estilos, tokens e identidad visual (Raíces cálida).

## 7. Evidencia y límites

- `[OBSERVED]` Los estados de §2 salen de `git ls-tree`, `grep` sobre `origin/ENTREGA_6` y la lectura de `requerimientos-e6.md`, `backlog-migracion-componentes.md` y SPEC-06 en sus ramas. No se ejecutó ningún test ni se levantó el compose.
- `[INFERRED]` Los tamaños son estimaciones por archivos y tests, no mediciones.
- `[PROPOSED]` El orden y las recomendaciones de §4 y §5.
- Los HB que figuran como faltantes se verificaron contra el código de `ENTREGA_6`; no se revisaron ramas `E4_*` o `E5_*` sin integrar más allá de `E4_donaciones_minio`, que ya está en `ENTREGA_6`.
