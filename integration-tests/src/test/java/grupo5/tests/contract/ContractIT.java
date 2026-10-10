package grupo5.tests.contract;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Suite de validación de contratos vivos OpenAPI 3.0 (Fase 3A del roadmap de QA). Erradica el
 * anti-patrón AP-01 (Green Smoke Contract) mediante verificación estricta bidireccional de payloads
 * contra las especificaciones formales en docs/arquitectura/contratos/.
 */
@Tag("contract")
class ContractIT extends BaseIT {

  private static String resolveContractPath(String filename) {
    Path pathInSubmodule = Path.of("../docs/arquitectura/contratos/", filename);
    if (Files.exists(pathInSubmodule)) {
      return pathInSubmodule.toAbsolutePath().toString();
    }
    return Path.of("docs/arquitectura/contratos/", filename).toAbsolutePath().toString();
  }

  @Test
  void testNotificacionesPersonasContract() {
    // donaciones-service sincroniza réplicas de personas mediante PUT /api/notificaciones/personas
    Assumptions.assumeTrue(
        isServiceAvailable(NOTIFICACIONES_URL),
        "notificaciones-service no disponible en " + NOTIFICACIONES_URL);

    String specPath = resolveContractPath("openapi-notificaciones.yaml");
    OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(specPath);

    String personaPayload =
        """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "denominacion": "Persona Replica Contract Test",
          "tipoPersona": "HUMANA",
          "mediosDeContacto": []
        }
        """;

    given()
        .filter(validationFilter)
        .contentType(ContentType.JSON)
        .body(personaPayload)
        .when()
        .put(NOTIFICACIONES_URL + "/api/notificaciones/personas")
        .then()
        .statusCode(200);
  }

  @Test
  void testAllOpenApiSpecsLoadSuccessfully() {
    // Valida que las 4 especificaciones OpenAPI se carguen sin errores sintácticos ni referencias
    // rotas
    for (String specFile :
        java.util.List.of(
            "openapi-donaciones.yaml",
            "openapi-notificaciones.yaml",
            "openapi-logistica.yaml",
            "openapi-incentivos.yaml")) {
      String path = resolveContractPath(specFile);
      OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(path).build();
      org.junit.jupiter.api.Assertions.assertNotNull(
          validator, "El validador para " + specFile + " debe construirse exitosamente");
    }
  }

  @Test
  void testIncentivosDonacionesContract() {
    // donaciones-service notifica eventos de donación mediante POST /api/incentivos/donaciones
    Assumptions.assumeTrue(
        isServiceAvailable(INCENTIVOS_URL),
        "incentivos-service no disponible en " + INCENTIVOS_URL);

    String specPath = resolveContractPath("openapi-incentivos.yaml");
    OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(specPath);

    UUID donanteId = UUID.randomUUID();
    UUID personaId = UUID.randomUUID();
    String registrarPayload =
        """
        {
          "idDonante": "%s",
          "idPersona": "%s",
          "nombre": "Donante Contract Test"
        }
        """
            .formatted(donanteId, personaId);

    given()
        .filter(validationFilter)
        .contentType(ContentType.JSON)
        .body(registrarPayload)
        .when()
        .post(INCENTIVOS_URL + "/api/incentivos/donantes/" + donanteId)
        .then()
        .statusCode(201);

    String donacionPayload =
        """
        {
          "donanteId": "%s",
          "categorias": ["Alimentos"],
          "cantidadBienes": 3,
          "fecha": "%s"
        }
        """
            .formatted(donanteId, java.time.LocalDate.now());

    given()
        .filter(validationFilter)
        .contentType(ContentType.JSON)
        .body(donacionPayload)
        .when()
        .post(INCENTIVOS_URL + "/api/incentivos/donaciones")
        .then()
        .statusCode(200);
  }

  @Test
  void testLogisticaEntregasContract() {
    // donaciones-service solicita planificación de entrega mediante POST /api/entregas
    Assumptions.assumeTrue(
        isServiceAvailable(LOGISTICA_URL), "logistica-service no disponible en " + LOGISTICA_URL);

    String specPath = resolveContractPath("openapi-logistica.yaml");
    OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(specPath);

    String entregaPayload =
        """
        {
          "idDonacion": "%s",
          "idBeneficiaria": "%s",
          "destino": {
            "calle": "Av. Corrientes",
            "altura": 1234,
            "codigoPostal": "1043",
            "localidad": "CABA",
            "provincia": "Buenos Aires",
            "pais": "Argentina"
          },
          "pesoTotalKG": 10.5,
          "volumenTotalM3": 2.0
        }
        """
            .formatted(UUID.randomUUID(), UUID.randomUUID());

    given()
        .filter(validationFilter)
        .contentType(ContentType.JSON)
        .body(entregaPayload)
        .when()
        .post(LOGISTICA_URL + "/api/entregas")
        .then()
        .statusCode(201);
  }

