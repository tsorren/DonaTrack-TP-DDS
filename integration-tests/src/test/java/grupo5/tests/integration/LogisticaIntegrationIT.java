package grupo5.tests.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import grupo5.tests.BaseIT;
import grupo5.tests.builders.LogisticaTestData;
import grupo5.tests.utils.PollingUtils;
import io.restassured.http.ContentType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class LogisticaIntegrationIT extends BaseIT {

  @BeforeEach
  void setUp() {
    requireServiceAvailable(LOGISTICA_URL, "logistica-service");
  }

  @Test
  void testCrearCamionYVerificarPersistencia() {
    Map<String, Object> camion = LogisticaTestData.camion();

    UUID camionId = logisticaClient.crearCamionOk(camion);
    assertNotNull(camionId, "El camión debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerCamion(camionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(camionId.toString()))
        .body("patente", equalTo(camion.get("patente")))
        .body("capacidadVolumen", equalTo(45.0f))
        .body("capacidadKG", equalTo(3500.0f))
        .body("estado", equalTo("DISPONIBLE"));

    logisticaClient
        .listarCamiones()
        .then()
        .statusCode(200)
        .body("id", hasItem(camionId.toString()));
  }

  @Test
  void testCrearChoferYVerificarPersistencia() {
    Map<String, Object> chofer = LogisticaTestData.chofer();

    UUID choferId = logisticaClient.crearChoferOk(chofer);
    assertNotNull(choferId, "El chofer debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerChofer(choferId)
        .then()
        .statusCode(200)
        .body("id", equalTo(choferId.toString()))
        .body("nombre", equalTo(chofer.get("nombre")))
        .body("apellido", equalTo(chofer.get("apellido")))
        .body("licencia", equalTo(chofer.get("licencia")))
        .body("estado", equalTo("DISPONIBLE"));

    logisticaClient
        .listarChoferes()
        .then()
        .statusCode(200)
        .body("id", hasItem(choferId.toString()));
  }

  /**
   * La entrega no tiene donación detrás, así que se lleva hasta ENTREGADA: una entrega ajena que
   * quedara pendiente caería en la ruta del E2E, y Donaciones descarta el {@code ruta.iniciada}
   * entero si menciona una donación que no conoce.
   */
  @Test
  void testEntregaNoRecibidaSeReplanificaYSeEntrega() {
    // Contrato vivo del POST /api/entregas (antes en ContractIT)
    UUID entregaId =
        UUID.fromString(
            given()
                .filter(new OpenApiValidationFilter(contrato("openapi-logistica.yaml")))
                .contentType(ContentType.JSON)
                .body(LogisticaTestData.entregaSinDonacion())
                .when()
                .post(LOGISTICA_URL + "/api/entregas")
                .then()
                .statusCode(201)
                .extract()
                .path("id"));

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("id", equalTo(entregaId.toString()))
        .body("estadoActual", equalTo("PENDIENTE"))
        .body("destino.calle", equalTo("Av. San Martin"))
        .body("destino.altura", equalTo(1234));

    planificarYSalir(entregaId);
    logisticaClient
        .cambiarEstadoEntrega(entregaId, "NO_RECIBIDA", "ENTIDAD", "Nadie atendió el timbre", true)
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("NO_RECIBIDA"));
    logisticaClient
        .cambiarEstadoEntrega(entregaId, "REVISION", "SUPERVISOR")
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("REVISION"));
    logisticaClient
        .cambiarEstadoEntrega(entregaId, "PENDIENTE", "SUPERVISOR")
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("PENDIENTE"));

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("PENDIENTE"))
        .body("idRuta", nullValue())
        .body(
            "historialEstado.estadoNuevo",
            contains("EN_TRASLADO", "NO_RECIBIDA", "REVISION", "PENDIENTE"));

    // Replanificada (vuelve a tener ruta), sale de nuevo y se entrega
    planificarYSalir(entregaId);
    logisticaClient
        .confirmarEntrega(entregaId, "ENTIDAD")
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("ENTREGADA"));
  }

  /** Planifica con un camión y un chofer nuevos y saca a la calle la ruta de la entrega. */
  private static void planificarYSalir(UUID entregaId) {
    logisticaClient.crearCamionOk(LogisticaTestData.camion());
    logisticaClient.crearChoferOk(LogisticaTestData.chofer());
    logisticaClient.ejecutarPlanificacion().then().statusCode(202);
    UUID rutaId = PollingUtils.esperarRutaAsignada(logisticaClient, entregaId);
    logisticaClient.cambiarEstadoRuta(rutaId, "EN_TRASLADO", "CHOFER").then().statusCode(200);
  }
}
