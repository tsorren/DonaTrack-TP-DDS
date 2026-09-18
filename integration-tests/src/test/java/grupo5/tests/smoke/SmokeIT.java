package grupo5.tests.smoke;

import static org.hamcrest.Matchers.containsString;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("smoke")
class SmokeIT extends BaseIT {

  @Test
  void testDonacionesServiceUp() {
    donacionesClient.obtenerOpenApi().then().statusCode(200).body(containsString("openapi"));
  }

  @Test
  void testNotificacionesServiceUp() {
    notificacionesClient.obtenerOpenApi().then().statusCode(200).body(containsString("openapi"));
  }

  @Test
  void testIncentivosServiceUp() {
    incentivosClient.obtenerOpenApi().then().statusCode(200).body(containsString("openapi"));
  }

  @Test
  void testLogisticaServiceUp() {
    logisticaClient.obtenerOpenApi().then().statusCode(200).body(containsString("openapi"));
  }
}
