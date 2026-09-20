package grupo5.notificaciones.contracts;

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
}
