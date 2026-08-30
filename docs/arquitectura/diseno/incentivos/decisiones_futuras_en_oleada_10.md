# Decisiones Técnicas Futuras para la Persistencia Real — `incentivos-service`

> **Documento de Referencia Técnica Complementario a la Oleada 10**  
> **Contexto:** Microservicio de Incentivos — Sistema DonaTrack  
> **Propósito:** Registrar formalmente las decisiones de arquitectura, esquemas relacionales, configuración de JPA/Hibernate 6, Transactional Outbox, Crypto-Shredding y Testcontainers que se implementarán en la fase de persistencia física en PostgreSQL, preservando la pureza y aislamiento del dominio.

---

## 1. Mapeo Objeto-Relacional (ORM) con JPA / Hibernate 6

```mermaid
classDiagram
    class DonanteIncentivosEntity {
        +UUID id
        +UUID personaId
        +String nombre
        +CategoriaDonante categoria
        +MetricasEmbeddable metricas
        +List~MisionEntity~ misiones
        +List~InsigniaGanadaEmbeddable~ insignias
        +Long version
    }

    class MetricasEmbeddable {
        <<embeddable>>
        +int totalDonaciones
        +int donacionesConsecutivas
        +int maxDonacionesConsecutivas
        +int totalDonacionesExitosas
        +LocalDate ultimaDonacionFecha
    }

    class InsigniaGanadaEmbeddable {
        <<embeddable>>
        +String nombre
        +String descripcion
        +String iconoUrl
        +boolean visible
        +LocalDateTime fechaObtencion
    }

    class MisionEntity {
        <<abstract>>
        +UUID id
        +UUID donanteId
        +String tipoMision
        +String nombre
        +String descripcion
        +boolean completada
        +LocalDateTime fechaCompletada
        +InsigniaEmbeddable recompensa
        +Long version
    }

    class MisionCompletitudEntity {
        +Set~String~ categoriasNecesarias
        +Set~String~ categoriasDonadas
    }

    class MisionDonacionesExitosasEntity {
        +int donacionesNecesarias
        +int donacionesActuales
    }

    class MisionHabilDonadorEntity {
        +int bienesRequeridos
        +int bienesActuales
    }

    class MisionRachaEntity {
        +int mesesRequeridos
        +int mesesConsecutivos
        +YearMonth ultimoMesDonado
    }

    DonanteIncentivosEntity *-- MetricasEmbeddable : @Embedded
    DonanteIncentivosEntity *-- InsigniaGanadaEmbeddable : @ElementCollection
    DonanteIncentivosEntity "1" *-- "many" MisionEntity : @OneToMany (Cascade ALL, OrphanRemoval)

    MisionEntity <|-- MisionCompletitudEntity : SINGLE_TABLE
    MisionEntity <|-- MisionDonacionesExitosasEntity : SINGLE_TABLE
    MisionEntity <|-- MisionHabilDonadorEntity : SINGLE_TABLE
    MisionEntity <|-- MisionRachaEntity : SINGLE_TABLE

    class RankingMensualEntity {
        +UUID id
        +YearMonth periodo
        +LocalDateTime fechaCalculo
        +List~EntradaRankingEmbeddable~ posiciones
        +Long version
    }

    class EntradaRankingEmbeddable {
        <<embeddable>>
        +UUID donanteId
        +String nombreDonante
        +int posicion
        +int misionesCompletadas
        +int donacionesTotales
    }

    RankingMensualEntity *-- EntradaRankingEmbeddable : @ElementCollection
```

---

### 1.1. Estrategia de Herencia Polimórfica para `Mision`
* **Estrategia Elegida**: `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` con discriminador `@DiscriminatorColumn(name = "tipo_mision", discriminatorType = DiscriminatorType.STRING)`.
* **Justificación**:
  - Todas las misiones (`MisionCompletitud`, `MisionDonacionesExitosas`, `MisionHabilDonador`, `MisionRacha`) comparten más del 70% de su estructura (`id`, `donante_id`, `nombre`, `descripcion`, `completada`, `fecha_completada`, `recompensa`, `version`).
  - La estrategia de tabla única optimiza las consultas del aggregate root (`DonanteIncentivos`) ejecutando un único `SELECT` directo sin `JOIN`s ni `UNION`s polimórficas costosas en base de datos.
  - Columnas discriminadas:
    - `COMPLETITUD`: Utiliza tablas secundarias de colecciones (`mision_categorias_necesarias`, `mision_categorias_donadas`).
    - `DONACIONES_EXITOSAS`: `donaciones_necesarias`, `donaciones_actuales`.
    - `HABIL_DONADOR`: `bienes_requeridos`, `bienes_actuales`.
    - `RACHA`: `meses_requeridos`, `meses_consecutivos`, `ultimo_mes_donado`.

