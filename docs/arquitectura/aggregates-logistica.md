# Modelo de Agregados — Servicio de Logística (DDD)

Este documento detalla el diseño táctico de **Domain-Driven Design (DDD)** para el **Servicio de Logística** en DonaTrack, especificando los límites de los agregados (*Aggregates*), sus raíces (*Aggregate Roots*), entidades internas, objetos de valor (*Value Objects*) y la organización de paquetes.

---

## 1. Principios de Diseño Aplicados

1.  **Encapsulación y Protección del Estado**: Se removieron todos los *setters* públicos automáticos (`@Setter` de Lombok) en las raíces de agregados para garantizar que el estado de los mismos solo pueda ser transicionado a través de métodos de negocio descriptivos con validaciones.
2.  **Inmutabilidad de Colecciones**: Las colecciones de agregados (como las entregas en `Ruta` o el historial en `Entrega`) están protegidas de modificaciones externas. Los getters manuales devuelven vistas inmutables (`List.copyOf`).
3.  **Referencias por ID entre Agregados**: La comunicación y relaciones cruzadas entre distintos agregados (del propio servicio o servicios externos) se realizan **únicamente mediante su identificador (`UUID`)**.
4.  **Inmutabilidad de Objetos de Valor**: Los objetos de valor (como el registro de auditoría `CambioEstadoEntrega` o la dirección postal `Direccion`) están implementados como Java `records` inmutables.

---

## 2. Catálogo Detallado de Agregados

### 2.1. Agregado: Chofer
*   **Aggregate Root**: `Chofer` (implements `AggregateRoot`).
*   **Responsabilidad**: Modelar el conductor del camión de la flota, controlando sus datos personales (nombre, apellido), licencia y número de contacto de forma encapsulada y protegida.
*   **Métodos de Negocio**:
    *   `actualizarLicencia(String nuevaLicencia)`
    *   `actualizarTelefonoContacto(String nuevoTelefono)`
*   **Paquete**: `grupo5.logistica.models.entities.choferes`

### 2.2. Agregado: Camión
*   **Aggregate Root**: `Camion` (implements `AggregateRoot`).
*   **Referencias Externas (por ID)**: `rutaId` (UUID nulable de la ruta activa asignada).
*   **Estados de Ciclo de Vida**: *DISPONIBLE*, *EN_RUTA*, *DESHABILITADO* (Enum `EstadoCamion`).
*   **Responsabilidad**: Modelar cada vehículo de la flota de distribución, validando sus dimensiones físicas y capacidades de carga (volumen, peso, altura) en su constructor, y controlando consistentemente la asignación a rutas y deshabilitación.
*   **Métodos de Negocio**:
    *   `asignarARuta(UUID rutaId)`
    *   `completarRuta()`
    *   `habilitar()`
    *   `deshabilitar()`
*   **Paquete**: `grupo5.logistica.models.entities.camiones`

### 2.3. Agregado: Ruta
*   **Aggregate Root**: `Ruta` (implements `AggregateRoot`).
*   **Referencias Externas (por ID)**:
    *   `choferId` (UUID del chofer asignado).
    *   `camionId` (UUID del camión asignado).
    *   `entregas` (Lista inmutable de UUIDs que apuntan a las `Entrega` que componen la ruta).
*   **Estados de Ciclo de Vida**: *PENDIENTE*, *EN_TRASLADO*, *COMPLETADA* (Enum `EstadoRuta`).
*   **Responsabilidad**: Planificar y rastrear la ruta operativa de reparto para un vehículo y conductor específicos. Garantiza que la ruta no se pueda iniciar si no tiene entregas asignadas.
*   **Métodos de Negocio**:
    *   `agregarEntrega(UUID entregaId)` (valida no duplicados y estado PENDIENTE).
    *   `iniciarRuta()` (valida existencia de entregas).
    *   `completarRuta()`
*   **Paquete**: `grupo5.logistica.models.entities.rutas`

### 2.4. Agregado: Entrega
*   **Aggregate Root**: `Entrega` (implements `AggregateRoot`).
*   **Objetos de Valor Internos**: 
    *   `CambioEstadoEntrega` (Auditoría inmutable de cambio de estado - Java record).
    *   `Direccion` (Dirección destino - Java record).
    *   `Localidad`, `Provincia`, `Pais` (Records geográficos).
*   **Referencias Externas (por ID)**:
    *   `idRuta` (UUID de la ruta asignada).
    *   `idDonacion` (UUID de la donación independiente correspondiente en `donaciones-service`).
    *   `idBeneficiaria` (UUID de la entidad beneficiaria destinataria).
*   **Estados de Ciclo de Vida**: *PENDIENTE*, *EN_TRASLADO*, *ENTREGADA*, *NO_RECIBIDA*, *REVISION* (Enum `EstadoEntrega`).
*   **Responsabilidad**: Registrar el ciclo de vida y trazabilidad física de una donación específica desde que sale del depósito hasta que es confirmada por la entidad beneficiaria, registrando un historial inmutable de auditoría para cada cambio de estado.
*   **Métodos de Negocio**:
    *   `iniciarRuta(String chofer)`
    *   `confirmarEntrega(String entidad)`
    *   `adjuntarFotoRecepcion(String fotoURL)`
    *   `negarEntrega(String entidad)`
    *   `regresarAlDeposito(String administrador)`
*   **Paquete**: `grupo5.logistica.models.entities.entregas`
