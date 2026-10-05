package grupo5.tests.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.tests.BaseIT;
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
    String patente = "AB" + ThreadLocalRandom.current().nextInt(100, 999) + "CD";
    Map<String, Object> body =
        Map.of(
            "patente", patente,
            "capacidadVolumen", 45.0f,
            "altura", 2.8f,
            "capacidadKG", 3500.0f);

    UUID camionId = logisticaClient.crearCamionOk(body);
    assertNotNull(camionId, "El camión debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerCamion(camionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(camionId.toString()))
        .body("patente", equalTo(patente))
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
    String nombre = "Roberto";
    String apellido = "Gomez";
    String licencia = "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    String telefono = "+5411" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    Map<String, Object> body =
        Map.of(
            "nombre", nombre,
            "apellido", apellido,
            "licencia", licencia,
            "telefonoContacto", telefono);

    UUID choferId = logisticaClient.crearChoferOk(body);
    assertNotNull(choferId, "El chofer debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerChofer(choferId)
        .then()
        .statusCode(200)
        .body("id", equalTo(choferId.toString()))
        .body("nombre", equalTo(nombre))
        .body("apellido", equalTo(apellido))
        .body("licencia", equalTo(licencia))
        .body("estado", equalTo("DISPONIBLE"));

    logisticaClient
        .listarChoferes()
        .then()
        .statusCode(200)
        .body("id", hasItem(choferId.toString()));
  }

  @Test
  void testCrearEntregaYActualizarEstadoPersistido() {
    UUID donacionId = UUID.randomUUID();
    UUID beneficiariaId = UUID.randomUUID();
    Map<String, Object> destino =
        Map.of(
            "calle", "Av. San Martin",
            "altura", 1234,
            "codigoPostal", "1416",
            "localidad", "CABA",
            "provincia", "Buenos Aires",
            "pais", "Argentina");
    Map<String, Object> body =
        Map.of(
            "idDonacion", donacionId,
            "idBeneficiaria", beneficiariaId,
            "destino", destino,
            "pesoTotalKG", 20.0f,
            "volumenTotalM3", 2.5f);

    UUID entregaId = logisticaClient.crearEntregaOk(body);
    assertNotNull(entregaId, "La entrega debería haberse creado con un ID asignado");

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("id", equalTo(entregaId.toString()))
        .body("estadoActual", equalTo("PENDIENTE"))
        .body("destino.calle", equalTo("Av. San Martin"))
        .body("destino.altura", equalTo(1234));

    logisticaClient
        .cambiarEstadoEntrega(entregaId, "REVISION", "SUPERVISOR")
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("REVISION"));

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("REVISION"))
        .body("historialEstado", hasSize(greaterThanOrEqualTo(2)));
  }
}
