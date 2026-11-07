# Módulo de Pruebas de Integración (`integration-tests`)

Este documento detalla el diseño, funcionamiento y arquitectura del módulo `integration-tests`, el cual valida el comportamiento cross-service y de extremo a extremo (E2E) de la plataforma DonaTrack de forma desacoplada, tipada y determinística.

---

## 1. Propósito y Alcance

El módulo `integration-tests` opera como un **cliente de caja negra HTTP** (Black-Box Testing) que interactúa con las APIs REST de los 4 microservicios de la plataforma:
* **`donaciones-service` (`:8080`)**: Administración de donantes, personas, entidades beneficiarias, donaciones, necesidades, normalización y matching.
* **`notificaciones-service` (`:8081`)**: Recepción/emisión de notificaciones y réplica del ciclo de vida de personas.
* **`incentivos-service` (`:8082`)**: Cálculo de ranking mensual, misiones, categorías e insignias de los colaboradores.
* **`logistica-service` (`:8083`)**: Gestión de camiones, choferes, rutas y entregas, con emisión de eventos asíncronos a RabbitMQ.

Las pruebas se ejecutan sobre el entorno de preproducción (`docker-compose.preprod.yml`), evaluando el ecosistema distribuido completo (incluyendo **RabbitMQ** y **n8n**).

---

## 2. Arquitectura y Estructura del Módulo

```
integration-tests/
├── pom.xml                                      # Configuración Maven (Spotless activo, Surefire + Failsafe)
└── src/test/java/grupo5/tests/
    ├── BaseIT.java                              # URLs base, RestAssured y clientes instanciados
    │
    ├── client/                                  # Clientes de API desacoplados (Driver Pattern)
    │   ├── DonacionesApiClient.java             # Métodos *Ok fuertemente tipados + Response crudo
    │   ├── NotificacionesApiClient.java
    │   ├── IncentivosApiClient.java
    │   └── LogisticaApiClient.java
    │
    ├── dto/                                     # Records inmutables locales de prueba
    │   ├── DireccionTestDTO.java
    │   ├── DonacionTestDTO.java
    │   ├── ErrorResponseTestDTO.java
    │   ├── ItemDonacionTestDTO.java
    │   ├── MedioContactoTestDTO.java
    │   ├── NecesidadTestDTO.java
    │   └── PersonaTestDTO.java
    │
    ├── builders/                                # Test Data Builders fluidos
    │   ├── DonacionTestDataBuilder.java
    │   ├── NecesidadTestDataBuilder.java
    │   ├── PersonaTestDataBuilder.java
    │   └── LogisticaTestData.java               # Camión, chofer y entrega sin donación
    │
    ├── utils/                                   # Generadores y sondeos inteligentes
    │   ├── TestIdGenerator.java                 # Generación de DNIs, CUITs, emails y bienes únicos
    │   ├── PollingUtils.java                    # Un sondeo Awaitility genérico con diagnóstico en timeout
    │   └── EspiaAmqp.java                       # Consumidor espía de RM-08
    │
    ├── smoke/                                   # @Tag("smoke") — Fail-Fast en < 2 segundos
    │   └── SmokeIT.java                         # Liveness de los 4 microservicios (OpenAPI 200)
    │
    ├── contract/                                # @Tag("contract")
    │   ├── ContractIT.java                      # Verificación OpenAPI de endpoints críticos
    │   └── TracingContractIT.java               # Header X-Trace-Id en las respuestas (la propagación es RM-12)
    │
    ├── integration/                             # @Tag("integration")
    │   ├── CrossServiceCommunicationIT.java     # Replicación REST y side-effects
    │   ├── DonationIntegrationIT.java
    │   ├── PersonIntegrationIT.java
    │   ├── LogisticaIntegrationIT.java          # Camiones, choferes, entregas y contrato del POST /api/entregas
    │   └── ErrorHandlingIntegrationIT.java      # Errores 400 y 404 con el código del ErrorCatalog
    │
    ├── e2e/                                     # @Tag("e2e")
    │   └── FullDistributedDonationE2EIT.java    # Entrega exitosa y fallida por el camino real de Logística
    │
    └── (k6 performance)                        # Pruebas de rendimiento desacopladas (tests/performance/k6/)
        ├── donaciones-creacion-carga.js         # Carga con rampa y SLA p(95) < 500ms
        └── incentivos-eventos-saturacion.js     # Saturación con rampa y SLA p(95) < 500ms
```

---

## 3. Patrones de Diseño Aplicados

### A. Clientes de API Fuertemente Tipados (Driver Pattern)
Cada microservicio cuenta con un cliente dedicado en `grupo5.tests.client.*` que encapsula las llamadas HTTP de RestAssured:
* **Métodos `*Ok`**: Ejecutan la petición, validan el status code esperado (`201 Created` / `200 OK`) y extraen directamente el identificador `UUID` o DTO correspondiente.
* **Métodos `Response`**: Retornan el objeto `io.restassured.response.Response` para pruebas de contrato o casos negativos (validación de status codes de error 400, 404, etc.).

