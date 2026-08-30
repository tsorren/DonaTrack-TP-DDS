# Modelo de Agregados — Servicio de Donaciones (DDD)

Este documento detalla el diseño táctico de **Domain-Driven Design (DDD)** para el **Servicio de Donaciones** en DonaTrack, especificando los límites de los agregados (*Aggregates*), sus raíces (*Aggregate Roots*), entidades internas, objetos de valor (*Value Objects*) y la organización de paquetes.

---

## 1. Principios de Diseño Aplicados

1.  **Encapsulación Mediante Visibilidad de Paquetes**: Cada agregado reside en su propio **paquete plano**. La raíz del agregado es pública (`public`), mientras que las entidades internas y objetos de valor se declaran con visibilidad de paquete por defecto (*package-private* / sin modificador) para evitar accesos indebidos desde el exterior.
2.  **Referencias por ID entre Agregados**: La comunicación y relaciones cruzadas entre distintos agregados se realizan **únicamente mediante su identificador (`UUID`)**.
3.  **Inmutabilidad de los Objetos de Valor**: Los objetos como `Bien` o `Direccion` son inmutables (sin *setters*). Los cambios de estado de un agregado que los involucren se realizan reemplazando la referencia completa en lugar de modificarlos internamente.
4.  **Desacoplamiento de Inventario y Carga Histórica**: La carga original (`Donacion`) no mantiene referencias hacia las subdivisiones de stock (`DonacionIndependiente`). Esto evita mutaciones en el registro histórico de aportes y previene problemas de carga en memoria de grandes colecciones.

---

## 2. Catálogo Detallado de Agregados

### 2.1. Agregado: Persona
*   **Aggregate Root**: `Persona` (Clase abstracta polimórfica implementada por `Humana` y `Juridica`).
*   **Objetos de Valor Internos**: 
    *   `Direccion` (calle, altura, piso, departamento, codigoPostal).
    *   `Localidad` (nombreLocalidad, provincia).
    *   `MedioContacto` (tipo, valor).
    *   `TipoDocumento`, `Genero`, `TipoJuridico` (Enums).
*   **Responsabilidad**: Validar la consistencia de los datos demográficos y fiscales de los actores del sistema, asegurando que se registren con al menos un contacto válido.
*   **Paquete**: `grupo5.donaciones.models.entities.personas`

### 2.2. Agregado: Donante
*   **Aggregate Root**: `Donante`.
*   **Referencias Externas (por ID)**: `personaId` (UUID de la persona asociada).
*   **Responsabilidad**: Modelar el perfil del donante para gamificación, historial de aportes y canal de contacto predeterminado.
*   **Paquete**: `grupo5.donaciones.models.entities.donantes`

### 2.3. Agregado: Donación (Carga Histórica)
*   **Aggregate Root**: `Donacion` (Representa la carga masiva original declarada por el donante).
*   **Componentes Internos**: 
    *   `ItemDonacion` (Entidad interna del agregado).
    *   `Bien` (Objeto de Valor compartido).
*   **Referencias Externas (por ID)**: `donanteId` (UUID).
*   **Responsabilidad**: Registrar y preservar la intención de donación del donante de forma inmutable y auditable.
*   **Paquete**: `grupo5.donaciones.models.entities.donaciones`

### 2.4. Agregado: Donación Independiente (Stock e Inventario)
*   **Aggregate Root**: `DonacionIndependiente` (Unidad física e individual de stock en el depósito).
*   **Componentes Internos**:
    *   `ItemDonacionIndependiente` (Objeto de valor).
    *   `Bien` (Objeto de valor compartido).
    *   `CambioEstado` (Registro inmutable de auditoría de transiciones).
*   **Estados de Ciclo de Vida (State Pattern con 7 clases concretas)**:
    *   `EnDeposito`: Bien ingresado físicamente en depósito disponible para matching.
    *   `AsignacionRealizada`: Bien emparejado formalmente con una necesidad insatisfecha.
    *   `ListaParaEntregar`: Bien preparado para ser retirado por el camión de logística.
    *   `EnTraslado`: Bien cargado en ruta de transporte hacia la entidad receptora.
    *   `Entregada`: Bien recepcionado exitosamente por la entidad beneficiaria.
    *   `EntregaFallida`: Entrega rechazada o frustrada, en proceso de resolución o retorno.
    *   `Vencida`: Bien perecedero que superó su fecha límite en depósito antes de ser entregado.
