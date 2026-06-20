# Módulo de Pruebas de Integración (`integration-tests`)

Este documento detalla el diseño, funcionamiento y arquitectura del módulo `integration-tests`, el cual valida el comportamiento cross-service y de extremo a extremo (E2E) de la plataforma DonaTrack. Además, provee una guía paso a paso sobre cómo agregar nuevos escenarios de prueba.

---

## 1. Propósito y Alcance

El módulo `integration-tests` realiza pruebas de caja negra golpeando las APIs HTTP expuestas por los distintos microservicios:
* **`donaciones-service`**: Administración de donantes, personas, entidades beneficiarias, donaciones y necesidades.
* **`notificaciones-service`**: Emisión y almacenamiento de notificaciones y replicación de personas.
* **`incentivos-service`**: Cálculo de ranking mensual, misiones, categorías e insignias de los colaboradores.

Las pruebas se ejecutan sobre un entorno de preproducción (`preprod`) levantado mediante Docker Compose, garantizando que se evalúe el comportamiento de red real, la serialización, y los efectos cross-service (sincrónicos y asíncronos).

---

## 2. Estructura del Módulo

El módulo está estructurado de la siguiente forma:

```
integration-tests/
├── pom.xml
└── src/
    └── test/
        ├── java/
        │   └── grupo5/
        │       └── tests/
        │           ├── BaseIT.java             # Clase base común con helpers de API y fixtures
        │           ├── contract/
        │           │   └── ContractIT.java     # Pruebas de compatibilidad OpenAPI
        │           ├── e2e/
        │           │   └── DonationFlowE2EIT.java # Pruebas funcionales E2E
        │           ├── integration/
        │           │   ├── CrossServiceCommunicationIT.java # Flujos complejos y side-effects
        │           │   ├── DonationIntegrationIT.java
        │           │   └── PersonIntegrationIT.java
        │           ├── performance/
        │           │   └── PerformanceStressIT.java # Pruebas de carga, estrés y latencia
        │           └── smoke/
        │               └── SmokeIT.java        # Pruebas de disponibilidad básica
        └── resources/
            └── fixtures/                       # Cuerpos JSON estructurados y reutilizables
                ├── donaciones/
                │   └── crear-donacion.json
                ├── necesidades/
                │   └── crear-necesidad.json
                └── personas/
                    ├── crear-persona-humana.json
                    └── crear-persona-juridica.json
```

---

## 3. Fixtures y Clase Base (`BaseIT.java`)

Para evitar la duplicación de código y payloads inline, el módulo utiliza plantillas JSON parametrizables llamadas **Fixtures** administradas mediante Jackson en `BaseIT.java`.

### Fixtures Reutilizables
* **[crear-persona-humana.json](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/resources/fixtures/personas/crear-persona-humana.json)**: Persona Humana con dirección (`Av. Medrano`) y medio de contacto tipo `CORREO` por defecto.
* **[crear-persona-juridica.json](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/resources/fixtures/personas/crear-persona-juridica.json)**: Persona Jurídica (organizaciones beneficiarias) con representantes y datos de contacto estructurados.
* **[crear-donacion.json](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/resources/fixtures/donaciones/crear-donacion.json)**: Donación estándar de arroz con depósito físico y dirección asignados.
* **[crear-necesidad.json](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/resources/fixtures/necesidades/crear-necesidad.json)**: Necesidad extraordinaria parametrizable.

### Helpers de API en `BaseIT`
`BaseIT` expone métodos para interactuar de forma simplificada con las APIs utilizando RestAssured:
* `fixture(path)`: Carga un JSON de `resources/fixtures/` devolviendo un `Map<String, Object>` mutable.
* `apiCrearPersonaHumana(documento, nombre, email)`: Crea una persona humana sobrescribiendo dinámicamente los datos de contacto y DNI.
* `apiCrearPersonaJuridica(documento, razonSocial, email)`: Registra una persona jurídica.
* `apiCrearDonante(personaId)`: Registra a una persona en el rol de Donante.
* `apiCrearDonacion(personaId, descripcion, item, cantidad)`: Registra una donación física en `donaciones-service`.
* `apiCrearNecesidad(...)` y `apiCrearEntidad(...)`: Registran necesidades de insumos y entidades beneficiarias.
* `esperarReplicacionPersona(personaId)`: Resuelve condiciones de carrera en flujos de replicación asíncrona. Realiza sondeos rápidos contra `notificaciones-service` con esperas mínimas de 10ms hasta confirmar que la persona ha sido replicada.

---

## 4. Tipos de Pruebas Implementadas

1. **Smoke Tests (`SmokeIT.java`)**:
   Verifican que los microservicios estén levantados y expongan correctamente su documentación técnica OpenAPI en `/v3/api-docs`.
2. **Contract Tests (`ContractIT.java`)**:
   Validan mediante aserciones sobre OpenAPI que los contratos de API esperados por otros servicios (ej: endpoints de `/api/notificaciones/personas` o `/api/incentivos/donaciones`) permanezcan estables y con los verbos correctos.
