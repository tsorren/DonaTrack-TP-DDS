# Plan de Implementación: Refactorización y Estandarización de DTOs y UUIDs en DonaTrack

Este plan detalla las refactorizaciones necesarias para reemplazar los DTOs locales de `donaciones-service` por DTOs persistibles unificados en `common-lib` con el sufijo `Data` e identificadores `UUID`, garantizando un desacoplamiento completo de la capa de dominio.

---

## User Review Required

> [!IMPORTANT]
> **Independencia y Desacoplamiento Absoluto**: Las entidades de dominio y las clases persistibles `...Data` **no se conocerán entre sí** (no tendrán referencias directas de código, imports o acoplamiento).
> 
> **Estructura de Entidades `...Data`**:
> *   Contendrán **todos los atributos** de su correspondiente entidad de dominio.
> *   Cualquier objeto que sea **parte del mismo agregado** se incluirá embebido/anidado en formato `...Data` (ej. `List<ItemDonacionData>` y `BienData` dentro de `DonacionData`).
> *   Cualquier relación con objetos que sean de **otros agregados** se realizará **únicamente por ID (`UUID`)** (ej. `DonacionData` contendrá `UUID donanteId` en lugar de una referencia al objeto `DonanteData`).

---

## Proposed Changes

### 1. Componente: common-lib (DTOs de Persistencia `...Data` con Relaciones Embebidas y por ID)

Definiremos las estructuras de datos persistibles `...Data` respetando los límites de cada agregado.

#### [NEW] [PersonaData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/PersonaData.java)
- Encapsula el estado persistido completo de una Persona (Humana o Jurídica) y sus Value Objects embebidos:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  import java.time.LocalDate;
  import java.util.List;
  import java.util.UUID;
  public record PersonaData(
      UUID id,
      String tipoPersona, // "HUMANA" o "JURIDICA"
      String nombre,
      String apellido,
      LocalDate fechaNacimiento,
      String genero,
      String tipoDocumento,
      String nroDocumento,
      DireccionData direccion,
      List<MedioContactoData> mediosContacto,
      // Solo para Jurídicas (relaciones externas por ID)
      String razonSocial,
      String tipoJuridico,
      String rubro,
      List<UUID> representantesIds
  ) {}
  ```

#### [NEW] [DireccionData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/DireccionData.java)
- Dirección embebida como Value Object del agregado:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  public record DireccionData(
      String calle,
      Integer altura,
      Integer piso,
      String departamento,
      String codigoPostal,
      LocalidadData localidad
  ) {}
  ```

#### [NEW] [LocalidadData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/LocalidadData.java)
- Localidad embebida:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  public record LocalidadData(String nombreLocalidad, String provincia, String pais) {}
  ```

#### [NEW] [MedioContactoData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/MedioContactoData.java)
- Medio de contacto embebido:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  public record MedioContactoData(String tipo, String valor) {}
  ```

#### [NEW] [DonanteData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/DonanteData.java)
- Contiene los atributos de Donante y referencia por ID a la persona asociada:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  import java.util.UUID;
  public record DonanteData(
      UUID id,
      UUID personaId, // Referencia externa por ID
  ) {}
  ```

#### [NEW] [EntidadBeneficiariaData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/EntidadBeneficiariaData.java)
- Contiene los atributos de Entidad Beneficiaria y referencia por ID a la persona asociada:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import grupo5.common.donaciones.donantes.commands.DireccionData;
  import java.util.UUID;
  public record EntidadBeneficiariaData(
      UUID id,
      UUID personaId, // Referencia externa por ID
  ) {}
  ```

#### [NEW] [NecesidadData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/NecesidadData.java)
- Atributos de Necesidad, referencia externa a Entidad Beneficiaria y períodos embebidos:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.time.LocalDate;
  import java.time.Period;
  import java.util.List;
  import java.util.UUID;
  public record NecesidadData(
      UUID id,
      UUID entidadId, // Referencia externa por ID
      UUID subcategoriaId, // Referencia externa por ID
      Integer cantidadNecesitada,
      String descripcion,
      String tipoNecesidad,
      Period periodo,
      LocalDate fechaInicio,
      List<PeriodoNecesidadData> periodos // Embebido del agregado
  ) {}
  ```

#### [NEW] [PeriodoNecesidadData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/PeriodoNecesidadData.java)
- Período de necesidad embebido, con referencias externas por ID a las donaciones independientes asignadas:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.time.LocalDate;
  import java.util.List;
  import java.util.UUID;
  public record PeriodoNecesidadData(
      LocalDate fechaFin,
      Integer cantidadObjetivo,
      List<UUID> donacionesAsignadasIds // Referencia externa por ID
  ) {}
  ```

#### [NEW] [DonacionData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donaciones/commands/DonacionData.java)
- Atributos de Donación, referencia externa a Donante e ítems embebidos del agregado:
  ```java
  package grupo5.common.donaciones.donaciones.commands;
  import java.time.LocalDateTime;
  import java.util.List;
  import java.util.UUID;
  public record DonacionData(
      UUID id,
      UUID donanteId, // Referencia externa por ID
      String descripcion,
      String estado,
      LocalDateTime fecha,
      List<ItemDonacionData> items // Embebido del agregado
  ) {}
  ```