---

### 1.2. Mapeo de Value Objects y Aplanamiento Escalar de `Metricas`
* **Problema de Rendimiento**: Cargar una colección no acotada `List<EventoDonacion>` en memoria en cada lectura del donante degrada el rendimiento a medida que el historial crece con los años.
* **Solución**:
  - `Metricas` se aplanará como `@Embeddable` con campos escalares directamente en la tabla `donante_incentivos`:
    - `total_donaciones INT`
    - `donaciones_consecutivas INT`
    - `max_donaciones_consecutivas INT`
    - `total_donaciones_exitosas INT`
    - `ultima_donacion_fecha DATE`
  - El detalle de eventos históricos de donación (`EventoDonacion`) se mantendrá desacoplado o como `@ElementCollection` con carga estrictamente diferida (`FetchType.LAZY`) en tabla secundaria `donante_historial_donacion`.

---

### 1.3. Mapeo de Value Objects `Insignia` e `InsigniaGanada`
* **`Insignia`**: Recompensa inmutable (`nombre`, `descripcion`, `iconoUrl`) embebida dentro de `Mision` (`@Embedded`).
* **`InsigniaGanada`**: Value object que representa el logro del donante (`nombre`, `descripcion`, `iconoUrl`, `visible`, `fechaObtencion`). Mapeado como `@ElementCollection` en la tabla `donante_insignia_ganada` con restricción de unicidad `UNIQUE (donante_id, nombre)` garantizando idempotencia.

---

### 1.4. Conversores de Atributos (`AttributeConverter`)
Para tipos de fecha y periodos no soportados nativamente por JDBC:

```java
@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

  @Override
  public String convertToDatabaseColumn(YearMonth attribute) {
    return attribute != null ? attribute.toString() : null; // Ej: "2026-08"
  }

  @Override
  public YearMonth convertToEntityAttribute(String dbData) {
    return dbData != null && !dbData.isBlank() ? YearMonth.parse(dbData) : null;
  }
}
```

---

## 2. Esquema Relacional DDL Optimizado (PostgreSQL 15+)