  @Test
  void testAdversarialBreakingChangeContractValidation() {
    // Verificación adversaria: si un cliente altera el contrato omitiendo campos requeridos
    // o alterando tipos de datos, OpenApiValidationFilter y el validador estricto deben
    // rechazar el request inmediatamente (detección de breaking changes).
    String specPath = resolveContractPath("openapi-notificaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();

    // 1. Request válido contra OpenAPI
    Request validRequest =
        SimpleRequest.Builder.put("/api/notificaciones/personas")
            .withContentType("application/json")
            .withBody(
                """
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "denominacion": "Persona Valida",
                  "tipoPersona": "HUMANA",
                  "mediosDeContacto": []
                }
                """)
            .build();
    ValidationReport validReport = validator.validateRequest(validRequest);
    assertFalse(
        validReport.hasErrors(),
        "El request válido no debe tener errores: " + validReport.getMessages());

    // 2. Request inválido: falta campo mandatorio 'tipoPersona'
    Request missingTipoRequest =
        SimpleRequest.Builder.put("/api/notificaciones/personas")
            .withContentType("application/json")
            .withBody(
                """
                {
                  "id": "11111111-1111-1111-1111-111111111111",
                  "denominacion": "Persona Sin Tipo"
                }
                """)
            .build();
    ValidationReport badTipoReport = validator.validateRequest(missingTipoRequest);
    assertTrue(
        badTipoReport.hasErrors(),
        "Debe detectar error de breaking change al omitir 'tipoPersona'");

    // 3. Request inválido: UUID malformado
    Request badUuidRequest =
        SimpleRequest.Builder.put("/api/notificaciones/personas")
            .withContentType("application/json")
            .withBody(
                """
                {
                  "id": "uuid-invalido-123",
                  "denominacion": "Persona Con Bad UUID",
                  "tipoPersona": "HUMANA",
                  "mediosDeContacto": []
                }
                """)
            .build();
    ValidationReport badUuidReport = validator.validateRequest(badUuidRequest);
    assertTrue(badUuidReport.hasErrors(), "Debe detectar error de formato al enviar UUID inválido");

    // 4. Response inválida: status code 500 no documentado en la spec para PUT
    // /api/notificaciones/personas
    com.atlassian.oai.validator.model.Response badResponse =
        com.atlassian.oai.validator.model.SimpleResponse.Builder.status(500).build();
    ValidationReport badResponseReport =
        validator.validateResponse("/api/notificaciones/personas", Request.Method.PUT, badResponse);
    assertTrue(
        badResponseReport.hasErrors(),
        "Debe detectar error cuando el status code de respuesta no está documentado en la spec");

    // 5. Verificación de intercepción en vivo cuando el servicio está activo
    if (isServiceAvailable(NOTIFICACIONES_URL)) {
      OpenApiValidationFilter filter = new OpenApiValidationFilter(specPath);
      assertThrows(
          OpenApiValidationFilter.OpenApiValidationException.class,
          () ->
              given()
                  .filter(filter)
                  .contentType(ContentType.JSON)
                  .body("{\"denominacion\": \"Sin Id Ni Tipo\"}")
                  .when()
                  .put(NOTIFICACIONES_URL + "/api/notificaciones/personas"));
    }
  }

  @Test
  void testRabbitMqMessagingContractsDeferred() {
    // [DEFERRED_PENDING_RABBITMQ_CONTRACTS]
    // Subfase 3B: La validación asincrónica de eventos AMQP contra JSON Schemas queda
    // catalogada y diferida hasta la congelación de contratos de mensajería con RabbitMQ
    // según docs/testing/auditoria/05-roadmap-migracion.md §2.3.
    Path schemaPath = Path.of(resolveContractPath("schemas/evento-entrega-exitosa.schema.json"));
    assertTrue(
        Files.exists(schemaPath),
        "Los esquemas base de eventos deben existir en docs/arquitectura/contratos/schemas/");
  }
}
