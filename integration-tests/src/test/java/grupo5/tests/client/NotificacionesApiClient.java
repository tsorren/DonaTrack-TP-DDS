package grupo5.tests.client;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import java.util.UUID;

public class NotificacionesApiClient {
  private final String baseUrl;

  public NotificacionesApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Response obtenerPersona(UUID personaId) {
    return given().when().get(baseUrl + "/api/notificaciones/personas/" + personaId);
  }

  public Response obtenerNotificacionesPorPersona(UUID personaId) {
    return given().when().get(baseUrl + "/notificaciones/persona/" + personaId);
  }

  public Response obtenerOpenApi() {
    return given().when().get(baseUrl + "/v3/api-docs");
  }
}
