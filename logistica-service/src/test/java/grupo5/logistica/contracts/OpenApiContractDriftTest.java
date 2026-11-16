package grupo5.logistica.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import grupo5.common.testutils.OpenApiTestUtils;
import grupo5.logistica.dto.entregas.PosicionCamionDTO;
import grupo5.logistica.dto.entregas.SeguimientoEntregaResponseDTO;
import grupo5.logistica.dto.rutas.DireccionDTO;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

  @Test
  void seguimiento_conYSinPosicionDelCamion_cumpleElContrato() throws Exception {
    String specPath = OpenApiTestUtils.resolveContractPath("openapi-logistica.yaml");
    OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(specPath).build();
    ObjectMapper jackson =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Instant ahora = Instant.parse("2026-11-08T12:00:30Z");
    PosicionCamionDTO posicion = new PosicionCamionDTO(-34.6, -58.4, ahora);

    for (PosicionCamionDTO valor : java.util.Arrays.asList(posicion, null)) {
      SeguimientoEntregaResponseDTO dto =
          new SeguimientoEntregaResponseDTO(
              UUID.randomUUID(),
              UUID.randomUUID(),
              EstadoEntrega.EN_TRASLADO,
              true,
              UUID.randomUUID(),
              LocalDate.of(2026, 11, 8),
              EstadoRuta.EN_TRASLADO,
              "AB123CD",
              1,
              1,
              0,
              0,
              ahora,
              null,
              ahora,
              List.of(),
              valor);
      SimpleResponse response =
          SimpleResponse.Builder.status(200)
              .withContentType("application/json")
              .withBody(jackson.writeValueAsString(dto))
              .build();

      ValidationReport report =
          validator.validateResponse(
              "/api/entregas/seguimiento/donaciones/" + UUID.randomUUID(),
              Request.Method.GET,
              response);
      assertFalse(
          report.hasErrors(),
          "El seguimiento con posicionCamion="
              + valor
              + " debe cumplir el contrato: "
              + report.getMessages());
    }
  }
}
