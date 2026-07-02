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
*   **Paquete**: `grupo5.donaciones.models.entities.persona`

### 2.2. Agregado: Donante
*   **Aggregate Root**: `Donante`.
*   **Referencias Externas (por ID)**: `personaId` (UUID de la persona asociada).
*   **Responsabilidad**: Modelar el perfil del donante para gamificación, historial de aportes y canal de contacto predeterminado.
*   **Paquete**: `grupo5.donaciones.models.entities.donante`

### 2.3. Agregado: Donación (Carga Histórica)
*   **Aggregate Root**: `Donacion` (Representa la carga masiva original declarada por el donante).
*   **Componentes Internos**: 
    *   `ItemDonacion` (Entidad interna del agregado).
    *   `Bien` (Objeto de Valor compartido).
*   **Referencias Externas (por ID)**: `donanteId` (UUID).
*   **Responsabilidad**: Registrar y preservar la intención de donación del donante de forma inmutable y auditable.
*   **Paquete**: `grupo5.donaciones.models.entities.donacion`

### 2.4. Agregado: Donación Independiente (Stock e Inventario)
*   **Aggregate Root**: `DonacionIndependiente` (Unidad física e individual de stock en el depósito).
*   **Componentes Internos**:
    *   `ItemDonacionIndependiente` (Objeto de valor).
    *   `Bien` (Objeto de valor compartido).
*   **Estados de Ciclo de Vida**: *EN_DEPOSITO*, *ASIGNADA*, *EN_TRASLADO*, *ENTREGADA*, *FALLIDA*, *VENCIDA*.
*   **Referencias Externas (por ID)**: 
    *   `donacionOriginalId` (UUID que apunta a la `Donacion` de origen).
    *   `necesidadId` (UUID nulable que apunta a la `Necesidad` de destino cuando el estado transiciona a *ASIGNADA*).
*   **Responsabilidad**: Gestionar de forma transaccional el estado físico, la fragmentación de cantidades para asignaciones parciales y el tracking logístico de cada lote en depósito.
*   **Paquete**: `grupo5.donaciones.models.entities.donacionesIndependientes`

### 2.5. Agregado: Entidad Beneficiaria
*   **Aggregate Root**: `EntidadBeneficiaria` (Actor que recibe las donaciones).
*   **Objetos de Valor Internos**: `Direccion`, `Localidad` (dirección postal física).
*   **Responsabilidad**: Validar los datos de contacto y direcciones físicas para coordinar los puntos de entrega de logística.
*   **Paquete**: `grupo5.donaciones.models.entities.entidad`

### 2.6. Agregado: Necesidad
*   **Aggregate Root**: `Necesidad` (Polimórfica: `NecesidadExtraordinaria` y `NecesidadRecurrente`).
*   **Componentes Internos**: `PeriodoNecesidad` (Objeto de valor).
*   **Referencias Externas (por ID)**: `entidadId` (UUID).
*   **Responsabilidad**: Declarar las demandas específicas por subcategoría y cantidades para que sean procesadas por los algoritmos de matchmaking.
*   **Paquete**: `grupo5.donaciones.models.entities.necesidad`

---

## 3. Kernel Compartido y Catálogo de Referencia

### 3.1. Paquete: `grupo5.donaciones.models.entities.bienes`
Actúa como el kernel compartido del servicio para describir bienes y parametrizar sus reglas:
*   **Bien**: Objeto de valor (descripción, foto, vencimiento) utilizado por los agregados de Donación y Donación Independiente.
*   **Estado**: Enum que representa la condición física (NUEVO, USADO).
*   **Categoria**: Raíz del catálogo de referencia que define si una categoría requiere fecha de vencimiento (perecedero) o control de estado de uso.
*   **SubCategoria**: Entidad del catálogo.
*   **Unidad**: Enum (KILOGRAMOS, LITROS, UNIDADES, etc.).

---

## 4. Notas de Diseño: Simplificación y Eliminación de Entidades

Durante la iteración de diseño del Servicio de Donaciones, se tomó la decisión de eliminar dos entidades del modelo táctico original:

### 1. Eliminación de `DonacionSegmentada`
*   **Razón**: Originalmente, `DonacionSegmentada` actuaba como el Aggregate Root que agrupaba las subdivisiones de la donación. Sin embargo, en el mundo real, cada `DonacionIndependiente` es empaquetada, asignada, enviada y confirmada de forma totalmente autónoma.
*   **Diseño**: Al promover `DonacionIndependiente` a **Aggregate Root** y relacionarla con la `Donacion` original a través de su `donacionOriginalId`, simplificamos drásticamente el modelo. Se evitan problemas de carga en memoria de grandes colecciones y se habilita la re-segmentación libre del stock mediante borrados y creaciones lógicas, bloqueándose únicamente cuando al menos un ítem cambia de estado a *ASIGNADA*.

### 2. Eliminación de `DonacionAsignada`
*   **Razón**: La asignación de bienes es un cambio de estado transaccional sobre la oferta física. En lugar de crear una tabla y entidad asociativa intermedia `DonacionAsignada`, es mucho más simple y coherente que la unidad de stock (`DonacionIndependiente`) maneje la máquina de estados directamente.
*   **Diseño**: Al transicionar el estado de la `DonacionIndependiente` a *ASIGNADA* y setear el `necesidadId` en su propio registro, se consolida la consistencia de inventario en una única transacción de base de datos.
