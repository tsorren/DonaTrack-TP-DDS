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
    return cambiarEstadoEntrega(entregaId, nuevoEstado, actor, null, null);
  }

  /** Variante para NO_RECIBIDA, que exige justificación e indicar si se puede replanificar. */
  public Response cambiarEstadoEntrega(
      UUID entregaId,
      String nuevoEstado,
      String actor,
      String justificacion,
      Boolean replanificable) {
    Map<String, Object> body = new HashMap<>();
    body.put("estado", nuevoEstado);
    body.put("actor", actor != null ? actor : "TRANSPORTISTA");
    body.put("justificacion", justificacion);
    body.put("replanificable", replanificable);
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .patch(baseUrl + "/api/entregas/" + entregaId + "/estado");
  }

  public Response ejecutarPlanificacion() {
    return given().when().post(baseUrl + "/api/logistica/planificaciones/ejecuciones");
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

  public Response crearCamion(Object body) {
    return given().contentType(ContentType.JSON).body(body).when().post(baseUrl + "/api/camiones");
  }

  public UUID crearCamionOk(Object body) {
    return UUID.fromString(crearCamion(body).then().statusCode(201).extract().path("id"));
  }

  public Response obtenerCamion(UUID camionId) {
    return given().when().get(baseUrl + "/api/camiones/" + camionId);
  }

  public Response listarChoferes() {
    return given().when().get(baseUrl + "/api/choferes");
  }

  public Response crearChofer(Object body) {
    return given().contentType(ContentType.JSON).body(body).when().post(baseUrl + "/api/choferes");
  }

  public UUID crearChoferOk(Object body) {
    return UUID.fromString(crearChofer(body).then().statusCode(201).extract().path("id"));
  }

  public Response obtenerChofer(UUID choferId) {
    return given().when().get(baseUrl + "/api/choferes/" + choferId);
  }

  public Response crearEntrega(Object body) {
    return given().contentType(ContentType.JSON).body(body).when().post(baseUrl + "/api/entregas");
  }

  public UUID crearEntregaOk(Object body) {
    return UUID.fromString(crearEntrega(body).then().statusCode(201).extract().path("id"));
  }
}
