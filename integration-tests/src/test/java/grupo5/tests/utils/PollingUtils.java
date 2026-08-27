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
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;

public final class PollingUtils {
  private PollingUtils() {}

  public static void esperarReplicacionPersona(NotificacionesApiClient client, UUID personaId) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await("Esperar replicación de persona " + personaId)
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(100))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerPersona(personaId);
                lastResponse.set(resp);
                return resp.getStatusCode() == 200;
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando replicación de persona "
              + personaId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static void esperarDenominacionPersona(
      NotificacionesApiClient client, UUID personaId, String denominacionEsperada) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await(
              "Esperar denominación '" + denominacionEsperada + "' en persona " + personaId)
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(100))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerPersona(personaId);
                lastResponse.set(resp);
                return resp.getStatusCode() == 200
                    && denominacionEsperada.equals(resp.path("denominacion"));
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando denominación '"
              + denominacionEsperada
              + "' en persona "
              + personaId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static void esperarDonacionSegmentada(DonacionesApiClient client, UUID donacionId) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await("Esperar estado SEGMENTADA para donación " + donacionId)
          .atMost(Duration.ofSeconds(5))
          .pollInterval(Duration.ofMillis(150))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerDonacion(donacionId);
                lastResponse.set(resp);
                return resp.getStatusCode() == 200
                    && "SEGMENTADA".equals(resp.path("estadoActual"));
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando estado SEGMENTADA para donación "
              + donacionId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static void esperarDonacionEstado(
      DonacionesApiClient client, UUID donacionId, String estadoEsperado) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await("Esperar estado " + estadoEsperado + " para donación " + donacionId)
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(200))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerDonacion(donacionId);
                lastResponse.set(resp);
                return resp.getStatusCode() == 200
                    && estadoEsperado.equals(resp.path("estadoActual"));
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando estado "
              + estadoEsperado
              + " para donación "
              + donacionId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static void esperarMinimoNotificaciones(
      NotificacionesApiClient client, UUID personaId, int minSize) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await("Esperar al menos " + minSize + " notificaciones para persona " + personaId)
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(300))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerNotificacionesPorPersona(personaId);
                lastResponse.set(resp);
                if (resp.getStatusCode() != 200) return false;
                List<?> lista = resp.as(List.class);
                return lista != null && lista.size() >= minSize;
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando notificaciones para persona "
              + personaId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static void esperarTotalDonacionesExitosas(
      IncentivosApiClient client, UUID donanteId, int minExitosas) {
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await(
              "Esperar totalDonacionesExitosas > " + minExitosas + " para donante " + donanteId)
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(300))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.obtenerMetricas(donanteId);
                lastResponse.set(resp);
                if (resp.getStatusCode() != 200) return false;
                Integer total = resp.path("totalDonacionesExitosas");
                return total != null && total > minExitosas;
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando métricas en incentivos para donante "
              + donanteId
              + ". Última respuesta: "
              + details,
          e);
    }
  }

  public static UUID esperarEntregaCreadaParaDonacion(LogisticaApiClient client, UUID donacionId) {
    AtomicReference<UUID> entregaIdRef = new AtomicReference<>();
    AtomicReference<Response> lastResponse = new AtomicReference<>();
    try {
      Awaitility.await("Esperar que logistica-service cree la entrega para donación " + donacionId)
          .atMost(Duration.ofSeconds(8))
          .pollInterval(Duration.ofMillis(300))
          .ignoreExceptions()
          .until(
              () -> {
                Response resp = client.listarEntregas();
                lastResponse.set(resp);
                if (resp.getStatusCode() != 200) return false;
                List<Map<String, Object>> entregas = resp.as(List.class);
                if (entregas == null) return false;
                for (Map<String, Object> e : entregas) {
                  if (donacionId.toString().equals(e.get("idDonacion"))) {
                    entregaIdRef.set(UUID.fromString((String) e.get("id")));
                    return true;
                  }
                }
                return false;
              });
    } catch (ConditionTimeoutException e) {
      Response r = lastResponse.get();
      String details =
          (r != null)
              ? "Status: " + r.getStatusCode() + ", Body: " + r.asString()
              : "Sin respuesta";
      throw new AssertionError(
          "Timeout esperando entrega creada en logistica para donación "
              + donacionId
              + ". Última respuesta: "
              + details,
          e);
    }
    return entregaIdRef.get();
  }
}