```sql
-- ============================================================================
-- DONATRACK: SERVICIO DE INCENTIVOS - ESQUEMA RELACIONAL (POSTGRESQL 15+)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. AGREGADO DONANTE INCENTIVOS
-- ----------------------------------------------------------------------------
CREATE TABLE donante_incentivos (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(20) NOT NULL DEFAULT 'COLABORADOR', -- 'COLABORADOR', 'BENEFACTOR', 'PATROCINADOR', 'HEROE'
    
    -- Value Object Embebido: Metricas (Escalares aplanados)
    total_donaciones INT NOT NULL DEFAULT 0 CHECK (total_donaciones >= 0),
    donaciones_consecutivas INT NOT NULL DEFAULT 0 CHECK (donaciones_consecutivas >= 0),
    max_donaciones_consecutivas INT NOT NULL DEFAULT 0 CHECK (max_donaciones_consecutivas >= 0),
    total_donaciones_exitosas INT NOT NULL DEFAULT 0 CHECK (total_donaciones_exitosas >= 0),
    ultima_donacion_fecha DATE NULL,
    
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_donante_inc_persona ON donante_incentivos(persona_id);
CREATE INDEX idx_donante_inc_categoria ON donante_incentivos(categoria);

-- ----------------------------------------------------------------------------
-- 2. JERARQUÍA MISIONES (SINGLE TABLE INHERITANCE)
-- ----------------------------------------------------------------------------
CREATE TABLE mision (
    id UUID PRIMARY KEY,
    donante_id UUID NOT NULL REFERENCES donante_incentivos(id) ON DELETE CASCADE,
    tipo_mision VARCHAR(30) NOT NULL, -- 'COMPLETITUD', 'DONACIONES_EXITOSAS', 'HABIL_DONADOR', 'RACHA'
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    completada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_completada TIMESTAMP WITH TIME ZONE NULL,
    
    -- Recompensa (Insignia Embebida)
    insignia_nombre VARCHAR(100) NOT NULL,
    insignia_descripcion VARCHAR(255) NOT NULL,
    insignia_icono_url VARCHAR(500) NOT NULL,
    
    -- Atributos de MisionDonacionesExitosas
    donaciones_necesarias INT NULL CHECK (donaciones_necesarias > 0),
    donaciones_actuales INT NULL CHECK (donaciones_actuales >= 0),
    
    -- Atributos de MisionHabilDonador
    bienes_requeridos INT NULL CHECK (bienes_requeridos > 0),
    bienes_actuales INT NULL CHECK (bienes_actuales >= 0),
    
    -- Atributos de MisionRacha
    meses_requeridos INT NULL CHECK (meses_requeridos > 0),
    meses_consecutivos INT NULL CHECK (meses_consecutivos >= 0),
    ultimo_mes_donado VARCHAR(7) NULL, -- 'YYYY-MM'
    
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_mision_donante ON mision(donante_id);
CREATE INDEX idx_mision_tipo_completada ON mision(tipo_mision, completada);

-- ----------------------------------------------------------------------------
-- 3. COLECCIONES DE CATEGORÍAS PARA MISION COMPLETITUD
-- ----------------------------------------------------------------------------
CREATE TABLE mision_categorias_necesarias (
    mision_id UUID NOT NULL REFERENCES mision(id) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    PRIMARY KEY (mision_id, categoria)
);

CREATE TABLE mision_categorias_donadas (
    mision_id UUID NOT NULL REFERENCES mision(id) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    PRIMARY KEY (mision_id, categoria)
);

-- ----------------------------------------------------------------------------
-- 4. VALUE OBJECTS: INSIGNIAS GANADAS POR EL DONANTE
-- ----------------------------------------------------------------------------
CREATE TABLE donante_insignia_ganada (
    id UUID PRIMARY KEY,
    donante_id UUID NOT NULL REFERENCES donante_incentivos(id) ON DELETE CASCADE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    icono_url VARCHAR(500) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_obtencion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_donante_insignia_nombre UNIQUE (donante_id, nombre)
);
CREATE INDEX idx_donante_insignias_donante ON donante_insignia_ganada(donante_id);

-- ----------------------------------------------------------------------------
-- 5. HISTORIAL DE DONACIONES DEL DONANTE
-- ----------------------------------------------------------------------------
CREATE TABLE donante_historial_donacion (
    id UUID PRIMARY KEY,
    donante_id UUID NOT NULL REFERENCES donante_incentivos(id) ON DELETE CASCADE,
    cantidad_bienes INT NOT NULL CHECK (cantidad_bienes > 0),
    fecha DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_donante_historial_donante ON donante_historial_donacion(donante_id);

CREATE TABLE donante_historial_donacion_categoria (
    historial_id UUID NOT NULL REFERENCES donante_historial_donacion(id) ON DELETE CASCADE,
    categoria VARCHAR(50) NOT NULL,
    PRIMARY KEY (historial_id, categoria)
);

-- ----------------------------------------------------------------------------
-- 6. AGREGADO RANKING MENSUAL (PROYECCIÓN HISTÓRICA)
-- ----------------------------------------------------------------------------
CREATE TABLE ranking_mensual (
    id UUID PRIMARY KEY,
    periodo VARCHAR(7) NOT NULL UNIQUE, -- 'YYYY-MM'
    fecha_calculo TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ranking_periodo ON ranking_mensual(periodo);

CREATE TABLE ranking_mensual_posicion (
    id UUID PRIMARY KEY,
    ranking_id UUID NOT NULL REFERENCES ranking_mensual(id) ON DELETE CASCADE,
    donante_id UUID NOT NULL,
    nombre_donante VARCHAR(100) NOT NULL,
    posicion INT NOT NULL CHECK (posicion > 0),
    misiones_completadas INT NOT NULL DEFAULT 0 CHECK (misiones_completadas >= 0),
    donaciones_totales INT NOT NULL DEFAULT 0 CHECK (donaciones_totales >= 0),
    CONSTRAINT uq_ranking_posicion UNIQUE (ranking_id, posicion),
    CONSTRAINT uq_ranking_donante UNIQUE (ranking_id, donante_id)
);
CREATE INDEX idx_ranking_pos_ranking ON ranking_mensual_posicion(ranking_id);
CREATE INDEX idx_ranking_pos_donante ON ranking_mensual_posicion(donante_id);

-- ----------------------------------------------------------------------------
-- 7. TABLA TRANSACTIONAL OUTBOX (CONSISTENCIA EVENTUAL)
-- ----------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL, -- 'DonanteIncentivos', 'RankingMensual'
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,     -- 'AscensoDonante', 'MisionCompletada'
    payload JSONB NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- 'PENDIENTE', 'PROCESADO', 'ERROR'
    reintentos INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NULL
);
CREATE INDEX idx_outbox_incentivos_pendientes ON outbox_events(estado, created_at);
```

