package grupo5.tests.client;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IncentivosApiClient {
  private final String baseUrl;

  public IncentivosApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public Response obtenerMetricas(UUID donanteId) {
    return given().when().get(baseUrl + "/api/incentivos/donantes/" + donanteId + "/metricas");
  }

  public Response enviarEventoDonacion(UUID donanteId, String fecha, int cantidadBienes) {
    Map<String, Object> payload =
        Map.of(
            "donanteId",
            donanteId,
            "categorias",
            List.of("Alimentos"),
            "cantidadBienes",
            cantidadBienes,
            "fecha",
            fecha);
    return given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post(baseUrl + "/api/incentivos/donaciones");
  }

  public Response calcularRanking(String periodo) {
    String endpoint =
        baseUrl
            + "/api/incentivos/ranking/calcular"
            + (periodo != null ? "?periodo=" + periodo : "");
    return given().when().post(endpoint);
  }

  public Response obtenerUltimoRanking() {
    return given().when().get(baseUrl + "/api/incentivos/ranking/ultimo");
  }

  public Response obtenerHistorialRanking() {
    return given().when().get(baseUrl + "/api/incentivos/ranking/historial");
  }

  public Response obtenerInsignias(UUID donanteId, Boolean soloVisibles) {
    String endpoint =
        baseUrl
            + "/api/incentivos/donantes/"
            + donanteId
            + "/insignias"
            + (soloVisibles != null ? "?soloVisibles=" + soloVisibles : "");
    return given().when().get(endpoint);
  }

  public Response cambiarVisibilidadInsignia(
      UUID donanteId, String nombreInsignia, boolean visible) {
    return given()
        .when()
        .put(
            baseUrl
                + "/api/incentivos/donantes/"
                + donanteId
                + "/insignias/"
                + nombreInsignia
                + "/visibilidad?visible="
                + visible);
  }

  public Response obtenerOpenApi() {
    return given().when().get(baseUrl + "/v3/api-docs");
  }
}
