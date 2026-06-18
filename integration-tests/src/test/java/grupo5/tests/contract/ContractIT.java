package grupo5.tests.contract;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Test;

public class ContractIT extends BaseIT {

  @Test
  public void testNotificacionesPersonasContract() {
    // donaciones-service espera que notificaciones-service tenga PUT /api/notificaciones/personas
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"/api/notificaciones/personas\".put", notNullValue());
  }

  @Test
  public void testIncentivosDonacionesContract() {
    // donaciones-service espera que incentivos-service tenga POST /api/incentivos/donaciones
    given()
        .when()
        .get(INCENTIVOS_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .body("paths.\"/api/incentivos/donaciones\".post", notNullValue());
  }
}