---

## 3. Patrón Transactional Outbox y Consistencia Eventual

```mermaid
graph TD
    subgraph "Transacción Local ACID (PostgreSQL)"
        T1["Mutación de Agregado<br>(DonanteIncentivos: Ascenso / Misión)"]
        T2["Insert atómico en outbox_events<br>(AscensoDonante / MisionCompletada + traceId)"]
        T1 -.->|"Commit Atómico"| T2
    end

    subgraph "Relay Asíncrono (Background Worker)"
        Worker["OutboxEventRelay<br>(@Scheduled cada 1s)"]
        Worker -->|"SELECT FOR UPDATE SKIP LOCKED<br>WHERE estado='PENDIENTE'"| T2
        Worker -->|"Publish con reintentos"| Broker["RabbitMQ Broker / FeignClient<br>(notificaciones-service)"]
        Worker -->|"UPDATE estado='PROCESADO'"| T2
    end
```

### 3.1. Mitigación del Problema de Doble Escritura (*Dual-Write*)
* Si se llama a `notificacionesClientAdapter.notificarAscenso(...)` dentro de una transacción activa y la base de datos hace rollback por un conflicto de concurrencia, se enviaría una notificación falsa al usuario.
* Mediante el patrón **Transactional Outbox**, la mutación del donante y el evento de dominio se escriben en la misma transacción local de PostgreSQL.
* Un worker desacoplado (`OutboxEventRelay`) despacha los eventos pendientes asegurando semántica de entrega *at-least-once* y propagando el `traceId` correspondiente.

---

## 4. Estrategia de Crypto-Shredding y Privacidad

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador / Solicitante
    participant API as DonanteIncentivosController
    participant Service as GestionDonanteService
    participant Crypto as CryptoKeyService (KMS)
    participant DB as PostgreSQL

    Admin->>API: DELETE /api/incentivos/donantes/{id}
    API->>Service: darDeBaja(donanteId)
    Service->>DB: UPDATE donante_incentivos SET nombre='[DONANTE_ANONIMIZADO]' WHERE id=donanteId;
    Service->>Crypto: destroyKey(donanteId)
    Crypto->>DB: DELETE FROM user_encryption_keys WHERE entity_id=donanteId;
    Service-->>API: 204 No Content
    
    Note over DB: En rankings y estadísticas históricas:<br>Las posiciones y totales no se corrompen.<br>Los datos de identificación personal son irrecuperables.
