# Alcance de las operaciones REST expuestas para EntidadBeneficiaria (sin PUT ni DELETE)
- Status: proposed
- Date: 2026-07-02
- Deciders: Grupal
- Tags: donaciones, api-rest, ddd, trazabilidad

## Contexto y Problema
Al verificar que la exposición REST de `donaciones-service` esté completa, se ve que `EntidadBeneficiariaController` solo expone `POST /api/entidades` y `GET /api/entidades/{id}`. `EntidadBeneficiaria` es un Aggregate Root muy simple: solo tiene su `id` y una referencia (`juridicaId`) a la `Persona Jurídica` que representa; no tiene ningún otro campo. Además, otros agregados del propio servicio (`Necesidad.entidadId`) la referencian por id para trazabilidad histórica. La pregunta que surge es: ¿corresponde completar el CRUD agregando `PUT` y `DELETE`, o solo agregar la lectura por lista que falta (`GET` de todas)?

## Atributos de Calidad y Drivers de Decisión
* Consistencia del modelo de dominio: `EntidadBeneficiaria` no tiene ningún campo con necesidad de mutación confirmada más allá de su creación.
* Integridad de la trazabilidad histórica: `Necesidad.entidadId` referencia esta entidad; borrarla físicamente rompería esa referencia en reportes o consultas históricas.
* Reutilización de mecanismos ya existentes: el proyecto ya resuelve la "baja" de una `Persona` (incluida una `Juridica`) anonimizando sus datos vía `PersonasService.eliminarPersona` (`DELETE /api/personas/{id}`), sin borrar el registro.
* Mantenibilidad: no introducir operaciones que no correspondan a un caso de uso real del negocio.

## Alternativas Consideradas
* Agregar los 4 verbos (GET, GET/{id}, PUT, DELETE) para cumplir "CRUD completo" en sentido literal, con `DELETE` haciendo hard-delete en cascada de las `Necesidad`es asociadas.
* Agregar un mecanismo de baja lógica propio (flag `activo`, o anonimizar variables de `EntidadBeneficiaria` y desactivar sus `Necesidad`es asociadas).
* Agregar solo `GET` (listar todas); no exponer `PUT` ni `DELETE`. La baja de una entidad se logra indirectamente anonimizando su `Persona Jurídica` asociada vía el endpoint ya existente.

## Resultado de la Decisión

Alternativa elegida: "Agregar solo `GET` (listar todas); no exponer `PUT` ni `DELETE`".

Justificación:

**Sobre `PUT`:** `EntidadBeneficiaria` es efectivamente inmutable — sus dos únicos datos (`id` y `juridicaId`) no tienen ninguna necesidad de negocio confirmada para ser editados después de la creación. Agregar un `PUT` obligaría a inventar semántica (¿se permite cambiar de Jurídica asociada? ¿qué pasa con las `Necesidad`es ya vinculadas?) sin que exista un caso de uso real que lo requiera.

**Sobre `DELETE`:** se evaluó replicar el patrón que ya usa el proyecto para el caso estructuralmente más parecido, `Donante` (otro Aggregate Root que solo referencia a una `Persona` sin tener datos propios), donde `DonantesService.eliminarDonante` hace un hard-delete real. Sin embargo, a diferencia de `Donante`, dar de baja una `EntidadBeneficiaria` en términos de negocio significa dar de baja a la organización que representa — y eso ya tiene una vía resuelta: anonimizar su `Persona Jurídica` asociada vía `DELETE /api/personas/{id}`, que ya coordina la anonimización de datos personales y la sincronización con `notificaciones-service`. No hay una necesidad de negocio confirmada de una operación de baja adicional y distinta sobre `EntidadBeneficiaria` en sí misma, y agregar una implicaría resolver preguntas no solicitadas (¿cascada sobre `Necesidad`es? ¿bloqueo si tiene actividad histórica? ¿soft-delete?) sin un caso de uso real que las fuerce.

### Consecuencias Positivas
* No se introduce estado ni lógica de negocio no solicitada por ningún caso de uso confirmado.
* Se reutiliza el mecanismo de anonimización de `Persona` ya existente y probado, en vez de duplicar lógica.
* Coherente con el criterio ya aplicado en el ADR de `Donacion` (no forzar los 4 verbos HTTP por defecto sobre un Aggregate Root).

### Consecuencias Negativas
* No existe una acción explícita de "dar de baja una entidad" distinguible de "anonimizar su Jurídica asociada".
* Las `Necesidad`es asociadas a una `EntidadBeneficiaria` cuya Jurídica fue anonimizada no tienen ningún mecanismo automático de desactivación (queda pendiente para cuando exista un caso de uso real que lo requiera).

### Validación
Se valida revisando que `IEntidadBeneficiariaController` exponga únicamente `crearEntidad` (POST), `obtenerEntidad` (GET/{id}) y `obtenerTodas` (GET) — sin `PUT` ni `DELETE`.

## Análisis de Alternativas

### Agregar los 4 verbos (CRUD completo literal)

Agregar `PUT /api/entidades/{id}` y `DELETE /api/entidades/{id}` (con cascada física sobre las `Necesidad`es asociadas).

#### Pros
* Cumple literalmente con la palabra "CRUD" del enunciado.
* Uniformidad superficial con otras entidades que sí tienen los 4 verbos.

#### Contras
* No hay ningún campo de `EntidadBeneficiaria` con necesidad de mutación confirmada para justificar un `PUT`.
* Un `DELETE` con cascada física rompe la trazabilidad histórica de las `Necesidad`es que referencian esa entidad.
* No hay ningún caso de uso de negocio, documentado o implícito, que requiera estas operaciones.

### Baja lógica propia (flag `activo` o anonimización coordinada)

Agregar un campo de estado a `EntidadBeneficiaria` (o reutilizar su interfaz `Anonimizable`) para marcarla como dada de baja sin borrarla, desactivando en cascada sus `Necesidad`es.

#### Pros
* Preserva la trazabilidad histórica.
* Permite una acción explícita de "dar de baja" distinta de anonimizar la Jurídica.

#### Contras
* Agrega un concepto de estado nuevo a una entidad que hoy es puramente una referencia, sin PII propia.
* Duplica en espíritu lo que ya resuelve `PersonasService.eliminarPersona` para la Jurídica asociada.
* No hay un caso de uso confirmado que requiera esta distinción hoy.

### Agregar solo GET (listar todas)

Completar únicamente la lectura faltante, dejando explícita la ausencia de `PUT`/`DELETE` como decisión de diseño.

#### Pros
* No introduce estado ni lógica de negocio no solicitada.
* Reutiliza el mecanismo de anonimización de `Persona` ya existente.
* Cubre la necesidad real detectada (no había forma de listar todas las entidades beneficiarias).

#### Contras
* Requiere documentar la decisión explícitamente para que no se interprete como trabajo incompleto.
* Deja sin resolver qué pasa con las `Necesidad`es de una entidad cuya Jurídica fue anonimizada (a revisar si surge un caso de uso real).
