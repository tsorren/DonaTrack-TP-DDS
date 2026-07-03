# Alcance de las operaciones REST expuestas para Donacion (sin PUT ni DELETE)
- Status: proposed
- Date: 2026-07-02
- Deciders: Grupal
- Tags: donaciones, api-rest, ddd, auditoria

## Contexto y Problema
Al verificar que la exposición REST de `donaciones-service`, esté completa, se ve que `DonacionesController` solo expone `POST /api/donaciones`. 
Esto se debe a que `Donacion` es un Aggregate Root con su propia máquina de estados interna (`CARGADA → NORMALIZADA → SEGMENTADA`), disparada automáticamente por `ProcesadorDeDonaciones` (sin intervención de un actor humano vía API), y con un `historialEstados` expuesto como lista inmutable (`Collections.unmodifiableList`), pensado como registro de auditoría. La pregunta que surge es: ¿Corresponde completar el CRUD agregando `PUT` y `DELETE`, o solo las operaciones de lectura?

## Atributos de Calidad y Drivers de Decisión
* Integridad del historial de auditoría: `historialEstados` no debe poder alterarse ni perderse
* Consistencia del modelo de dominio: las transiciones de estado de `Donacion` son automáticas y no deben poder forzarse arbitrariamente desde la API
* Completitud de la API: debe ser posible consultar una donación ya cargada, incluyendo su historial
* Mantenibilidad: no introducir operaciones que no correspondan a un caso de uso real del negocio

## Alternativas Consideradas
* Agregar los 4 verbos (GET, GET/{id}, PUT, DELETE) para cumplir "CRUD completo" en sentido literal
* Agregar solo GET (listar) y GET/{id} (detalle); no exponer PUT ni DELETE

## Resultado de la Decisión

Alternativa elegida: "Agregar solo GET (listar) y GET/{id} (detalle); no exponer PUT ni DELETE"

Justificación:
En Domain-Driven Design, un Aggregate Root expone las operaciones que tienen sentido de negocio, no los 4 verbos HTTP por defecto. Permitir `PUT` habilitaría reescribir `items` de una donación ya `NORMALIZADA` o `SEGMENTADA`, rompiendo la invariante que el propio dominio ya protege (los métodos `marcarNormalizada()`/`marcarSegmentada()` validan el estado antes de avanzar). Permitir `DELETE` eliminaría un registro de auditoría, contradiciendo el propósito de `historialEstados`. El mismo criterio ya se aplica de forma consistente a `DonacionIndependiente`, que tampoco expone `DELETE`.

### Consecuencias Positivas
* Se preserva la invariante de historial inmutable como registro de auditoría
* No se introduce lógica de negocio no solicitada por la issue ni por el dominio existente
* Queda un precedente documentado para decisiones similares sobre otros agregados con máquina de estados

### Consecuencias Negativas
* No expone todas las operaciones de la palabra "CRUD".

### Validación
Se valida revisando que `IDonacionesController` exponga únicamente `cargarDonacion` (POST), `listarDonaciones` (GET) y `obtenerDonacion` (GET/{id}); que el diagrama de clases y la colección de Postman reflejen esa misma superficie; y que el detalle de una donación (`GET /api/donaciones/{id}`) permita consultar `historialEstados` para fines de auditoría.

## Análisis de Alternativas

### Agregar los 4 verbos (CRUD completo literal)

Agregar `PUT /api/donaciones/{id}` y `DELETE /api/donaciones/{id}` sin condiciones adicionales.

#### Pros
* Cumple literalmente con la palabra "CRUD" del enunciado
* Uniformidad superficial con otras entidades que sí tienen los 4 verbos

#### Contras
* `PUT` permitiría mutar items de una donación ya procesada, rompiendo la invariante de estado que el propio dominio valida en `marcarNormalizada()`/`marcarSegmentada()`
* `DELETE` eliminaría un registro que existe específicamente para trazabilidad/auditoría
* No hay ningún caso de uso de negocio, documentado o implícito, que requiera estas operaciones

### Agregar solo GET y GET/{id}

Completar únicamente las operaciones de lectura faltantes, dejando explícita la ausencia de `PUT`/`DELETE` como decisión de diseño.

#### Pros
* Respeta las invariantes de estado y auditoría ya modeladas en el dominio
* Coherente con el tratamiento que ya recibe `DonacionIndependiente` (sin `DELETE`)
* Cubre la necesidad real detectada (no había forma de consultar una donación ya cargada)

#### Contras
* Requiere documentar la decisión explícitamente para que no se interprete como trabajo incompleto
