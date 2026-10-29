# Plan: persistir CSV de padrones y fotos de bienes en MinIO (Donaciones)

> Estado: **implementado** en la rama `E4_donaciones_minio` (sale de `E4_persistencia_donaciones`, #897). Nivel de tarea: ARCHITECTURAL (dependencia nueva, cambio de contrato, infraestructura). Decisiones y avance: [`bitacora-minio.md`](bitacora-minio.md).

## Punto de partida

- `[OBSERVED]` **CSV:** hoy no se sube ningún archivo. `POST /donantes/archivos` recibe un `path` (ruta del disco del servidor) y `LectorDonantesCSV` lo abre con `Files.newBufferedReader(Paths.get(path))`. El archivo tiene que estar ya en el sistema de archivos del contenedor, que no sobrevive a un reinicio.
- `[OBSERVED]` **Fotos:** el cliente manda `fotoUrl` como texto (`Bien.fotoUrl`) y el servicio lo guarda en `foto_url TEXT` (`V9`) sin subir ni descargar nada.
- `[OBSERVED]` Donaciones no tiene dependencia de MinIO, ni `StoragePort`, ni `MinioStorageAdapter`. El contenedor `minio` de los compose y el bucket `insignias` pertenecen a Incentivos.
- `[DOCUMENTED]` El ADR `20260901-almacenamiento-de-objetos-minio-s3-para-padrones-y-archivos` (`proposed` al escribir este plan; `accepted` desde el 2026-10-29) propone MinIO para los padrones y las fotos.
- `[OBSERVED]` Modelo a seguir: Incentivos (`ImagenesInsigniasConfig`, `MinioImagenesInsigniasAdapter`, `MinioInsigniasSeeder`, `MinioInsigniasIntegracionTest`, dependencia `io.minio:minio:8.5.17`).

## Paso 0 · Decisiones previas (del equipo)

1. **Subida de fotos.** `[PROPOSED]` URL prefirmada: Donaciones entrega al cliente una URL temporal de MinIO y el cliente sube directo, así el servicio no maneja los bytes. Alternativa: subida *multipart* a través del servicio (más simple, más pesada).
2. **Contrato del CSV.** Pasar de `path` a *multipart* es un cambio no aditivo (AGENTS.md §8.2). Opciones: endpoint nuevo y el viejo deprecado, o aceptar el breaking change con aprobación explícita.
3. **Dónde se hace.** `[PROPOSED]` Rama aparte, no la #897.

## Paso 1 · Puerto de almacenamiento (dominio)

- Crear `StoragePort` (nombre del ADR) con: guardar un objeto, abrirlo como stream y generar una URL temporal.
- Expresado en bucket y clave, sin tipos de MinIO.

## Paso 2 · Dependencia y configuración

- Agregar `io.minio:minio:8.5.17` al `pom.xml` (misma versión que Incentivos). AGENTS.md §6 exige justificar toda dependencia nueva.
- `@ConditionalOnProperty(donaciones.minio.enabled)`: apagado por defecto, encendido en el perfil `postgres` (como `ImagenesInsigniasConfig`).
- Variables de entorno en `docker-compose.yml`, `docker-compose.preprod.yml` y `.env.example`. El contenedor `minio` ya existe.
- Sin credenciales fijas (AGENTS.md §4.3).

## Paso 3 · Adapters y buckets

- `MinioStorageAdapter` para el perfil `postgres`; adapter en memoria o de disco para el perfil por defecto (simetría con los repositorios).
- Inicializador idempotente que cree los buckets al arrancar: `padrones` (privado) y `fotos-bienes`.

## Paso 4 · CSV de padrones

- El endpoint recibe el archivo, genera una clave propia (UUID; nunca el nombre del cliente, por *path traversal*) y lo guarda en MinIO.
- `Archivo.path` pasa a ser la clave del objeto. La tabla `archivo` no cambia.
- `LectorDonantesCSV` lee un `InputStream` del storage en vez de una ruta. Conservar el manejo del BOM.
- Orden: subir el objeto y después guardar `Archivo`. Si la base falla queda un objeto huérfano (costo aceptable).

## Paso 5 · Fotos

- Endpoint que devuelve `{uploadUrl, referencia}`. El cliente sube a MinIO y manda la `referencia` en `fotoUrl` al crear la donación.
- Al leer, un resolver convierte la referencia en una URL temporal de descarga (mismo patrón que `IImagenesInsignias` de Incentivos: `urlPublica` / `referencia`).
- Las URLs externas ya guardadas pasan sin cambios.
- Validar tipo de contenido y tamaño máximo.

## Paso 6 · Persistencia

- Sin migración: `foto_url TEXT` ya existe y pasa a guardar una referencia.

## Paso 7 · Tests

- Testcontainers con MinIO (modelo: `MinioInsigniasIntegracionTest`).
- Importador leyendo desde stream.
- Una clave con `../` no escapa del bucket.
- `DonacionesPostgresContextTests` sigue arrancando con y sin MinIO.
- Gate 4 con el compose preprod.

## Paso 8 · Documentación

- Actualizar el ADR de MinIO (sigue `proposed`; nunca promoverlo a `accepted`. Nota: lo aceptó tsorren el 2026-10-29), el OpenAPI, la bitácora, `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md`.

## Riesgos a tener presentes

- Cambio de contrato del endpoint de CSV (Paso 0.2).
- Objetos huérfanos si falla la base después de subir (Paso 4).
- Pruebas con MinIO: suele ser la parte que más tiempo lleva.
- Sin Docker no se pueden verificar Gate 3 ni Gate 4: declarar `[DEFERRED_NO_DOCKER]`.
