package grupo5.tests.smoke;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Test;

public class SmokeIT extends BaseIT {

  @Test
  public void testDonacionesServiceUp() {
    given()
        .when()
        .get(DONACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body(containsString("openapi"));
  }

  @Test
  public void testNotificacionesServiceUp() {
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body(containsString("openapi"));
  }

  @Test
  public void testIncentivosServiceUp() {
    given()
        .when()
        .get(INCENTIVOS_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body(containsString("openapi"));
  }
}
