# Bitácora — Padrones CSV y fotos de bienes en MinIO (Donaciones)

> Registro vivo de la integración de Donaciones con MinIO: decisiones, preguntas y avances, en orden. Para retomar el trabajo, leer primero el **TLDR**.
>
> **Documentos relacionados:** [`plan-minio-csv-y-fotos.md`](plan-minio-csv-y-fotos.md) · [ADR de MinIO](../../adr/donaciones-service/20260901-almacenamiento-de-objetos-minio-s3-para-padrones-y-archivos.md) (sigue `proposed`) · [`persistencia/README.md`](../../../persistencia/README.md) · [OpenAPI de Donaciones](../../arquitectura/contratos/openapi-donaciones.yaml)
>
> **Rama:** `E4_donaciones_minio`, que sale de `E4_persistencia_donaciones` (#897) porque necesita la tabla `archivo` y la columna `foto_url`.

---

## TLDR

> Última actualización: **2026-10-28** — Pasos 1 a 8 hechos.

- **Qué estamos haciendo:** los padrones CSV de donantes y las fotos de los bienes dejan de depender del disco del contenedor (o de una URL cualquiera) y se guardan en MinIO.
- **Cómo:** un puerto `StoragePort` (bucket y clave, sin tipos de MinIO) con dos adapters: MinIO (perfil `postgres`) y memoria (perfil por defecto). El CSV se sube por `multipart` y el servicio lo guarda. Las fotos se suben directo a MinIO con una URL prefirmada, y en la base queda solo una referencia.
- **Dónde estamos:** pasos 1 a 8 hechos, con un commit por paso. Contrato del CSV cambiado (aprobado). Un usuario de MinIO por servicio.
- **Pendiente:** aprobación del ADR por el equipo (sigue `proposed`); progreso por fila del padrón (resto de HB-14 de E6); limpieza de objetos huérfanos.
- **Próximo paso:** revisión del PR.

---

## Estado por paso

| Paso | Contenido | Estado |
|---|---|---|
| 0 | Decisiones previas del equipo | ✅ Cerrado (D-1 a D-3) |
| 1 | Puerto `StoragePort` | ✅ Hecho |
| 2 | Dependencia, configuración y usuario de MinIO por servicio | ✅ Hecho |
| 3 | Adapters (MinIO y memoria) y buckets | ✅ Hecho |
| 4 | Padrón CSV por multipart | ✅ Hecho |
| 5 | Fotos con URL prefirmada | ✅ Hecho |
| 6 | Persistencia | ✅ Sin cambios (no hace falta migración) |
| 7 | Tests con MinIO real | ✅ Hecho |
| 8 | Documentación | ✅ Hecho |

---

## Decisiones

- **D-1 — Contrato del CSV (Paso 0.2).** `POST /api/donantes/archivos` pasa de `{path}` a `multipart/form-data` con la parte `archivo`, en el mismo endpoint. Es un cambio no aditivo, aprobado explícitamente. Motivos: el `{path}` dejaba leer cualquier archivo del contenedor, el front ya esperaba multipart y el OpenAPI no documentaba el cuerpo. Alternativa descartada: endpoint nuevo con el viejo deprecado (mantenía el riesgo mientras existiera).
- **D-2 — Subida de fotos (Paso 0.1).** URL prefirmada, como proponían el plan y el ADR: el servicio no maneja los bytes. Alternativa descartada: multipart a través del servicio (más simple, pero más carga para el servicio).
- **D-3 — Rama.** Rama nueva `E4_donaciones_minio` desde `E4_persistencia_donaciones`, para no mezclar el cambio con la #897.
- **D-4 — Forma del puerto.** `guardar`, `abrir` (stream), `metadatos` y `urlTemporal(bucket, clave, vigencia, Operacion)`, con `Operacion` = `SUBIR` | `DESCARGAR` (un método con enum en lugar de dos métodos). `metadatos` se sumó en el Paso 3 para validar las fotos.
- **D-5 — Credenciales.** Un usuario de MinIO por servicio, con acceso solo a sus buckets: `incentivos-service` → `insignias`, `donaciones-service` → `donaciones-*`. Los crea el contenedor `minio-init` en cada `docker compose up`. Incentivos también dejó de usar el usuario raíz. Primero se había elegido usar las credenciales raíz, como Incentivos, pero se corrigió porque el equipo definió un usuario por servicio.
- **D-6 — Nombres de buckets.** `donaciones-padrones` y `donaciones-fotos-bienes`. El prefijo del servicio permite una política `donaciones-*` que no hay que tocar si se agrega un bucket. Difiere de los nombres del ADR (`donatrack-donantes-csv`, `donatrack-bienes-fotos`), y el ADR quedó actualizado.
- **D-7 — Endpoint público para firmar.** La firma de una URL incluye el host. El servicio habla con MinIO por la red interna (`http://minio:9000`), pero firma con el host que ve el cliente (`DONACIONES_MINIO_PUBLIC_ENDPOINT`) usando un segundo cliente con región fija, que no usa la red para firmar.
- **D-8 — Límites.** CSV de hasta 50 MB; fotos JPEG, PNG o WebP de hasta 10 MB. Son configurables (`donaciones.almacenamiento.*`). El límite multipart de Spring está atado al del CSV.
- **D-9 — Validación de fotos.** Un `PUT` prefirmado no limita el tamaño, así que la foto se valida al cargar la donación con los metadatos del objeto: que exista, que sea de un tipo permitido y que no supere el tamaño. Solo se aceptan referencias con una clave generada por el servicio (UUID y extensión conocida).
- **D-10 — URLs externas.** Un `fotoUrl` que no es una referencia (las URLs ya guardadas o de otro origen) se acepta y se devuelve sin cambios: el cambio es aditivo para quien manda URLs.
- **D-11 — Sin migración.** `archivo.path` guarda la clave del objeto y `foto_url` la referencia. El ADR proponía una columna `object_key`; no hace falta.
- **D-12 — Errores HTTP de multipart.** En el `GlobalExceptionHandler` de `common-lib`: parte faltante → 400, archivo demasiado grande → 413, tipo de contenido no soportado → 415 (antes caían en 500). Son excepciones de Spring, neutras respecto del dominio.

---

## Q&A

**¿Por qué no se guarda el nombre del archivo que manda el cliente?**
Porque el cliente lo controla: un nombre como `../../otro-bucket/x.csv` podría escaparse del bucket (*path traversal*). El servicio genera una clave UUID propia. El nombre original solo se usa para validar la extensión `.csv`.

**¿Qué pasa si la base falla después de subir el padrón a MinIO?**
Queda un objeto huérfano en el bucket. Es un costo aceptado: no hay una transacción entre MinIO y PostgreSQL. El orden (primero MinIO, después la base) evita lo contrario, que sería un `Archivo` que apunta a un objeto inexistente.

**¿Por qué en la base se guarda una referencia y no la URL de la foto?**
Porque la URL prefirmada vence a las 2 horas. Se guarda la referencia (`/fotos-bienes/<uuid>.jpg`) y la URL se firma al leer. Es el mismo patrón que Incentivos con `urlPublica`/`referencia`.

**¿Cómo sube el cliente una foto?**
1. `POST /api/donaciones/fotos` con `{"tipoContenido":"image/jpeg"}`.
2. `PUT` a `uploadUrl` con el header `Content-Type` indicado y los bytes de la foto.
3. `POST /api/donaciones` con `fotoUrl` igual a la `referencia`.

**¿Qué impide que alguien suba un archivo enorme con la URL prefirmada?**
La URL sola no lo impide. Por eso, al cargar la donación se leen los metadatos del objeto y se rechaza si supera los 10 MB o no es una imagen permitida. Además, la URL dura 15 minutos y solo sirve para una clave.

**¿Por qué un usuario de MinIO por servicio?**
Por mínimo privilegio, como los roles de PostgreSQL por schema: si se filtran las credenciales de un servicio, solo exponen sus buckets. Con el usuario raíz, cualquier servicio podía leer o borrar todo.

**¿Funciona sin MinIO?**
Sí. En el perfil por defecto (en memoria), y con `DONACIONES_MINIO_ENABLED=false`, se usa `StorageEnMemoria`. El padrón funciona igual. Las URLs de fotos usan el esquema `memoria://`, que sirve para identificar el objeto pero no para subir ni descargar por HTTP.

**¿Por qué el CSV pasa por el servicio y las fotos no?**
El servicio necesita leer el CSV para importarlo, así que de todos modos tiene que acceder a los bytes; además lo valida antes de aceptarlo. Las fotos el servicio no las procesa, así que pasarlas por él solo sumaría carga.

---

## Detalle por paso

### Paso 2 — usuario por servicio

`persistencia/minio/init-usuarios.sh` corre en el contenedor `minio-init`, que usa la misma imagen que `minio` (trae `sh` y `mc`; no trae `grep`). Se probó contra un MinIO descartable: es idempotente, falla si falta una variable, cada usuario crea y escribe solo en sus buckets, e Incentivos puede hacer lo que necesita su seed (crear `insignias`, ponerle política pública y subir imágenes).

### Paso 4 — contrato

Se eliminó `ArchivoInputDTO`. El controller convierte el `MultipartFile` en `ArchivoDonantesSubido` (sin tipos de Spring) y el servicio valida extensión, que no esté vacío y el tamaño. `LectorDonantesCSV` lee un `InputStream` y conserva el manejo del BOM.

### Paso 7 — tests

- `MinioStorageAdapterIntegracionTest`: subida y descarga con URL prefirmada sin credenciales, buckets privados, `../` no escapa del bucket.
- `DonacionesPostgresMinioContextTests`: perfil `postgres` con Postgres y MinIO reales. Cubre el cableado, la creación de buckets, el padrón por multipart de punta a punta y el ciclo de una foto.
- `DonacionesPostgresContextTests` sigue arrancando sin MinIO.
