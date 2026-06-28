package grupo5.tests.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import java.util.*;
import org.junit.jupiter.api.Test;

public class CrossServiceCommunicationIT extends BaseIT {

  // Polling helper to wait for async events with a timeout
  private void esperarAsync() {
    try {
      Thread.sleep(1500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void esperarHastaTotalDonacionesExitosas(String donanteId, int min, long timeoutMs) {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeoutMs) {
      try {
        Integer total =
            given()
                .when()
                .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donanteId + "/metricas")
                .then()
                .statusCode(200)
                .extract()
                .path("totalDonacionesExitosas");
        if (total != null && total > min) {
          return;
        }
      } catch (Exception e) {
        // ignore and retry until timeout
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    fail("Timeout waiting for totalDonacionesExitosas > " + min);
  }

  private void esperarHastaNotificaciones(String personaId, int minSize, long timeoutMs) {
    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < timeoutMs) {
      try {
        List<?> resp =
            given()
                .when()
                .get(NOTIFICACIONES_URL + "/notificaciones/persona/" + personaId)
                .then()
                .statusCode(200)
                .extract()
                .as(List.class);
        if (resp != null && resp.size() >= minSize) {
          return;
        }
      } catch (Exception e) {
        // ignore and retry
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    fail("Timeout waiting for notifications size >= " + minSize);
  }

  @Test
  public void testPersonaReplicationLifecycle() {
    // 1. Create a persona in donaciones-service
    Map<String, Object> personaPayload = fixture("personas/crear-persona-humana.json");
    personaPayload.put("documento", "55554444");
    personaPayload.put("nombre", "Juan");
    personaPayload.put("apellido", "Perez");

    String personaId =
        given()
            .contentType(ContentType.JSON)
            .body(personaPayload)
            .when()
            .post(DONACIONES_URL + "/api/personas")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    assertNotNull(personaId);

    // 2. Verify replication in notificaciones-service
    esperarAsync();
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/api/notificaciones/personas/" + personaId)
        .then()
        .statusCode(200)
        .body("id", equalTo(personaId))
        .body("denominacion", equalTo("Juan Perez"))
        .body("tipoPersona", equalTo("HUMANA"))
        .body("mediosDeContacto[0].direccionCorreo", equalTo("juan.perez@example.com"));

    // 3. Update the persona in donaciones-service
    personaPayload.put("nombre", "Juan Carlos");
    given()
        .contentType(ContentType.JSON)
        .body(personaPayload)
        .when()
        .put(DONACIONES_URL + "/api/personas/" + personaId)
        .then()
        .statusCode(200)
        .body("nombre", equalTo("Juan Carlos"));

    // 4. Verify updated replica in notificaciones-service
    esperarAsync();
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/api/notificaciones/personas/" + personaId)
        .then()
        .statusCode(200)
        .body("denominacion", equalTo("Juan Carlos Perez"));

    // 5. Delete (anonimyze) the persona in donaciones-service
    given()
        .when()
        .delete(DONACIONES_URL + "/api/personas/" + personaId)
        .then()
        .statusCode(204);

    // 6. Verify anonimized replica in notificaciones-service
    esperarAsync();
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/api/notificaciones/personas/" + personaId)
        .then()
        .statusCode(200)
        .body("denominacion", equalTo("ANONIMIZADO"))
        .body("mediosDeContacto[0].direccionCorreo", equalTo("ANONIMIZADO"));
  }

  @Test
  public void testDonanteRegistrationAndWelcomeNotification() {
    // 1. Create a persona
    String personaId = apiCrearPersonaHumana("11112222", "Maria", "maria.gomez@example.com");

    // 2. Register the persona as a donor
    String donorId = apiCrearDonante(personaId);

    assertNotNull(donorId);

    // 3. Verify donor metrics exist in incentivos-service
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(200)
        .body("totalDonacionesExitosas", equalTo(0))
        .body("categoria", equalTo("COLABORADOR"));

    // 4. Verify welcome notification exists in notificaciones-service
    esperarHastaNotificaciones(personaId, 1, 10000);
    given()
        .when()
        .get(NOTIFICACIONES_URL + "/notificaciones/persona/" + personaId)
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("mensaje[0]", containsString("Usuario:"));
  }

  @Test
  public void testDonanteBaja() {
    // 1. Create a persona
    String personaId = apiCrearPersonaHumana("33332222", "Pedro", "pedro.sosa@example.com");

    // 2. Register as a donor
    String donorId = apiCrearDonante(personaId);

    // Verify profile is created in incentives
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(200);

    // 3. Perform delete (baja) of donor in donaciones-service
    given().when().delete(DONACIONES_URL + "/api/donantes/" + donorId).then().statusCode(204);

    // 4. Verify profile is removed in incentives
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(anyOf(equalTo(400), equalTo(404)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testE2EDonationFlowAndSideEffects() {
    // 1. Create Donante Persona & Donor
    String donantePersonaId = apiCrearPersonaHumana("88887777", "Ana", "ana.lopez@example.com");
    String donorId = apiCrearDonante(donantePersonaId);

    // 2. Create Beneficiary Persona (Juridica) & EntidadBeneficiaria
    String benefPersonaId = apiCrearPersonaJuridica("30-11112222-3", "Comedor Solidario", "comedor@example.com");
    String entidadId = apiCrearEntidad(benefPersonaId);

    // 3. Load Donation (using subcategory arroz)
    String donacionId = apiCrearDonacion(donantePersonaId, "Donación de arroz preprod", "arroz", 10);
    assertNotNull(donacionId);
    esperarAsync(); // wait for async processing of donation (normalization and segmentation)

    // 4. Fetch subcategories and find "No Perecederos" ID
    String subcategoryId =
        given()
            .when()
            .get(DONACIONES_URL + "/api/necesidades/subcategorias")
            .then()
            .statusCode(200)
            .extract()
            .path("find { it.nombre == 'No Perecederos' }.id");

    assertNotNull(subcategoryId);

    // 5. Load Necessity
    apiCrearNecesidad(entidadId, subcategoryId, 10, "Necesidad de arroz preprod");

    // 6. Execute Matching
    String responseBody =
        given()
            .when()
            .post(DONACIONES_URL + "/api/asignaciones/ejecuciones")
            .then()
            .statusCode(201)
            .extract()
            .body()
            .asString();
    System.out.println("JSON PROPUESTAS RESPONSE: " + responseBody);

    List<Map<String, Object>> propuestas = io.restassured.path.json.JsonPath.from(responseBody).get();
    assertFalse(propuestas.isEmpty());

    // Find the proposal matching our necessity description
    Map<String, Object> propuesta = null;
    for (Map<String, Object> p : propuestas) {
      Map<String, Object> nec = (Map<String, Object>) p.get("necesidadQueSatisface");
      if (nec != null && nec.get("descripcion").equals("Necesidad de arroz preprod")) {
        propuesta = p;
        break;
      }
    }

    assertNotNull(propuesta, "Propuesta no encontrada para la necesidad de arroz");
    String propuestaId = (String) propuesta.get("id");

    // Get the DonacionIndependiente ID from posiblesFragmentaciones
    List<Map<String, Object>> frags =
        (List<Map<String, Object>>) propuesta.get("posiblesFragmentaciones");
    assertNotNull(frags);
    assertFalse(frags.isEmpty());
    Map<String, Object> frag = frags.get(0);
    Map<String, Object> donacionInd = (Map<String, Object>) frag.get("donacionOriginal");
    String donacionIndependienteId = (String) donacionInd.get("id");
    assertNotNull(donacionIndependienteId);

    // 7. Approve Propuesta
    Map<String, Object> approveBody = new HashMap<>();
    approveBody.put("estado", "APROBADA");
    given()
        .contentType(ContentType.JSON)
        .body(approveBody)
        .when()
        .put(DONACIONES_URL + "/api/asignaciones/propuestas/" + propuestaId + "/estado")
        .then()
        .statusCode(200);

    // 8. Transition DonacionIndependiente States
    // Transition to EN_TRASLADO
    Map<String, Object> patchBody = new HashMap<>();
    patchBody.put("estado", "EN_TRASLADO");
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .header("X-Actor", "TRANSPORTISTA")
        .when()
        .patch(DONACIONES_URL + "/donaciones-independientes/" + donacionIndependienteId + "/estado")
        .then()
        .statusCode(200);

    // Transition to LISTA_PARA_ENTREGAR
    patchBody.put("estado", "LISTA_PARA_ENTREGAR");
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .header("X-Actor", "TRANSPORTISTA")
        .when()
        .patch(DONACIONES_URL + "/donaciones-independientes/" + donacionIndependienteId + "/estado")
        .then()
        .statusCode(200);

    // Transition to ENTREGADA
    patchBody.put("estado", "ENTREGADA");
    given()
        .contentType(ContentType.JSON)
        .body(patchBody)
        .header("X-Actor", "TRANSPORTISTA")
        .when()
        .patch(DONACIONES_URL + "/donaciones-independientes/" + donacionIndependienteId + "/estado")
        .then()
        .statusCode(200);

    // 9. Verify Side-Effects in incentivos-service (puntos > 0)
    esperarHastaTotalDonacionesExitosas(donorId, 0, 10000);

    // 10. Verify Side-Effects in notificaciones-service
    // Donante notifications: welcome (registrado), asignada, recibida
    esperarHastaNotificaciones(donantePersonaId, 3, 10000);
  }

  @Test
  public void testRankingMensualYPosicion() {
    // 1. Create Persona and Donor
    String personaId = apiCrearPersonaHumana("99112233", "Raul", "raul.gomez@example.com");
    String donorId = apiCrearDonante(personaId);

    assertNotNull(donorId);

    // 2. Submit consecutive monthly donations to complete MisionRacha (COLABORADOR, objetivo = 3)
    apiEnviarEventoDonacionIncentivos(donorId, "2026-04-01", 1);
    apiEnviarEventoDonacionIncentivos(donorId, "2026-05-01", 1);
    apiEnviarEventoDonacionIncentivos(donorId, "2026-06-01", 1);

    // 3. Calculate ranking for 2026-06
    given()
        .when()
        .post(INCENTIVOS_URL + "/api/incentivos/ranking/calcular?periodo=2026-06")
        .then()
        .statusCode(200);

    // 4. Query ultimo ranking
    int actualPosicion =
        given()
            .when()
            .get(INCENTIVOS_URL + "/api/incentivos/ranking/ultimo")
            .then()
            .statusCode(200)
            .body("periodo", equalTo("2026-06"))
            .body("entradas", hasSize(greaterThanOrEqualTo(1)))
            .body("entradas.find { it.donanteId == '" + donorId + "' }.misionesCompletadas", equalTo(1))
            .extract()
            .path("entradas.find { it.donanteId == '" + donorId + "' }.posicion");

    assertTrue(actualPosicion >= 1);

    // 5. Query donante metrics to check position is updated
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(200)
        .body("posicionEnRanking", equalTo(actualPosicion))
        .body("misionesCompletadasTotal", equalTo(1));

    // 6. Query ranking history
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/ranking/historial")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  public void testInsigniaVisibilityFlow() {
    // 1. Create Persona and Donor
    String personaId = apiCrearPersonaHumana("99112244", "Laura", "laura.perez@example.com");
    String donorId = apiCrearDonante(personaId);

    assertNotNull(donorId);

    // 2. Submit consecutive monthly donations to complete MisionRacha
    apiEnviarEventoDonacionIncentivos(donorId, "2026-04-01", 1);
    apiEnviarEventoDonacionIncentivos(donorId, "2026-05-01", 1);
    apiEnviarEventoDonacionIncentivos(donorId, "2026-06-01", 1);

    // 3. Verify insignia "Racha Inicial" is returned and is visible
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/insignias")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].nombre", equalTo("Racha Inicial"))
        .body("[0].visible", equalTo(true));

    // 4. Toggle visibility to false
    given()
        .when()
        .patch(
            INCENTIVOS_URL
                + "/api/incentivos/donantes/"
                + donorId
                + "/insignias/Racha Inicial/visibilidad?visible=false")
        .then()
        .statusCode(200);

    // 5. Verify insignia is no longer listed
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/insignias")
        .then()
        .statusCode(200)
        .body("size()", equalTo(0));

    // 6. Toggle visibility to true
    given()
        .when()
        .patch(
            INCENTIVOS_URL
                + "/api/incentivos/donantes/"
                + donorId
                + "/insignias/Racha Inicial/visibilidad?visible=true")
        .then()
        .statusCode(200);

    // 7. Verify insignia is visible again
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/insignias")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].nombre", equalTo("Racha Inicial"))
        .body("[0].visible", equalTo(true));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testComplexE2EMultipleDonationsFlow() {
    // 1. Create Donante Persona & Donor
    String donantePersonaId = apiCrearPersonaHumana("77889900", "Carlos", "carlos.gimenez@example.com");
    String donorId = apiCrearDonante(donantePersonaId);

    // 2. Create Beneficiary Persona (Juridica) & EntidadBeneficiaria
    String benefPersonaId = apiCrearPersonaJuridica("30-22223333-4", "Hogar de Dia", "hogar@example.com");
    String entidadId = apiCrearEntidad(benefPersonaId);

    // 3. Fetch subcategories and find "No Perecederos" ID
    String subcategoryId =
        given()
            .when()
            .get(DONACIONES_URL + "/api/necesidades/subcategorias")
            .then()
            .statusCode(200)
            .extract()
            .path("find { it.nombre == 'No Perecederos' }.id");

    assertNotNull(subcategoryId);

    // 4. Load Necessity of 10 items
    apiCrearNecesidad(entidadId, subcategoryId, 10, "Necesidad de arroz/fideos preprod");

    // 5. Load two separate donations (5 units of arroz, 5 units of fideos)
    apiCrearDonacion(donantePersonaId, "Donación de arroz preprod complex", "arroz", 5);
    apiCrearDonacion(donantePersonaId, "Donación de fideos preprod complex", "fideos", 5);
    esperarAsync(); // wait for async processing

    // 6. Execute Matching
    String responseBody =
        given()
            .when()
            .post(DONACIONES_URL + "/api/asignaciones/ejecuciones")
            .then()
            .statusCode(201)
            .extract()
            .body()
            .asString();

    List<Map<String, Object>> propuestas = io.restassured.path.json.JsonPath.from(responseBody).get();
    assertFalse(propuestas.isEmpty());

    // Find the proposal matching our necessity description
    Map<String, Object> propuesta = null;
    for (Map<String, Object> p : propuestas) {
      Map<String, Object> nec = (Map<String, Object>) p.get("necesidadQueSatisface");
      if (nec != null && nec.get("descripcion").equals("Necesidad de arroz/fideos preprod")) {
        propuesta = p;
        break;
      }
    }

    assertNotNull(propuesta, "Propuesta no encontrada para la necesidad de arroz/fideos");
    String propuestaId = (String) propuesta.get("id");

    // Approve Propuesta (this confirms assignments and outputs fragmentations)
    Map<String, Object> approveBody = new HashMap<>();
    approveBody.put("estado", "APROBADA");
    given()
        .contentType(ContentType.JSON)
        .body(approveBody)
        .when()
        .put(DONACIONES_URL + "/api/asignaciones/propuestas/" + propuestaId + "/estado")
        .then()
        .statusCode(200);

    // Get the DonacionIndependiente IDs from posiblesFragmentaciones
    List<Map<String, Object>> frags =
        (List<Map<String, Object>>) propuesta.get("posiblesFragmentaciones");
    assertNotNull(frags);
    assertEquals(
        2, frags.size(), "Deberían haber 2 fragmentaciones para completar la necesidad de 10 unidades");

    String diId1 = (String) ((Map<String, Object>) frags.get(0).get("donacionOriginal")).get("id");
    String diId2 = (String) ((Map<String, Object>) frags.get(1).get("donacionOriginal")).get("id");

    // 7. Transition both DonacionIndependiente states to ENTREGADA
    for (String diId : List.of(diId1, diId2)) {
      Map<String, Object> patchBody = new HashMap<>();
      patchBody.put("estado", "EN_TRASLADO");
      given()
          .contentType(ContentType.JSON)
          .body(patchBody)
          .header("X-Actor", "TRANSPORTISTA")
          .when()
          .patch(DONACIONES_URL + "/donaciones-independientes/" + diId + "/estado")
          .then()
          .statusCode(200);

      patchBody.put("estado", "LISTA_PARA_ENTREGAR");
      given()
          .contentType(ContentType.JSON)
          .body(patchBody)
          .header("X-Actor", "TRANSPORTISTA")
          .when()
          .patch(DONACIONES_URL + "/donaciones-independientes/" + diId + "/estado")
          .then()
          .statusCode(200);

      patchBody.put("estado", "ENTREGADA");
      given()
          .contentType(ContentType.JSON)
          .body(patchBody)
          .header("X-Actor", "TRANSPORTISTA")
          .when()
          .patch(DONACIONES_URL + "/donaciones-independientes/" + diId + "/estado")
          .then()
          .statusCode(200);
    }

    // 8. Verify metrics in incentivos-service (totalDonacionesExitosas == 2)
    esperarHastaTotalDonacionesExitosas(donorId, 1, 10000);

    // 9. Verify notifications in notificaciones-service
    // Expected: welcome, 2 assignments, 2 deliveries = 5 notifications
    esperarHastaNotificaciones(donantePersonaId, 5, 10000);
  }
}
