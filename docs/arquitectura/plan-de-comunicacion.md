# Plan de Comunicación — RabbitMQ y Eventos de Dominio en `common-lib`

Este documento define la estructura de empaquetado, los contratos de comandos y eventos, el análisis de propiedad de datos (*data ownership*) y las estrategias de consistencia eventual para el sistema **DonaTrack** dentro de `common-lib`.

---

## 1. Ownership de Datos y Consistencia Eventual

Para evitar el acoplamiento y garantizar la integridad referencial en un ecosistema distribuido, definimos el **propietario de los datos (Data Owner)** para cada entidad y el mecanismo para propagar cambios de forma asincrónica.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              SERVICIO DE DONACIONES (Data Owner)                       │
│  [Donantes DB (Truth)] ──registro/cambio──> Outbox ──> Event: DonanteCreado/Actualizado │
└────────────────────────────────────────────────────────────────────────────────────────┘
                                                               │
                                                         RabbitMQ Broker
                                                               │
                                                               ▼ (Suscripción asincrónica)
                          ┌────────────────────────────────────────────────────────┐
                          │              SERVICIO DE INCENTIVOS                    │
                          │  [Perfil de Gamificación local] ──> Consistencia       │
                          └────────────────────────────────────────────────────────┘
```

### 1.1. Perfil del Donante y Preferencias de Contacto
*   **Data Owner**: *Servicio de Donaciones*. Este servicio valida y almacena los datos reales (nombre, email, teléfono, dirección, tipo de documento) y las preferencias (medio de contacto predeterminado) de los donantes. El endpoint REST de modificación del canal predeterminado reside en este servicio.
*   **Consistencia Eventual**: Cuando un donante se registra, actualiza su perfil o modifica su medio de contacto predeterminado, *Donaciones* publica `DonanteCreadoEvent` o `DonanteActualizadoEvent`.
    *   *Servicio de Incentivos* y *Servicio de Notificaciones* consumen este evento asincrónicamente para actualizar su caché o tablas de ruteo y lectura local.

### 1.2. Estados de Donación y Trazabilidad
*   **Data Owner**: *Servicio de Donaciones* (para estados "En depósito", "Asignación realizada" y "Vencida") y *Servicio de Logística* (para estados "Lista para entregar", "En traslado", "Entregada" y "Entrega fallida").
*   **Consistencia Eventual**: Cuando Logística cambia el estado a "En traslado", publica el evento. Donaciones se suscribe a este evento y actualiza de forma eventual el estado de la donación en su base de datos transaccional para auditoría e informes.

### 1.3. Progreso de Misiones y Puntos
*   **Data Owner**: *Servicio de Incentivos*. Es el único que puede calcular puntos, validar insignias y ascender categorías de donante.
*   **Consistencia Eventual**: Tras procesar un evento de entrega, Incentivos calcula el progreso. Si el donante sube de categoría, publica `CategoriaAscendidaEvent`. Donaciones se suscribe para mostrar la insignia en la interfaz del donante de manera eventual.

---

## 2. Estructura de Paquetes en `common-lib`

Los DTOs de comandos y eventos se agrupan en `common-lib` bajo la siguiente jerarquía de paquetes:

```
common-lib/src/main/java/grupo5/common/
  ├── events/
  │     └── DomainEvent.java   <-- Interfaz base de metadatos comunes para eventos
  ├── donaciones/
  │     ├── shared/            <-- Dirección, Localidad y Medio de Contacto DTOs (Objetos de valor)
  │     ├── donantes/
  │     │     ├── commands/    <-- CrearPersona..., ModificarPersona..., CrearDonante...
  │     │     └── events/      <-- PersonaHumanaCreadaEvent, DonanteCreadoEvent, etc.
  │     ├── donaciones/
  │     │     ├── commands/    <-- RegistrarCargaDonacionDTO, ConfirmarAsignacionDonacionDTO
  │     │     └── events/      <-- DonacionRegistradaEvent, DonacionAsignadaEvent, etc.
  │     └── entidades/
  │           ├── commands/    <-- RegistrarEntidadBeneficiariaDTO, RegistrarNecesidadDTO
  │           └── events/      <-- EntidadBeneficiariaCreadaEvent, NecesidadRegistradaEvent
  ├── logistica/
  │     ├── shared/            <-- CoordenadaDTO (Objetos de valor)
  │     └── envios/
  │           ├── commands/    <-- PlanificarRutaDTO, IniciarRecorridoDTO, RegistrarEntregaDTO
  │           └── events/      <-- DonacionListaParaEntregarEvent, DonacionEnTrasladoEvent, etc.
  ├── incentivos/
  │     ├── gamificacion/
  │     │     ├── commands/    <-- CrearMisionDTO
  │     │     └── events/      <-- MisionCompletadaEvent, CategoriaAscendidaEvent
  │     └── analiticas/
  │           ├── commands/    <-- CalcularRankingMensualDTO
  │           └── events/      <-- RankingMensualCalculadoEvent
  └── notificaciones/
        └── alertas/
              ├── commands/    <-- EnviarNotificacionSincronicaDTO
              └── events/      <-- NotificacionEnviadaEvent, NotificacionFallidaEvent
