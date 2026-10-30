package grupo5.tests.utils;

import grupo5.tests.client.DonacionesApiClient;
import grupo5.tests.client.IncentivosApiClient;
import grupo5.tests.client.LogisticaApiClient;
import grupo5.tests.client.NotificacionesApiClient;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

public final class PollingUtils {
  /**
   * Multiplica todos los timeouts (AP-09). Para un stack más lento que el preprod local, como el
   * híbrido o un runner cargado: {@code -Dit.timeout.factor=2}.
   */
  private static final double FACTOR_TIMEOUT =
      Double.parseDouble(System.getProperty("it.timeout.factor", "1"));

  private PollingUtils() {}

  /**
   * Consulta hasta que la condición se cumpla y devuelve la última respuesta. Si vence el tiempo,
   * falla con esa respuesta en el mensaje para diagnosticar sin mirar los logs.
   */
  public static Response esperar(
      String descripcion,
      Duration atMost,
      Supplier<Response> consulta,
      Predicate<Response> condicion) {
    AtomicReference<Response> ultima = new AtomicReference<>();
    try {
      Awaitility.await(descripcion)
          .atMost(Duration.ofMillis(Math.round(atMost.toMillis() * FACTOR_TIMEOUT)))
          .pollInterval(Duration.ofMillis(200))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = consulta.get();
                ultima.set(resp);
                return condicion.test(resp);
              });
    } catch (ConditionTimeoutException e) {
      Response r = ultima.get();
      String detalle =
          r != null ? "Status: " + r.getStatusCode() + ", Body: " + r.asString() : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando " + descripcion + ". Última respuesta: " + detalle, e);
    }
    return ultima.get();
  }

  public static void esperarReplicacionPersona(NotificacionesApiClient client, UUID personaId) {
    esperar(
        "replicación de persona " + personaId,
        Duration.ofSeconds(5),
        () -> client.obtenerPersona(personaId),
        r -> r.getStatusCode() == 200);
  }

  public static void esperarDenominacionPersona(
      NotificacionesApiClient client, UUID personaId, String denominacionEsperada) {
    esperar(
        "denominación '" + denominacionEsperada + "' en persona " + personaId,
        Duration.ofSeconds(5),
        () -> client.obtenerPersona(personaId),
        r -> r.getStatusCode() == 200 && denominacionEsperada.equals(r.path("denominacion")));
  }

  public static void esperarDonacionSegmentada(DonacionesApiClient client, UUID donacionId) {
    esperar(
        "estado SEGMENTADA para donación " + donacionId,
        Duration.ofSeconds(5),
        () -> client.obtenerDonacion(donacionId),
        r -> r.getStatusCode() == 200 && "SEGMENTADA".equals(r.path("estadoActual")));
  }

  /**
   * Espera el estado de una donación independiente; los cambios por eventos son asíncronos. La API
   * expone el estado con el nombre de su clase: {@code ListaParaEntregar}, {@code EnTraslado}.
   */
  public static void esperarDonacionIndependienteEstado(
      DonacionesApiClient client, UUID donacionIndependienteId, String estadoEsperado) {
    esperar(
        "estado " + estadoEsperado + " para donación independiente " + donacionIndependienteId,
        Duration.ofSeconds(10),
        () -> client.obtenerDonacionIndependiente(donacionIndependienteId),
        r -> r.getStatusCode() == 200 && estadoEsperado.equals(r.path("estadoActual")));
  }

  public static void esperarMinimoNotificaciones(
      NotificacionesApiClient client, UUID personaId, int minSize) {
    esperar(
        "al menos " + minSize + " notificaciones para persona " + personaId,
        Duration.ofSeconds(10),
        () -> client.obtenerNotificacionesPorPersona(personaId),
        r -> r.getStatusCode() == 200 && r.as(List.class).size() >= minSize);
  }

  /** Espera una notificación cuyo mensaje contenga el texto: identifica el evento que la generó. */
  public static void esperarNotificacionCon(
      NotificacionesApiClient client, UUID personaId, String texto) {
    esperar(
        "notificación con '" + texto + "' para persona " + personaId,
        Duration.ofSeconds(10),
        () -> client.obtenerNotificacionesPorPersona(personaId),
        r ->
            r.getStatusCode() == 200
                && r.jsonPath().getList("mensaje", String.class).stream()
                    .anyMatch(m -> m != null && m.contains(texto)));
  }

  public static void esperarTotalDonacionesExitosas(
      IncentivosApiClient client, UUID donanteId, int minExitosas) {
    esperar(
        "totalDonacionesExitosas > " + minExitosas + " para donante " + donanteId,
        Duration.ofSeconds(10),
        () -> client.obtenerMetricas(donanteId),
        r -> {
          Integer total = r.getStatusCode() == 200 ? r.path("totalDonacionesExitosas") : null;
          return total != null && total > minExitosas;
        });
  }

  public static void esperarDonanteEnIncentivos(IncentivosApiClient client, UUID donanteId) {
    esperar(
        "que el donante " + donanteId + " exista en incentivos",
        Duration.ofSeconds(10),
        () -> client.obtenerMetricas(donanteId),
        r -> r.getStatusCode() == 200);
  }

  public static void esperarBajaDonanteEnIncentivos(IncentivosApiClient client, UUID donanteId) {
    esperar(
        "baja de donante en incentivos " + donanteId,
        Duration.ofSeconds(5),
        () -> client.obtenerMetricas(donanteId),
        r -> r.getStatusCode() == 400 || r.getStatusCode() == 404);
  }

  @SuppressWarnings("unchecked")
  public static UUID esperarEntregaCreadaParaDonacion(LogisticaApiClient client, UUID donacionId) {
    // ponytail: lista todas las entregas en cada intento (DIT-05); filtrar si el stack crece mucho
    Predicate<Map<String, Object>> deLaDonacion =
        e -> donacionId.toString().equals(e.get("idDonacion"));
    Response resp =
        esperar(
            "que logistica-service cree la entrega para donación " + donacionId,
            Duration.ofSeconds(8),
            client::listarEntregas,
            r ->
                r.getStatusCode() == 200
                    && ((List<Map<String, Object>>) r.as(List.class))
                        .stream().anyMatch(deLaDonacion));
    return ((List<Map<String, Object>>) resp.as(List.class))
        .stream()
            .filter(deLaDonacion)
            .map(e -> UUID.fromString((String) e.get("id")))
            .findFirst()
            .orElseThrow();
  }

  /** Espera a que el planificador asigne la entrega a una ruta y devuelve el id de esa ruta. */
  public static UUID esperarRutaAsignada(LogisticaApiClient client, UUID entregaId) {
    Response resp =
        esperar(
            "que el planificador asigne una ruta a la entrega " + entregaId,
            Duration.ofSeconds(10),
            () -> client.obtenerEntrega(entregaId),
            r -> r.getStatusCode() == 200 && r.path("idRuta") != null);
    return UUID.fromString(resp.path("idRuta"));
  }
}
