# ADR-E6-08 — Fotos de recepción en almacenamiento de objetos

- Status: proposed
- Date: 2026-11-06
- Deciders: Grupo 5 (D27, marcada ✅ en [`requerimientos-e6.md`](../../requerimientos/requerimientos-e6.md)); dependencia `io.minio` en `logistica-service`, aprobada por tsorren el 2026-11-06 con el plan de trabajo sin autenticación; quitar el EXIF re-codificando la imagen, lectura por redirección y nombres de bucket y usuario, propuestos por el agente
- Tags: logistica, minio, s3, almacenamiento, fotos, privacidad, multipart, entrega-6

> **Origen:** §11 de `requerimientos-e6.md` (ADR-E6-08, "Listo" para redactar), decisión D27 y
> RNF-PRIV-03/04. La implementación es la tarea 8 del
> [plan E6 sin autenticación](../../requerimientos/plan-e6-trabajo-sin-auth.md) (HB-10) y la deuda
> queda en [DTI-37](../DEUDA_TECNICA.md#dti-37). No se toca código en esta tarea.

## Contexto y Problema

Para confirmar una recepción, la entidad tiene que cargar al menos una foto (RF-12). Esas fotos se
muestran después en la portada y en el mapa público (RF-01, RF-02).

Qué hay hoy:

- `[OBSERVED]` Logística no sube archivos: `PATCH /api/entregas/{id}/fotos` recibe URLs ya
  publicadas en otro lado (`AdjuntarFotoRecepcionRequestDTO.fotoRecepcionUrl` o `fotosRecepcion`), y
  `CambioEstadoEntregaRequestDTO.fotosRecepcion` es obligatoria para `ENTREGADA`. Nadie valida que la
  URL sea una imagen, ni su tamaño, ni sus metadatos.
- `[OBSERVED]` `Entrega.fotosRecepcion` es `List<String>`, y el documento Mongo ya la guarda como
  `FotoRecepcionSubdocument(clave, subidaEn)`, con el Javadoc "Clave del objeto en MinIO". El modelo
  documental previó el almacenamiento de objetos, pero `logistica-service/pom.xml` no tiene `io.minio`.
- `[OBSERVED]` MinIO ya está en el compose y lo usan Incentivos (bucket `insignias`) y Donaciones
  (`donaciones-padrones`, `donaciones-fotos-bienes`) con `io.minio:minio:8.5.17`. Cada servicio tiene
  su usuario y una política limitada a sus buckets (`persistencia/minio/init-usuarios.sh`).
- `[OBSERVED]` Donaciones ya tiene el puerto `StoragePort` (`guardar`, `abrir`, `metadatos`,
  `urlTemporal`) y el adaptador `MinioStorageAdapter`, que firma las URLs contra un endpoint público
  aparte (`DONACIONES_MINIO_PUBLIC_ENDPOINT`).
- `[DOCUMENTED]` Una foto puede traer coordenadas GPS en el EXIF (RNF-PRIV-04), y las fotos tienen
  que ser privadas, con lectura por URL firmada de vida corta (RNF-PRIV-03).

Problema: ¿cómo llega la foto desde el navegador hasta un almacenamiento privado, dónde se valida y
quita el EXIF, y cómo se lee después?

## Atributos de Calidad y Drivers de Decisión

Pesos `[INFERRED]`: privacidad (EXIF, bucket privado) 30 · validación antes de guardar 25 ·
superficie expuesta (MinIO y CORS) 15 · costo antes del 13/11 15 · consistencia con lo que ya usan
Donaciones e Incentivos 10 · escala con archivos grandes 5.

## Relaciones Arquitectónicas

1. **Extiende:** [almacenamiento de objetos MinIO (S3) para padrones y archivos](../donaciones-service/20260901-almacenamiento-de-objetos-minio-s3-para-padrones-y-archivos.md)
   (`accepted`): mismo producto, misma librería, un usuario por servicio y buckets con prefijo.
2. **Se diferencia de:** las fotos de bienes de ese mismo ADR, que se suben con una URL prefirmada
   directa a MinIO. Para las fotos de recepción, la D27 eligió subirlas a Logística, porque hay que
   quitar el EXIF antes de guardarlas.
3. **Respeta:** [persistencia documental de Logística en MongoDB](./20261025-persistencia-documental-de-logistica-en-mongodb.md)
   y el [modelo documental](./20261025-modelo-documental-de-logistica-embebidos-y-referencias.md)
   (`proposed` los dos): la entrega guarda la clave del objeto en `FotoRecepcionSubdocument`, nunca
   los bytes.
4. **Respeta:** [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
   (`accepted`): el endpoint multipart y la lectura son endpoints nuevos; el `PATCH .../fotos`
   actual se conserva.
5. **Se usa en:** el ADR de réplicas por evento (`proposed`, en un PR aparte), que publica la URL
   estable de cada foto en `entrega.exitosa`.

## Alternativas Consideradas

Puntajes de 1 (peor) a 5 (mejor), estimados por el agente (`[INFERRED]`). Columnas: privacidad ·
validación · superficie · costo · consistencia · escala.

### Subida (D27)

| Alternativa | Priv. | Valid. | Superf. | Costo | Consist. | Escala | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **S1. Multipart a Logística, que valida, quita el EXIF y guarda en MinIO** | 5 | 5 | 5 | 4 | 4 | 2 | **4,6** |
| S2. URL prefirmada: el navegador sube directo a MinIO y Logística valida después | 2 | 2 | 2 | 4 | 5 | 5 | 2,75 |
| S3. El cliente liviano sube a MinIO con sus propias credenciales | — | — | — | — | — | — | Descartada `[REJECTED]` en la D27: credenciales de almacenamiento en la capa de UI |

### Cómo se quita el EXIF

| Alternativa | Priv. | Valid. | Superf. | Costo | Consist. | Escala | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **E1. Re-codificar la imagen con `javax.imageio` (JDK): se escriben solo los píxeles** | 5 | 5 | 5 | 4 | 4 | 3 | **4,65** |
| E2. Cortar los segmentos de metadatos (APP1, XMP) del archivo, sin re-codificar | 4 | 3 | 5 | 3 | 4 | 4 | 3,75 |
| E3. Librería de metadatos (Apache Commons Imaging o similar) | 5 | 4 | 5 | 3 | 2 | 3 | 4,05 |

### Lectura

| Alternativa | Priv. | Valid. | Superf. | Costo | Consist. | Escala | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **L1. URL estable de Logística que responde `302` hacia una URL firmada de 5 minutos** | 4 | 5 | 3 | 4 | 5 | 5 | **4,25** |
| L2. Logística lee el objeto y sirve los bytes (proxy) | 4 | 5 | 5 | 3 | 3 | 2 | 4,05 |
| L3. Bucket de lectura pública | — | — | — | — | — | — | Descartada: incumple RNF-PRIV-03 |

## Resultado de la Decisión

Se eligen **S1**, **E1** y **L1**.

1. **Subida.** `POST /api/entregas/{id}/fotos` (`multipart/form-data`, una o más partes `foto`).
   - Solo para una entrega que todavía no está `ENTREGADA`.
   - Devuelve, por cada foto, su `fotoId` y su URL estable. La entidad confirma después con
     `PATCH /api/entregas/{id}/estado` (`ENTREGADA`) y esas URLs en `fotosRecepcion`, como hoy.
   - El `PATCH /api/entregas/{id}/fotos` con URLs externas se conserva (HB-10).
2. **Validación, en este orden, antes de guardar:**
   - tope de **5 MB** por archivo (`spring.servlet.multipart.max-file-size`, y el chequeo en el
     servicio); un archivo más grande da `413`;
   - tipo real por **magic bytes**, no por la extensión ni el `Content-Type`: JPEG (`FF D8 FF`) y PNG
     (`89 50 4E 47 0D 0A 1A 0A`); cualquier otro da `415`;
   - la imagen tiene que decodificar: un archivo que no decodifica da `400`.
3. **EXIF.** Logística re-codifica la imagen con `ImageIO` y guarda solo los píxeles, en el mismo
   formato. Así se van EXIF, GPS, XMP y miniaturas embebidas, sin depender del formato de cada
   segmento.
4. **Almacenamiento.**
   - Bucket privado `logistica-fotos-recepcion`, creado al arrancar de forma idempotente.
   - Usuario de MinIO `logistica-service`, con política sobre `logistica-*`, creado por
     `minio-init` como los otros dos.
   - Clave `entregas/<idEntrega>/<fotoId>.<ext>`, con `fotoId` UUID generado por Logística. La entrega
     guarda la clave y la hora de subida en `FotoRecepcionSubdocument`.
   - Puerto `AlmacenFotosRecepcion` en Logística y adaptador MinIO, con la misma forma que el
     `StoragePort` de Donaciones. No va a `common-lib`: no es lo bastante neutro ni compartido para
     ese módulo.
5. **Lectura.** `GET /api/entregas/{id}/fotos/{fotoId}` responde `302` hacia una URL firmada `GET`
   de **5 minutos**, firmada contra el endpoint público de MinIO
   (`LOGISTICA_MINIO_PUBLIC_ENDPOINT`). La URL estable es la que se publica en la API y en
   `entrega.exitosa`.
6. **Dependencia.** `io.minio:minio:8.5.17` en `logistica-service/pom.xml`, la misma versión que
   Donaciones e Incentivos. Para el EXIF no se suma nada: `javax.imageio` es del JDK.

Justificación: validar y limpiar en el servidor, antes de guardar, es la única forma de cumplir
RNF-PRIV-04 sin confiar en el navegador. Logística es el Experto (GRASP) de la entrega y de su
confirmación, y el puerto de almacenamiento mantiene el dominio libre de MinIO (inversión de
dependencias, pureza del dominio de AGENTS.md §4.1). Re-codificar resuelve el EXIF y la validación de
"es una imagen de verdad" con un solo paso del JDK.

## Consecuencias

### Positivas

- Ninguna foto se guarda sin pasar por el tope, el tipo real y la limpieza de metadatos.
- MinIO no recibe escrituras desde el navegador y no necesita CORS.
- Las fotos que viajan a Donaciones y a la portada son URLs que no vencen; lo que vence es la firma.
- La misma librería y el mismo patrón de usuarios que ya usan los otros servicios.

### Negativas

- **Tres saltos por foto** (navegador → gateway → Logística → MinIO) y hasta 5 MB por archivo en la
  memoria de Logística. Para fotos de un celular alcanza; para archivos grandes, la subida directa
  escala mejor.
- **Re-codificar cambia el archivo:** un JPEG pierde algo de calidad y el tamaño puede variar. Se
  re-codifica con calidad alta (0,9). WebP, HEIC y otros formatos quedan fuera hasta que el JDK o una
  librería los lea.
- **MinIO queda expuesto al navegador para lectura:** la URL firmada apunta a su endpoint público.
  Sin la firma no se lee nada.
- **Objetos huérfanos:** una foto subida para una entrega que nunca se confirma queda en el bucket.
  Se acepta como en Donaciones; una regla de ciclo de vida del bucket la limpiaría.
- **La URL estable es pública** mientras la foto esté publicada, porque RF-01 y RF-02 la muestran
  sin login. Con HB-13, la lectura de una entrega no confirmada se limita a la entidad dueña.
- El compose suma un usuario y una variable de contraseña, en el mismo archivo que edita A1-01 de
  SPEC-06: hay que coordinar.

## Análisis por alternativa

- **S1.** Pro: valida y limpia antes de guardar; MinIO interno para escritura. Contra: tres saltos y
  el archivo pasa por la memoria de Logística.
- **S2.** Pro: la mejor escala y el patrón de las fotos de bienes. Contra: el archivo con GPS ya está
  guardado cuando Logística lo mira, y MinIO queda expuesto para escritura, con CORS.
- **S3.** Pro: ninguno que compense. Contra: credenciales de almacenamiento en el navegador.
- **E1.** Pro: elimina todos los metadatos y prueba que es una imagen; sin dependencias. Contra:
  re-codificar cambia el archivo y limita los formatos a los del JDK.
- **E2.** Pro: el archivo queda igual. Contra: hay que conocer cada segmento de cada formato; un
  segmento desconocido puede dejar pasar datos.
- **E3.** Pro: borrado de metadatos probado y más formatos. Contra: una dependencia nueva, que
  AGENTS.md §6 pide justificar y que el usuario no aprobó.
- **L1.** Pro: cumple RNF-PRIV-03 literal; Logística no transfiere los bytes. Contra: el navegador
  habla con MinIO para leer.
- **L2.** Pro: MinIO no se expone nunca. Contra: cada imagen de la portada pasa por Logística, y
  RNF-PRIV-03 pide URL firmada.
- **L3.** Pro: lo más simple. Contra: cualquiera lista y lee las fotos.

## Validación

- Test del validador: un PNG y un JPEG válidos pasan; un `.jpg` que es un PDF da `415`; un archivo de
  más de 5 MB da `413`; bytes al azar con cabecera JPEG dan `400`.
- Test de privacidad: un JPEG de fixture con EXIF GPS (sintético) sale sin ningún segmento APP1.
- Test de integración con MinIO por Testcontainers (como en Donaciones): la subida crea el objeto con
  la clave esperada y la lectura responde `302` hacia una URL firmada que vence.
- Test de contrato: el endpoint multipart y la lectura en `openapi-logistica.yaml`.

## Trabajo futuro

- [DTI-37](../DEUDA_TECNICA.md#dti-37): implementación (tarea 8 del plan, HB-10).
- Regla de ciclo de vida del bucket para las fotos huérfanas.
- HB-13: limitar la subida y la lectura de fotos no publicadas a la entidad dueña de la entrega.
