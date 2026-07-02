package grupo5.tests.contract;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

class ContractIT extends BaseIT {

  @Test
  void testNotificacionesPersonasContract() {
    // donaciones-service espera que notificaciones-service tenga PUT /api/notificaciones/personas
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"/api/notificaciones/personas\".put", notNullValue());
  }

  @Test
  void testIncentivosDonacionesContract() {
    // donaciones-service espera que incentivos-service tenga POST /api/incentivos/donaciones
    given()
        .when()
        .get(INCENTIVOS_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"/api/incentivos/donaciones\".post", notNullValue());
  }
}
