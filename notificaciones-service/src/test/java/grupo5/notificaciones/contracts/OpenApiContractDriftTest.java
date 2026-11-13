package grupo5.notificaciones.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import grupo5.common.testutils.OpenApiTestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class OpenApiContractDriftTest {

  @Test
  void openApiSpec_debeCargarSinErroresSintacticos() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-notificaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    assertNotNull(
        validator, "El validador de OpenAPI debe instanciarse correctamente para notificaciones");
  }

  @Test
  void personaResponse_debeSerValidaContraSpec() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-notificaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();

    SimpleResponse response = SimpleResponse.Builder.status(200).build();

    ValidationReport report =
        validator.validateResponse("/api/notificaciones/personas", Request.Method.PUT, response);
    assertFalse(
        report.hasErrors(),
        "La respuesta de sincronizacion de personas debe ser valida contra openapi-notificaciones.yaml: "
            + report.getMessages());
  }

  private static OpenApiInteractionValidator validador() {
    return OpenApiInteractionValidator.createFor(
            OpenApiTestUtils.resolveContractPath("openapi-notificaciones.yaml"))
        .build();
  }

  private static SimpleResponse json(int status, String cuerpo) {
    return SimpleResponse.Builder.status(status)
        .withContentType("application/json")
        .withBody(cuerpo)
        .build();
  }

  @Test
  void contacto_pedidoYRespuestasDelPost_cumplenElContrato() {
    OpenApiInteractionValidator validator = validador();
    SimpleRequest pedido =
        SimpleRequest.Builder.post("/api/contacto")
            .withContentType("application/json")
            .withHeader("X-Forwarded-For", "203.0.113.7")
            .withBody(
                """
                {"tema": "ENTREGA", "texto": "No llegó", "codigoError": null,
                 "correo": "ana@ejemplo.org", "claveEnvio": "6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4"}
                """)
            .build();
    String error =
        """
        {"code": "ERR-EST-903", "type": "BusinessStateException", "details": null,
         "traceId": "0123456789abcdef0123456789abcdef", "timestamp": "2026-11-10T09:00:00",
         "errors": null}
        """;

    ValidationReport report =
        validator
            .validateRequest(pedido)
            .merge(
                validator.validateResponse(
                    "/api/contacto", Request.Method.POST, json(201, "{\"numero\": 4821}")))
            .merge(
                validator.validateResponse(
                    "/api/contacto", Request.Method.POST, json(200, "{\"numero\": 4821}")))
            .merge(
                validator.validateResponse("/api/contacto", Request.Method.POST, json(429, error)));

    assertFalse(report.hasErrors(), "POST /api/contacto: " + report.getMessages());
  }

  @Test
  void contacto_mensajeDelAdmin_cumpleElContrato() {
    OpenApiInteractionValidator validator = validador();
    String mensaje =
        """
        {"numero": 4821, "tema": "ENTREGA", "texto": "No llegó", "codigoError": null,
         "correo": "ana@ejemplo.org", "estado": "RESPONDIDO",
         "recibidoEn": "2026-11-10T09:00:00", "respuesta": "Listo",
         "respondidoEn": "2026-11-10T10:00:00"}
        """;

    ValidationReport report =
        validator
            .validateResponse("/api/contacto", Request.Method.GET, json(200, "[" + mensaje + "]"))
            .merge(
                validator.validateResponse(
                    "/api/contacto/4821", Request.Method.GET, json(200, mensaje)))
            .merge(
                validator.validateResponse(
                    "/api/contacto/4821/respuesta", Request.Method.POST, json(200, mensaje)));

    assertFalse(report.hasErrors(), "Mensajes de contacto: " + report.getMessages());
  }

  @Test
  void bandeja_pedidoPaginadoConFiltrosYRespuestaConElTotal_cumplenElContrato() {
    OpenApiInteractionValidator validator = validador();
    String ruta = "/api/notificaciones/persona/6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4";
    SimpleRequest pedido =
        SimpleRequest.Builder.get(ruta)
            .withQueryParam("leida", "false")
            .withQueryParam("q", "camion")
            .withQueryParam("tipo", "DONACION_EN_CAMINO")
            .withQueryParam("excluirTipo", "DONACION_ASIGNADA", "ENTREGA_FALLIDA")
            .withQueryParam("orden", "antiguas")
            .withQueryParam("page", "1")
            .withQueryParam("size", "20")
            .build();
    SimpleResponse pagina =
        SimpleResponse.Builder.status(200)
            .withContentType("application/json")
            .withHeader("X-Total-Count", "45")
            .withBody(
                """
                [{"id": "6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c5", "mensaje": "El camión salió",
                  "estado": "ENVIADA", "fechaCreacion": "2026-10-15T14:30:00Z", "leida": false,
                  "fechaLectura": null, "tipoEvento": "DONACION_EN_CAMINO", "referenciaId": null}]
                """)
            .build();

    ValidationReport report =
        validator
            .validateRequest(pedido)
            .merge(validator.validateResponse(ruta, Request.Method.GET, pagina));

    assertFalse(report.hasErrors(), "La bandeja debe cumplir el contrato: " + report.getMessages());
  }

  @Test
  void bandeja_conUnTamanioFueraDeRango_noCumpleElContrato() {
    SimpleRequest pedido =
        SimpleRequest.Builder.get(
                "/api/notificaciones/persona/6f1c2b9e-3d4a-4b5c-8d6e-7f8091a2b3c4")
            .withQueryParam("size", "101")
            .build();

    assertTrue(validador().validateRequest(pedido).hasErrors());
  }
}
