package grupo5.tests.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DonationIntegrationIT extends BaseIT {

  @Test
  void testRegistrarDonacion() {
    // 1. Crear donante usando la plantilla base con overrides especificos para este test
    Map<String, Object> donantePayload = fixture("personas/crear-persona-humana.json");
    donantePayload.put("documento", "99998888");
    donantePayload.put("nombre", "Donante");
    donantePayload.put("apellido", "Preprod");
    donantePayload.put("genero", "MUJER");
    donantePayload.put("fechaNacimiento", "1990-10-10");

    String donanteId =
        given()
            .contentType(ContentType.JSON)
            .body(donantePayload)
            .when()
            .post(DONACIONES_URL + ROUTES.getProperty("donatrack.routes.donaciones.personas-base"))
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // 2. Crear donacion usando la plantilla e inyectando el id dinamico del donante
    Map<String, Object> donacionPayload = fixture("donaciones/crear-donacion.json");
    donacionPayload.put("idDonante", donanteId);

    given()
        .contentType(ContentType.JSON)
        .body(donacionPayload)
        .when()
        .post(DONACIONES_URL + ROUTES.getProperty("donatrack.routes.donaciones.donaciones-base"))
        .then()
        .statusCode(201)
        .body("id", notNullValue());
  }
}
