package grupo5.tests.contract;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class ContractIT extends BaseIT {

  @Test
  void testNotificacionesPersonasContract() {
    String path = ROUTES.getProperty("donatrack.routes.notificaciones.personas-base");
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"" + path + "\".put", notNullValue());
  }

  @Test
  void testIncentivosDonacionesContract() {
    String path =
        ROUTES.getProperty("donatrack.routes.incentivos.base")
            + ROUTES.getProperty("donatrack.routes.incentivos.donaciones");
    given()
        .when()
        .get(INCENTIVOS_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"" + path + "\".post", notNullValue());
  }
}
