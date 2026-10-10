# Plan de implementación del refactor — DonaTrack / Servicio de Donaciones

## Objetivo

Organizar el refactor del Servicio de Donaciones entre 3 integrantes del equipo, todos con acceso a agentes de IA, de forma que:

- el trabajo avance en paralelo cuando sea seguro;
- cada cambio tenga revisión independiente;
- todos los integrantes entiendan las decisiones arquitectónicas;
- se eviten ramas enormes y refactors incompatibles;
- el código evolucione incrementalmente hacia el DC actualizado;
- los agentes ayuden a implementar y revisar, pero no reemplacen la validación humana.

La estrategia recomendada es un esquema híbrido de:

**ownership rotativo + reviewer independiente + integrador + paralelismo solo entre slices desacoplados.**

---

# 1. Principio general de trabajo

No se recomienda:

- que los 3 integrantes implementen todo el refactor completo en paralelo;
- ni una cascada rígida donde una persona implementa todo un RF y las demás esperan.

La estrategia propuesta busca aprovechar el paralelismo sin generar conflictos de código.

Cada RF tendrá tres roles:

| Rol | Responsabilidad |
|---|---|
| **Owner / Generator** | Estudia el problema, prepara o confirma tests e implementa el RF |
| **Reviewer / Evaluator** | Revisa el diff contra el DC, el spec y los tests. No debe haber participado de la implementación principal |
| **Integrador / Arquitecto** | Verifica impacto global, ejecuta suite completa y controla coherencia con RF anteriores |

Los roles deben rotar.

Ejemplo:

```text
RF-01
Nico → Owner
Compañero A → Reviewer
Compañero B → Integrador

RF-02
Compañero A → Owner
Compañero B → Reviewer
Nico → Integrador

RF-03
Compañero B → Owner
Nico → Reviewer
Compañero A → Integrador
```

Así los 3 terminan practicando implementación, revisión y arquitectura.

---

# 2. Paso previo obligatorio — Baseline verde

Antes de empezar los RF hay que restaurar una baseline confiable.

Actualmente se confirmó una inconsistencia en la aprobación de `Propuesta`:

```text
Propuesta.aceptar(String actor)

AsignacionService:
propuesta.aceptar()

AsignacionServiceTest:
propuesta.confirmar()
```

Esto debe resolverse como un fix previo:

## FIX-00 — Restaurar baseline compilable

Objetivo:

- resolver la inconsistencia `aceptar()/confirmar()`;
- ejecutar el build;
- ejecutar la suite completa;
- dejar la branch base verde.

Regla:

> Ningún RF debe comenzar sobre una suite roja.

Esto permite distinguir errores preexistentes de regresiones introducidas por el refactor.

---

# 3. Estrategia de branches

Crear una branch madre del refactor:

```text
E4_refactor-donaciones
```

Cada RF debe nacer desde la última versión actualizada de esa branch.

Ejemplos:

```text
E4_RF01_propuesta
E4_RF02_archivo
E4_RF03_persona
E4_RF04_domain-events-donacion
...
```

Flujo:

```text
E4_refactor-donaciones
        ↓
crear branch RF
        ↓
spec
        ↓
tests
        ↓
implementación
        ↓
tests
        ↓
review
        ↓
merge
        ↓
actualizar E4_refactor-donaciones
```

No comenzar un RF nuevo sobre una branch vieja que todavía no incorporó los cambios anteriores.

---

# 4. Cómo usar los agentes de IA

No usar el mismo agente como implementador y único reviewer del cambio.

## Agente del Owner

Primero debe actuar como tutor/analista:

```text
Explicame:
- qué responsabilidad está mal ubicada;
- qué indica el DC;
- qué comportamiento debemos preservar;
- qué tests deberían protegerlo;
- qué riesgos tiene este cambio.

No modifiques código todavía.
```

Una vez que el humano entiende y valida el diseño:

```text
Implementá exclusivamente este RF.
No modifiques comportamiento fuera del scope.
```

## Agente del Reviewer

Debe trabajar en otro contexto/chat:

