package grupo5.tests;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseIT {

  protected static final String DONACIONES_URL =
      System.getProperty("donaciones.url", "http://localhost:8080");
  protected static final String NOTIFICACIONES_URL =
      System.getProperty("notificaciones.url", "http://localhost:8081");
  protected static final String INCENTIVOS_URL =
      System.getProperty("incentivos.url", "http://localhost:8082");

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Carga un fixture JSON desde src/test/resources/fixtures/{path} como Map mutable.
   * Los campos dinamicos (ej: idDonante) se sobreescriben via map.put() en el test.
   */
  protected Map<String, Object> fixture(String path) {
    String resourcePath = "/fixtures/" + path;
    try (InputStream is = BaseIT.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Fixture no encontrado: " + resourcePath);
      }
      return MAPPER.readValue(is, new TypeReference<>() {});
    } catch (IOException e) {
      throw new RuntimeException("Error al leer fixture: " + resourcePath, e);
    }
  }

  protected String apiCrearPersonaHumana(String documento, String nombre, String email) {
    Map<String, Object> payload = fixture("personas/crear-persona-humana.json");
    payload.put("documento", documento);
    payload.put("nombre", nombre);
    if (payload.get("mediosDeContacto") instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> medios = (List<Map<String, Object>>) payload.get("mediosDeContacto");
      medios = new ArrayList<>(medios); // make sure it's mutable
      if (medios.isEmpty()) {
        Map<String, Object> medio = new HashMap<>();
        medio.put("tipo", "CORREO");
        medio.put("esPredeterminado", true);
        medio.put("direccionCorreo", email);
        medios.add(medio);
      } else {
        medios.get(0).put("direccionCorreo", email);
      }
      payload.put("mediosDeContacto", medios);
    }
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/personas")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  protected String apiCrearPersonaJuridica(String documento, String razonSocial, String email) {
    Map<String, Object> payload = fixture("personas/crear-persona-juridica.json");
    payload.put("documento", documento);
    payload.put("razonSocial", razonSocial);
    if (payload.get("mediosDeContacto") instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> medios = (List<Map<String, Object>>) payload.get("mediosDeContacto");
      medios = new ArrayList<>(medios); // make sure it's mutable
      if (medios.isEmpty()) {
        Map<String, Object> medio = new HashMap<>();
        medio.put("tipo", "CORREO");
        medio.put("esPredeterminado", true);
        medio.put("direccionCorreo", email);
        medios.add(medio);
      } else {
        medios.get(0).put("direccionCorreo", email);
      }
      payload.put("mediosDeContacto", medios);
    }
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/personas")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  protected String apiCrearDonante(String personaId) {
    esperarReplicacionPersona(personaId);
    Map<String, Object> payload = new HashMap<>();
    payload.put("idPersona", personaId);
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/donantes")
        .then()
        .statusCode(201)
        .extract()
        .path("idDonante");
  }

  protected String apiCrearEntidad(String personaJuridicaId) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("juridicaId", personaJuridicaId);
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/entidades")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  protected String apiCrearDonacion(String donanteId, String descripcion, String itemDescripcion, int cantidad) {
    Map<String, Object> payload = fixture("donaciones/crear-donacion.json");
    payload.put("idDonante", donanteId);
    payload.put("descripcion", descripcion);
    if (payload.get("items") instanceof List) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
      if (!items.isEmpty()) {
        items.get(0).put("descripcionBien", itemDescripcion);
        items.get(0).put("cantidad", cantidad);
      }
    }
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/donaciones")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  protected String apiCrearNecesidad(String entidadId, String subcategoriaId, int cantidad, String descripcion) {
    Map<String, Object> payload = fixture("necesidades/crear-necesidad.json");
    payload.put("idEntidad", entidadId);
    payload.put("idSubcategoria", subcategoriaId);
    payload.put("cantidadNecesitada", cantidad);
    payload.put("descripcion", descripcion);
    return io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(DONACIONES_URL + "/api/necesidades")
        .then()
        .statusCode(201)
        .extract()
        .path("id");
  }

  protected void apiEnviarEventoDonacionIncentivos(String donanteId, String fecha, int cantidadBienes) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("donanteId", donanteId);
    payload.put("categorias", List.of("Alimentos"));
    payload.put("cantidadBienes", cantidadBienes);
    payload.put("fecha", fecha);
    io.restassured.RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(payload)
        .when()
        .post(INCENTIVOS_URL + "/api/incentivos/donaciones")
        .then()
        .statusCode(200);
  }

  protected void esperarReplicacionPersona(String personaId) {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < 5000) {
      try {
        int statusCode = io.restassured.RestAssured.given()
            .when()
            .get(NOTIFICACIONES_URL + "/api/notificaciones/personas/" + personaId)
            .getStatusCode();
        if (statusCode == 200) {
          return;
        }
      } catch (Exception e) {
        // ignore and retry
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    throw new RuntimeException("Persona " + personaId + " no se replicó a tiempo");
  }
}
