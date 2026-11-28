package grupo5.donaciones.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void evolucionDelInicioDelAdmin_debeSerValidaContraSpec_R1142() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-donaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    String conDatos =
        """
        {
          "desde": "2026-09",
          "hasta": "2026-10",
          "meses": [
            { "mes": "2026-09", "registradas": 4, "entregadas": 0 },
            { "mes": "2026-10", "registradas": 2, "entregadas": 3 }
          ],
          "categorias": [
            {
              "categoriaId": "11111111-1111-1111-1111-111111111111",
              "nombre": "Alimentos",
              "registradas": 6,
              "entregadas": 3
            }
          ],
          "otras": null,
          "calculadoEn": "2026-10-15T18:30:00-03:00"
        }
        """;
    String sinDonaciones =
        """
        {
          "desde": null,
          "hasta": null,
          "meses": [],
          "categorias": [],
          "otras": null,
          "calculadoEn": "2026-10-15T18:30:00-03:00"
        }
        """;
    for (String cuerpo : new String[] {conDatos, sinDonaciones}) {
      ValidationReport report =
          validator.validateResponse(
              "/api/donaciones/resumen/evolucion",
              Request.Method.GET,
              SimpleResponse.Builder.status(200)
                  .withContentType("application/json")
                  .withBody(cuerpo)
                  .build());
      assertFalse(
          report.hasErrors(),
          "La evolución debe ser válida contra openapi-donaciones.yaml: " + report.getMessages());
    }
  }

  /** HG-04: el período trae el avance en unidades; una cifra negativa no cumple el contrato. */
  @Test
  void periodoConAvanceEnUnidades_debeSerValidoContraSpec_HG04() {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-donaciones.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    String periodo =
        """
        [{
          "id": "11111111-1111-1111-1111-111111111111",
          "inicio": "2026-10-01",
          "fin": "2026-10-31",
          "objetivo": 24,
          "asignado": 18,
          "recibido": 12,
          "resultado": "EN_CURSO",
          "avance": { "llegaron": %s, "asignadas": 6, "sinAsignar": 6 }
        }]
        """;
    String ruta = "/api/necesidades/11111111-1111-1111-1111-111111111111/periodos";

    ValidationReport valido =
        validator.validateResponse(ruta, Request.Method.GET, json(periodo.formatted("12")));
    ValidationReport negativo =
        validator.validateResponse(ruta, Request.Method.GET, json(periodo.formatted("-1")));

    assertFalse(
        valido.hasErrors(), "El período con avance debe ser válido: " + valido.getMessages());
    assertTrue(negativo.hasErrors(), "Una cifra negativa del avance no cumple el contrato");
  }

  private static SimpleResponse json(String cuerpo) {
    return SimpleResponse.Builder.status(200)
        .withContentType("application/json")
        .withBody(cuerpo)
        .build();
  }
}
