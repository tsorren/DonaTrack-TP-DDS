package grupo5.tests.integration;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class PersonIntegrationIT extends BaseIT {

  @Test
  void testCrearPersonaReplicada() {
    Map<String, Object> payload = fixture("personas/crear-persona-humana.json");

    given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + ROUTES.getProperty("donatrack.routes.donaciones.personas-base"))
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("nombre", equalTo("Preprod"));
  }
}
