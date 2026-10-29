package grupo5.tests;

import grupo5.tests.client.DonacionesApiClient;
import grupo5.tests.client.IncentivosApiClient;
import grupo5.tests.client.LogisticaApiClient;
import grupo5.tests.client.NotificacionesApiClient;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseIT {

  protected static final String DONACIONES_URL =
      System.getProperty("donaciones.url", "http://localhost:8080");
  protected static final String NOTIFICACIONES_URL =
      System.getProperty("notificaciones.url", "http://localhost:8081");
  protected static final String INCENTIVOS_URL =
      System.getProperty("incentivos.url", "http://localhost:8082");
  protected static final String LOGISTICA_URL =
      System.getProperty("logistica.url", "http://localhost:8083");

  /**
   * Gate cerrado ante la duda (RM-02): con {@code PREPROD_STRICT=true} (variable de entorno o
   * {@code -Dpreprod.strict=true}) un servicio caído hace fallar el test en lugar de saltearlo.
   */
  protected static final boolean PREPROD_STRICT =
      Boolean.parseBoolean(
          System.getProperty("preprod.strict", System.getenv().getOrDefault("PREPROD_STRICT", "")));

  protected static DonacionesApiClient donacionesClient;
  protected static NotificacionesApiClient notificacionesClient;
  protected static IncentivosApiClient incentivosClient;
  protected static LogisticaApiClient logisticaClient;

  @BeforeAll
  static void setUpBase() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    donacionesClient = new DonacionesApiClient(DONACIONES_URL);
    notificacionesClient = new NotificacionesApiClient(NOTIFICACIONES_URL);
    incentivosClient = new IncentivosApiClient(INCENTIVOS_URL);
    logisticaClient = new LogisticaApiClient(LOGISTICA_URL);
  }

  /** Saltea el test si el servicio no responde; en modo estricto, lo hace fallar. */
  protected static void requireServiceAvailable(String url, String servicio) {
    String motivo = servicio + " no disponible en " + url;
    if (PREPROD_STRICT) {
      Assertions.assertTrue(isServiceAvailable(url), motivo + " (PREPROD_STRICT=true)");
    } else {
      Assumptions.assumeTrue(isServiceAvailable(url), motivo);
    }
  }

  protected static boolean isServiceAvailable(String url) {
    try {
      java.net.URI uri = java.net.URI.create(url + "/v3/api-docs");
      java.net.http.HttpClient client =
          java.net.http.HttpClient.newBuilder()
              .connectTimeout(java.time.Duration.ofMillis(500))
              .build();
      java.net.http.HttpRequest request =
          java.net.http.HttpRequest.newBuilder(uri)
              .timeout(java.time.Duration.ofMillis(500))
              .GET()
              .build();
      java.net.http.HttpResponse<Void> response =
          client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    } catch (Exception e) {
      return false;
    }
  }
}
