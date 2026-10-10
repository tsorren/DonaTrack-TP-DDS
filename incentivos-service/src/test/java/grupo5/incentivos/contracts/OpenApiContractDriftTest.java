package grupo5.incentivos.contracts;

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
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-incentivos.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    assertNotNull(
        validator, "El validador de OpenAPI debe instanciarse correctamente para incentivos");
  }

  @Test
  void rankingResponse_debeSerValidaContraSpec() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-incentivos.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();

    String jsonResponse =
        """
        {
          "periodo": "2026-09",
          "entradas": [
            {
              "posicion": 1,
              "donanteId": "11111111-1111-1111-1111-111111111111",
              "nombreDonante": "Donante Test",
              "misionesCompletadas": 5
            }
          ],
          "podio": []
        }
        """;

    SimpleResponse response =
        SimpleResponse.Builder.status(200)
            .withContentType("application/json")
            .withBody(jsonResponse)
            .build();

    ValidationReport report =
        validator.validateResponse("/api/incentivos/ranking/ultimo", Request.Method.GET, response);
    assertFalse(
        report.hasErrors(),
        "La respuesta de ranking debe ser valida contra openapi-incentivos.yaml: "
            + report.getMessages());
  }
}
