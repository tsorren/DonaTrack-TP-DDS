# Almacenamiento de Objetos MinIO (S3) para Padrones y Archivos

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: infraestructura, minio, s3, almacenamiento, donaciones, persistencia

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
  - `donatrack-donantes-csv`: Bucket privado para CSVs de donantes, accesible solo por el backend.
  - `donatrack-bienes-fotos`: Bucket con acceso mediante URLs prefirmadas temporales (`GET` con TTL de 2 horas).
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
2. Tests unitarios del `MinioStorageAdapter` simulando operaciones con `MinioClient`.

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