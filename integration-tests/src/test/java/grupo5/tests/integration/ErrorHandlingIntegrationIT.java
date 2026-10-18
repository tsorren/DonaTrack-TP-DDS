package grupo5.tests.integration;

import static org.hamcrest.Matchers.notNullValue;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.PersonaTestDTO;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class ErrorHandlingIntegrationIT extends BaseIT {

  @Test
  void testCrearPersonaInvalidaRetornaBadRequestEstructurado() {
    // Persona humana sin nombre obligatorio
    PersonaTestDTO personaInvalida = PersonaTestDataBuilder.humana().conNombre("").build();

    donacionesClient
        .crearPersona(personaInvalida)
        .then()
        .statusCode(400)
        .body("code", notNullValue())
        .body("traceId", notNullValue());
  }

  @Test
  void testObtenerDonacionInexistenteRetornaNotFound() {
    UUID idInexistente = UUID.randomUUID();
    donacionesClient
        .obtenerDonacion(idInexistente)
        .then()
        .statusCode(404)
        .body("code", notNullValue())
        .body("traceId", notNullValue());
  }

  @Test
  void testCalcularRankingPeriodoInvalidoRetornaBadRequest() {
    incentivosClient
        .calcularRanking("periodo-invalido")
        .then()
        .statusCode(400)
        .body("code", notNullValue());
  }
}
