package grupo5.tests.client;

import static io.restassured.RestAssured.given;

import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.NecesidadTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;

public class DonacionesApiClient {
  private final String baseUrl;

  public DonacionesApiClient(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public UUID crearPersonaOk(PersonaTestDTO dto) {
    return UUID.fromString(
        given()
            .contentType(ContentType.JSON)
            .body(dto)
            .when()
            .post(baseUrl + "/api/personas")
            .then()
            .statusCode(201)
            .extract()
            .path("id"));
  }

  public UUID crearDonanteOk(UUID personaId) {
    return UUID.fromString(
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("idPersona", personaId))
            .when()
            .post(baseUrl + "/api/donantes")
            .then()
            .statusCode(201)
            .extract()
            .path("idDonante"));
  }

  public UUID crearEntidadOk(UUID personaJuridicaId) {
    return UUID.fromString(
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("juridicaId", personaJuridicaId))
            .when()
            .post(baseUrl + "/api/entidades")
            .then()
            .statusCode(201)
            .extract()
            .path("id"));
  }

  public UUID crearDonacionOk(DonacionTestDTO dto) {
    return UUID.fromString(
        given()
            .contentType(ContentType.JSON)
            .body(dto)
            .when()
            .post(baseUrl + "/api/donaciones")
            .then()
            .statusCode(201)
            .extract()
            .path("id"));
  }

  public UUID crearNecesidadOk(NecesidadTestDTO dto) {
    return UUID.fromString(
        given()
            .contentType(ContentType.JSON)
            .body(dto)
            .when()
            .post(baseUrl + "/api/necesidades")
            .then()
            .statusCode(201)
            .extract()
            .path("id"));
  }

  public Response crearPersona(PersonaTestDTO dto) {
    return given().contentType(ContentType.JSON).body(dto).when().post(baseUrl + "/api/personas");
  }

  public Response obtenerPersona(UUID id) {
    return given().when().get(baseUrl + "/api/personas/" + id);
  }

  public Response actualizarPersona(UUID id, PersonaTestDTO dto) {
    return given()
        .contentType(ContentType.JSON)
        .body(dto)
        .when()
        .put(baseUrl + "/api/personas/" + id);
  }

  public Response eliminarPersona(UUID id) {
    return given().when().delete(baseUrl + "/api/personas/" + id);
  }

  public Response crearDonante(UUID personaId) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("idPersona", personaId))
        .when()
        .post(baseUrl + "/api/donantes");
  }

  public Response eliminarDonante(UUID donanteId) {
    return given().when().delete(baseUrl + "/api/donantes/" + donanteId);
  }

  public Response crearEntidad(UUID personaJuridicaId) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("juridicaId", personaJuridicaId))
        .when()
        .post(baseUrl + "/api/entidades");
  }

  public Response crearDonacion(DonacionTestDTO dto) {
    return given().contentType(ContentType.JSON).body(dto).when().post(baseUrl + "/api/donaciones");
  }

  public Response obtenerDonacion(UUID id) {
    return given().when().get(baseUrl + "/api/donaciones/" + id);
  }

  public Response obtenerSubcategorias() {
    return given().when().get(baseUrl + "/api/subcategorias");
  }

  public Response crearNecesidad(NecesidadTestDTO dto) {
    return given()
        .contentType(ContentType.JSON)
        .body(dto)
        .when()
        .post(baseUrl + "/api/necesidades");
  }

  public Response ejecutarAsignacion() {
    return given().when().post(baseUrl + "/api/asignaciones/ejecuciones");
  }

  public Response actualizarEstadoPropuesta(UUID propuestaId, String estado) {
    return given()
        .contentType(ContentType.JSON)
        .body(Map.of("estado", estado))
        .when()
        .put(baseUrl + "/api/asignaciones/propuestas/" + propuestaId + "/estado");
  }

  public Response cambiarEstadoDonacionIndependiente(UUID diId, String estado, String actor) {
    return given()
        .contentType(ContentType.JSON)
        .header("X-Actor", actor)
        .body(Map.of("estado", estado))
        .when()
        .patch(baseUrl + "/donaciones-independientes/" + diId + "/estado");
  }

  public Response obtenerOpenApi() {
    return given().when().get(baseUrl + "/v3/api-docs");
  }
}
