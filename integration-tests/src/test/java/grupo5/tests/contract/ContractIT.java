package grupo5.tests.contract;

import static org.hamcrest.Matchers.notNullValue;

import grupo5.tests.BaseIT;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("contract")
class ContractIT extends BaseIT {

  @Test
  void testNotificacionesPersonasContract() {
    // donaciones-service espera que notificaciones-service tenga PUT /api/notificaciones/personas
    notificacionesClient
        .obtenerOpenApi()
        .then()
        .statusCode(200)
        .body("paths.\"/api/notificaciones/personas\".put", notNullValue());
  }

  @Test
  void testIncentivosDonacionesContract() {
    // donaciones-service espera que incentivos-service tenga POST /api/incentivos/donaciones
    incentivosClient
        .obtenerOpenApi()
        .then()
        .statusCode(200)
        .body("paths.\"/api/incentivos/donaciones\".post", notNullValue());
  }

  @Test
  void testLogisticaEntregasContract() {
    // donaciones-service espera que logistica-service tenga POST /api/entregas
    logisticaClient
        .obtenerOpenApi()
        .then()
        .statusCode(200)
        .body("paths.\"/api/entregas\".post", notNullValue());
  }
}
