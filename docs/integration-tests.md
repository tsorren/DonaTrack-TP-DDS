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
├── pom.xml                                      # Configuración Maven (Spotless activo, Surefire)
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
    │   └── PersonaTestDataBuilder.java
    │
    ├── utils/                                   # Generadores y sondeos inteligentes
    │   ├── TestIdGenerator.java                 # Generación de DNIs, CUITs, emails y bienes únicos
    │   └── PollingUtils.java                    # Sondeos Awaitility con diagnóstico detallado en timeout
    │
    ├── smoke/                                   # @Tag("smoke") — Fail-Fast en < 2 segundos
    │   └── SmokeIT.java                         # Liveness de los 4 microservicios (OpenAPI 200)
    │
    ├── contract/                                # @Tag("contract")
    │   ├── ContractIT.java                      # Verificación OpenAPI de endpoints críticos
    │   └── TracingContractIT.java               # Verificación y propagación de header X-Trace-Id
    │
    ├── integration/                             # @Tag("integration")
    │   ├── CrossServiceCommunicationIT.java     # Replicación REST y side-effects
    │   ├── DonationIntegrationIT.java
    │   ├── PersonIntegrationIT.java
    │   └── ErrorHandlingIntegrationIT.java      # Contratos de error 400, 404, 409 y fieldErrors
    │
    ├── e2e/                                     # @Tag("e2e")
    │   └── FullDistributedDonationE2EIT.java    # Flujo completo con Logística y RabbitMQ
    │
    └── performance/                             # @Tag("performance")
        └── PerformanceStressIT.java             # Caracterización volumétrica con bienes aislados
```

---

## 3. Patrones de Diseño Aplicados

### A. Clientes de API Fuertemente Tipados (Driver Pattern)
Cada microservicio cuenta con un cliente dedicado en `grupo5.tests.client.*` que encapsula las llamadas HTTP de RestAssured:
* **Métodos `*Ok`**: Ejecutan la petición, validan el status code esperado (`201 Created` / `200 OK`) y extraen directamente el identificador `UUID` o DTO correspondiente.
* **Métodos `Response`**: Retornan el objeto `io.restassured.response.Response` para pruebas de contrato o casos negativos (validación de status codes de error 400, 404, etc.).

### B. Test Data Builders Locales
En lugar de mutar JSONs no tipados (`Map<String, Object>`), los payloads se construyen mediante constructores fluidos e inmutables:
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

---

## 4. Ejecución de Pruebas

### A. Validación Local con `run-preprod-tests.sh` (Recomendado)
El script automatiza la compilación, puesta en marcha de Docker Compose, espera de healthchecks y ejecución de Maven:

```bash
# Fail-Fast inmediato (Smoke Tests)
./run-preprod-tests.sh --skip-build --groups smoke

# Pruebas de Contratos y Trazabilidad (X-Trace-Id)
./run-preprod-tests.sh --skip-build --groups contract

# Pruebas de Integración y Errores Estructurados
./run-preprod-tests.sh --skip-build --groups integration

# Flujo E2E Distribuido Completo (Logística + RabbitMQ)
./run-preprod-tests.sh --skip-build --groups e2e

# Pruebas de Rendimiento y Volumen
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
