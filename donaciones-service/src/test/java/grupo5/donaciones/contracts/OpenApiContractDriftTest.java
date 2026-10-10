package grupo5.donaciones.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import grupo5.common.testutils.OpenApiTestUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class OpenApiContractDriftTest {

  @Test
  void openApiSpec_debeCargarSinErroresSintacticos() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-donaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    assertNotNull(
        validator, "El validador de OpenAPI debe instanciarse correctamente para donaciones");
  }

  @Test
  void donacionResponse_debeSerValidaContraSpec() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-donaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();

    String jsonResponse =
        """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "donacionOriginalId": "22222222-2222-2222-2222-222222222222",
          "estadoActual": "EN_DEPOSITO",
          "fechaRegistro": "2026-09-08T12:00:00Z"
        }
        """;

    SimpleResponse response =
        SimpleResponse.Builder.status(200)
            .withContentType("application/json")
            .withBody(jsonResponse)
            .build();

    ValidationReport report =
        validator.validateResponse(
            "/donaciones-independientes/11111111-1111-1111-1111-111111111111",
            Request.Method.GET,
            response);
    assertFalse(
        report.hasErrors(),
        "La respuesta de donacion debe ser valida contra openapi-donaciones.yaml: "
            + report.getMessages());
  }
}