```

### 4.1. Supresión Efectiva de Datos Personales
* Cuando un usuario ejerce su derecho al olvido (Art. 16 Ley 25.326 / GDPR), se destruye la clave simétrica DEK asociada a su identidad.
* El nombre del donante en `donante_incentivos` y `ranking_mensual_posicion` se anonimiza.
* **Beneficio**: Las métricas consolidadas del sistema (`total_donaciones`, `ranking_mensual`) permanecen consistentes matemáticamente sin alterar el podio ni romper la integridad referencial.

---

## 5. Estrategia de Testing de Persistencia con Testcontainers

Para validar la persistencia relacional real sin depender de bases de datos embebidas (como H2, que no replican el comportamiento de PostgreSQL con JSONB o `SKIP LOCKED`):

```java
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IncentivosPersistenceIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("donatrack_incentivos_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Test
  void deberiaPersistirDonanteConMisionesPolimorficasYSingleTable() {
    // Valida persistencia de DonanteIncentivos con MisionCompletitud, MisionRacha y Insignias
  }

  @Test
  void deberiaVerificarConcurrenciaOptimistaConVersion() {
    // Valida que dos actualizaciones concurrentes sobre DonanteIncentivos disparen OptimisticLockingFailureException
  }
}
```

---

## 6. Estrategia de Cómputo de Ranking Escalable en PostgreSQL (SQL Aggregation vs. Heap Memory)

### 6.1. Problema de Rendimiento y Memoria Heap con `findAll()`
Actualmente, el cálculo en memoria de rankings itera sobre la totalidad de los donantes (`List<DonanteIncentivos> todos`). En producción con 500.000+ donantes registrados:
- Cargar todos los agregados completos a la memoria heap genera pausas masivas de Garbage Collection (Stop-The-World) y riesgo inminente de `OutOfMemoryError`.
- La complejidad espacial en JVM es $O(N)$.

### 6.2. Solución: Cómputo Nativo en Base de Datos con Funciones de Ventana
El cálculo mensual se delega al motor relacional PostgreSQL mediante una única consulta SQL de agregación e inserción directa:

```sql
INSERT INTO ranking_mensual_posicion (
    id, ranking_id, donante_id, nombre_donante, posicion, misiones_completadas, donaciones_totales
)
SELECT 
    gen_random_uuid(),
    :rankingId,
    d.id,
    d.nombre,
    ROW_NUMBER() OVER (
        ORDER BY 
            (SELECT COUNT(*) FROM mision m WHERE m.donante_id = d.id AND m.completada = TRUE) DESC,
            (SELECT COUNT(*) FROM donante_historial_donacion h WHERE h.donante_id = d.id AND TO_CHAR(h.fecha, 'YYYY-MM') = :periodo) DESC,
            d.created_at ASC
    ) as posicion,
    (SELECT COUNT(*) FROM mision m WHERE m.donante_id = d.id AND m.completada = TRUE),
    (SELECT COUNT(*) FROM donante_historial_donacion h WHERE h.donante_id = d.id AND TO_CHAR(h.fecha, 'YYYY-MM') = :periodo)
FROM donante_incentivos d;
```

* **Complejidad en Heap JVM**: $O(1)$ (cero entidades cargadas en memoria).
* **Aprovechamiento de Índices**: Utiliza los índices `idx_mision_tipo_completada` e `idx_donante_historial_donante`.

---

## 7. Índices Parciales y Optimización de Almacenamiento en PostgreSQL 15+

### 7.1. Índice Parcial para `outbox_events`
Para evitar indexar millones de filas históricas en estado `PROCESADO`, se define un índice parcial:

```sql
CREATE INDEX idx_outbox_pendientes_parcial 
ON outbox_events(created_at) 
WHERE estado = 'PENDIENTE';
```

* **Beneficio**: El índice mantiene un tamaño mínimo ($< 1$ MB) conteniendo solo los eventos pendientes ($< 100$), garantizando que el polling del relay (`SELECT ... FOR UPDATE SKIP LOCKED`) responda en $< 1$ ms independientemente de los millones de eventos históricos acumulados.

---

## 8. Concurrencia Optimista (`@Version`) y Políticas de Reintento (`@Retryable`)

### 8.1. Manejo de Colisiones Concurrentes
Cuando múltiples eventos de donación simultáneos (ej: ráfagas desde RabbitMQ) impactan sobre el mismo donante, Hibernate detecta el conflicto de versión mediante el campo `@Version version` y lanza `OptimisticLockException` / `ObjectOptimisticLockingFailureException`.

### 8.2. Integración de Spring Retry
Para evitar la pérdida de eventos y no forzar reintentos manuales al cliente, la capa de aplicación se blindará con Spring Retry y backoff exponencial:

```java
@Service
public class GestionDonanteService implements IGestionDonanteService {

