# Spec: R11 p1-importaciones — La pantalla de importaciones

> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario; ver la sección final de aprobación). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Estado:** PROPOSED (pendiente de aprobación del usuario). Diseño del lote `p1-importaciones` del
> [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md) (R11-20,
> P1, tanda 1). Ningún ADR se promueve.
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (cambia el contrato REST de Donaciones y suma una
> migración), escrita con el formato corto de la ruta STANDARD de `define-spec`, como pide el lote.
> **Fecha:** 2026-11-12
> **Base:** `ebe332642`.
> **Módulos:** `donaciones-service` (controller y servicio de archivos, el modelo `Archivo` y su
> persistencia, migración y tests), `cliente-liviano` (pantalla nueva `admin-importaciones`, su
> controller y su adaptador; cambios chicos en el avance y en Donantes), `api-gateway` (solo un caso
> más en `RutasGatewayTest`), `docs/arquitectura/contratos/openapi-donaciones.yaml`.
> **Ficha de flujo:** [`r11-p1-importaciones.md`](../../interfaz/flujos/r11-p1-importaciones.md)
> (pantallas, estados y textos: esta spec no los repite).
> **Cierra además:** de [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40), el ítem 7 en su parte de la
> importación (el avance que se vacía al pasar a `ERROR` sin filas); de
> [DTI-39](../../adr/DEUDA_TECNICA.md#dti-39), la descarga de las filas con error. El backlog asigna
> los dos a este lote.

---

## 1. Objetivo Funcional (Goal)

Hoy no hay dónde ver las importaciones de donantes hechas: el admin solo ve la última que subió en
su sesión, y si sale y entra, la pierde. Este lote suma una lista de todas las importaciones, en
curso e históricas, de a una página, con búsqueda por nombre del archivo y filtros por estado y por
fecha de subida; cada una lleva a su avance o a su resultado con las filas con error, que además se
pueden descargar.

`[DOCUMENTED]` Decisión del usuario "Avance de la importación" (backlog §3) y reglas generales 1 a
3 (backlog §2): nada se pide completo; búsqueda, filtros y total se resuelven en el servidor.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (dentro del alcance)

1. **Donaciones guarda el nombre y la fecha de subida** de cada archivo. Migración nueva
   `V22__archivo_nombre_y_fecha_de_subida.sql` (la siguiente a `V21`; si choca al integrar, la
   renumera el integrador):
   - `archivo.nombre_original VARCHAR(255) NULL`: el nombre con que se subió, sin la ruta del
     navegador, recortado a 255 caracteres.
   - `archivo.subido_en TIMESTAMP NULL`: fecha y hora de la subida, del `Clock` del servicio
     (`ClockConfig`, zona de Buenos Aires), igual que `donante.fecha_alta`.
   - Las filas existentes quedan en `NULL` en las dos columnas: no se inventa un dato.
   - Índice `ix_archivo_subido_en ON archivo (subido_en DESC NULLS LAST, id)`.
2. **Endpoint nuevo `GET /api/donantes/archivos`** (§3.1), en el controller y el servicio de
   archivos, resuelto en la base en el adaptador JPA y con el mismo resultado en el repositorio en
   memoria.
3. **Campos nuevos, opcionales, en `ArchivoOutputDTO`:** `nombre` y `subidoEn` (§3.2). Valen para
   el `POST`, el `GET /{id}` y el listado.
4. **Cliente liviano:**
   - Pantalla `admin-importaciones` (plantilla, controller, modelo de vista, método nuevo en el
     puerto de personas del admin con su adaptador REST y su adaptador de maqueta, con escenarios
     `vacia`, `sin-resultados` y `con-viejas`).
   - `Pantallas` marca `admin-importaciones` como parte de la sección Donantes.
   - Pestañas `Donantes · Importaciones` en `admin-donantes` y en `admin-importaciones`.
   - Avance y resultado: migas, nombre del archivo y fecha desde Donaciones (con `?nombre=` como
     respaldo), "Ver todas las importaciones", y "Descargar las filas con error" con datos reales.
   - La descarga: una dirección del cliente que arma el `.csv` pidiendo `filas-con-error` de a 100 y
     escribiendo cada tanda en la respuesta mientras baja (§3.3).
   - El sondeo del avance que encuentra `ERROR` sin filas leídas responde `HX-Redirect` a la página
     de error de la importación (DTI-40, ítem 7), con su test.
   - `verificacion/paginas.txt` suma `admin-importaciones` (y sus escenarios).
5. **OpenAPI** de Donaciones a la versión 1.10.0 con §3.1 y §3.2. Cambios aditivos.
6. **Gateway:** sin cambios en `RutasGateway.java` (`/api/donantes/**` ya cubre la ruta nueva,
   `RutasGateway.java:41`); `RutasGatewayTest` suma el caso `GET, /api/donantes/archivos,
   donaciones`.
7. **Documentos al construir:** la ficha pasa a "construida", el [mapa del sitio](../../interfaz/mapa-del-sitio.md)
   (fila de Donantes → importar CSV), la [bitácora](../../interfaz/bitacora.md) con las decisiones
   de la ficha §9, `requerimientos-e6.md` (RF-20 y HB-14), el estado de R11-20 y R11-02 en el
   backlog, y DTI-39 y DTI-40 con lo que se cerró.

### Out-of-Scope (fuera del alcance)

- **Quién subió cada archivo:** Administración es una sola cuenta hasta R11-10 (`p1-admins`). Ver
  dudas.
- **La hora en que terminó una importación:** la escribe el importador, que es de `p1-importador`
  (tanda 3). La lista no la necesita.
- **Que la franja "Se está importando…" la vea cualquier admin y el aviso al terminar:** es de
  `p2-campanita` (R11-29, R11-30). La franja sigue con la memoria de la sesión.
- **Elegir dos fechas a mano en la pantalla:** llega con el componente de filtros nuevo (R11-B04).
  El endpoint ya acepta cualquier `desde` y `hasta`.
- **Actualizar sola la lista:** lo en vivo es el avance de cada importación.
- **Un endpoint de descarga en Donaciones** (decisión de la ficha §9.6).
- **El paginador definitivo:** se usa el provisorio (`Paginadores.de`), que `p1-paginador` reemplaza
  sin cambiar las direcciones.
- **El importador y la lectura del CSV:** `p1-importador`.
- **Los textos "entidad" → "Organización":** la pantalla no los usa; el cambio general es de
  `p1-nombres`.

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Decisiones del diseñador del lote (sin matriz 5D, como pide el lote). Las de pantalla están en la
[ficha §9](../../interfaz/flujos/r11-p1-importaciones.md#9-decisiones-y-preguntas).

- **Alternativa elegida para el listado:** el mismo patrón de R11-01 (arreglo y `X-Total-Count`).
  Descartado: un sobre `{elementos, total}`, distinto de todos los otros listados.
- **Nombre y fecha de subida en la tabla `archivo`.** Descartado: seguir con el nombre en la sesión
  del cliente (no se puede buscar, y otro admin no lo ve) y dar a las filas viejas la hora de la
  migración (dato falso; `fecha_alta` lo aceptó porque el orden de donantes lo necesitaba en todas
  las filas, y acá alcanza con mandarlas al final).
- **Sin hora de fin.** Descartado en este lote: la escribe el importador (`p1-importador`).
- **Descarga armada por el cliente de a tandas.** Descartado: `GET .../filas-con-error.csv` en
  Donaciones (el texto de "Qué pasó" vive en el cliente; duplicarlo o mandar los códigos internos).
- **Gateway sin cambios.** `/api/donantes/**` ya enruta la ruta nueva.


### 3.1 `GET /api/donantes/archivos` (nuevo, solo ADMIN)

**Antes:** no existe. Solo `POST /api/donantes/archivos` (subir), `GET /api/donantes/archivos/{id}`
y `GET /api/donantes/archivos/{id}/filas-con-error`.

**Después:** sigue el patrón de listados ya decidido en R11-01 (arreglo en el cuerpo, total en
`X-Total-Count`, `ParametrosListado`; [ADR `proposed`](../../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)).
No hay decisión nueva de formato.

| Parámetro | Valores | Qué hace |
|---|---|---|
| `page` | desde 0; por defecto 0 | |
| `size` | 1 a 100; por defecto 20 | Sin `page` ni `size`: la página 0 de 20. Nunca la lista completa |
| `q` | texto, hasta 100 caracteres | "Contiene" sobre `nombre_original`, sin tildes ni mayúsculas (`lower(unaccent(...))`, como R11-01), con `%`, `_` y `\` escapados. Las filas sin nombre no coinciden con ninguna búsqueda |
| `estado` | `PENDIENTE`, `PROCESANDO`, `PROCESADO`, `PROCESADO_CON_ERRORES`, `ERROR`; repetible | Una o varias. "En curso" del cliente es `estado=PENDIENTE&estado=PROCESANDO` |
| `desde`, `hasta` | `AAAA-MM-DD` | Día de `subido_en` en la zona de Buenos Aires, inclusivos. Con alguno de los dos, las filas sin fecha quedan afuera. `desde` posterior a `hasta`: 400 |
| `orden` | `recientes` (por defecto), `antiguas` | Por `subido_en` descendente o ascendente; las filas sin fecha, siempre al final; desempata por `id` |

Respuesta `200`: arreglo de `ArchivoOutputDTO` (§3.2), una por importación, y el header
`X-Total-Count` con cuántas cumplen la búsqueda y los filtros. Página fuera de rango: arreglo vacío
con el total real.

Errores: `page`, `size`, `q`, `estado`, `orden` o una fecha inválidos, o `desde > hasta`: `400` con
`ERR-CSR-003`. Sin token de admin: `401` o `403`, como el resto del controller.

Para el número "N importaciones en curso", el cliente pide
`GET /api/donantes/archivos?estado=PENDIENTE&estado=PROCESANDO&size=1` y lee `X-Total-Count`: un
conteo, no una lista.

### 3.2 `ArchivoOutputDTO` (respuesta de `POST /archivos`, `GET /archivos/{id}` y del listado)

**Antes:** `id`, `path`, `estado`, `totalFilas`, `procesadas`, `creadas`, `actualizadas`,
`conError`, `ultimos`.

**Después:** los mismos, más dos campos opcionales (no van en `required`):

| Campo | Tipo | Qué es |
|---|---|---|
| `nombre` | `string`, nullable | El nombre con que se subió el archivo. `null` en las importaciones anteriores a `V22` |
| `subidoEn` | `string` `date-time` (hora local de Argentina, sin zona, como `fechaAlta`), nullable | Cuándo se subió. `null` en las anteriores a `V22` |

`path` sigue siendo la clave interna del almacenamiento y no se muestra. La descripción del
`requestBody` del `POST` cambia de "el nombre solo se usa para validar la extensión" a "se usa
para validar la extensión y se guarda como `nombre`".

`GET /api/donantes/archivos/{id}/filas-con-error` no cambia.

### 3.3 Cliente liviano (direcciones propias, no de la API)

| Dirección | Qué hace |
|---|---|
| `GET /admin-importaciones?buscar=&estado=&subida=&desde=&hasta=&orden=&pagina=` | La pantalla. `estado` del cliente: `en-curso`, `terminadas`, `con-errores`, `cortadas`; `subida`: `hoy`, `7-dias`, `30-dias`, `este-mes`, `mes-anterior` (se traducen a `desde` y `hasta`). Un valor que no entiende se ignora y se avisa (ficha §5). Con `HX-Request`, la misma página (el layout toma el `main`) |
| `GET /admin-donantes-importado/filas-con-error.csv?archivo=<id>` | La descarga: `text/csv; charset=UTF-8` con BOM, separador `;`, `Content-Disposition: attachment; filename="filas-con-error-<nombre>.csv"` (o `filas-con-error.csv` sin nombre). Pide las filas de a 100 hasta `conError` y escribe cada tanda; el texto de "Qué pasó" es el mismo de la tabla |
| `admin-donantes-importando` y `admin-donantes-importado` | Sin cambio de dirección. `?nombre=` sigue aceptándose como respaldo |

## 4. Invariantes

- Ninguna respuesta del listado trae más de 100 importaciones; sin `page` ni `size`, 20.
- El total de `X-Total-Count` es el de la base con la búsqueda y los filtros, nunca el de la página.
- Las cifras de cada importación (`procesadas = creadas + actualizadas + conError`) son las mismas
  en el listado, en `GET /{id}` y en la pantalla: el cliente no calcula ninguna.
- Una importación sin `nombre` o sin `subidoEn` nunca recibe uno inventado.
- Ni el cliente ni el servicio cargan en memoria todas las filas con error de un archivo: la
  descarga va de a tandas.
- `path` sigue siendo interno: no se muestra ni se busca por él.

## 5. Criterios de Aceptación

Cada uno se verifica con un test o una captura; entre paréntesis, dónde. Los escenarios principales,
en Dado / Cuando / Entonces:

```gherkin
Escenario: la lista trae de a una página
  Dado 25 importaciones guardadas
  Cuando el admin pide GET /api/donantes/archivos sin parámetros
  Entonces recibe las 20 más recientes y X-Total-Count: 25

Escenario: filtrar por las que están en curso
  Dado una importación PENDIENTE, una PROCESANDO y tres PROCESADO
  Cuando pide estado=PENDIENTE&estado=PROCESANDO&size=1
  Entonces recibe una y X-Total-Count: 2

Escenario: una importación vieja no inventa datos
  Dado una importación sin nombre ni fecha de subida
  Cuando el admin abre admin-importaciones con un filtro de fecha
  Entonces no aparece; sin el filtro aparece al final con "Archivo sin nombre guardado" y "Sin fecha guardada"

Escenario: el avance que termina en ERROR sin filas
  Dado una importación que pasa a ERROR sin ninguna fila leída
  Cuando la página de avance se actualiza sola
  Entonces la respuesta es HX-Redirect a admin-donantes-importar-error
```

Lista completa:

- **C1. Página por defecto.** Dado 25 archivos, `GET /api/donantes/archivos` sin parámetros
  devuelve 20, los más recientes primero, con `X-Total-Count: 25` (controller y servicio).
- **C2. Búsqueda sin tildes.** Con "padrón-octubre.csv" y "donantes.csv", `q=PADRON` devuelve solo
  el primero (repositorio en memoria y test sobre PostgreSQL).
- **C3. Estado repetible.** `estado=PENDIENTE&estado=PROCESANDO` devuelve solo esos dos estados, y
  su `X-Total-Count` es el número de la línea "en curso" (servicio y cliente).
- **C4. Fechas inclusivas en hora argentina.** Un archivo subido el 15/10 a las 23:30 de Buenos
  Aires entra con `desde=2026-10-15&hasta=2026-10-15` y no con `desde=2026-10-16` (servicio, con
  `Clock` fijo).
- **C5. Filas viejas.** Un archivo con `nombre` y `subido_en` en `NULL` aparece sin filtros, al final
  en los dos órdenes, no aparece con un filtro de fecha ni con una búsqueda, y la pantalla dice
  "Archivo sin nombre guardado" y "Sin fecha guardada" (servicio y escenario `con-viejas`).
- **C6. Errores de parámetros.** `size=0`, `size=101`, `estado=OTRO`, `orden=otro`,
  `desde=2026-13-01` y `desde` posterior a `hasta` dan 400 con `ERR-CSR-003` (controller).
- **C7. La subida guarda nombre y fecha.** `POST /api/donantes/archivos` con
  `donantes-octubre.csv` responde `nombre: "donantes-octubre.csv"` y `subidoEn` con la hora del
  `Clock`, y `GET /{id}` los devuelve igual (controller y adaptador JPA).
- **C8. Compatibilidad del DTO.** Un cliente que lee el `ArchivoOutputDTO` de antes sigue
  funcionando: los campos de siempre no cambian de nombre ni de tipo (test de contrato y oasdiff sin
  cambios incompatibles).
- **C9. Gateway.** `GET /api/donantes/archivos` llega a Donaciones por el gateway
  (`RutasGatewayTest`).
- **C10. La pantalla pide de a una página.** `admin-importaciones` hace como máximo dos pedidos a
  Donaciones por visita: la página pedida y el conteo en curso, los dos con `size` ≤ 20 (test del
  adaptador REST con `MockRestServiceServer`).
- **C11. Cada fila lleva a donde corresponde.** En curso → `admin-donantes-importando?archivo=<id>`;
  terminada, con errores o cortada con filas → `admin-donantes-importado?archivo=<id>` (o la de
  avance cortada, la que elige hoy el controller); `ERROR` sin filas → sin enlace y con "El archivo no
  se pudo leer." (test del controller).
- **C12. Filtros en la dirección.** Buscar, filtrar y cambiar de página dejan todo en la dirección;
  el paginador conserva la búsqueda y los filtros; sin JavaScript anda igual (test del controller y
  captura sin JavaScript).
- **C13. Nombre desde Donaciones.** El avance abierto desde la lista, sin `?nombre=`, muestra el
  nombre del archivo (test del adaptador).
- **C14. Descarga.** Con 250 filas con error, la descarga trae el encabezado y 250 líneas, hace 3
  pedidos de a 100 y nunca guarda más de una tanda (test con `MockRestServiceServer`). Si un pedido
  falla en el medio, el archivo termina con la línea de aviso de la ficha.
- **C15. `ERROR` sin filas durante el sondeo.** Con `HX-Request` y `?etapa=`, una importación que
  pasó a `ERROR` sin filas responde `HX-Redirect` a `admin-donantes-importar-error` (test del
  controller).
- **C16. Verificación visual.** `admin-importaciones` (con datos, `vacia`, `sin-resultados` y
  `con-viejas`) pasa axe, el toque y el desborde a 360, 1280 y 1920 px (`-Pvisual`).
- **C17. Calidad.** `mvn spotless:check`, `mvn clean test -pl donaciones-service -am`,
  `mvn clean test -pl cliente-liviano -am` y `mvn clean test -pl api-gateway -am` en verde;
  pre-flight de SonarCloud ([guía](../../IA/07-errores-frecuentes-sonarcloud-ia.md)).

## 6. Compatibilidad con los clientes actuales

- **Cliente liviano:** su record `DonacionesApi.Archivo` ignora los campos nuevos hasta que se los
  agregue: nada se rompe antes. Las direcciones de avance y resultado no cambian, y `?nombre=` sigue
  sirviendo para los enlaces que ya existen (la franja de la sesión).
- **Seeder de demo** (`demo/seeder/`): sube su CSV con `POST /api/donantes/archivos` y lee el
  avance por id; los campos nuevos no lo afectan. No se cambia.
- **Ensayo de 20.000 filas** (`demo/ensayo-importacion/`): igual que el seeder.
- **Gateway:** la ruta ya está en la allowlist; no hay ruta pública nueva.
- **Base:** `V22` solo agrega columnas que aceptan `NULL` y un índice; no reescribe filas.

## 7. Especificación Técnica de Diseño

Sin diseño de clases, como pide el lote: solo lo que el constructor necesita decidir.

- **Two-Gate (AGENTS.md §9.1):** Gate A no se cumple. El listado aplica el patrón ya decidido en el
  ADR `proposed` de [listados paginados](../../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  y la búsqueda el de [`unaccent`](../../adr/donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md);
  las dos columnas son datos del propio archivo, como `fecha_alta` y `ultimos` en el ADR
  [`fecha_alta` y últimos importados](../../adr/donaciones-service/20261111-fecha-de-alta-del-donante-y-ultimos-importados-en-el-archivo.md).
  Sin ADR nuevo. Si el revisor humano lo pide, se escribe uno `proposed` sucesor de ese último.
- **Dónde se toca:** `DonantesController` y `IDonantesController` (el `GET` nuevo),
  `IArchivoDonantesService` y `ArchivoDonantesService` (listado y `Clock` para `subidoEn`), `Archivo`
  (los dos datos nuevos), `IArchivoDonantesRepository` con su adaptador JPA y su versión en memoria,
  `ArchivoEntity`, `ArchivosPersistenciaMapper`, `ArchivoOutputDTO`, `V22`. En el cliente:
  `DonacionesApi`, el puerto de personas del admin con sus adaptadores REST y de maqueta, el
  controller nuevo, `AdminDonantesController` (el `HX-Redirect`, la descarga y el nombre), `Pantallas`
  (la sección), las plantillas `admin-importaciones.html`, `admin-donantes.html`,
  `admin-donantes-importando.html` y `admin-donantes-importado.html`, y `paginas.txt`.
- **Plan de pruebas (TDD, primero el test que falla):**
  1. Servicio y repositorio en memoria: C1 a C6.
  2. Controller de Donaciones: C6 y C7; test sobre PostgreSQL de la consulta (C2 y C4).
  3. Contrato: OpenAPI 1.10.0 y oasdiff (C8); `RutasGatewayTest` (C9).
  4. Cliente: adaptador REST con `MockRestServiceServer` (C10, C13, C14), controllers con MockMvc
     (C11, C12, C15), y la verificación visual (C16).
  5. Gate 1 y Gate 2 de los tres módulos (C17). Gate 3/4 en el cierre de la oleada, con el stack
     propio.

## 8. Riesgos

| Riesgo | Qué se hace |
|---|---|
| `V22` choca con otra migración de Donaciones de la misma tanda (`p1-admins` también toca Donaciones) | La renumera el integrador, como dice la regla de la corrida |
| La descarga de un archivo con muchas filas con error tarda (hasta 200 pedidos para 20.000) y pasa los límites de espera | Cada pedido tiene su propio límite de 15 s y la respuesta se va escribiendo, así que el total no corta la descarga. Se prueba en el cierre con el CSV del ensayo. Si no alcanza, la salida es un endpoint de descarga en Donaciones (la alternativa descartada en la ficha §9.6), en otro lote |
| El nombre del archivo puede traer datos personales ("padron-ana-ejemplo.csv") | Se muestra solo al admin, que ya ve los donantes; no va a logs ni a notificaciones. Se recorta a 255 caracteres y se guarda sin la ruta del navegador |
| La lista muestra "Importando" en una que ya terminó (no se actualiza sola) | La hora de las cifras y "Actualizar" a la vista, y la ayuda lo dice (ficha §4.1) |
| `admin-importaciones` no empieza con `admin-donantes-`, y `Pantallas` marca la sección por prefijo | Entrada explícita en `Pantallas` (fuera de los archivos del lote, imprescindible) y un test que verifica que Donantes queda marcado |
| Cambio en el modelo `Archivo` sin ADR | Ver §7 (Two-Gate) |
| El DER de Donaciones no tiene estas columnas | Se dice en la descripción del PR. El DER de la entrega no se edita (es un registro histórico) |


## Aprobación del diseño p1-importaciones (2026-11-12)

Aprobado por el usuario de forma automática: pidió que, en P1 y P2, cada duda se resuelva con la opción recomendada por el diseñador, sin parada. Cada duda queda resuelta así:

1. Cuando existan las cuentas por admin (R11-10, lote p1-admins), ¿la lista suma una columna 'Subió' con el nombre de quien cargó el archivo? Recomiendo que sí, en el lote que cierre R11-10, sumando subido_por al archivo. **Resuelto: la opción recomendada.**
2. V22 puede chocar con otra migración de Donaciones de la misma tanda (p1-admins también toca Donaciones). Recomiendo que la renumere el integrador, como dice la regla de la corrida. **Resuelto: la opción recomendada.**
3. Una descarga con muchas filas con error (hasta 200 pedidos para 20.000) podría tardar. Recomiendo probarla en el cierre con el CSV del ensayo y, solo si no alcanza, pasar a un endpoint de descarga en Donaciones en otro lote. **Resuelto: la opción recomendada.**
4. El DER de Donaciones no tiene nombre_original ni subido_en en Archivo. Recomiendo decirlo en la descripción del PR y no editar el DER de la entrega, que es un registro histórico. **Resuelto: la opción recomendada.**
