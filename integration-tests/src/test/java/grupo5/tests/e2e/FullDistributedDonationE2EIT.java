package grupo5.tests.e2e;

import static org.hamcrest.Matchers.equalTo;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.LogisticaTestData;
import grupo5.tests.contract.ContratosAmqp;
import grupo5.tests.utils.EspiaAmqp;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Flujo distribuido por el camino de producción (F-01): Donaciones pide la entrega por AMQP,
 * Logística planifica y publica {@code ruta.*} y {@code entrega.*}, y Donaciones aplica esos
 * eventos. Ningún estado de la donación se cambia por REST.
 */
@Tag("e2e")
class FullDistributedDonationE2EIT extends BaseIT {

  /** Tipos que los dos flujos de esta clase tienen que publicar (DIT-02). */
  private static final Set<String> EVENTOS_ESPERADOS =
      Set.of(
          "donacion.asignada.v1",
          "entrega.solicitada.v1",
          "ruta.asignada",
          "ruta.iniciada",
          "donacion.en-camino.v1",
          "entrega.exitosa",
          "donacion.recibida.v1",
          "entrega.fallida",
          "donacion.entrega-fallida.v1");

  private static final Logger LOG = Logger.getLogger(FullDistributedDonationE2EIT.class.getName());

  // RM-08: el espía copia los mensajes AMQP del flujo; al final se validan contra sus schemas
  private static EspiaAmqp espia;
  private static String errorEspia;

  @BeforeAll
  static void iniciarEspiaAmqp() {
    try {
      espia = EspiaAmqp.conectar();
    } catch (IOException | TimeoutException e) {
      errorEspia = "no se pudo conectar el espía AMQP: " + e;
    }
  }

  @AfterAll
  static void validarMensajesAmqpCapturados() throws IOException {
    List<String> violaciones = new ArrayList<>();
    if (espia == null) {
      violaciones.add(errorEspia);
    } else {
      List<EspiaAmqp.Mensaje> mensajes = espia.mensajes();
      violaciones.addAll(ContratosAmqp.validar(mensajes));
      violaciones.addAll(ContratosAmqp.faltantes(mensajes, EVENTOS_ESPERADOS));
      List<String> sinEjercitar = ContratosAmqp.sinEjercitar(mensajes);
      LOG.info(() -> "[contratos-amqp] schemas evento-* sin ejercitar en el E2E: " + sinEjercitar);
      espia.close();
    }
    // Política H de D5: falla con CONTRATOS_ESTRICTO=true (post-merge y corte); si no, avisa
    ContratosAmqp.reportar(violaciones);
  }

  @Test
  void testFlujoCompletoDistribuidoConLogisticaYRabbitMQ() {
    Donante donante = crearDonante("Roberto");
    List<UUID> asignadas = donarYAprobar(donante, "arroz dist");
    List<UUID> entregas = sacarARuta(asignadas);

    // La entidad confirma la recepción en Logística: entrega.exitosa vuelve a Donaciones
    for (UUID entregaId : entregas) {
      logisticaClient.confirmarEntrega(entregaId, "ENTIDAD").then().statusCode(200);
    }
    esperarEstado(asignadas, "Entregada");

    // Efectos en Incentivos y Notificaciones, un aviso por evento
    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donante.donanteId(), 0);
    for (String aviso : List.of("fue asignada a", "salió a entregar", "recibió tu donación")) {
      PollingUtils.esperarNotificacionCon(notificacionesClient, donante.personaId(), aviso);
    }
  }

  @Test
  void testEntregaNoRecibidaLlegaComoFallidaADonaciones() {
    Donante donante = crearDonante("Lucia");
    List<UUID> asignadas = donarYAprobar(donante, "fideos falla");
    List<UUID> entregas = sacarARuta(asignadas);

    // La entidad no la recibe y no se replanifica: entrega.fallida vuelve a Donaciones
    String motivo = "Comedor cerrado " + UUID.randomUUID().toString().substring(0, 8);
    for (UUID entregaId : entregas) {
      logisticaClient
          .cambiarEstadoEntrega(entregaId, "NO_RECIBIDA", "ENTIDAD", motivo, false)
          .then()
          .statusCode(200);
    }
    esperarEstado(asignadas, "EntregaFallida");

    // Notificaciones avisa al donante con el motivo
    PollingUtils.esperarNotificacionCon(notificacionesClient, donante.personaId(), motivo);
  }

  /** Dona 10 unidades de un bien único, lo asigna a una necesidad de 10 y aprueba la propuesta. */
  private static List<UUID> donarYAprobar(Donante donante, String bien) {
    String nombreBien = TestIdGenerator.uniqueItemName(bien);
    UUID donacionId =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(nombreBien, 10)
                .conDonante(donante.donanteId())
                .conDescripcion("Donación distribuida de " + nombreBien)
                .build());
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId);
    return asignarYAprobar(crearEntidad("Comedor Esperanza"), "Necesidad para " + nombreBien, 10);
  }

  /**
   * Espera las entregas que Logística crea a partir del comando AMQP, planifica con un camión y un
   * chofer nuevos y saca las rutas a la calle. Devuelve las entregas, ya EN_TRASLADO.
   */
  private static List<UUID> sacarARuta(List<UUID> asignadas) {
    List<UUID> entregas =
        asignadas.stream()
            .map(di -> PollingUtils.esperarEntregaCreadaParaDonacion(logisticaClient, di))
            .toList();
    for (UUID entregaId : entregas) {
      logisticaClient
          .obtenerEntrega(entregaId)
          .then()
          .statusCode(200)
          .body("estadoActual", equalTo("PENDIENTE"));
    }

    logisticaClient.crearCamionOk(LogisticaTestData.camion());
    logisticaClient.crearChoferOk(LogisticaTestData.chofer());
    logisticaClient.ejecutarPlanificacion().then().statusCode(202);
    List<UUID> rutas =
        entregas.stream()
            .map(entregaId -> PollingUtils.esperarRutaAsignada(logisticaClient, entregaId))
            .distinct()
            .toList();
    // ruta.asignada lleva la donación a LISTA_PARA_ENTREGAR. Se espera antes de iniciar la ruta:
    // las dos colas no garantizan orden y ruta.iniciada sobre una donación sin asignar se pierde
    esperarEstado(asignadas, "ListaParaEntregar");
    for (UUID rutaId : rutas) {
      logisticaClient.cambiarEstadoRuta(rutaId, "EN_TRASLADO", "CHOFER").then().statusCode(200);
    }
    esperarEstado(asignadas, "EnTraslado");
    return entregas;
  }

  private static void esperarEstado(List<UUID> donacionesIndependientes, String estado) {
    donacionesIndependientes.forEach(
        di -> PollingUtils.esperarDonacionIndependienteEstado(donacionesClient, di, estado));
  }
}