```text
No modifiques código.

Corré el git diff real de esta oleada (no leas solo la bitácora) contra:
- DC actualizado;
- spec del RF;
- tests existentes;
- arquitectura objetivo.

Para cada ítem que la bitácora marque ✅ o "refactorizado", verificá contra el diff real que el
archivo:línea citado existe y prueba lo que se afirma. Si no lo prueba, es un hallazgo a reportar,
no un detalle menor.

Buscá:
- regresiones;
- reglas que sigan mal ubicadas;
- cambios fuera de scope;
- sobreingeniería;
- falta de tests;
- acoplamientos nuevos;
- desalineación entre la bitácora y el diff real (ver sección 16, "Trazabilidad de la bitácora").
```

La revisión humana sigue siendo obligatoria.

---

# 5. Oleada 1 — Tres RF pequeños en paralelo

Después de `FIX-00`, conviene empezar con tres áreas relativamente desacopladas.

## Persona 1 — Archivo

### RF — `Archivo.finalizarProcesamiento`

Mover la decisión:

```text
errores == 0
→ PROCESADO

errores > 0
→ PROCESADO_CON_ERRORES
```

desde `ImportadorService` hacia `Archivo`.

Concepto principal a aprender:

**Tell, Don't Ask / comportamiento en la entidad.**

---

## Persona 2 — Persona duplicada

### RF — `Persona.esDuplicadaDe`

Analizar y alinear:

```text
Persona
Humana
Juridica
```

con el DC.

Actualmente existen:

```text
CriterioDuplicado
CriterioPorDocumento
CriterioPorMedioDeContacto
ValidadorPersonaDuplicada
```

Antes de eliminar Strategies hay que decidir si siguen aportando valor.

Conceptos principales:

- modelo de dominio rico;
- Strategy;
- responsabilidad de entidad vs servicio.

---

## Persona 3 — Propuesta

### RF — Propuesta / Domain Events baseline

Consolidar el comportamiento de:

```text
aceptar(actor)
rechazar()
PropuestaAprobada
domainEvents
```

El objetivo es que `Propuesta` quede como el ejemplo de referencia del patrón:

```text
Aggregate
    ↓
genera Domain Event
    ↓
Application Service
    ↓
publica
    ↓
limpia eventos
```

Conceptos principales:

- Aggregate Root;
- Domain Events;
- separación dominio/aplicación.

---

# 6. Revisión cruzada de la Oleada 1

Ejemplo:

```text
Nico implementa Archivo
→ Compañero A revisa

Compañero A implementa Persona
→ Compañero B revisa

Compañero B implementa Propuesta
→ Nico revisa
```

Después del review:

1. ejecutar tests del RF;
2. ejecutar suite completa;
3. mergear a `E4_refactor-donaciones`;
4. actualizar las ramas siguientes desde esa base.

---

# 7. Reunión de sincronización

Después de la primera oleada, hacer una reunión corta.

Cada Owner debe explicar:

```text
ANTES
¿Dónde estaba la responsabilidad?

PROBLEMA
¿Por qué estaba mal según el DC?

DESPUÉS
¿Dónde quedó?

TESTS
¿Cómo sabemos que el comportamiento sigue funcionando?
```

Objetivo:

que ningún miembro llegue a la entrega diciendo:

> “esa parte la hizo otro con IA y no sé cómo funciona”.

Los 3 deben poder explicar las decisiones importantes.

---

# 8. Oleada 2 — Donacion y normalización

Estas áreas están conectadas, por lo que no conviene modificarlas simultáneamente desde ramas distintas.

## RF — Domain Events de `Donacion`

Objetivo:

```text
Donacion
→ registra eventos internamente
```

Ejemplos conceptuales:

```text
crear Donacion
→ EventoDonacion(CARGADA)

marcarNormalizada()
→ EventoDonacion(NORMALIZADA)

marcarSegmentada()
→ EventoDonacion(SEGMENTADA)
```

El Application Service:

```text
ejecuta comportamiento
→ guarda
→ obtiene eventos
→ publica
→ limpia
```

---

## RF — Normalización

Después de integrar Domain Events de `Donacion`.

Revisar:

```text
ProcesadorDeDonaciones
ItemDonacionNormalizadoService
BienNormalizado
Donacion
```

Problema principal detectado:

la regla que decide si una donación terminó de normalizarse está duplicada.

Objetivo:

- eliminar duplicación;
- determinar correctamente quién decide que la normalización terminó;
- evitar meter en `Donacion` dependencias que no pertenecen naturalmente al Aggregate;
- revisar `BienNormalizado.cambiarEstado(...)`.

Este RF requiere una decisión de diseño previa a implementar.

---

