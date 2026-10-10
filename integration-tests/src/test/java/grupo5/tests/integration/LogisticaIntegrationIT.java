package grupo5.tests.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.tests.BaseIT;
import grupo5.tests.utils.PollingUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class LogisticaIntegrationIT extends BaseIT {

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(
        isServiceAvailable(LOGISTICA_URL), "logistica-service no disponible en " + LOGISTICA_URL);
  }

  @Test
  void testCrearCamionYVerificarPersistencia() {
    Map<String, Object> camion = nuevoCamion();

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
    Map<String, Object> chofer = nuevoChofer();

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

  @Test
  void testEntregaNoRecibidaVuelveAPendienteSinRuta() {
    UUID entregaId = logisticaClient.crearEntregaOk(nuevaEntrega());
    assertNotNull(entregaId, "La entrega debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("id", equalTo(entregaId.toString()))
        .body("estadoActual", equalTo("PENDIENTE"))
        .body("destino.calle", equalTo("Av. San Martin"))
        .body("destino.altura", equalTo(1234));

    logisticaClient.crearCamionOk(nuevoCamion());
    logisticaClient.crearChoferOk(nuevoChofer());
    logisticaClient.ejecutarPlanificacion().then().statusCode(202);
    UUID rutaId = PollingUtils.esperarRutaAsignada(logisticaClient, entregaId);

    logisticaClient.cambiarEstadoRuta(rutaId, "EN_TRASLADO", "CHOFER").then().statusCode(200);
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
  }

  private static Map<String, Object> nuevoCamion() {
    return Map.of(
        "patente", "AB" + ThreadLocalRandom.current().nextInt(100, 999) + "CD",
        "capacidadVolumen", 45.0f,
        "altura", 2.8f,
        "capacidadKG", 3500.0f);
  }

  private static Map<String, Object> nuevoChofer() {
    return Map.of(
        "nombre",
        "Roberto",
        "apellido",
        "Gomez",
        "licencia",
        "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
        "telefonoContacto",
        "+5411" + ThreadLocalRandom.current().nextInt(10000000, 99999999));
  }

  private static Map<String, Object> nuevaEntrega() {
    return Map.of(
        "idDonacion", UUID.randomUUID(),
        "idBeneficiaria", UUID.randomUUID(),
        "destino",
            Map.of(
                "calle", "Av. San Martin",
                "altura", 1234,
                "codigoPostal", "1416",
                "localidad", "CABA",
                "provincia", "Buenos Aires",
                "pais", "Argentina"),
        "pesoTotalKG", 20.0f,
        "volumenTotalM3", 2.5f);
  }
}
