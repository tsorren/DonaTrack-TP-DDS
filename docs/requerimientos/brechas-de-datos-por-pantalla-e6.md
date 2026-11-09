# Brechas de datos por pantalla (E6)

> **Estado:** inventario `[OBSERVED]` del 2026-11-07, con clasificación `[INFERRED]`. Insumo de
> la F2 del [plan del cliente con datos reales y gateway](plan-e6-cliente-datos-reales-y-gateway.md):
> dice qué pantallas se pueden conectar hoy y qué le falta al backend para el resto.
> Parte de la [ficha de huecos de backend de Raíces](../interfaz/flujos/raices-ab-huecos-backend.md)
> (2026-11-02) y la re-verifica contra el backend actual.

## 1. Cómo se armó

- **Pantallas:** las plantillas de `E6_raices` en `d2ba4b6a`, después del merge del PR 935 (86
  plantillas con prefijo de rol en la raíz de `templates/`, con las variantes de estado agrupadas en su pantalla). Para cada pantalla se leyó qué datos de
  ejemplo usa.
- **Backend:** controllers y DTOs de `origin/ENTREGA_6` (`e43e50a9`). El endpoint de cada pantalla
  sale de la tabla RF de los [requerimientos](requerimientos-e6.md) §6.1.
- **Lectura:** dos exploraciones de solo lectura, una por cada par de roles. Las afirmaciones que
  deciden la clasificación se releyeron en el código: los filtros de `FiltroDonacionesIndependientes`,
  `NotificacionDTO`, `RankingMensualDTO`, `CategoriaOutputDTO` y el filtro de activas de
  `EntidadBeneficiariaService`.
- No se corrió la aplicación.

Estados de una pantalla:

| Marca | Quiere decir |
|---|---|
| 🟢 | Conecta ya: cada dato lo da un DTO o se deriva, sin cambiar el backend, aunque cueste llamadas de más |
| 🟡 | Conecta perdiendo datos (se nombran) |
| 🔴 | Bloqueada: falta el endpoint o la operación |
| ⚪ | Sin datos de API: texto fijo |

Por dato: ✅ lo da un DTO · 🔁 se deriva (de otra llamada, del orden o de otro campo) · ❌ no existe.

**Todas las pantallas privadas** necesitan además saber quién es la cuenta. Hasta el login, sale
del puerto `CuentaActual`, con el id por parámetro (decisión E de ese plan). El estado de abajo es
el de los datos.

## 2. Público