```

---

## 3. Catálogo de DTOs de Comandos y Eventos de Integración (Event-Carried State Transfer)

Todos los eventos de integración implementan la interfaz `DomainEvent` (con `eventId`, `aggregateId` y `timestamp`). Para respetar las directrices de Domain-Driven Design (DDD):
1.  **Objetos de Valor**: Componentes internos que forman parte del ciclo de vida del Agregado (como `DireccionDTO` o `ItemDonacionDTO`) se anidan completamente en el evento.
2.  **Relaciones entre Agregados**: La vinculación con otros agregados distintos se realiza **únicamente mediante ID (`UUID`)**, evitando la duplicidad de datos en tránsito.

### 3.1. Servicio de Donaciones (`donaciones`)

#### Contexto: Donantes y Personas (`donantes`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `CrearPersonaHumanaDTO`: `nombre`, `apellido`, `fechaNacimiento`, `genero`, `tipoDocumento`, `nroDocumento`, `direccion`, `email`, `telefono`.
    *   `ModificarPersonaHumanaDTO`: `nombre`, `apellido`, `genero`, `direccion`, `email`, `telefono`.
    *   `CrearPersonaJuridicaDTO`: `razonSocial`, `tipoJuridico`, `rubro`, `tipoDocumento`, `nroDocumento`, `direccion`, `email`, `telefono`, `representantes`.
    *   `ModificarPersonaJuridicaDTO`: `razonSocial`, `tipoJuridico`, `rubro`, `direccion`, `email`, `telefono`, `representantes`.
    *   `CrearDonanteDTO`: `personaId` (UUID), `tipoDonante` ("HUMANA"/"JURIDICA"), `canalPredeterminado`, `whatsapp`.
    *   `ActualizarDonanteDTO`: `nombreORazonSocial`, `email`, `telefono`, `direccion`, `canalPredeterminado`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `PersonaHumanaCreadaEvent` / `PersonaHumanaModificadaEvent`: Lleva la información demográfica completa y sus Value Objects: `DireccionDTO` y `List<MedioContactoDTO>`.
    *   `PersonaJuridicaCreadaEvent` / `PersonaJuridicaModificadaEvent`: Lleva los datos legales, su `DireccionDTO`, `List<MedioContactoDTO>`, y una lista de IDs de representantes (`List<UUID> representanteIds`) que referencian a otros agregados de personas humanas.
    *   `DonanteCreadoEvent` / `DonanteActualizadoEvent`: Lleva la vinculación del perfil y canal predeterminado, referenciando a su persona por ID (`personaId`).

#### Contexto: Donaciones y Bienes (`donaciones`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `RegistrarCargaDonacionDTO`: `donanteId` (UUID), `descripcion`, `items` (lista de `ItemCargaDTO`).
    *   `ConfirmarAsignacionDonacionDTO`: `donacionId` (UUID), `entidadId` (UUID), `mensajeNotificacion`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `DonacionRegistradaEvent`: Lleva la carga registrada, referenciando al donante por ID (`donanteId`) y detallando sus ítems (`List<ItemDonacionDTO>`).
    *   `DonacionSegmentadaEvent`: Informa la generación de subdonaciones con su respectivo `UUID` independiente.
    *   `DonacionAsignadaEvent`: Indica la asignación de una donación, referenciando únicamente por ID a los agregados correspondientes (`donacionId`, `entidadId`, `donanteId`).
    *   `DonacionVencidaEvent`: Informa la caducidad física de bienes.

#### Contexto: Entidades y Necesidades (`entidades`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `CrearEntidadBeneficiariaDTO`: `razonSocial`, `direccion`, `telefono`, `emailsRepresentantes`.
    *   `RegistrarNecesidadDTO`: `entidadId` (UUID), `subcategoria`, `cantidadNecesitada`, `tipoNecesidad` (RECURRENTE/EXTRAORDINARIA).
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `EntidadBeneficiariaCreadaEvent`: Contiene la razón social, dirección (`DireccionDTO`), teléfono y correos.
    *   `NecesidadRegistradaEvent`: Contiene los detalles de la necesidad y referencia a la entidad beneficiaria por ID (`entidadId`).

---

### 3.2. Servicio de Logística (`logistica`)

#### Contexto: Rutas y Envíos (`envios`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `PlanificarRutaDTO`: `donacionIds` (lista), `camionId` (UUID), `puntosRuta` (lista).
    *   `IniciarRecorridoDTO`: `rutaId` (UUID), `camionId` (UUID).
    *   `RegistrarEntregaDTO`: `donacionId` (UUID), `urlsFotosRecepcion` (lista).
    *   `RegistrarFalloEntregaDTO`: `donacionId` (UUID), `motivoFallo`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `DonacionListaParaEntregarEvent`: Indica disponibilidad de la donación para entrega (referencia `donacionId`).
    *   `DonacionEnTrasladoEvent`: Lleva la ubicación geográfica actual (`CoordenadaDTO`), referenciando por ID a los agregados `donacionId`, `rutaId` y `camionId`.
    *   `DonacionEntregadaEvent`: Contiene urls de fotos de recepción, receptor y fecha.
    *   `DonacionEntregaFallidaEvent`: Detalla el motivo de falla y si es reintentable.

---

### 3.3. Servicio de Incentivos (`incentivos`)

#### Contexto: Gamificación (`gamificacion`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `CrearMisionDTO`: `nombre`, `categoriaAsociada`, `cantidadObjetivo`, `tipoMision`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `MisionCompletadaEvent`: Informa la misión completada y el donante asociado por ID (`donanteId`).
    *   `CategoriaAscendidaEvent`: Detalla el ascenso de categoría del donante (referencia `donanteId`).

#### Contexto: Analíticas y Rankings (`analiticas`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `CalcularRankingMensualDTO`: `mes`, `anio`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `RankingMensualCalculadoEvent`: Contiene la lista ordenada del top de donantes (`List<DonanteRankingDTO>`), referenciando a cada uno por ID.

---

### 3.4. Servicio de Notificaciones (`notificaciones`)

#### Contexto: Alertas y Mensajería (`alertas`)
*   **Comandos Sincrónicos (Input REST)**:
    *   `EnviarNotificacionSincronicaDTO`: `usuarioId` (UUID), `mensaje`, `canal`.
*   **Eventos Asincrónicos (RabbitMQ)**:
    *   `NotificacionEnviadaEvent` / `NotificacionFallidaEvent`: Detalla el resultado del envío y referencia al usuario por ID (`usuarioId`).
