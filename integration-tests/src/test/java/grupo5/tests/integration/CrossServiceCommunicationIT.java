package grupo5.tests.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import io.restassured.http.ContentType;
import java.util.*;

import org.junit.jupiter.api.Disabled;
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

  @Test
  public void testPersonaReplicationLifecycle() {
    // 1. Create a persona in donaciones-service
    Map<String, Object> personaPayload = new HashMap<>();
    personaPayload.put("tipo", "HUMANA");
    personaPayload.put("tipoDocumento", "DNI");
    personaPayload.put("documento", "55554444");
    personaPayload.put("nombre", "Juan");
    personaPayload.put("apellido", "Perez");
    personaPayload.put("genero", "HOMBRE");
    personaPayload.put("fechaNacimiento", "2000-01-01");

    Map<String, Object> medio = new HashMap<>();
    medio.put("tipo", "CORREO");
    medio.put("esPredeterminado", true);
    medio.put("direccionCorreo", "juan.perez@example.com");
    personaPayload.put("mediosDeContacto", List.of(medio));

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
  public void testDonanteRegistrationAndWelcomeNotification() {
    // 1. Create a persona
    Map<String, Object> personaPayload = new HashMap<>();
    personaPayload.put("tipo", "HUMANA");
    personaPayload.put("tipoDocumento", "DNI");
    personaPayload.put("documento", "11112222");
    personaPayload.put("nombre", "Maria");
    personaPayload.put("apellido", "Gomez");
    personaPayload.put("genero", "MUJER");
    personaPayload.put("fechaNacimiento", "1995-05-05");

    Map<String, Object> medio = new HashMap<>();
    medio.put("tipo", "CORREO");
    medio.put("esPredeterminado", true);
    medio.put("direccionCorreo", "maria.gomez@example.com");
    personaPayload.put("mediosDeContacto", List.of(medio));

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

    // 2. Register the persona as a donor
    Map<String, Object> donorPayload = new HashMap<>();
    donorPayload.put("idPersona", personaId);

    String donorId =
        given()
            .contentType(ContentType.JSON)
            .body(donorPayload)
            .when()
            .post(DONACIONES_URL + "/api/donantes")
            .then()
            .statusCode(201)
            .extract()
            .path("idDonante");

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
    esperarAsync();
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
    Map<String, Object> personaPayload = new HashMap<>();
    personaPayload.put("tipo", "HUMANA");
    personaPayload.put("tipoDocumento", "DNI");
    personaPayload.put("documento", "33332222");
    personaPayload.put("nombre", "Pedro");
    personaPayload.put("apellido", "Sosa");
    personaPayload.put("genero", "HOMBRE");
    personaPayload.put("fechaNacimiento", "1992-02-02");

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

    // 2. Register as a donor
    Map<String, Object> donorPayload = new HashMap<>();
    donorPayload.put("idPersona", personaId);

    String donorId =
        given()
            .contentType(ContentType.JSON)
            .body(donorPayload)
            .when()
            .post(DONACIONES_URL + "/api/donantes")
            .then()
            .statusCode(201)
            .extract()
            .path("idDonante");

    // Verify profile is created in incentives
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(200);

    // 3. Perform delete (baja) of donor in donaciones-service
    given().when().delete(DONACIONES_URL + "/api/donantes/" + donorId).then().statusCode(204);

    // 4. Verify profile is removed in incentives (should return 400 or 404 because donor is not
    // found)
    given()
        .when()
        .get(INCENTIVOS_URL + "/api/incentivos/donantes/" + donorId + "/metricas")
        .then()
        .statusCode(anyOf(equalTo(400), equalTo(404)));
  }

  @Disabled
  @Test
  @SuppressWarnings("unchecked")
  public void testE2EDonationFlowAndSideEffects() {
    // 1. Create Donante Persona & Donor
    Map<String, Object> donantePersonaPayload = new HashMap<>();
    donantePersonaPayload.put("tipo", "HUMANA");
    donantePersonaPayload.put("tipoDocumento", "DNI");
    donantePersonaPayload.put("documento", "88887777");
    donantePersonaPayload.put("nombre", "Ana");
    donantePersonaPayload.put("apellido", "Lopez");
    donantePersonaPayload.put("genero", "MUJER");
    donantePersonaPayload.put("fechaNacimiento", "1988-08-08");

    Map<String, Object> dMedio = new HashMap<>();
    dMedio.put("tipo", "CORREO");
    dMedio.put("esPredeterminado", true);
    dMedio.put("direccionCorreo", "ana.lopez@example.com");
    donantePersonaPayload.put("mediosDeContacto", List.of(dMedio));

    String donantePersonaId =
        given()
            .contentType(ContentType.JSON)
            .body(donantePersonaPayload)
            .when()
            .post(DONACIONES_URL + "/api/personas")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    Map<String, Object> donorPayload = new HashMap<>();
    donorPayload.put("idPersona", donantePersonaId);

    String donorId =
        given()
            .contentType(ContentType.JSON)
            .body(donorPayload)
            .when()
            .post(DONACIONES_URL + "/api/donantes")
            .then()
            .statusCode(201)
            .extract()
            .path("idDonante");

    // 2. Create Beneficiary Persona (Juridica) & EntidadBeneficiaria
    Map<String, Object> benefPersonaPayload = new HashMap<>();
    benefPersonaPayload.put("tipo", "JURIDICA");
    benefPersonaPayload.put("tipoDocumento", "CUIT");
    benefPersonaPayload.put("documento", "30-11112222-3");
    benefPersonaPayload.put("razonSocial", "Comedor Solidario");
    benefPersonaPayload.put("rubro", "Comedor");
    benefPersonaPayload.put("fechaConstitucion", "2010-01-01");

    Map<String, Object> representante = new HashMap<>();
    representante.put("tipo", "HUMANA");
    representante.put("tipoDocumento", "DNI");
    representante.put("documento", "12345678");
    representante.put("nombre", "Juan");
    representante.put("apellido", "Representante");
    representante.put("genero", "HOMBRE");
    representante.put("fechaNacimiento", "1980-01-01");
    benefPersonaPayload.put("representantes", List.of(representante));

    Map<String, Object> bMedio = new HashMap<>();
    bMedio.put("tipo", "CORREO");
    bMedio.put("esPredeterminado", true);
    bMedio.put("direccionCorreo", "comedor@example.com");
    benefPersonaPayload.put("mediosDeContacto", List.of(bMedio));

    String benefPersonaId =
        given()
            .contentType(ContentType.JSON)
            .body(benefPersonaPayload)
            .when()
            .post(DONACIONES_URL + "/api/personas")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    Map<String, Object> entidadPayload = new HashMap<>();
    entidadPayload.put("juridicaId", benefPersonaId);

    String entidadId =
        given()
            .contentType(ContentType.JSON)
            .body(entidadPayload)
            .when()
            .post(DONACIONES_URL + "/api/entidades")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    // 3. Load Donation (using subcategory arroz)
    Map<String, Object> donacionPayload = new HashMap<>();
    donacionPayload.put("idDonante", donantePersonaId);
    donacionPayload.put("descripcion", "Donación de arroz preprod");

    Map<String, Object> item = new HashMap<>();
    item.put("descripcionBien", "arroz");
    item.put("cantidad", 10);
    item.put("estadoBien", "NUEVO");
    item.put("fechaVencimiento", "2026-12-31");
    donacionPayload.put("items", List.of(item));

    Map<String, Object> direccion = new HashMap<>();
    direccion.put("calle", "Av. Medrano");
    direccion.put("altura", 951);
    direccion.put("codigoPostal", "1179");
    direccion.put("localidad", "CABA");
    direccion.put("provincia", "CABA");
    direccion.put("pais", "Argentina");
    donacionPayload.put("direccion", direccion);
    donacionPayload.put("nombreDeposito", "Deposito Central");

    String donacionId =
        given()
            .contentType(ContentType.JSON)
            .body(donacionPayload)
            .when()
            .post(DONACIONES_URL + "/api/donaciones")
            .then()
            .statusCode(201)
            .extract()
            .path("id");

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
    Map<String, Object> necesidadPayload = new HashMap<>();
    necesidadPayload.put("tipo", "EXTRAORDINARIA");
    necesidadPayload.put("idEntidad", entidadId);
    necesidadPayload.put("idSubcategoria", subcategoryId);
    necesidadPayload.put("cantidadNecesitada", 10);
    necesidadPayload.put("descripcion", "Necesidad de arroz preprod");
    necesidadPayload.put("fechaInicio", "2026-06-18");

    given()
        .contentType(ContentType.JSON)
        .body(necesidadPayload)
        .when()
        .post(DONACIONES_URL + "/api/necesidades")
        .then()
        .statusCode(201);

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
}