| Pantalla | Endpoints | Estado | Qué falta o se deriva | HB |
|---|---|---|---|---|
| Entidades y necesidades (`publico-entidades`) | `/api/entidades/publicas`, `/api/necesidades/publicas`, `/api/subcategorias` | 🟡 | 🔁 categoría y **unidad**: la subcategoría trae su categoría, y la categoría su `unidad`. ❌ barrio. ❌ "qué es" (solo `rubro`, texto libre). ❌ lo recibido ("faltan 30 de 40"). ❌ período de las recurrentes. HB-18 ([PR #957](https://github.com/tsorren/DonaTrack-TP-DDS/pull/957)) no cambió `NecesidadPublicaDTO`: solo saca las vencidas | HB-18 |
| Portada | `/api/entregas/mapa`, `/api/entidades/publicas` (cabecera `X-Total-Count`) | 🟡 | 🔁 total de entidades. ❌ destacadas con foto. ❌ contadores de donantes y donaciones. ❌ qué entidades confirmaron | HB-01, HB-02 |
| Mapa de entregadas | `/api/entregas/mapa` | 🟡 | ✅ coordenadas exactas y fecha. 🔁 barrio con `localidad`. ❌ categoría, entidad, qué y foto. El dibujo usa posiciones `x, y`, no coordenadas | HB-01 |
| Donaciones realizadas | `/api/entregas/mapa` | 🔴 | El DTO excluye a propósito qué, categoría, entidad y foto | HB-01 |
| Ingresar, Recuperar, Activar, Registro de donante y de entidad | IdP | 🔴 | Dependen del login (SPEC-06). La entidad, además, del estado pendiente | HB-11, HB-12, HB-20 |
| Contacto | Ninguno | 🔴 | No hay dónde mandar el mensaje | HB-21 |
| Registrarse, Legales, Nosotros, Para donantes, Para entidades | Ninguno | ⚪ | Las cifras de Nosotros son fijas | HB-02 (cifras) |

## 3. Donante

| Pantalla | Endpoints | Estado | Qué falta o se deriva | HB |
|---|---|---|---|---|
| Mis donaciones | `/donaciones-independientes?donanteId&estado&categoriaId` | 🟢 | 🔁 la categoría del filtro pasa de nombre a id con `/api/categorias`. 🔁 el número de la donación, por posición. 🔁 el destino: `entidadBeneficiariaId` (sumado al DTO al conectarla, 2026-11-07) y una llamada a `/api/entidades/{id}` por entidad distinta | HB-03 y HB-04, hechos |
| Misiones e insignias | `/api/incentivos/donantes/{id}/misiones`, `/insignias`, `/donantes/{id}`, `/ascensos`; tipo de persona en Donaciones | 🟡 | 🔁 "en espera" y "se abre después", por el orden de la lista y la categoría del donante. ❌ unidad del progreso ("12 de 50 **bienes**"). ❌ número de misión. ❌ "tu donación más grande". Cuatro llamadas a dos servicios | — |
| Notificaciones | `/api/notificaciones/persona/{id}`, conteo, marcar leídas | 🟡 | ✅ mensaje, fecha y leída. ❌ tipo de evento (sin sello) e id de la donación (sin enlace). El evento de entrada los trae; `NotificacionDTO` no los devuelve | HB-09 (nuevo campo) |
| Detalle de una donación | `/donaciones-independientes/{id}`, seguimiento, `/api/entregas/{id}` | 🟡 | ✅ etapas y fechas (historial propio de la donación). 🔁 fotos con `fotosRecepcion` de la entrega. 🔁 causa del problema, con la justificación del cambio. ❌ nueva salida. ❌ nombre visible. Conectada el 2026-11-07: el número y la pertenencia salen del listado del donante | HB-20 |
| Inicio | Donaciones, seguimiento por donación, conteo de notificaciones | 🟡 | 🔁 entidades que confirmaron, con una llamada por entidad. ❌ nueva fecha de salida | HB-16 |
| Seguimiento | Seguimiento por donación (una llamada por donación) | 🟡 | ✅ estado, paradas, patente y última actualización. ❌ posición del camión. ❌ nombre del destino | HB-05, HB-06, HB-16 |
| Entidades | `/api/entidades?nombre&page&size`, `/api/necesidades?entidadId` | 🟡 | ✅ HB-18: filtro por `estado`; la salida suma `estado` y `periodicidad`. ❌ lo recibido de una extraordinaria: el contrato da su `estado`, no la cantidad (en las recurrentes sale de `/periodos`) | HB-18 |
| Mi perfil | `GET /api/personas`, `PUT /api/personas/{id}` | 🟡 | ✅ datos de la cuenta y medios. ❌ nombre visible. ❌ "Qué te avisamos". Sin `/me` | HB-11, HB-20 |

## 4. Entidad

| Pantalla | Endpoints | Estado | Qué falta o se deriva | HB |
|---|---|---|---|---|
| Notificaciones | Las mismas del donante | 🟡 | ❌ tipo de evento e id de la donación o la entrega (el botón "Confirmar" no tiene a dónde ir) | HB-09 (nuevo campo) |
| Seguir entregas | `/api/entregas/seguimiento/beneficiarios/{id}`, `/api/entregas/{id}` | 🟡 | ✅ estado, paradas y patente, en una llamada. 🔁 dirección exacta con la entrega. ❌ posición del camión. El endpoint trae también las entregadas: las filtra el cliente | HB-05, HB-06 |
| Donaciones recibidas | `/donaciones-independientes?entidadId&estado&desde&hasta` | 🟡 | 🔁 quién donó, con una llamada por donación. ❌ el pedido que cubre (se guarda, pero el DTO no lo devuelve). Las fechas filtran por registro, no por asignación | HB-17, HB-20 |
| Necesidades | CRUD de `/api/necesidades` | 🟡 | ✅ HB-18: `unidad`, `estado`, `fechaLimite`, `periodicidad`, `version` (409), `ultimoCambio` y la historia en `/api/necesidades/{id}/periodos` y `/donaciones-independientes?necesidadId=`. ❌ lo recibido de una extraordinaria como cantidad | HB-17, HB-18 |
| Inicio | Seguimiento, donaciones, necesidades, conteo | 🟡 | 🔁 lo recibido por pedido: el `estado` de cada necesidad y, en las recurrentes, `/periodos` (HB-18). "Espera confirmación" no es un estado de la entrega | HB-18 |
| Confirmar recepción (y Llegó con un problema) | `PATCH /api/entregas/{id}/estado`, `PATCH .../fotos` | 🟡 | ✅ fotos por URL. ❌ subir el archivo. ❌ remito y cantidad recibida parcial. ❌ pedido que cubre | HB-10, HB-17 |
| Mi perfil | `/api/entidades/{id}`, `PUT /api/personas/{id}` | 🟡 | ❌ "qué es" como lista. ❌ "Qué te avisamos". Sin `/me` | HB-11 |

## 5. Admin

| Pantalla | Endpoints | Estado | Qué falta o se deriva | HB |
|---|---|---|---|---|
| Ranking | `/api/incentivos/ranking/ultimo`, `/{periodo}`, `/historial` | 🟡 | ✅ período, entradas y podio. 🔁 nombre del mes. ❌ solo la fecha de cálculo ("Calculado el 30 de septiembre"): `RankingMensual` no la guarda, así que suma un campo en el dominio, la persistencia y el DTO de Incentivos | Nuevo |
| Detalle de una donación | `/donaciones-independientes/{id}`, `PATCH .../estado` | 🟡 | ✅ historial con actor y la marca de vencida. ❌ nombre del depósito. ❌ entidad asignada | — |
| Camiones | `/api/camiones`, `PATCH .../estado` | 🟡 | 🔁 capacidad. ❌ los deshabilitados (el listado trae solo activos). ❌ motivo y fecha del último cambio | — |
| Donaciones | `/donaciones-independientes`, `/api/donaciones` | 🟡 | 🔁 donante y estado, uniendo las dos listas. ❌ depósito, entidad, filtro por vencimiento | — |
| Asignar donaciones | `/api/asignaciones/...` | 🟡 | 🔁 entidad del pedido (3 llamadas) y donante. ❌ algoritmo y puntaje. ❌ motivo y fecha de la decisión (se reciben, no se guardan) | — |
| Donantes (registrar, importar CSV) | `/api/donantes`, `/api/personas`, `/api/donantes/archivos` | 🟡 | ❌ búsqueda y fecha de alta. ❌ avance por fila del CSV | HB-14 |
| Inicio | Varias listas y `/api/incentivos/admin/resumen` | 🟡 | ✅ total de donantes. ❌ entidades por aprobar. 🔁 el resto de las cifras, contando listas | HB-12 |
| Entidades (registrar y aprobar) | `/api/entidades`, `/api/personas` | 🔴 | ❌ estado pendiente, aprobar, pedir corrección y rechazar | HB-12, RF-21 |

## 6. Lo que más destraba

Huecos que se repiten en varias pantallas, de más a menos pantallas:

| Hueco | Pantallas | Ya tiene HB |
|---|---|---|
| Identidad de la cuenta (`/me`, pertenencia) | Todas las privadas | HB-11, HB-13 (con el login) |
| Lo recibido y el período de cada necesidad | Entidades y necesidades, Entidades del donante, Necesidades, Inicio de la entidad | HB-18 |
| Tipo de evento e id de la donación o la entrega en `NotificacionDTO` | Notificaciones del donante y de la entidad | **No**. Aditivo en el contrato, pero `Notificacion` tampoco los guarda: toca dominio, esquema y DTO |
| Posición del camión | Seguimiento del donante, Seguir entregas de la entidad | HB-05, HB-06 |
| Pedido que cubre cada donación (`necesidadId` en el DTO) | Donaciones recibidas, Confirmar recepción | HB-17 |
| Destacadas con foto y contadores | Portada, Donaciones realizadas | HB-01, HB-02 |
| "Qué es" la entidad como lista fija | Entidades y necesidades, Registro de entidad, Mi perfil de la entidad | **No** |

Huecos nuevos que no estaban en la ficha: período y recibido de las necesidades (el período sí
está en HB-18), fecha de cálculo del ranking, "activa desde" de una entidad y preferencias de avisos
del perfil de la entidad.

## 7. Candidatas para la primera pantalla real

La F0 del plan es el esqueleto; esta es la primera rebanada de la F2, que se hace junto con él.

| Pantalla | Por qué | Contra |
|---|---|---|
| **Ranking del admin** | Un servicio, un endpoint, una sola falta. El admin no tiene "mis datos": no necesita `CuentaActual` | La fecha de cálculo pide un cambio en Incentivos (o se oculta hasta tenerlo). Su plantilla está en `E6_raices`, todavía no en `ENTREGA_6` |
| Mis donaciones | 🟢: los filtros ya existen | Necesita `CuentaActual` y una llamada por entidad para el destino (conectada el 2026-11-07) |
| Entidades y necesidades | Pública y en `ENTREGA_6`; la unidad se deriva | Pierde barrio, "qué es", lo recibido y el período |

## 8. Líneas de la ficha que cambiaron

Contra la [ficha de huecos](../interfaz/flujos/raices-ab-huecos-backend.md):

- **Resueltas:**
  - filtro por categoría (HB-03) y donaciones de un donante por entidad (HB-04, con `donanteId` y
    `entidadId`);
  - paginado y búsqueda de entidades (HB-15);
  - notificaciones de la entidad por evento (HB-09, en código);
  - entregas activas de una entidad en una llamada (HB-16, del lado de la entidad);
  - fecha del ascenso (`/ascensos`).
- **Desactualizadas:**
  - el mapa ya da coordenadas exactas (D24 revisada);
  - las etapas de una donación están en su propio historial, no solo en Logística;
  - las fotos de recepción salen de `GET /api/entregas/{id}`, aunque el seguimiento no las traiga;
  - la cita de la divergencia `donanteId`/`idDonante` apunta a una línea vieja de
    `openapi-donaciones.yaml`.
- **Parciales:**
  - DTOs públicos (HB-08): solo en `/publicas`, sin barrio;
  - total de entidades: sale de `X-Total-Count`;
  - edición del perfil: hay un `PUT` por id, falta `/me`;
  - cifras del inicio del admin: el total de donantes ya existe.
- **Para corregir en los requerimientos (§6.1):**
  - RF-01 apunta a `GET /donaciones-independientes`, que no es apto para lo público (trae el
    historial con el actor);
  - RF-06 y RF-22 apuntan a `GET /api/entidades`, que no filtra activas y expone documento y
    medios; lo público usa `/publicas`.

## 9. Límites

- `[OBSERVED]` Cada estado sale de leer las plantillas y los DTOs; no se llamó a ningún endpoint.
- `[INFERRED]` Las derivaciones (🔁) suponen que los ids cruzan entre servicios como dicen los
  DTOs: por ejemplo, que `idBeneficiaria` de Logística es la persona jurídica.
- La ficha de huecos vive en `docs/interfaz/` y no se edita en este cambio: este inventario la
  actualiza por referencia.