# 9. Oleada 3 — DonacionIndependiente

La máquina State existente está bastante alineada con el DC.

No reconstruir innecesariamente:

```text
EnDeposito
AsignacionRealizada
ListaParaEntregar
EnTraslado
EntregaFallida
Entregada
Vencida
```

Trabajar alrededor del State existente.

## RF — Domain Events de `DonacionIndependiente`

Agregar conceptualmente:

```text
EventoDonacionIndependiente
    ├── EventoDonacionAsignada
    ├── EventoRutaIniciada
    ├── EventoDonacionRecibida
    └── EventoDonacionFallida
```

Separar explícitamente:

```text
Domain Event
    ↓
hecho del dominio

Integration DTO
    ↓
payload enviado a otro microservicio
```

Flujo esperado:

```text
DonacionIndependiente
        ↓
Domain Event
        ↓
Application Service / Handler
        ↓
Mapper
        ↓
Integration DTO
        ↓
Feign / RabbitMQ / mecanismo externo
```

---

## RF — `SolicitudCambioEstadoDonacionIndependiente`

Actualmente el Application Service recibe un DTO y contiene un `switch` para desarmarlo y elegir la transición.

El DC propone un objeto de dominio:

```text
SolicitudCambioEstadoDonacionIndependiente
```

Objetivo:

```text
Controller
    ↓
DTO
    ↓
Application / Mapper
    ↓
SolicitudCambioEstadoDonacionIndependiente
    ↓
DonacionIndependiente
```

Esto debería adelgazar `DonacionesIndependientesService`.

---

# 10. Oleada 4 — Asignación y propuestas

Es una de las zonas de mayor riesgo.

`AsignacionService` tiene mucho fan-in y no debe ser modificado en paralelo por varias personas.

Dividirlo en RF pequeños.

## RF — `PosibleFragmentacion.confirmar`

Mover la decisión:

```text
¿hay que fragmentar?
```

desde `PropuestaDeAsignacionService` hacia:

```text
PosibleFragmentacion.confirmar(...)
```

tal como indica el DC.

---

## RF — `GestorPropuestasDeAsignacion`

Revisar responsabilidades actuales repartidas entre:

```text
AsignacionService
PropuestaDeAsignacionService
```

Objetivo conceptual:

```text
GestorPropuestasDeAsignacion
        ↓
Algoritmos
        ↓
StockDeDonaciones
        ↓
Propuestas
```

---

## RF — Consolidación y adelgazamiento de `AsignacionService`

Mover la regla pura:

```text
consolidar(...)
```

fuera del Application Service.

Resultado esperado:

```text
Application Service
→ recupera datos
→ ejecuta gestor
→ persiste
→ publica/comunica
```

sin decidir cómo se consolidan las propuestas.

---

# 11. Oleada 5 — Necesidades

Resolver la duplicación entre:

```text
PlanificadorDeNecesidades
PlanificacionNecesidadesService
```

Objetivo:

```text
Scheduler
    ↓
Application Service
    ↓
GestorNecesidades
```

El Scheduler solo dispara.

Las decisiones deben quedar fuera del mecanismo de activación.

---

# 12. Oleada 6 — Reorganización de paquetes

No mover paquetes al comienzo del refactor.

Primero corregir responsabilidades.

Después evaluar:

```text
infrastructure/algoritmos
infrastructure/segmentadores
infrastructure/analizadores
```

Si realmente son dominio, moverlos recién cuando su comportamiento esté estabilizado.

Motivo:

los movimientos de paquetes producen diffs grandes y ruido sin agregar valor funcional inmediato.

---

# 13. Oleada 7 — Limpieza legacy

Recién al final revisar:

```text
IArchivoDonantesService
IValidadorPersonaDuplicada
métodos muertos
interfaces sin implementación
routes/.gitkeep
imports no usados
naming ambiguo
```

También decidir explícitamente el destino de:

```text
IAsignacionesRepository
EjecucionAsignacionDTO
```

No eliminarlos automáticamente solo porque no aparezcan en el DC.

Primero verificar si corresponden a un requisito válido que el diagrama no representa.

---

# 14. Trabajo en pipeline

No es necesario que dos personas estén esperando mientras una implementa.

Ejemplo:

```text
                 RF actual        RF siguiente
──────────────────────────────────────────────
Persona A        implementa       -
Persona B        review           analiza
Persona C        integra          prepara tests/spec
```

