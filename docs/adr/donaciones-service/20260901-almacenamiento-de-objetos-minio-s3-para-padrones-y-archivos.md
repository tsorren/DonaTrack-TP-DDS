# Almacenamiento de Objetos MinIO (S3) para Padrones y Archivos

- Status: accepted
- Date: 2026-09-01 (aceptado el 2026-10-29)
- Deciders: Decisión Grupal; aceptado por tsorren (responsable de integración) por instrucción explícita del 2026-10-29
- Tags: infraestructura, minio, s3, almacenamiento, donaciones, persistencia

> **Nota de auditoría (2026-10-14):** el contenedor `minio` de `docker-compose.yml` y `docker-compose.preprod.yml` pertenecía solo a `incentivos-service` (bucket `insignias`). En `donaciones-service` no existían `StoragePort`, `MinioStorageAdapter` ni la dependencia `io.minio`.
>
> **Nota de implementación (2026-10-28, rama `E4_donaciones_minio`):** la decisión se implementó en `donaciones-service`. El ADR se aceptó el 2026-10-29. Lo que difiere de la propuesta original está detallado en [Implementación](#implementación).

## Contexto y Problema

En `donaciones-service`, el sistema requiere gestionar archivos no estructurados de gran tamaño:
1. Archivos CSV con padrones masivos de donantes subidos por administradores para importación diferida.
2. Fotografías de los bienes donados (`Bien.fotoUrl`) para auditoría y validación del estado de uso.
Almacenar estos archivos directamente en el sistema de archivos local (`/tmp` o rutas relativas) es inviable en entornos contenerizados (Docker/Kubernetes) debido a la naturaleza efímera de los contenedores (los archivos se perderían al reiniciar el pod). Guardarlos como datos binarios (`BLOB` o `BYTEA`) dentro de la base de datos relacional PostgreSQL degrada masivamente el rendimiento, satura la memoria compartida e infla los backups de la base.

## Atributos de Calidad y Drivers de Decisión

* **Escalabilidad y Rendimiento:** Descargar a la base de datos relacional del almacenamiento de archivos pesados.
* **Persistencia y Durabilidad:** Garantizar que los archivos sobrevivan a reinicios o repliegues de contenedores.
* **Seguridad y Control de Acceso:** Proteger padrones confidenciales de acceso público y permitir acceso temporal a fotos de bienes.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `donaciones-service` ([decisiones_futuras_en_oleada_10.md](../../arquitectura/diseno/donaciones/decisiones_futuras_en_oleada_10.md) §3).
* **Hallazgo:** Se diseñó la integración con MinIO mediante un puerto de almacenamiento (`StoragePort`) y su adaptador `MinioStorageAdapter` utilizando el cliente oficial de MinIO compatible con la API de Amazon S3.

## Alternativas Consideradas

* **Almacenamiento de Objetos Compatible con S3 (MinIO):** Desplegar un contenedor MinIO en Docker Compose con buckets segregados:
  - `donaciones-padrones` (propuesto como `donatrack-donantes-csv`): Bucket privado para CSVs de donantes, accesible solo por el backend.
  - `donaciones-fotos-bienes` (propuesto como `donatrack-bienes-fotos`): Bucket privado con acceso mediante URLs prefirmadas temporales (`GET` con TTL de 2 horas).
  La base de datos relacional solo almacena la clave de objeto (`object_key VARCHAR(500)`).
* **Almacenamiento en Base de Datos Relacional (`BYTEA` en PostgreSQL):** Guardar los binarios directamente en tablas SQL.
* **Volumen Compartido en File System Local (NFS / Bind Mounts):** Guardar archivos en un directorio montado de disco.

## Resultado de la Decisión

Alternativa elegida: "Almacenamiento de Objetos Compatible con S3 (MinIO)"

Justificación:
MinIO es el estándar de la industria para almacenamiento de objetos compatible con AWS S3 en entornos on-premise y Docker. Desacopla la persistencia de binarios del motor relacional, permite generar URLs prefirmadas seguras para que el cliente acceda a fotos sin sobrecargar el backend de Spring Boot, y facilita una migración transparente a la nube (AWS S3 o Google Cloud Storage) en el futuro sin modificar código.

### Consecuencias Positivas

* Cero sobrecarga de BLOBs en PostgreSQL; backups de base de datos rápidos y livianos.
* Seguridad garantizada: padrones inaccesibles desde el exterior y fotos con URLs temporales firmadas.
* Alta disponibilidad y compatibilidad universal mediante SDK S3 estándar.

### Consecuencias Negativas

* Requiere un contenedor adicional (`minio`) y configuración de credenciales en `docker-compose.yml`.

### Validación

Se valida mediante:
1. Contenedor MinIO en `docker-compose.preprod.yml` con healthcheck activo.
2. Tests de integración del `MinioStorageAdapter` contra un MinIO real (Testcontainers), en lugar de simular `MinioClient`.

## Análisis de Alternativas

### MinIO (S3 Object Storage)

#### Pros
* Arquitectura moderna de almacenamiento desacoplado.
* URLs prefirmadas para descargas directas seguras.
* Portable a cualquier proveedor cloud.

#### Contras
* Nuevo componente de infraestructura para orquestar.

### Base de Datos Relacional (BLOB/BYTEA)

#### Pros
* Transacciones ACID unificadas sobre archivos.

#### Contras
* Degrada severamente el rendimiento de PostgreSQL.
* Backups gigantescos y costosos.

### File System Local

#### Pros
* No requiere servicios adicionales.

#### Contras
* Frágil en clústeres multi-pod; requiere configurar almacenamiento de red compartido complejo (NFS).

## Implementación

`[OBSERVED]` Implementado en la rama `E4_donaciones_minio`. Plan y decisiones: [`plan-minio-csv-y-fotos.md`](../../entrega-4/donaciones/plan-minio-csv-y-fotos.md) y [`bitacora-minio.md`](../../entrega-4/donaciones/bitacora-minio.md).

* **Puerto y adapters:** `StoragePort` (`models/ports`) expresado en bucket y clave, sin tipos de MinIO: `guardar`, `abrir` (stream), `metadatos` y `urlTemporal` (`SUBIR`/`DESCARGAR`). `MinioStorageAdapter` con `donaciones.minio.enabled=true` (perfil `postgres`) y `StorageEnMemoria` en el modo por defecto. `StoragePort.validarClave` rechaza claves que podrían escapar del bucket (`..`, absolutas, barras invertidas).
* **Buckets:** `donaciones-padrones` y `donaciones-fotos-bienes`, los dos privados. `InicializadorBuckets` los crea al arrancar, de forma idempotente.
* **Credenciales:** un usuario de MinIO por servicio, creado por el contenedor `minio-init` (`persistencia/minio/init-usuarios.sh`). `donaciones-service` solo tiene acceso a `donaciones-*`. Ningún servicio usa las credenciales raíz.
* **Padrones CSV:** `POST /api/donantes/archivos` recibe `multipart/form-data` (cambio de contrato no aditivo, aprobado). El servicio guarda el archivo con una clave UUID propia y `Archivo.path` pasa a ser esa clave (sin migración). El importador lee el objeto como stream.
* **Fotos de bienes:** URL prefirmada. `POST /api/donaciones/fotos` devuelve una URL `PUT` (15 min) y una referencia `/fotos-bienes/<uuid>.<ext>` que va en `fotoUrl`. Al cargar la donación se valida con los metadatos del objeto (existe, JPEG/PNG/WebP, hasta 10 MB), porque un `PUT` prefirmado no limita el tamaño. Al leer, la referencia se firma como URL `GET` de 2 h. `foto_url TEXT` no cambia. Las URLs externas pasan sin cambios.
* **Diferencias con la propuesta:** nombres de buckets con prefijo del servicio (para que la política del usuario sea `donaciones-*`); la base guarda la clave en las columnas existentes (`archivo.path`, `foto_url`) en lugar de una columna nueva `object_key VARCHAR(500)`; las fotos se suben con `PUT` prefirmado, además del `GET`.
* **Consecuencia aceptada:** si la base falla después de subir el padrón queda un objeto huérfano en el bucket. Lo mismo pasa con una foto subida que nunca se usa en una donación.

