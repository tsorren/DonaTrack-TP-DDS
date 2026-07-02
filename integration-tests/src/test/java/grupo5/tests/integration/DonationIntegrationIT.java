package grupo5.tests.integration;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class DonationIntegrationIT extends BaseIT {

  @Test
  void testCrearDonacion() {
    // 1. Crear persona
    String personaId = apiCrearPersonaHumana("11112222", "Maria", "maria.gomez@example.com");

    // 2. Crear donante en base a persona
    String donanteId = apiCrearDonante(personaId);

    // 3. Crear donacion usando la plantilla e inyectando el id dinamico del donante
    Map<String, Object> donacionPayload = fixture("donaciones/crear-donacion.json");
    donacionPayload.put("idDonante", donanteId);

    given()
            .contentType(ContentType.JSON)
            .body(donacionPayload)
            .when()
            .post(DONACIONES_URL + "/api/donaciones")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
  }
}