#### [NEW] [ItemDonacionData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donaciones/commands/ItemDonacionData.java)
- Ítem de donación embebido:
  ```java
  package grupo5.common.donaciones.donaciones.commands;
  public record ItemDonacionData(BienData bien, Integer cantidad) {}
  ```

#### [NEW] [BienData.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donaciones/commands/BienData.java)
- Bien embebido como Value Object:
  ```java
  package grupo5.common.donaciones.donaciones.commands;
  import java.time.LocalDate;
  public record BienData(
      UUID subcategoriaId,
      String descripcion,
      String fotoUrl,
      LocalDate fechaVencimiento,
      String estado,
      Boolean conVencimiento,
      Boolean conUso,
      String unidad
  ) {}
  ```
#### [NEW] [ActualizarDonanteDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donantes/commands/ActualizarDonanteDTO.java)
- DTO para actualizar la persona asociada a un donante:
  ```java
  package grupo5.common.donaciones.donantes.commands;
  import java.util.UUID;
  public record ActualizarDonanteDTO(UUID personaId) {}
  ```

#### [MODIFY] [CrearEntidadBeneficiariaDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/CrearEntidadBeneficiariaDTO.java)
- Recibir únicamente la referencia por ID de la persona jurídica asociada:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.util.UUID;
  public record CrearEntidadBeneficiariaDTO(UUID personaId) {}
  ```

#### [NEW] [ActualizarEntidadBeneficiariaDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/ActualizarEntidadBeneficiariaDTO.java)
- DTO para actualizar la persona asociada a una entidad:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.util.UUID;
  public record ActualizarEntidadBeneficiariaDTO(UUID personaId) {}
  ```

#### [MODIFY] [RegistrarNecesidadDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/RegistrarNecesidadDTO.java)
- Enriquecer con `periodo` y `fechaInicio`:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.time.LocalDate;
  import java.time.Period;
  import java.util.UUID;
  public record RegistrarNecesidadDTO(
      UUID entidadId,
      String subcategoria,
      Integer cantidadNecesitada,
      String tipoNecesidad,
      Period periodo,
      LocalDate fechaInicio) {}
  ```

#### [NEW] [ActualizarNecesidadDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/entidades/commands/ActualizarNecesidadDTO.java)
- DTO para actualizar la necesidad:
  ```java
  package grupo5.common.donaciones.entidades.commands;
  import java.time.LocalDate;
  import java.time.Period;
  import java.util.UUID;
  public record ActualizarNecesidadDTO(
      UUID idNecesidad,
      UUID entidadId,
      String subcategoria,
      Integer cantidadNecesitada,
      String tipoNecesidad,
      Period periodo,
      LocalDate fechaInicio) {}
  ```

#### [NEW] [ActualizarDonacionDTO.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/donaciones/donaciones/commands/ActualizarDonacionDTO.java)
- DTO para modificar únicamente la descripción (inmutabilidad de ítems):
  ```java
  package grupo5.common.donaciones.donaciones.commands;
  public record ActualizarDonacionDTO(String descripcion) {}
  ```

---

### 2. Componente: donaciones-service (Dominio y Persistencia Local)

#### [MODIFY] [Persona.java](file:///c:/Users/Pc/Documents/DDS/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/personas/Persona.java)
- Quitar el campo `id` de la entidad de dominio.

#### [DELETE] Local DTO package (donaciones-service)
- Eliminar por completo el paquete `grupo5.donaciones.dto` local.

---

### 3. Componente: Persistencia (Repositories)

Los repositorios locales de persistencia en memoria implementarán `BaseRepository` y almacenarán las entidades `...Data` indexadas por `UUID`.

- **`PersonasRepository`**: Implementa `BaseRepository<PersonaData, UUID>`.
- **`DonantesRepository`**: Implementa `BaseRepository<DonanteData, UUID>`.
- **`EntidadesBeneficiariasRepository`**: Implementa `BaseRepository<EntidadBeneficiariaData, UUID>`.
- **`NecesidadesRepository`**: Implementa `BaseRepository<NecesidadData, UUID>`.
- **`DonacionesRepository`**: Implementa `BaseRepository<DonacionData, UUID>`.

---

### 4. Componente: Servicios (Services y Mapeos de Dominio)

Los servicios heredarán de `BaseService<...Data, UUID>`. Serán los responsables de:
1.  Recibir los comandos/DTOs de entrada.
2.  Mapear los DTOs de entrada / objetos `...Data` a entidades de dominio puras para ejecutar las validaciones y lógica de negocio.
3.  Si las validaciones en el dominio son exitosas, mapear el resultado a objetos `...Data` (generando nuevos `UUID` para IDs propios y vinculando agregados externos por ID de forma desacoplada) y guardarlos en el repositorio correspondiente.
4.  Lanzar eventos a RabbitMQ.

---

### 5. Componente: Controladores (Controllers)

Heredan de `BaseController` para lectura y exponen endpoints de escritura con DTOs de `common-lib`.

- **`PersonasController`**: Hereda de `BaseController<PersonaData, UUID>`.
- **`DonantesController`**: Hereda de `BaseController<DonanteData, UUID>`.
- **`EntidadesBeneficiariasController`**: Hereda de `BaseController<EntidadBeneficiariaData, UUID>`.
- **`NecesidadesController`**: Hereda de `BaseController<NecesidadData, UUID>`.
- **`DonacionesController`**: Hereda de `BaseController<DonacionData, UUID>`.