### B. Test Data Builders Locales (Erradicación de Fixtures Huérfanos F-08)
En lugar de depender de fixtures JSON estáticos externos (los cuales fueron depurados y erradicados de `src/test/resources/fixtures/` al constituir código muerto no referenciado, resolviendo el hallazgo F-08), los payloads se construyen mediante constructores fluidos e inmutables:
```java
PersonaTestDTO persona = PersonaTestDataBuilder.humana()
    .conNombre("Carlos")
    .conDocumento(TestIdGenerator.randomDni())
    .conEmail(TestIdGenerator.randomEmail("carlos"))
    .build();

UUID personaId = donacionesClient.crearPersonaOk(persona);
```

### C. Aislamiento Total de Datos y Determinismo
* **Identidades Únicas**: `TestIdGenerator` produce DNIs, CUITs y correos aleatorios para evitar colisiones de unicidad.
* **Bienes Aislados en Matching**: Cada prueba genera nombres de bienes diferenciados (`TestIdGenerator.uniqueItemName("arroz_e2e")`), evitando que las donaciones creadas por pruebas volumétricas interfieran con los algoritmos de matching de otras suites.
* **Sin `runOrder=alphabetical`**: Las suites son 100% determinísticas e independientes del orden de ejecución.

### D. Sondeos Inteligentes de Negocio (`PollingUtils`)
Se eliminaron por completo las esperas ciegas (`esperarAsync` / `Thread.sleep`). Las sincronizaciones asíncronas son gestionadas por Awaitility evaluando condiciones de dominio reales (ej: esperar que `GET /api/donaciones/{id}` tenga estado `SEGMENTADA`), capturando e imprimiendo el último cuerpo de respuesta en caso de timeout para diagnóstico inmediato.

Todas las esperas pasan por `PollingUtils.esperar(descripción, timeout, consulta, condición)`, que consulta cada 200 ms. Los timeouts son de 5 a 10 s. Para un stack más lento, como el híbrido o un runner cargado, `-Dit.timeout.factor=2` los multiplica todos.

### E. Escenarios compartidos (`BaseIT`)
`BaseIT` arma los pasos que repiten varios tests:

* `crearDonante`: persona replicada en Notificaciones y registrada como donante.
* `crearEntidad`: entidad beneficiaria.
* `asignarYAprobar`: necesidad, matching y aprobación; devuelve las donaciones independientes.
* `entregarPorRest`: atajo que lleva las donaciones a `ENTREGADA` por REST, como un transportista.

El camino real lo cubre solo el E2E: Logística planifica, saca la ruta y confirma o rechaza la entrega, y Donaciones aplica `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa` y `entrega.fallida`.

> **Sin entregas ajenas pendientes.** La planificación es global y pone todas las entregas pendientes en el primer camión con capacidad. Si `ruta.iniciada` menciona una donación que no es del proveedor, Donaciones descarta el evento entero. Por eso, un test que crea una entrega sin donación detrás (`LogisticaIntegrationIT`) la lleva hasta `ENTREGADA`. Ver [diagnóstico E5](diagnostico-integration-tests-e5.md), DIT-12.

---

## 4. Ejecución de Pruebas

### A. Validación Local con `run-preprod-tests.sh` (Recomendado)
El script es el **punto de entrada único** (RM-04 del [roadmap de preprod fiel](./preprod-fiel-roadmap.md)): CI lo invoca tal cual (`main.yml`, job `preprod-validation`) y en local corre los mismos grupos con los mismos criterios de falla. Automatiza la compilación, la puesta en marcha de Docker Compose, la espera de healthchecks, la ejecución de Maven y el reporte.

| Opción | Valores | Efecto |
|---|---|---|
| `--images` | `local` (default) · `ghcr` | `local` compila los JARs y construye las imágenes; `ghcr` las baja de GHCR con `scripts/resolver-imagenes-ghcr.sh`, que lee `PR_TAG`, `CHANGED_SERVICES`, `MANIFIESTO` (RM-06: servicios no modificados por digest) y `BASE_TAG` del entorno |
| `--target` | `compose` (default) · `hybrid` | `hybrid` corre el stack híbrido con la nube (`scripts/preprod-hybrid.sh`); acepta los argumentos de `run-hybrid-tests.sh` |
| `--skip-build` | — | Reusa los JARs de `*/target/` (`--images local`) |
| `--test <Clase>` | — | Corre una clase puntual (`-Dit.test`) |
| `--groups <Grupo>` | `smoke`, `contract`, `integration`, `e2e`, `performance` | Corre un grupo; `performance` corre k6 |
| `--stay` | — | Deja el stack arriba hasta presionar una tecla |

