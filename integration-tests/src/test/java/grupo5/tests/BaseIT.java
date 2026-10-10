package grupo5.tests;

import grupo5.tests.client.DonacionesApiClient;
import grupo5.tests.client.IncentivosApiClient;
import grupo5.tests.client.LogisticaApiClient;
import grupo5.tests.client.NotificacionesApiClient;
import io.restassured.RestAssured;
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