Después:

```text
                 RF siguiente     RF + 2
──────────────────────────────────────────────
Persona B        implementa
Persona C        review           analiza
Persona A        integra          prepara tests/spec
```

Esto permite paralelismo sin escribir simultáneamente sobre las mismas clases.

---

# 15. Cuándo sí usar trabajo paralelo de los 3

No se recomienda que los 3 implementen el mismo RF completo.

Sí puede ser útil que los 3 diseñen alternativas cuando una decisión sea discutible.

Ejemplo:

```text
Persona.esDuplicadaDe()
```

Podrían pedir:

```text
Agente A → diseño modelo rico
Agente B → diseño Strategy
Agente C → diseño híbrido
```

Luego comparar las propuestas antes de escribir código.

Regla:

> Usar parallel design, no parallel implementation.

---

# 16. Template de cada PR

Cada PR debería incluir:

## Problema

¿Qué responsabilidad estaba mal ubicada o qué divergencia existía?

## Evidencia

¿Qué muestra el DC y qué hacía el código?

## Objetivo

¿Qué cambia con este RF?

## Fuera de scope

¿Qué decidimos explícitamente no tocar?

## Tests

¿Qué comportamiento protege la suite?

## Diseño resultante

¿Por qué la responsabilidad queda en esa clase/capa?

## IA utilizada

¿Para qué se utilizó el agente?

Ejemplos:

- análisis;
- generación de tests;
- implementación;
- review;
- explicación.

## Verificación humana

¿Qué revisó el integrante antes de aprobar el PR?

## Trazabilidad de la bitácora (Etapa 5, agregado 2026-08-26)

Causa raíz detectada en el análisis cruzado de Oleadas 7-10: en 3 de 4 oleadas la bitácora declaraba
trabajo como hecho ("refactorizado", ✅) sin que el diff real lo respaldara (cita de líneas del DC que
no contienen lo afirmado, suites declaradas "refactorizadas" sin ningún diff, checkmarks de diseño
conceptual marcados igual que trabajo terminado). Corregir cada caso puntual no alcanza — hace falta
una regla de proceso:

1. **Todo ítem marcado ✅ o "refactorizado" en la bitácora tiene que citar el archivo:línea real que
   lo prueba.** Si no se puede citar una línea concreta, no se marca ✅.
2. **Convención de símbolos** — no todos los ítems de una bitácora significan lo mismo:
   - `✅` = implementado, con diff real y tests verdes que lo prueban.
   - `📝` (o `🔵`) = diseño/análisis conceptual, documentado pero **sin** cambio de código todavía
     (por ejemplo, un documento de decisiones futuras). Nunca usar `✅` para esto — se confunde con
     "terminado" y una oleada futura puede asumir erróneamente que ya está resuelto.
3. **El Reviewer tiene que correr el `git diff` real de la oleada contra lo que declara la bitácora
   antes de aprobar** — no alcanza con leer el texto. Ver `## Agente del Reviewer` en la sección 4:
   el prompt de revisión ya lo exige explícitamente.

---

# 17. Regla de tamaño de PR

Cada RF debe ser explicable y defendible en aproximadamente 10 minutos.

Si el cambio necesita demasiado tiempo solo para enumerar qué archivos fueron modificados, probablemente el RF sea demasiado grande.

Preferir:

```text
RF pequeño
→ entender
→ tests
→ implementar
→ review
→ merge
```

antes que:

```text
RF enorme
→ 40 archivos
→ difícil de revisar
→ difícil de explicar
```

---

# 18. Plan inicial de reparto

Después de `FIX-00`:

| Integrante | Primer RF | Reviewer |
|---|---|---|
| Nico | `Archivo.finalizarProcesamiento` | Compañero A |
| Compañero A | `Persona.esDuplicadaDe` | Compañero B |
| Compañero B | `Propuesta / Domain Events baseline` | Nico |

El Integrador del RF puede ser el tercer integrante restante.

Después los roles rotan.

---

# 19. Roadmap completo

