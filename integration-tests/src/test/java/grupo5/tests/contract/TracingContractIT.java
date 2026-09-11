package grupo5.tests.contract;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class TracingContractIT extends BaseIT {

  @Test
  void testTraceIdHeaderGeneratedWhenMissing() {
    given()
        .when()
        .get(DONACIONES_URL + "/v3/api-docs")
        .then()
        .statusCode(200)
        .header("X-Trace-Id", notNullValue())
        .header("X-Trace-Id", not(emptyOrNullString()));
  }

  @Test
  void testTraceIdHeaderPresentInAllServices() {
    for (String url :
        new String[] {DONACIONES_URL, NOTIFICACIONES_URL, INCENTIVOS_URL, LOGISTICA_URL}) {
      given()
          .when()
          .get(url + "/v3/api-docs")
          .then()
          .statusCode(200)
          .header("X-Trace-Id", notNullValue())
          .header("X-Trace-Id", not(emptyOrNullString()));
    }
  }
}
