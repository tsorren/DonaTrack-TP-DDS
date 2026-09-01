# Registro de Deuda Técnica y Decisiones de Arquitectura

## [Refactor] Automatización de Anonimización e Implementación de Surrogate Keys (DTI-01)

* **ADR Propuesto:** [20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa.md](./donaciones-service/20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa.md)
* **Etiquetas:** `arquitectura`, `deuda-tecnica`, `privacidad`, `entrega-2`

### 1. Descripción del Problema

Durante la implementación del mecanismo de ofuscación de datos (Ley de Protección de Datos Personales) para la Entrega
1, se detectaron dos puntos de mejora críticos para la escalabilidad y persistencia del sistema:

* **Anonimización Manual:** Actualmente, la ofuscación se delega manualmente en cada clase mediante la interfaz
  `Anonimizable`. Esto es propenso a errores humanos (si un desarrollador agrega un nuevo campo sensible y olvida
  incluirlo en el método de anonimización, el dato queda expuesto).
* **Riesgo de Integridad Referencial:** Se observó una dependencia temprana en claves naturales (como el DNI /
  documento) para identificar a las Personas. Al ofuscar o eliminar el DNI por motivos de privacidad, se corre el riesgo
  de perder el rastro de las relaciones (claves foráneas) en los registros históricos.

### 2. Propuesta de Solución (Para Entrega 2)

Para la siguiente iteración, cuando se integre la base de datos, se aplicarán las siguientes refactorizaciones:

* **AOP y Reflexión:** Reemplazar la delegación manual por Anotaciones Personalizadas (ej: `@DatoSensible`). Utilizar
  Programación Orientada a Aspectos (AspectJ) o Reflexión para que el sistema barra todo el árbol de objetos y ofusque
  automáticamente cualquier campo marcado con esta anotación.
* **Surrogate Keys (Claves Sustitutas):** Añadir explícitamente un atributo `Long id` autogenerado a la clase abstracta
  `Persona` y demás entidades principales. Este `id` será intocable durante la anonimización, garantizando que el
  borrado del DNI cumpla la ley sin romper las relaciones en la base de datos relacional.

**Prioridad:** Alta (Debe resolverse antes o durante la integración de la capa de Persistencia).

---

> [!NOTE]
> **Formalización de ADRs:**  
> Todos los ítems de deuda técnica (DTI-01 a DTI-06) han sido formalmente redactados e incorporados como ADRs propuestos (`Status: proposed`) en formato Log4brains / MADR, vinculados en cada sección respectiva para su trazabilidad y ciclo de vida de aprobación.

---

## [Refactor] Reubicación de ProcesadorDeDonaciones a Capa de Aplicación (DTI-02)
* **ADR Propuesto:** [20260901-dti-02-reubicacion-de-procesador-de-donaciones-a-capa-de-aplicacion.md](./donaciones-service/20260901-dti-02-reubicacion-de-procesador-de-donaciones-a-capa-de-aplicacion.md)
* **Etiquetas:** `arquitectura`, `donaciones-service`, `responsabilidades`, `refactor`
* **Problema:** `ProcesadorDeDonaciones` orquesta la normalización asíncrona, segmentación y registro de donaciones actuando como un Application Service, pero está ubicado en el paquete `infrastructure/`.
* **Propuesta:** Reubicar la clase al paquete `grupo5.donaciones.services/` (capa de aplicación), manteniendo los adaptadores tecnológicos en infraestructura.
* **Prioridad:** Media.

---

## [Refactor] Desacoplamiento de SegmentacionEventListener (DTI-03)
* **ADR Propuesto:** [20260901-dti-03-desacoplamiento-de-segmentacion-event-listener-en-servicio-de-aplicacion.md](./donaciones-service/20260901-dti-03-desacoplamiento-de-segmentacion-event-listener-en-servicio-de-aplicacion.md)
* **Etiquetas:** `arquitectura`, `donaciones-service`, `eventos`, `cohesion`
* **Problema:** `SegmentacionEventListener` acumula 9 dependencias inyectadas y ejecuta un caso de uso completo de negocio en lugar de ser un listener ligero.
* **Propuesta:** Extraer un servicio de aplicación `SegmentacionService` con dependencias acotadas y delegar la ejecución desde el listener.
* **Prioridad:** Media.

---

## [Refactor] Descomposición de cambiarEstado() en DonacionesIndependientesService (DTI-04)
* **ADR Propuesto:** [20260901-dti-04-descomposicion-de-cambiarestado-en-donaciones-independientes-service.md](./donaciones-service/20260901-dti-04-descomposicion-de-cambiarestado-en-donaciones-independientes-service.md)
* **Etiquetas:** `diseno`, `donaciones-service`, `cohesion`, `mantenibilidad`
* **Problema:** El método `cambiarEstado()` agrupa 6 transiciones de negocio heterogéneas (asignar, preparar, trasladar, entregar, fallar) en un único switch extenso con llamadas acopladas a OpenFeign.
* **Propuesta:** Dividir en métodos o servicios especializados de caso de uso (ej: `asignarDonacion()`, `iniciarTraslado()`, `confirmarEntrega()`).
* **Prioridad:** Media.

---

## [Refactor] Separación de Responsabilidades en AlgoritmosService (DTI-05)
* **ADR Propuesto:** [20260901-dti-05-segregacion-de-responsabilidades-en-algoritmos-service.md](./donaciones-service/20260901-dti-05-segregacion-de-responsabilidades-en-algoritmos-service.md)
* **Etiquetas:** `diseno`, `donaciones-service`, `responsabilidades`
* **Problema:** `AlgoritmosService` ejecuta los algoritmos de asignación y al mismo tiempo gestiona el ciclo de vida CRUD y notificaciones de propuestas.
* **Propuesta:** Segregar en un servicio puro de ejecución algorítmica y un `PropuestasService` dedicado a la gestión de propuestas.
* **Prioridad:** Baja / Media.

---

## [Arquitectura] Desacoplamiento de Referencias Directas entre Aggregates (DTI-06)
* **ADR Propuesto:** [20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md](./donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
* **ADR Complementario:** [20260901-evaluacion-de-interfaz-asignable-vs-identificador-entidad-beneficiaria.md](./donaciones-service/20260901-evaluacion-de-interfaz-asignable-vs-identificador-entidad-beneficiaria.md)
* **Etiquetas:** `ddd`, `donaciones-service`, `persistencia`, `entrega-2`, `entrega-4`
* **Problema:** `NecesidadExtraordinaria` mantiene una colección directa `List<DonacionIndependiente>` en memoria, y `DonacionIndependiente` retiene una referencia a `Asignable`, rompiendo la regla de DDD de referencias cruzadas únicamente por ID (`UUID`).
* **Propuesta:** Reemplazar por identificadores `UUID` tanto en la asignación de la necesidad como en el receptor de la donación cuando se migre a JPA/Hibernate en la Entrega 2 / Entrega 4.
* **Prioridad:** Alta (crítico para el mapeo relacional de la Entrega 2 y 4).