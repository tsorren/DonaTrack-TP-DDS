package grupo5.logistica.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import grupo5.common.testutils.OpenApiTestUtils;
import grupo5.logistica.dto.rutas.DireccionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.RecordComponent;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class OpenApiContractDriftTest {

  @Test
  void openApiSpec_debeCargarSinErroresSintacticos() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    assertNotNull(validator, "El validador de OpenAPI debe instanciarse correctamente");
  }

  @Test
  void direccionDTO_debeSoportarNulosEnPisoYDepartamentoSegunSpec() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();

    // Response sintética con campos nulos tal como los emite Jackson
    String jsonConNulos =
        """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "idDonacion": "22222222-2222-2222-2222-222222222222",
          "idBeneficiaria": "33333333-3333-3333-3333-333333333333",
          "destino": {
            "calle": "Av. Corrientes",
            "altura": 1234,
            "piso": null,
            "departamento": null,
            "codigoPostal": "1043",
            "localidad": "CABA",
            "provincia": "Buenos Aires",
            "pais": "Argentina"
          },
          "estadoActual": "PENDIENTE",
          "volumenTotalM3": 2.0,
          "pesoTotalKG": 10.5,
          "historialEstado": []
        }
        """;

    SimpleResponse response =
        SimpleResponse.Builder.status(201)
            .withContentType("application/json")
            .withBody(jsonConNulos)
            .build();

    ValidationReport report =
        validator.validateResponse("/api/entregas", Request.Method.POST, response);
    assertFalse(
        report.hasErrors(),
        "La respuesta con piso y departamento nulos debe ser valida contra openapi-logistica.yaml: "
            + report.getMessages());
  }

  @Test
  void direccionDTO_inspeccionReflexiva_camposOpcionalesSonNullable() {
    // Valida que los campos no obligatorios en el Record Java sean efectivamente nulos en runtime
    for (RecordComponent rc : DireccionDTO.class.getRecordComponents()) {
      boolean hasNotNull =
          rc.isAnnotationPresent(NotNull.class) || rc.isAnnotationPresent(NotBlank.class);
      if ("piso".equals(rc.getName()) || "departamento".equals(rc.getName())) {
        assertFalse(hasNotNull, rc.getName() + " debe ser opcional (sin @NotNull/@NotBlank)");
      }
    }
  }
}