3. **Integration Tests (`CrossServiceCommunicationIT.java`)**:
   Validan flujos con efectos cross-service:
   * Replicación y actualización asíncrona del ciclo de vida de Personas.
   * Registro de Donantes y envío de notificaciones de bienvenida a canales reales simulados.
   * Flujo E2E de Matching y Asignación: Carga de donaciones, carga de necesidades, ejecución de matching de propuestas, aprobación de propuestas, transición de estados de la donación (CARGADA -> EN_TRASLADO -> ENTREGADA) y posterior impacto en puntos/categorías de incentivos.
   * Rankings y misiones consecutivas del donante.
4. **Performance & Stress Tests (`PerformanceStressIT.java`)**:
   Valida la respuesta del sistema bajo carga secuencial rápida:
   * Registra secuencialmente 100 donantes completos (Persona + Donante + Donación) validando tiempos promedio (< 500ms).
   * Ingiere 200 eventos de donación directamente a `incentivos-service` (latencia promedio < 150ms).

---

## 5. Cómo Ejecutar las Pruebas

### Prerrequisitos
Asegurarse de que el entorno de preproducción esté corriendo. Puede levantarse con:
```powershell
docker compose -f docker-compose.preprod.yml up -d --build
```

### Ejecutar todas las pruebas del módulo
Por defecto, las pruebas de integración están desactivadas en Surefire (`skipTests=true`). Para correrlas, ejecute:
```powershell
mvn clean verify -pl integration-tests -DskipTests=false
```

### Ejecutar un test en específico
```powershell
mvn test -pl integration-tests -Dtest=PerformanceStressIT -DskipTests=false
```

### Parametrizar URLs
Si ejecutas los microservicios en puertos o hosts distintos a los locales por defecto (`localhost:8080`, `localhost:8081`, `localhost:8082`), puedes sobrescribirlos mediante variables de sistema:
```powershell
mvn clean verify -pl integration-tests -DskipTests=false "-Ddonaciones.url=http://mi-servidor:8080" "-Dnotificaciones.url=http://mi-servidor:8081" "-Dincentivos.url=http://mi-servidor:8082"
```

---

## 6. Guía: Cómo Agregar un Nuevo Test de Integración

Sigue estos pasos para expandir la cobertura de pruebas de integración en el sistema:

### Paso 1: Determinar la categoría y crear la clase
* Crea una nueva clase en el subpaquete adecuado dentro de `grupo5.tests.*` (ej: `grupo5.tests.integration`).
* El nombre de la clase **debe terminar en `IT.java`** (convención de Failsafe/Surefire para pruebas de integración).
* La clase debe extender de **`BaseIT`** para heredar las configuraciones de endpoints y métodos utilitarios.

```java
package grupo5.tests.integration;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class MiNuevaFuncionalidadIT extends BaseIT {
    // Tus tests van aquí
}
```

### Paso 2: Usar o crear fixtures para los payloads
* Si necesitas enviar un cuerpo de petición complejo, revisa si ya existe una plantilla adecuada en `src/test/resources/fixtures/`.
* Si requieres un nuevo tipo de payload, crea un archivo JSON en una subcarpeta descriptiva dentro de `fixtures` (ej: `fixtures/beneficios/crear-beneficio.json`).
* En tu test, carga el fixture con `fixture("beneficios/crear-beneficio.json")` y modifica las propiedades que requieran valores dinámicos utilizando `payload.put("propiedad", valor)`.

### Paso 3: Implementar la lógica del test
* Escribe tu método anotado con `@Test`.
* Invoca los helpers heredados de `BaseIT` para crear entidades y registrar donaciones/necesidades si es necesario.
* **Importante (Replicación Asincrónica)**: Si tu prueba crea una persona y luego interactúa con un servicio que consume esa persona de manera indirecta (ej: `notificaciones-service`), asegúrate de esperar la replicación llamando a `esperarReplicacionPersona(personaId)` antes de hacer llamadas concurrentes.

```java
@Test
public void testMiFlujoNuevo() {
    // 1. Crear una persona base
    String personaId = apiCrearPersonaHumana("90001111", "Lucas", "lucas@example.com");

    // 2. Ejecutar la acción bajo prueba
    given()
        .contentType(ContentType.JSON)
        .body(fixture("beneficios/crear-beneficio.json"))
        .when()
        .post(INCENTIVOS_URL + "/api/beneficios")
        .then()
        .statusCode(201)
        .body("id", notNullValue());
}
```

### Paso 4: Formatear y verificar
1. Formatea el código para cumplir con Google Java Format:
   ```powershell
   mvn spotless:apply
   ```
2. Ejecuta tu nueva prueba individualmente para comprobar que sea exitosa:
   ```powershell
   mvn test -pl integration-tests -Dtest=MiNuevaFuncionalidadIT -DskipTests=false
   ```
3. Corre la suite completa para descartar interferencias:
   ```powershell
   mvn clean verify -pl integration-tests -DskipTests=false
   ```