  @Retryable(
      retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 100, multiplier = 2))
  @Transactional
  public void registrarDonacion(UUID donanteId, EventoDonacion evento) {
    DonanteIncentivosEntity donante = donanteRepository.findById(donanteId)
        .orElseThrow(() -> new NotFoundException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO));
    donante.registrarDonacion(evento);
    donanteRepository.save(donante);
  }
}
```

---

## 9. Carga Diferida (`FetchType.LAZY`) y Prevención de Producto Cartesiano

### 9.1. Regla Estricta: `FetchType.LAZY` en Colecciones
- Todas las relaciones `@OneToMany` (`List<MisionEntity>`) y `@ElementCollection` (`List<InsigniaGanadaEmbeddable>`) se configuran obligatoriamente como `FetchType.LAZY`.
- Esto previene la excepción `MultipleBagFetchException` de Hibernate y evita productos cartesianos de $M \times N$ filas al cargar donantes.

### 9.2. Grafo de Entidades para Lecturas Específicas
Para casos de uso donde se requiere el aggregate root completo en una única consulta:

```java
@Repository
public interface DonanteIncentivosJpaRepository extends JpaRepository<DonanteIncentivosEntity, UUID> {

  @EntityGraph(attributePaths = {"misiones", "insignias"})
  Optional<DonanteIncentivosEntity> findWithDetailsById(UUID id);
}
```

---

## 10. Ciclo de Vida de Outbox Relay, Dead Letter Queue (DLQ) y Purga Automática

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: Mutación en Aggregate (Commit Atómico)
    PENDIENTE --> PROCESADO: Worker despacha evento con éxito
    PENDIENTE --> ERROR: Fallo en broker / red
    ERROR --> PENDIENTE: Reintento con backoff (intentos < 5)
    ERROR --> DEAD_LETTER: Superado límite de reintentos (>= 5)
    DEAD_LETTER --> [*]: Alerta en Observabilidad y Cuarentena
    PROCESADO --> [*]: Purga automática tras 14 días
```

### 10.1. Job de Purga Programada (`OutboxCleanupJob`)
Para mantener acotado el tamaño de la tabla `outbox_events` y optimizar los backups de base de datos:

```sql
DELETE FROM outbox_events 
WHERE estado = 'PROCESADO' 
  AND processed_at < CURRENT_TIMESTAMP - INTERVAL '14 days';
```

---

## 11. Idempotencia en Consumo y Observabilidad Micrometer

### 11.1. Deduplicación de Mensajes
Para garantizar que la ingesta asíncrona (*At-Least-Once*) sea estrictamente idempotente:
- Restricción de clave natural única: `UNIQUE (donacion_id)` en la tabla `donante_historial_donacion`.
- Opcionalmente, tabla de control `consumed_messages (message_id UUID PRIMARY KEY, consumer_name VARCHAR(100), consumed_at TIMESTAMP WITH TIME ZONE)`.

### 11.2. Métricas de Negocio con Micrometer
Instrumentación de métricas para dashboards en Grafana / Prometheus:
- `Counter.builder("incentivos.misiones.completadas").tag("categoria", ...).tag("tipo", ...).register(meterRegistry)`
- `Counter.builder("incentivos.donantes.ascensos").tag("categoria_nueva", ...).register(meterRegistry)`
- `Timer.builder("incentivos.ranking.calculo.duration").register(meterRegistry)`

### 11.3. Contrato de Ingesta Idempotente con `donacionId`
* **Problema en Sistemas Distribuidos**: En redes con reintentos automáticos (Feign / RabbitMQ), peticiones repetidas a `/donaciones` y `/donaciones/exitosa` sin identificador de transacción provocarían cómputo doble de métricas y medallas duplicadas.
* **Diseño para la Fase Física**:
  - Actualizar `NuevaDonacionRequest` y `DonacionExitosaRequest` para requerir `@NotNull UUID donacionId`.
  - Actualizar sincronizadamente `IncentivosFeignClient` en `donaciones-service`.
  - Persistencia física: la tabla `donante_historial_donacion` incorporará `CONSTRAINT uq_donante_donacion UNIQUE (donacion_id)`. Ante inserción duplicada, PostgreSQL arroja `UniqueConstraintViolationException` que el servicio capturará para responder `200 OK` de forma idempotente sin re-procesar.

---

## 12. Coordinación de Tareas Programadas en Clúster (ShedLock)

```mermaid
graph TD
    subgraph "Instancias de incentivos-service (K8s Pods / Docker)"
        Pod1["Pod 1<br>(InactividadJob / RankingMensualJob)"]
        Pod2["Pod 2<br>(InactividadJob / RankingMensualJob)"]
    end

    subgraph "PostgreSQL 15+"
        LockTable[("Tabla shedlock<br>(lock_until, locked_at, locked_by)")]
    end

    Pod1 -->|"Adquiere Lock (Éxito)"| LockTable
    Pod2 -.->|"Lock Ocupado (Skip)"| LockTable
```