```text
FASE 0
Auditoría ✅
        │
        ▼
FIX-00
Baseline compilable y tests verdes
        │
        ▼
OLEADA 1 — paralelo seguro
├── Archivo
├── Persona
└── Propuesta
        │
        ▼
Sincronización del equipo
        │
        ▼
OLEADA 2
├── Domain Events Donacion
└── Normalización
        │
        ▼
OLEADA 3
├── Domain Events DonacionIndependiente
└── SolicitudCambioEstado
        │
        ▼
OLEADA 4
├── PosibleFragmentacion
├── GestorPropuestas
└── Consolidación / AsignacionService
        │
        ▼
OLEADA 5
└── GestorNecesidades
        │
        ▼
OLEADA 6
└── Reorganización de paquetes
        │
        ▼
OLEADA 7
└── Limpieza legacy y persistencia pura
        │
        ▼
OLEADA 8
└── Refactor profundo de testing (Object Mothers / Tell Don't Ask)
        │
        ▼
OLEADA 9
└── Validación por capas, HTTP clásico y TraceID distribuido
        │
        ▼
OLEADA 10
├── Desacoplamiento de Agregados (Eliminación Asignable / UUIDs)
├── Strategy de Almacenamiento (FileSystem vs MinIO S3)
├── Concurrencia (CAS en memoria vs Optimistic Locking)
├── Privacidad sin polución (Crypto-Shredding)
└── Documento complementario (decisiones_futuras_en_oleada_10.md)
        │
        ▼
OLEADA 11–14
├── Hardening de seguridad, reentrancia y desanotación de Domain Services
├── Trazabilidad estricta de bitácora
└── Erradicación completa de constructores manuales en tests
        │
        ▼
OLEADA 15
├── Resolución de 25 issues de SonarCloud (Blocker NPE & Code Smells)
├── Centralización de Domain Events (AgregadoConEventos en common-lib)
├── DonanteParser en Dominio & Desacoplamiento de LectorDonantesCSV
├── Polimorfismo en Persona (getNombreCompleto / Tell, Don't Ask)
└── Modularización de NormalizadorSemanticoBien (Complejidad Cognitiva <= 15)
        │
        ▼
Integración completa
        │
        ▼
Auditoría final DC ↔ código
```

---

# 20. Auditoría final

Al terminar el refactor, repetir la Fase 0:

```text
DC actualizado
        VS
Código final
```

Validar al menos:

```text
Donacion Domain Events (AgregadoConEventos)  ✅
DonacionIndependiente Domain Events         ✅
Propuesta Domain Events (AgregadoConEventos)✅
DonanteParser en Dominio                    ✅
Archivo.finalizarProcesamiento              ✅
Persona.esDuplicadaDe                       ✅
Persona.getNombreCompleto (Polimórfico)     ✅
PosibleFragmentacion.confirmar              ✅
GestorPropuestas                            ✅
GestorNecesidades                           ✅
Application Services delgados               ✅
Listeners delgados                          ✅
Schedulers delgados                         ✅
Desacoplamiento Asignable -> UUIDs          📝 (diseñado en Oleada 10, no implementado)
Strategy FileSystem / MinIO                 📝 (diseñado en Oleada 10, no implementado)
Crypto-shredding modelado                   📝 (diseñado en Oleada 10, no implementado)
Tests verdes (386 tests donaciones-service) ✅
```

> Corrección de Etapa 5 (2026-08-26): estos 3 ítems estaban marcados ✅ pese a ser solo diseño
> conceptual documentado en `decisiones_futuras_en_oleada_10.md`, sin ningún cambio de código —
> señalado tanto en la revisión de Oleada 10 como por un segundo revisor (ndelorte) de forma
> independiente. Ver la convención de símbolos en la sección 16.

La auditoría inicial y la auditoría final deberían permitir explicar claramente:

```text
ANTES
        ↓
problemas detectados
        ↓
refactors incrementales
        ↓
DESPUÉS
```

---

# 21. Ciclo obligatorio de aprendizaje por RF

Para cada RF:

```text
1. Entender el problema
2. Leer el código actual
3. Leer la parte correspondiente del DC
4. Explicar qué responsabilidad está mal
5. Confirmar o escribir characterization tests
6. Proponer diseño
7. Validación humana
8. Implementación con o sin IA
9. Ejecutar tests
10. Review independiente
11. Explicar al equipo qué cambió y por qué
12. Commit / PR / merge
```

Regla final:

> Un RF no está terminado solamente porque los tests pasan.

También debe cumplirse:

```text
✅ tests verdes
✅ alineación con el DC
✅ responsabilidad bien ubicada
✅ cambio acotado
✅ review independiente
✅ el Owner puede explicarlo
✅ los otros dos entienden el resultado
```
