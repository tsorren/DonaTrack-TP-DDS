# Spec: R11 p2-actividad — El registro de actividad de administración

> **Cerrada el 2026-11-12** (cierre documental de la oleada P2, rama `E6_backlog_r11_p2`): construida e integrada, y verificada con el stack propio (`run-preprod-tests.sh`, 26/26); pasa a `completed/`. ADR `proposed`, escrito por el constructor después del código: [registro de actividad](../../adr/donaciones-service/20261112-registro-de-actividad-de-administracion-en-donaciones.md). `[OBSERVED]` Desvío de §4: la fila se guarda después de la acción y no en su transacción; lo decide el revisor ([DTI-43](../../adr/DEUDA_TECNICA.md#dti-43), ítem 2). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario para P1 y P2; ver "Aprobación del diseño" al final).
> **Estado:** PROPOSED (pendiente de aprobación del usuario). Diseño del lote `p2-actividad` del
> [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md) (R11-26,
> P2, tanda 1, ruta CONTRATO). Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (contrato REST nuevo en Donaciones, tabla nueva y un
> canal nuevo por el que el sitio anota acciones), escrita con el formato corto de la ruta STANDARD
> de `define-spec`, como pide el lote: sin matriz 5D ni diseño de clases.
> **Fecha:** 2026-11-12
> **Base:** `e9331b3a2`.
> **Módulos:** `donaciones-service` (tabla, registro en la misma transacción de cada acción,
> `GET` y `POST /api/actividad`, migración y tests), `cliente-liviano` (pantalla nueva
> `admin-actividad`, pestañas en Administradores, enlace en Mi perfil, y la anotación de las
> acciones que no pasan por Donaciones), `docs/arquitectura/contratos/openapi-donaciones.yaml`.
> `auth-service`: **sin cambios** (§3, decisión 4).
> **Ficha de flujo:** [`r11-p2-actividad.md`](../../interfaz/flujos/r11-p2-actividad.md)
> (pantallas, estados y textos: esta spec no los repite).
> **Depende de:** R11-10 (`p1-admins`, hecho): una cuenta por admin y el `sub` del token como autor
> ([ADR `proposed` del autor de una propuesta](../../adr/donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md)).
> **Cierra además:** de [DTI-42](../../adr/DEUDA_TECNICA.md#dti-42), ítem 2, "el autor de las otras
> acciones (`p2-actividad`)".

---

## 1. Objetivo Funcional (Goal)

Hoy solo aprobar o descartar una propuesta deja quién y cuándo; las demás acciones del admin no
dejan rastro, o lo dejan con un dato que manda el sitio (`X-Actor`). Este lote suma un registro de
la actividad de administración: cada acción del admin queda con quién la hizo (el `sub` de su
token), qué hizo, sobre qué y cuándo; y una pantalla que lo muestra de a una página, con búsqueda
por administrador, acción y fecha resueltas en el servidor.

`[DOCUMENTED]` Decisión del usuario "Cuentas de administración" (backlog §3): "cuenta y perfil por
admin, un admin crea otro y registro de actividad", todo en la entrega 6. Reglas generales 1 a 3
(backlog §2). RI-SVC-04: el actor de auditoría sale del `sub`, no de `X-Actor`.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (dentro del alcance)

1. **Donaciones guarda la actividad** en una tabla nueva. Migración `V25__actividad_de_administracion.sql`
   (la siguiente a `V24`; si choca al integrar, la renumera el integrador):

   | Columna | Tipo | Qué es |
   |---|---|---|
   | `id` | `UUID` PK | |
   | `ocurrio_en` | `TIMESTAMP NOT NULL` | Del `Clock` del servicio (`ClockConfig`, zona de Buenos Aires), como `fecha_alta` |
   | `autor` | `TEXT NOT NULL` | El `sub` del token de quien hizo la acción |
   | `accion` | `VARCHAR(40) NOT NULL` | Uno de los 14 valores de §3.3 |
   | `objeto_id` | `TEXT NULL` | El identificador de lo que tocó (propuesta, donación, donante, archivo, organización, cuenta o camión) |
   | `detalle` | `VARCHAR(200) NULL` | Un rótulo sin datos personales, como era al hacerse la acción (§3.3) |

   Índices: `(ocurrio_en DESC, id)`, `(autor, ocurrio_en DESC)` y `(accion, ocurrio_en DESC)`.
   La misma migración copia las propuestas ya decididas con autor (`decidida_por` no nulo y distinto
   de `SISTEMA`, con `fecha_decision`) como `PROPUESTA_APROBADA` o `PROPUESTA_DESCARTADA`, con la
   razón social de su organización: es un dato real guardado por V22, no inventado.
2. **Registro en la misma transacción de cada acción que pasa por Donaciones**, solo cuando el token
   es de ADMIN (no un token de servicio, de donante ni de organización):
   - `PUT /api/asignaciones/propuestas/{id}/estado` → `PROPUESTA_APROBADA` o `PROPUESTA_DESCARTADA`.
   - `POST /api/asignaciones/ejecuciones` → `ASIGNACION_CORRIDA`.
   - `POST /api/donaciones` → `DONACION_REGISTRADA` (la donación que registra el admin; la del
     donante sobre sí mismo no entra).
   - `PATCH /donaciones-independientes/{id}/estado` a vencida → `DONACION_VENCIDA`. El autor es el
     `sub` del token, no `X-Actor` (RI-SVC-04); `X-Actor` sigue igual para el historial de la
     donación (ADR-E6-05 lo retira, no este lote).
   - `POST /api/donantes` → `DONANTE_REGISTRADO`.
   - `POST /api/donantes/archivos` → `IMPORTACION_SUBIDA`.
   - `POST /api/entidades` → `ORGANIZACION_APROBADA` u `ORGANIZACION_REGISTRADA`, según el campo
     nuevo `origen` (§3.4).
   Si la acción falla y se deshace, su registro también: no queda una actividad que no pasó.
3. **Endpoint nuevo `GET /api/actividad`** (§3.1), resuelto en la base en el adaptador JPA y con el
   mismo resultado en el repositorio en memoria.
4. **Endpoint nuevo `POST /api/actividad`** (§3.2), solo para anotar las acciones que no pasan por
   Donaciones: dar de alta un administrador y generar otra contraseña (Keycloak), y agregar,
   deshabilitar o volver a habilitar un camión (Logística).
5. **Cliente liviano:**
   - Pantalla `admin-actividad` (plantilla, controller, modelo de vista, puerto nuevo con su
     adaptador REST y su adaptador de maqueta, con escenarios `vacia`, `sin-resultados`,
     `sin-nombres` y `muchas`).
   - `Pantallas` marca `admin-actividad` como parte de la sección Administradores (no empieza con
     `admin-administradores`).
   - Pestañas `Cuentas · Actividad` en `admin-administradores` y en `admin-actividad`; "Ver su
     actividad" en cada cuenta; "Ver mi actividad" en `admin-perfil`.
   - La búsqueda por administrador: el sitio busca en Keycloak las cuentas marcadas con
     `administracion` que coinciden (`/users?q=administracion:si&search=<texto>&max=21`, el mismo
     cliente de `AdministradoresKeycloak`) y pide la actividad de esas cuentas; con 21, avisa que
     son más de 20 y no filtra.
   - Los nombres: cada cuenta distinta de la página se lee una vez por pedido, como
     `AutoresDelHistorial` (como mucho 20 lecturas; con pocas cuentas, de 1 a 3).
   - La anotación: después de que la acción sale bien en Keycloak o en Logística, el sitio llama a
     `POST /api/actividad` con el token de la sesión. Si la anotación falla, se registra un WARN con
     el `traceId` y el aviso de la acción no cambia.
   - `paginas.txt` de la verificación visual suma `admin-actividad` y sus escenarios.
6. **OpenAPI** de Donaciones: versión menor siguiente a la vigente al integrar, con §3.1 a §3.4.
   Cambios aditivos.
7. **Documentos al construir:** la ficha pasa a "construida", el [mapa del sitio](../../interfaz/mapa-del-sitio.md)
   (fila "Administradores → Actividad"), la [bitácora](../../interfaz/bitacora.md) con las
   decisiones de la ficha §9, `requerimientos-e6.md` (RI-SVC-04 y RF-AUTH-07), el estado de R11-26
   en el backlog, DTI-42 ítem 2 y un ADR `proposed` (§7).

### Out-of-Scope (fuera del alcance)

- **Cambios en Logística, Keycloak o el realm:** las acciones que pasan por ahí las anota el sitio
  (§3.2). Ni Logística ni `auth-service` cambian.
- **Acciones hechas en la consola de Keycloak** (quitar o desactivar un admin, recuperar la
  contraseña por correo): no pasan por el sitio y no se registran. La ayuda de la pantalla lo dice.
- **Pedir corrección o rechazar una organización:** hoy no tienen envío real (solo la maqueta,
  `AdminEntidadesEnvioController`). Se suman al registro cuando existan.
- **Mi perfil (cambiar el propio nombre):** no es una acción sobre el sistema.
- **La columna "Subió" en Importaciones** (duda 1 de la spec de `p1-importaciones`): sale de este
  registro, pero sumarla a esa lista es de otro lote.
- **La búsqueda por quién aprobó en Asignaciones** (R11-35): usa `decidida_por`, no este registro.
- **Borrar o vencer filas del registro:** se guarda todo; no hay pantalla ni proceso de borrado.
- **Elegir dos fechas a mano y ordenar al revés:** con el componente de filtros nuevo (R11-B04).
- **Notificar al admin** de una acción de otro: es de `p2-campanita`.
- **La ruta en el gateway:** el sitio llama a Donaciones directo (`cliente.api.donaciones.url`);
  `RutasGateway` no tiene `/api/actividad` y sumarla es fuera de la zona del lote (duda 1).
- **"Entidad" → "Organización"** en rutas, clases y campos: no se renombran (lo visible ya lo hizo
  `p1-nombres`). Los valores `ORGANIZACION_*` son nuevos y ya nacen con el nombre visible.

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Decisiones del diseñador del lote (agente, sin consulta: la corrida pide no frenar). Las de pantalla
están en la [ficha §9](../../interfaz/flujos/r11-p2-actividad.md#9-decisiones-y-preguntas).

1. **Dónde se guarda: una tabla en Donaciones.** Es el servicio por el que pasan casi todas las
   acciones del admin, con el token del admin (`ClientesHttp`), y guardarlo en la misma
   transacción hace que nunca quede una acción sin registro ni un registro sin acción. Descartado:
   los eventos de administración de Keycloak (registran al cliente de servicio que llama, no a la
   persona, y no ven lo que pasa en Donaciones); una tabla por servicio unida en el sitio (paginar
   y contar entre dos fuentes no se puede sin traer listas enteras, regla 1); el cliente liviano
   como dueño del registro (no tiene base); un servicio de auditoría nuevo (otro despliegue para un
   registro chico, y una decisión de arquitectura más grande que este lote).
2. **El autor siempre del token, nunca del cuerpo ni de una cabecera.** El mismo criterio del ADR del
   autor de una propuesta y de RI-SVC-04. `POST /api/actividad` no acepta un autor.
3. **Lo que no pasa por Donaciones lo anota el sitio, con un catálogo cerrado.** `POST
   /api/actividad` acepta solo las cinco acciones de Keycloak y Logística: las de Donaciones las
   registra Donaciones, así el sitio no puede anotar una aprobación que no pasó. El sitio anota
   después de que la acción salió bien. Descartado: que Logística publique un evento que Donaciones
   consuma (un canal nuevo entre servicios y cambios en Logística, fuera de los módulos del lote);
   dejar los camiones y las cuentas afuera (el registro quedaría sin la acción más sensible, dar de
   alta un administrador).
4. **`auth-service` sin cambios.** El realm ya marca a los admins (`administracion`) y el sitio ya
   los busca y los lee; activar los eventos de administración del realm no sirve (decisión 1). El
   manifiesto lo nombraba porque el alta de un admin pasa por Keycloak; eso se resuelve con la
   anotación.
5. **Sin datos personales en el registro.** `autor` y `objeto_id` de una cuenta son `sub`; el
   nombre se lee de Keycloak al mostrar (RNF-AUTH-09 y el ADR del autor). `detalle` guarda solo
   rótulos que no son de una persona humana: la razón social de una organización o de un donante
   persona jurídica, el nombre del archivo (que el admin ya ve en Importaciones), la patente de un
   camión, la cantidad de propuestas nuevas. Una persona donante humana no se nombra.
6. **Listado con el patrón de R11-01** (arreglo y `X-Total-Count`, `ParametrosListado`). Descartado:
   un sobre `{elementos, total}`, distinto de todos los otros listados.
7. **Filtro por cuentas, no por nombre, en Donaciones.** Donaciones no conoce nombres; el sitio
   traduce el nombre buscado a cuentas en Keycloak. Descartado: guardar el nombre en cada fila (dato
   personal copiado que envejece).
8. **Grupos de acciones en el sitio.** Donaciones filtra por `accion` (repetible); los grupos de la
   pantalla ("Propuestas y asignación", etc.) son del sitio, que los traduce a varias `accion`.

### 3.1 `GET /api/actividad` (nuevo, solo ADMIN)

**Antes:** no existe.

**Después:**

| Parámetro | Valores | Qué hace |
|---|---|---|
| `page` | desde 0; por defecto 0 | |
| `size` | 1 a 100; por defecto 20 | Sin `page` ni `size`: la página 0 de 20. Nunca la lista completa |
| `autor` | `sub` de una cuenta, hasta 64 caracteres; repetible, hasta 20 | Una o varias cuentas. Sin `autor`, todas |
| `accion` | los 14 valores de §3.3; repetible | Una o varias. Sin `accion`, todas |
| `desde`, `hasta` | `AAAA-MM-DD` | Día de `ocurrio_en` en la zona de Buenos Aires, inclusivos. `desde` posterior a `hasta`: 400 |

Orden fijo: `ocurrio_en` descendente, desempata por `id`.

Respuesta `200`: arreglo de `ActividadResponse`, una por acción, y el header `X-Total-Count` con
cuántas cumplen los filtros. Página fuera de rango: arreglo vacío con el total real.

`ActividadResponse`:

| Campo | Tipo | Qué es |
|---|---|---|
| `id` | `string` `uuid` | |
| `ocurrioEn` | `string` `date-time` (hora local de Argentina, sin zona, como `fechaAlta`) | Cuándo |
| `autor` | `string` | El `sub` de quien la hizo |
| `accion` | `string`, enum de §3.3 | Qué |
| `objetoId` | `string`, nullable | Sobre qué |
| `detalle` | `string`, nullable | El rótulo de §3.3 |

Errores: `page`, `size`, `autor`, `accion` o una fecha inválidos, más de 20 `autor`, o
`desde > hasta`: `400` con `ERR-CSR-003`. Sin token: `401`; token que no es de ADMIN: `403`.

### 3.2 `POST /api/actividad` (nuevo, solo ADMIN)

**Antes:** no existe.

**Después:** cuerpo JSON:

| Campo | Requerido | Qué es |
|---|---|---|
| `accion` | sí | Solo `ADMINISTRADOR_DADO_DE_ALTA`, `CONTRASENA_PROVISORIA_NUEVA`, `CAMION_AGREGADO`, `CAMION_DESHABILITADO` o `CAMION_HABILITADO` |
| `objetoId` | sí | El `sub` de la cuenta o el id del camión, hasta 64 caracteres |
| `detalle` | no | Para los camiones, la patente, hasta 20 caracteres; para las cuentas, no se acepta (400) |

El autor y la hora los pone Donaciones (token y `Clock`). Respuesta `201` con `ActividadResponse`.
Errores: otra `accion` (una de las que registra Donaciones), falta `objetoId` o un largo excedido:
`400` con `ERR-CSR-003`; sin token o sin ADMIN, `401` o `403`. No es idempotente: el sitio llama una
vez por acción hecha y no reintenta.

### 3.3 Las acciones

| `accion` | La registra | `objetoId` | `detalle` |
|---|---|---|---|
| `PROPUESTA_APROBADA` | Donaciones | propuesta | razón social de la organización |
| `PROPUESTA_DESCARTADA` | Donaciones | propuesta | razón social de la organización |
| `ASIGNACION_CORRIDA` | Donaciones | ejecución | cantidad de propuestas nuevas (`"3"`) |
| `DONACION_REGISTRADA` | Donaciones | donación | — |
| `DONACION_VENCIDA` | Donaciones | donación | — |
| `DONANTE_REGISTRADO` | Donaciones | donante | razón social si es persona jurídica; vacío si es humana |
| `IMPORTACION_SUBIDA` | Donaciones | archivo | nombre del archivo (`nombre_original`), si lo tiene |
| `ORGANIZACION_APROBADA` | Donaciones | organización | razón social |
| `ORGANIZACION_REGISTRADA` | Donaciones | organización | razón social |
| `ADMINISTRADOR_DADO_DE_ALTA` | el sitio (§3.2) | `sub` de la cuenta nueva | — |
| `CONTRASENA_PROVISORIA_NUEVA` | el sitio (§3.2) | `sub` de la cuenta | — |
| `CAMION_AGREGADO` | el sitio (§3.2) | camión | patente |
| `CAMION_DESHABILITADO` | el sitio (§3.2) | camión | patente |
| `CAMION_HABILITADO` | el sitio (§3.2) | camión | patente |

### 3.4 `POST /api/entidades` (cambio aditivo)

**Antes:** cuerpo `{ "juridicaId": uuid }`; aprobar una solicitud y registrar una organización
mandan lo mismo.

**Después:** el mismo cuerpo, más `origen` opcional: `SOLICITUD` (el sitio lo manda al aprobar una
solicitud, `SolicitudesDeEntidad.aprobar`) o `ADMIN`. Sin `origen`, `ADMIN`. Solo cambia la acción
que se registra; la respuesta, los errores y el resto del comportamiento, iguales. Un valor que no
es ninguno de los dos: `400` con `ERR-CSR-003`.

### 3.5 Cliente liviano (direcciones propias, no de la API)

| Dirección | Qué hace |
|---|---|
| `GET /admin-actividad?buscar=&autor=&accion=&fecha=&desde=&hasta=&pagina=` | La pantalla. `autor`: un `sub` o `yo` (la cuenta de la sesión); `accion` del sitio: `propuestas`, `donaciones`, `donantes`, `organizaciones`, `cuentas`, `camiones`; `fecha`: `hoy`, `7-dias`, `30-dias`, `este-mes`, `mes-anterior` (se traducen a `desde` y `hasta`). Un valor que no entiende se ignora y se avisa (ficha §5). Con `HX-Request`, la misma página (el layout toma el `main`) |
| `admin-administradores` y `admin-perfil` | Sin cambio de dirección; suman los enlaces de la ficha §4.2 y §4.3 |

## 4. Invariantes

- El autor de toda fila es el `sub` del token de quien hizo la acción; ningún cuerpo ni cabecera lo
  fija.
- Una acción que pasa por Donaciones y su fila del registro se guardan juntas o no se guarda
  ninguna.
- Solo un token de ADMIN genera filas; un token de servicio, de donante o de organización, nunca.
- Ninguna respuesta del listado trae más de 100 filas; sin `page` ni `size`, 20.
- `X-Total-Count` es el total de la base con los filtros, nunca el de la página.
- El registro no guarda el nombre, el correo ni el documento de ninguna persona humana.
- `POST /api/actividad` nunca acepta una acción que registra Donaciones.
- Las filas no se editan ni se borran.

## 5. Criterios de Aceptación

Cada uno se verifica con un test o una captura; entre paréntesis, dónde.

```gherkin
Escenario: aprobar una propuesta queda registrado a nombre de quien la aprobó
  Dado una propuesta pendiente para "Comedor Barrio Ejemplo"
  Cuando un admin con sub "admin-a" la aprueba con PUT /api/asignaciones/propuestas/{id}/estado
  Entonces GET /api/actividad trae una fila PROPUESTA_APROBADA con autor "admin-a",
    objetoId la propuesta y detalle "Comedor Barrio Ejemplo"

Escenario: una acción que falla no deja registro
  Dado una propuesta cuya necesidad ya no está activa
  Cuando un admin intenta aprobarla y Donaciones responde 409
  Entonces GET /api/actividad no trae ninguna fila nueva

Escenario: un token de servicio no genera actividad
  Dado el token de servicio del sitio
  Cuando registra un donante con POST /api/donantes
  Entonces GET /api/actividad no trae ninguna fila nueva

Escenario: la lista trae de a una página
  Dado 25 filas de actividad
  Cuando un admin pide GET /api/actividad sin parámetros
  Entonces recibe las 20 más recientes y X-Total-Count: 25

Escenario: filtrar por quién, acción y fecha
  Dado filas de "admin-a" y "admin-b", de varias acciones y días
  Cuando pide autor=admin-a&accion=DONACION_VENCIDA&desde=2026-11-10&hasta=2026-11-12&size=1
  Entonces recibe solo filas de "admin-a", de esa acción y de esos días, y X-Total-Count con cuántas son

Escenario: el sitio no puede anotar una acción de Donaciones
  Cuando un admin llama a POST /api/actividad con accion PROPUESTA_APROBADA
  Entonces recibe 400 con ERR-CSR-003 y no se guarda nada

Escenario: dar de alta un administrador queda anotado
  Dado el admin "admin-a" con la sesión iniciada
  Cuando da de alta a otra cuenta desde Administradores y Keycloak la crea
  Entonces Donaciones guarda una fila ADMINISTRADOR_DADO_DE_ALTA con autor "admin-a"
    y objetoId el sub de la cuenta nueva, y la pantalla dice "Dio de alta a {nombre} como administrador."
```

| # | Criterio | Dónde |
|---|---|---|
| C1 | Cada una de las 9 acciones de Donaciones de §3.3 genera su fila con el `sub` del token, la hora del `Clock` y el `objetoId` y `detalle` de la tabla | Tests de servicio y de controller de Donaciones, uno por acción |
| C2 | Una acción que se deshace no deja fila; un token que no es de ADMIN no genera fila | Tests de servicio y de controller |
| C3 | Marcar vencida registra el `sub` del token aunque `X-Actor` diga otra cosa | Test de controller |
| C4 | `GET /api/actividad`: página por defecto de 20, tope de 100, orden por `ocurrio_en` descendente, filtros combinados, total en `X-Total-Count`, página fuera de rango vacía con el total | Repositorio en memoria y test sobre PostgreSQL de la consulta |
| C5 | Parámetros inválidos, más de 20 `autor` y `desde > hasta` responden 400 `ERR-CSR-003`; sin ADMIN, 401 o 403 | Test de controller |
| C6 | `POST /api/actividad` acepta solo las 5 acciones del sitio, ignora cualquier autor y responde 201 con la fila | Test de controller |
| C7 | `POST /api/entidades` con `origen=SOLICITUD` registra `ORGANIZACION_APROBADA`; sin `origen`, `ORGANIZACION_REGISTRADA`; con otro valor, 400 | Test de controller |
| C8 | `V25` copia las propuestas decididas con autor y no copia las de `SISTEMA` ni las que no tienen fecha | Test de migración sobre PostgreSQL |
| C9 | OpenAPI de Donaciones con §3.1 a §3.4 y oasdiff sin cambios incompatibles | Contrato |
| C10 | El sitio pide la actividad de a 20 con los filtros traducidos (grupos a `accion`, opciones de fecha a `desde` y `hasta`, `yo` al `sub` de la sesión) y nunca sin página | Adaptador REST con `MockRestServiceServer` |
| C11 | Buscar por administrador consulta Keycloak con `max=21`; sin coincidencias o con más de 20 no pide a Donaciones con ese filtro y muestra el aviso de la ficha | Adaptador y controller con MockMvc |
| C12 | Cada cuenta distinta de la página se lee una vez; si Keycloak no responde, la lista sale con "Una cuenta de administración" y el aviso | Adaptador y controller |
| C13 | Alta de un admin, otra contraseña y los tres cambios de camión llaman a `POST /api/actividad` después de salir bien; si la anotación falla, el aviso de la acción no cambia | Tests de los controllers de envío |
| C14 | Todos los estados de la ficha §5 se dibujan en la maqueta; capturas a 360, 1280 y 1920 px sin desbordes, contraste AA y foco visible | Verificación visual (`paginas.txt`) |
| C15 | Gate 1 y Gate 2 de Donaciones y del cliente liviano en verde, `spotless:check` en verde | Maven |

## 6. Compatibilidad con los clientes actuales

- **Cliente liviano:** las rutas que ya usa no cambian. `POST /api/entidades` sin `origen` sigue
  funcionando (registra `ORGANIZACION_REGISTRADA`); el sitio suma `origen=SOLICITUD` solo al
  aprobar una solicitud.
- **Seeder de demo** (`demo/seeder/`) y **ensayo de 20.000 filas:** llaman con su propio token. Si
  es un token de ADMIN, sus acciones entran al registro (es lo que pasó); si es de servicio, no. No
  se cambian. `[INFERRED]` El seeder corre con una cuenta de admin: la demo va a mostrar su
  actividad a nombre de esa cuenta.
- **n8n, Logística y los demás servicios:** usan token de servicio; no generan filas y no ven las
  rutas nuevas.
- **Gateway:** sin la ruta nueva, `/api/actividad` no se alcanza desde afuera; el sitio no pasa por
  el gateway (duda 1).
- **Base:** `V25` crea una tabla nueva y copia filas de `propuesta`; no cambia ninguna tabla
  existente.
- **Eventos:** ninguno nuevo ni cambiado.

## 7. Especificación Técnica de Diseño

Sin diseño de clases, como pide el lote: solo lo que el constructor necesita decidir.

- **Two-Gate (AGENTS.md §9.1):** Gate A se cumple (decisión nueva: dónde vive el registro de
  actividad del admin y que el sitio anote lo que pasa por Keycloak y Logística). Gate B también
  (tabla nueva, contrato nuevo, un canal nuevo sitio → Donaciones para datos de otros sistemas,
  seguridad del autor). **Lleva un ADR `proposed`** en `docs/adr/donaciones-service/`, escrito por
  el constructor antes del código (duda 4), con las alternativas de §3, decisión 1 y 3.
- **Dónde se toca, en Donaciones:** un controller nuevo de actividad con su interfaz; un servicio de
  registro que los servicios de propuestas, ejecuciones, donaciones, donaciones independientes,
  donantes, archivos y entidades llaman dentro de su transacción; su repositorio con el adaptador
  JPA y la versión en memoria; `V25`; el DTO de entrada de `POST /api/entidades` (`origen`). El
  autor se lee del token en cada controller con la misma regla que `PropuestaDeAsignacionController`
  (`autorDe`): conviene llevarla a un solo lugar del paquete de controllers, sin tocar el dominio.
- **En el cliente:** `DonacionesApi` (las dos rutas), un puerto de actividad con sus adaptadores
  REST y de maqueta, el controller y la plantilla de `admin-actividad`, `Pantallas`, las
  plantillas `admin-administradores.html` y `admin-perfil.html`, `SolicitudesDeEntidad` (manda
  `origen`), `AdminAdministradoresController` y `AdminCamionesEnvios` (la anotación), la búsqueda
  de cuentas en `AdministradoresKeycloak` y `paginas.txt`.
- **Plan de pruebas (TDD, primero el test que falla):**
  1. Servicio y repositorio en memoria: C1, C2 y C4.
  2. Controllers de Donaciones: C3, C5, C6 y C7; test sobre PostgreSQL de la consulta y de `V25`
     (C4 y C8).
  3. Contrato: OpenAPI y oasdiff (C9).
  4. Cliente: adaptadores con `MockRestServiceServer` (C10 a C12), controllers con MockMvc (C11 a
     C13) y la verificación visual (C14).
  5. Gate 1 y Gate 2 de los dos módulos (C15). Gate 3/4 en el cierre de la oleada, con el stack
     propio.

## 8. Riesgos

| Riesgo | Qué se hace |
|---|---|
| La anotación del sitio falla después de que la acción salió bien en Keycloak o Logística, y esa acción no queda en el registro | WARN con el `traceId`; la ayuda de la pantalla no promete más de lo que hay. Es el costo de no tocar Logística ni Keycloak (§3, decisión 3) |
| `V25` choca con otra migración de Donaciones de la misma tanda | La renumera el integrador, como dice la regla de la corrida |
| Varios servicios de Donaciones suman una llamada al registro: un olvido deja una acción sin fila | Un test por acción (C1) |
| `search` y `q` juntos en `/users` de Keycloak 26 `[INFERRED]` | El constructor lo prueba contra el Keycloak del stack propio; si no se combinan, filtra con `q=administracion:si` y `search` en dos pasos acotados a 21 |
| La copia de propuestas en `V25` toma la razón social de hoy, no la de cuando se decidió | Es la única que existe; la diferencia es rara y no es un dato personal. Se dice en el ADR |
| El seeder corre con una cuenta de admin y llena el registro con miles de filas de la demo | Es lo que pasó; el registro pagina. Si molesta en la demo, se ve con el filtro "Solo lo mío" |
| Lectura de nombres en Keycloak por cada cuenta de la página | Como mucho 20 por página, en general 1 a 3; las mismas lecturas que ya hace el historial de una donación |
| El DER de Donaciones no tiene la tabla nueva | Se dice en la descripción del PR. El DER de la entrega no se edita (registro histórico) |

## 9. Dudas

Cada una con la opción que recomienda el diseñador.

1. **La ruta en el gateway.** `RutasGateway` no lleva `/api/actividad` y el gateway no es de este
   lote. Recomiendo sumar la línea y su caso en `RutasGatewayTest` al integrar (fuera de zona,
   imprescindible para que el contrato sea el mismo por las dos entradas). Alternativa: dejarla sin
   ruta pública hasta otro lote.
2. **Camiones y cuentas anotados por el sitio.** Recomiendo anotarlos con `POST /api/actividad`
   (catálogo cerrado, autor del token). Alternativa: dejarlos afuera del registro hasta que
   Logística publique un evento (otro lote, con ADR propio).
3. **Copiar las propuestas ya decididas.** Recomiendo copiarlas en `V25`: son datos reales con autor
   y fecha. Alternativa: que el registro empiece vacío con esta versión.
4. **El ADR.** Recomiendo que el constructor lo escriba `proposed` antes del código, como pide
   AGENTS.md §7.3. Alternativa: escribirlo en el cierre documental de la oleada, como en P1 (desvío
   ya acordado entonces).

## Aprobación del diseño p2-actividad (2026-11-12)

Aprobado por el usuario el 2026-11-12 con las opciones recomendadas: pidió aprobar en forma automática todas las opciones recomendadas de P1 y P2 para que la corrida termine sin su intervención.

1. Ruta en el gateway: RutasGateway no tiene /api/actividad y el gateway no es de este lote; el sitio llama a Donaciones directo. Recomiendo sumar la línea y su caso en RutasGatewayTest al integrar.
   **Resuelto: la opción recomendada.**
2. Camiones y cuentas: ¿entran al registro anotados por el sitio (POST /api/actividad)? Recomiendo que sí, con el catálogo cerrado y el autor del token. Aceptamos el riesgo de que, si falla la anotación, esa acción no quede registrada (se escribe un WARN con el traceId).
   **Resuelto: la opción recomendada.**
3. ¿Copiar las propuestas ya decididas en V25? Recomiendo que sí: son datos reales con autor y fecha.
   **Resuelto: la opción recomendada.**
4. ADR: recomiendo que el constructor lo escriba como proposed antes del código, como pide AGENTS.md §7.3, y no en el cierre como se hizo en P1.
   **Resuelto: la opción recomendada.**
5. [INFERRED] Sin verificar: que Keycloak 26 acepte search y q juntos en /users. Recomiendo que el constructor lo pruebe en el stack propio y, si no anda, filtre en dos pasos acotados a 21.
   **Resuelto: la opción recomendada.**
6. El seeder corre con una cuenta de admin, así que la demo va a mostrar muchas filas a su nombre. Recomiendo dejarlo así (es lo que pasó) y mostrar 'Solo lo mío' en la demo.
   **Resuelto: la opción recomendada.**

## Cierre (2026-11-12): revisión y lo que quedó

Resumen del cierre de la oleada P2 para el lote `p2-actividad` (`268ba1dff` y arreglos `18195a3b1`). La revisión la hizo el
revisor del workflow `backlog-r11`, separado del constructor (`[INFERRED]` por cómo lo orquesta el
workflow: lee la rama y los informes, no el razonamiento del constructor). Este bloque lo redactó
el agente del cierre documental de P2 con lo que consta en los commits y en
[DTI-43](../../adr/DEUDA_TECNICA.md#dti-43), ítem 2, donde está lo pendiente.

```text
=== REVIEW CONTRACT ===

Mode: INDEPENDENT_REVIEW (revisión de integración del workflow backlog-r11)
Task Level: ARCHITECTURAL

V1 Regression risks:
  Ver DTI-43, ítem 2.

V2 Architecture/invariant violations:
  NONE_DETECTED en el cierre.

V3 Contracts/integration:
  [OBSERVED] V25 y OpenAPI de Donaciones 1.11.0; ADR `proposed` del lote, anotado en el cierre.
  Desvío transaccional abierto (DTI-43, ítem 2).

V4 Tests/coverage:
  [VERIFIED] run-preprod-tests.sh en el cierre (smoke 4/4; contrato, integración y E2E 26/26,
  stack propio) y recorrido con login real de la administración.

V5 Scope violations:
  NONE_DETECTED.

V6 Evidence integrity:
  NONE_DETECTED.

V7 SonarCloud pre-flight:
  [TESTS_NOT_EXECUTED_BY_EVALUATOR] No consta; lo mide el Quality Gate del PR.

V8 Security/privacy:
  NONE_DETECTED.

V9 Documentation graph integrity:
  [OBSERVED] Spec movida a completed/ con README, ESTADO_DOCUMENTACION, specs/README y
  DEUDA_TECNICA en el cierre documental de P2.
```