**Criterios de falla, iguales en local y en CI:** sin filtros, corre `smoke` primero (fail-fast) y después `contract,integration,e2e`. `PREPROD_STRICT=true` por defecto: un servicio caído hace fallar el test en lugar de saltearlo (RM-02). `report_test_failures.py` falla con 0 tests o con un test salteado de un grupo requerido, y marca como *heredadas de la base* las fallas de servicios que el PR no modificó (`CHANGED_SERVICES`).

Los envoltorios conservan su nombre y sus argumentos: `run-preprod-tests-stay.sh` equivale a `--stay` y `run-hybrid-tests.sh` a `--target hybrid`.

**Contratos vivos (RM-08, [política de contratos](../adr/20261028-politica-de-evolucion-de-contratos-entre-servicios.md)):**

* `FullDistributedDonationE2EIT` arranca un consumidor espía (`EspiaAmqp`), que copia los mensajes de `donaciones.exchange`, `incentivos.exchange` y `logistica.exchange` desde el puerto AMQP publicado en `127.0.0.1:5672`. En `donaciones.exchange` se liga solo a `donacion.#`, `donante.#`, `persona.#` y al comando de la instancia del preprod, para no ocultar los rechazos `mandatory` del broker: un evento con otro prefijo requiere sumar su clave en `EspiaAmqp`. Al terminar, `ContratosAmqp` valida cada mensaje contra su schema `evento-*` (JSON Schema draft 2020-12, elegido por el header `__TypeId__`); un tipo sin schema es una violación. También es violación que falte uno de los tipos que el flujo tiene que publicar (`EVENTOS_ESPERADOS` del E2E). El log de la corrida lista los schemas que el E2E no ejercitó (`[contratos-amqp]`).
* Con `CONTRATOS_ESTRICTO=true` (post-merge y corte de entrega) una violación hace fallar el E2E; sin él (PR y local), queda como `::warning::` en `integration-tests/target/contratos-amqp-avisos.txt` y el script la muestra al final.
* `scripts/diff-contratos.sh <ref-base> aviso|error` compara las specs OpenAPI con `oasdiff` (imagen `tufin/oasdiff`, fijada por digest). Para probar en local un cambio de spec antes del PR: `scripts/diff-contratos.sh origin/ENTREGA_5 aviso`. Un cambio incompatible aceptado se registra en `docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt` (lo revisa el responsable de integración).

```bash
# Fail-Fast inmediato (Smoke Tests)
./run-preprod-tests.sh --skip-build --groups smoke

# Pruebas de Contratos y Trazabilidad (X-Trace-Id)
./run-preprod-tests.sh --skip-build --groups contract

# Pruebas de Integración y Errores Estructurados
./run-preprod-tests.sh --skip-build --groups integration

# Flujo E2E Distribuido Completo (Logística + RabbitMQ)
./run-preprod-tests.sh --skip-build --groups e2e

# Pruebas de Rendimiento y Carga con k6 (ADR 20260906-migracion-pruebas-rendimiento-a-k6)
./run-preprod-tests.sh --skip-build --groups performance

# Ejecución Completa
./run-preprod-tests.sh
```

### B. Ejecución Directa con Maven (con Stack Docker ya activo)
```powershell
mvn clean verify -pl integration-tests -DskipTests=false `
  "-Ddonaciones.url=http://localhost:8080" `
  "-Dnotificaciones.url=http://localhost:8081" `
  "-Dincentivos.url=http://localhost:8082" `
  "-Dlogistica.url=http://localhost:8083"
```

### C. Testing Acelerado e Impact Analysis (TIA)
Para ciclos de desarrollo rápido (Gate 1 y Gate 2) sin levantar infraestructura Docker completa, consultar la guía de optimización en [testing-performance.md](./testing-performance.md):

* **PowerShell (Windows):**
  ```powershell
  ./scripts/test-changed.ps1 -Fast
  ```
* **Bash / Git Bash / WSL:**
  ```bash
  ./scripts/test-changed.sh --fast
  ```

---

## 5. Comandos de Arranque de Infraestructura (Gate 4)

### Bash / WSL (Linux, macOS, Git Bash)

```bash
# Ejecución integral E2E con ciclo de vida automático
./run-preprod-tests.sh

# Mismas imágenes que CI (GHCR), por ejemplo las de la rama ENTREGA_5
BASE_TAG=entrega_5 ./run-preprod-tests.sh --images ghcr

# Ejecución manteniendo la infraestructura activa para depuración
./run-preprod-tests-stay.sh        # equivale a ./run-preprod-tests.sh --stay

# Stack híbrido (Logística en Render, RabbitMQ en CloudAMQP)
./run-hybrid-tests.sh --env <ruta>  # equivale a ./run-preprod-tests.sh --target hybrid --env <ruta>
```

### Windows (PowerShell)

```powershell
# Levantar entorno preproducción con JARs precompilados
docker compose -f docker-compose.preprod.yml up --build -d

# Ejecutar integración con maven apuntando a integration-tests (fase verify)
mvn verify -pl integration-tests -DskipTests=false

# Detener y limpiar recursos
docker compose -f docker-compose.preprod.yml down -v
```