### 12.1. Mitigación de Ejecución Duplicada en Schedulers
* **Problema**: Cuando `incentivos-service` se despliega con múltiples réplicas (alta disponibilidad), los cron jobs nativos `@Scheduled` (`InactividadJob`, `RachaJob`, `RankingMensualJob`) se disparan concurrentemente en todos los pods a la misma hora, duplicando cálculos de rankings y notificaciones.
* **Solución**: Integrar **ShedLock** con backend relacional PostgreSQL:

```sql
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

```java
@Component
public class RankingMensualJob {

  @Scheduled(cron = "0 59 23 L * *")
  @SchedulerLock(name = "RankingMensualJob_ejecutar", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
  public void ejecutarRankingMensual() { ... }
}
```

---

## 13. Integración de Webhooks n8n Mediante Transactional Outbox

### 13.1. Reemplazo de `WebClient.subscribe()` Fire-and-Forget
* **Problema**: `N8nClientAdapter` despacha llamadas HTTP reactivas con `.subscribe()` sin reintentos exponenciales ni almacenamiento de contingencia. Si n8n sufre una caída transitoria o sobrecarga, las notificaciones de insignias y rankings se pierden silenciosamente.
* **Solución**:
  - En la fase JPA, las publicaciones a n8n se enrutan a través de la tabla `outbox_events` con `event_type = 'PUBLICAR_INSIGNIA_N8N'` y `'NOTIFICAR_RANKING_N8N'`.
  - El `OutboxEventRelay` asíncrono se encarga de realizar las llamadas HTTP hacia los webhooks de n8n, aplicando reintentos exponenciales con `backoff`, circuit breaker y cuarentena en DLQ tras 5 fallos.

---

## 14. Proyecciones SQL Nativas para Evolución de Métricas Históricas

### 14.1. Cómputo de `donacionesPorPeriodo()` en Base de Datos
* **Problema**: Al aplanar `Metricas` a columnas escalares en `donante_incentivos` para evitar cargar colecciones históricas en memoria, `MetricasIncentivosService.obtenerMetricas()` ya no debe iterar sobre `historialDonaciones`.
* **Solución**: Definir una proyección JPA nativa:

```java
@Repository
public interface DonanteHistorialDonacionJpaRepository extends JpaRepository<DonanteHistorialEntity, UUID> {

  @Query(value = """
      SELECT TO_CHAR(h.fecha, 'YYYY-MM') AS periodo, COUNT(*) AS cantidad
      FROM donante_historial_donacion h
      WHERE h.donante_id = :donanteId
      GROUP BY TO_CHAR(h.fecha, 'YYYY-MM')
      ORDER BY periodo ASC
      """, nativeQuery = true)
  List<PeriodoConteoProjection> obtenerEvolucionDonacionesPorPeriodo(@Param("donanteId") UUID donanteId);
}
```

---

## 15. Seguridad Perimetral y Control de Acceso a Nivel de Recurso (JWT)

### 15.1. Validación de Identidad en Endpoints de Perfil e Insignias
* **Regla de Seguridad**: Endpoints como `PUT /donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad` y `PATCH /donantes/{donanteId}` validarán en el `SecurityFilterChain` (OAuth2 Resource Server / JWT) que:
  1. El claim `sub` / `personaId` del token JWT coincida con el `idPersona` del donante correspondiente al `donanteId` solicitado.
  2. O bien el token contenga el rol administrativo `ROLE_ADMIN`.
* **Manejo de Errores**: Intentos de mutación sobre recursos ajenos retornarán `403 Forbidden` (`ErrorCatalog.ACCESO_DENEGADO`).

---

## 16. Modelo de Concurrencia y Progresión de Misiones

### 16.1. Evaluación Paralela vs. Serial dentro de una Misma Categoría
* **Decisión de Gamificación**:
  - Actualmente, `DonanteIncentivos.getMisionActiva()` evalúa una sola misión por categoría en orden secuencial (`min(numeroMision)`).
  - Para la fase física, se define la opción de **evaluación en paralelo** (`donante.getMisionesActivas()`), permitiendo que eventos de donación simultáneos impacten sobre la misión de racha y la de completitud a la vez sin pérdida de progreso intermedio.


