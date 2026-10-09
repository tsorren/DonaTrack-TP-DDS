# [DTI-14] Normalización y Geocodificación de Direcciones

- Status: proposed
- Date: 2026-10-09
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-14, direcciones, normalizacion, geocodificacion, persistencia, logistica

## Contexto y Problema

En el DER de `donaciones-service`, `Direccion` guarda `localidad`, `provincia` y `pais` como texto libre, en lugar de tablas `Localidad → Provincia → Pais`. Fue una desnormalización deliberada: leer una dirección no requiere tres joins y no hace falta mantener un catálogo geográfico que ningún caso de uso administra. En el dominio, `Localidad`, `Provincia` y `Pais` son records que envuelven un `String`, sin validación contra ninguna fuente.

Esa decisión trae costos que hoy nadie absorbe:

1. **Inconsistencia de nombres:** el mismo lugar puede quedar cargado como "CABA" o "Capital Federal". Listar provincias requiere `DISTINCT` y la agrupación por zona no es confiable.
2. **Sin integridad geográfica:** nada valida que la localidad pertenezca a la provincia (por ejemplo, "Rosario, Mendoza" se acepta).
3. **Logística no lo resuelve:** `logistica-service` recibe la dirección en el evento de donación asignada (`DonacionAsignadaEventListener`) y la guarda con sus propios records `Localidad → Provincia → Pais`, también como texto libre y sin validar. Además, la dirección no tiene coordenadas, y por eso `AlgoritmoOrdenadorSimple` ordena las entregas por UUID. Esa deuda ya está documentada en su Javadoc: el criterio real (agrupar por zona o por cercanía al depósito) necesita una referencia geográfica que se pueda procesar.

¿Quién debe garantizar que una dirección sea válida y quién debe obtener sus coordenadas?

## Atributos de Calidad y Drivers de Decisión

* **Integridad de datos en el origen:** un dato se valida donde se carga, que es el único punto donde todavía se puede rechazar o corregir.
* **Responsabilidad por bounded context:** cada servicio resuelve lo que necesita su propio dominio.
* **Mantenibilidad:** no mantener catálogos de referencia que el sistema no administra.
* **Eficiencia logística:** las rutas tienen que poder ordenarse por proximidad real.

## Alternativas Consideradas

* **Delegar todo a `logistica-service`:** donaciones guarda la dirección tal como la carga el usuario y logística la valida y la geocodifica cuando la recibe.
* **Catálogo geográfico propio en `donaciones-service`:** tablas `Pais`, `Provincia` y `Localidad`, con FK desde `Direccion`.
* **Validación en el origen contra fuente externa y geocodificación en logística:** donaciones normaliza al cargar contra la API Georef y logística geocodifica la dirección ya válida.

## Resultado de la Decisión

Alternativa elegida: "Validación en el origen contra fuente externa y geocodificación en logística"

Justificación:
La responsabilidad se reparte según quién puede cumplirla y quién necesita el dato:

1. **`donaciones-service` valida y normaliza al cargar la dirección** (alta de persona o de depósito) contra la API Georef del Estado argentino (`apis.datos.gob.ar/georef`, gratuita, con provincias, departamentos y localidades oficiales). Guarda el nombre oficial devuelto, o su código Georef, en las mismas columnas de texto de `Direccion`. La desnormalización del DER se mantiene sin joins y desaparecen los problemas de nombres inconsistentes y de localidades que no pertenecen a su provincia.
2. **`logistica-service` geocodifica:** con la dirección ya válida obtiene las coordenadas que necesita para ordenar las entregas. Es su responsabilidad porque las rutas son de su dominio, y permite saldar la deuda anotada en `AlgoritmoOrdenadorSimple`.

### Consecuencias Positivas

* Los datos geográficos son consistentes desde el origen y todos los consumidores de eventos los reciben limpios.
* No se mantiene ningún catálogo propio: la fuente oficial se encarga de actualizarlo.
* El esquema de `Direccion` no cambia: siguen siendo columnas de texto, y opcionalmente se agregan columnas para los códigos Georef.
* Habilita el ordenamiento geográfico de entregas en logística.

### Consecuencias Negativas

* La carga de una dirección pasa a depender de un servicio externo. Si Georef no está disponible, hay que elegir entre rechazar la carga o aceptarla marcada como "sin validar" para revisión posterior, el mismo criterio que la categorización automática con revisión de bienes ([20260613-categorizacion-de-bienes](./donaciones-service/20260613-categorizacion-de-bienes.md)).
* Hace falta un adaptador de infraestructura (puerto en el dominio, cliente HTTP en `infrastructure/`) y un timeout o circuit breaker para la llamada.
* Las direcciones ya cargadas necesitan una migración que las normalice, y las que no se puedan resolver quedan para revisión manual.

### Validación

Se considerará saldada cuando:
1. El alta de persona y de depósito en `donaciones-service` rechace o marque para revisión una dirección cuya localidad no pertenece a la provincia indicada (test con adaptador Georef simulado).
2. Dos cargas de la misma localidad con nombres distintos ("CABA" y "Capital Federal") persistan el mismo nombre oficial.
3. `logistica-service` reemplace `AlgoritmoOrdenadorSimple` por un ordenamiento basado en coordenadas.

## Análisis de Alternativas

### Delegar todo a `logistica-service`

#### Pros
* Donaciones no cambia.
* Logística ya necesita los datos geográficos para rutear.

#### Contras
* Valida tarde: cuando logística recibe la dirección, la donación ya está asignada y no hay a quién pedirle una corrección.
* Los demás consumidores de la dirección (notificaciones, el propio donaciones) siguen con datos inconsistentes.
* Hoy logística tampoco valida, así que delegarlo solo traslada la deuda de un servicio a otro.

### Catálogo geográfico propio en `donaciones-service`

#### Pros
* Integridad garantizada por FK.
* Sin dependencia externa en tiempo de carga.

#### Contras
* Hay que cargar y mantener unas 4.000 localidades para un dato que donaciones no consulta: ningún algoritmo de asignación filtra por ubicación.
* Revierte la desnormalización del DER y agrega joins en cada lectura de dirección.
* Es un catálogo de referencia externo del que el servicio no es dueño, y quedaría desactualizado sin un proceso de sincronización.

### Validación en el origen contra fuente externa y geocodificación en logística

#### Pros
* Valida donde todavía se puede corregir.
* Cada servicio resuelve lo que su dominio necesita.
* No hay catálogo propio que mantener.

#### Contras
* Dependencia de un servicio externo en el alta de direcciones.
* Requiere coordinar el cambio en dos servicios.
