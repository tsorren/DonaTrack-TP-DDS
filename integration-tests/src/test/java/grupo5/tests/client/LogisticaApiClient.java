package grupo5.tests.client;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogisticaApiClient {
  private final String baseUrl;

  public LogisticaApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Response obtenerOpenApi() {
    return given().when().get(baseUrl + "/v3/api-docs");
  }

  public Response listarEntregas() {
    return given().when().get(baseUrl + "/api/entregas");
  }

  public Response obtenerEntrega(UUID entregaId) {
    return given().when().get(baseUrl + "/api/entregas/" + entregaId);
  }

  public Response cambiarEstadoEntrega(UUID entregaId, String nuevoEstado, String actor) {
    Map<String, Object> body = new HashMap<>();
    body.put("estado", nuevoEstado);
    body.put("actor", actor != null ? actor : "TRANSPORTISTA");
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(baseUrl + "/api/entregas/" + entregaId + "/estado");
  }

  public Response listarRutas() {
    return given().when().get(baseUrl + "/api/rutas");
  }

  public Response obtenerRuta(UUID rutaId) {
    return given().when().get(baseUrl + "/api/rutas/" + rutaId);
  }

  public Response agregarEntregaARuta(UUID rutaId, UUID entregaId) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("entregaId", entregaId))
        .when()
        .post(baseUrl + "/api/rutas/" + rutaId + "/entregas");
  }

  public Response cambiarEstadoRuta(UUID rutaId, String estado, String actor) {
    Map<String, Object> body = new HashMap<>();
    body.put("estado", estado);
    body.put("actor", actor != null ? actor : "LOGISTICA");
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(baseUrl + "/api/rutas/" + rutaId + "/estado");
  }

  public Response listarCamiones() {
    return given().when().get(baseUrl + "/api/camiones");
  }

  public Response listarChoferes() {
    return given().when().get(baseUrl + "/api/choferes");
  }
}