*   **Referencias Externas (por ID)**: 
    *   `donacionOriginalId` (UUID que apunta a la `Donacion` de origen).
    *   `necesidadId` (UUID nulable que apunta a la `Necesidad` de destino cuando transiciona a `AsignacionRealizada`).
*   **Responsabilidad**: Gestionar de forma transaccional el estado físico, la fragmentación de cantidades para asignaciones parciales y el tracking logístico de cada lote en depósito.
*   **Paquete**: `grupo5.donaciones.models.entities.donacionesIndependientes`

### 2.5. Agregado: Entidad Beneficiaria
*   **Aggregate Root**: `EntidadBeneficiaria` (Actor que recibe las donaciones).
*   **Objetos de Valor Internos**: `Direccion`, `Localidad` (dirección postal física).
*   **Responsabilidad**: Validar los datos de contacto y direcciones físicas para coordinar los puntos de entrega de logística.
*   **Paquete**: `grupo5.donaciones.models.entities.beneficiarios`

### 2.6. Agregado: Necesidad
*   **Aggregate Root**: `Necesidad` (Polimórfica: `NecesidadExtraordinaria` y `NecesidadRecurrente`).
*   **Componentes Internos**: `PeriodoNecesidad` (Objeto de valor).
*   **Referencias Externas (por ID)**: `entidadId` (UUID).
*   **Responsabilidad**: Declarar las demandas específicas por subcategoría y cantidades para que sean procesadas por los algoritmos de matchmaking.
*   *Nota de Deuda Técnica (DTI-06):* `NecesidadExtraordinaria` mantiene actualmente una lista directa en memoria `List<DonacionIndependiente>` que será refactorizada a referencias puras por UUID en la Entrega 2.
*   **Paquete**: `grupo5.donaciones.models.entities.necesidades`

### 2.7. Agregado: Propuesta (Matchmaking de Asignación)
*   **Aggregate Root**: `Propuesta` (Representa el lote de sugerencias generado por un algoritmo de asignación).
*   **Componentes Internos**: `DonacionAsignadaItem` (Entidad interna que vincula `donacionIndependienteId`, `necesidadId` y `cantidadAsignada`).
*   **Responsabilidad**: Almacenar temporalmente las propuestas calculadas antes de su confirmación y ejecución transaccional en el inventario.
*   **Paquete**: `grupo5.donaciones.models.entities.propuestas`

---

## 3. Catálogo de Bienes y Normalización

### 3.1. Paquetes: `grupo5.donaciones.models.entities.categorias` y `entities.donaciones`
*   **Bien**: Objeto de valor (descripción, foto, vencimiento) utilizado por los agregados de Donación y Donación Independiente.
*   **ItemDonacionNormalizado**: Entidad que asocia descripciones libres de donantes con su subcategoría formal homologada.
*   **Categoria**: Raíz del catálogo de referencia que define si una categoría requiere fecha de vencimiento (perecedero) o control de estado de uso.
*   **Subcategoria**: Entidad del catálogo con sus alias semánticos (`AliasSubcategoria`).
*   **Unidad**: Enum (KILOGRAMOS, LITROS, UNIDADES, etc.).

---

## 4. Algoritmos de Asignación y Matchmaking

Ubicados en el paquete de dominio **`grupo5.donaciones.models.algoritmos`**:
*   `AlgoritmoAsignacion`: Clase abstracta base que implementa el patrón **Template Method** para coordinar el cruce entre necesidades insatisfechas y el inventario disponible (`StockDeDonaciones`).
*   `AlgoritmoPrioridadSubAtendidos`: Criterio que prioriza entidades con menor porcentaje de asistencia histórica.
*   `AlgoritmoCompatibilidadSemantica`: Criterio de emparejamiento directo por subcategorías y atributos de bienes.
