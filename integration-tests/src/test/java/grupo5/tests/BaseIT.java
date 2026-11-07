package grupo5.tests;

import grupo5.tests.builders.NecesidadTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.client.DonacionesApiClient;
import grupo5.tests.client.IncentivosApiClient;
import grupo5.tests.client.LogisticaApiClient;
import grupo5.tests.client.NotificacionesApiClient;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  /** Ruta de un archivo de docs/arquitectura/contratos/, corriendo desde el módulo o la raíz. */
  protected static String contrato(String archivo) {
    Path desdeModulo = Path.of("../docs/arquitectura/contratos/", archivo);
    return (Files.exists(desdeModulo)
            ? desdeModulo
            : Path.of("docs/arquitectura/contratos/", archivo))
        .toAbsolutePath()
        .toString();
  }

  protected record Donante(UUID personaId, UUID donanteId) {}

  /** Persona humana replicada en Notificaciones y registrada como donante. */
  protected static Donante crearDonante(String nombre) {
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre(nombre)
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail(nombre))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    return new Donante(personaId, donacionesClient.crearDonanteOk(personaId));
  }

  /** Persona jurídica registrada como entidad beneficiaria. */
  protected static UUID crearEntidad(String razonSocial) {
    PersonaTestDTO persona =
        PersonaTestDataBuilder.juridica()
            .conRazonSocial(TestIdGenerator.uniqueName(razonSocial))
            .build();
    return donacionesClient.crearEntidadOk(donacionesClient.crearPersonaOk(persona));
  }

  protected static UUID subcategoriaNoPerecederos() {
    Response resp = donacionesClient.obtenerSubcategorias();
    resp.then().statusCode(200);
    String id = resp.path("find { it.nombre == 'No Perecederos' }.id");
    Assertions.assertNotNull(id, "Falta la subcategoría 'No Perecederos'");
    return UUID.fromString(id);
  }

  /**
   * Crea la necesidad, ejecuta el matching, aprueba la propuesta de esa necesidad y devuelve las
   * donaciones independientes asignadas. El matching es global (DIT-04): la propuesta se busca por
   * la descripción, que cada test hace única.
   */
  @SuppressWarnings("unchecked")
  protected static List<UUID> asignarYAprobar(
      UUID entidadId, String descripcionNecesidad, int cantidad) {
    donacionesClient.crearNecesidadOk(
        NecesidadTestDataBuilder.extraordinaria(entidadId, subcategoriaNoPerecederos(), cantidad)
            .conDescripcion(descripcionNecesidad)
            .build());

    Response ejecucion = donacionesClient.ejecutarAsignacion();
    ejecucion.then().statusCode(201);
    List<Map<String, Object>> propuestas = JsonPath.from(ejecucion.asString()).get();
    Map<String, Object> propuesta =
        propuestas.stream()
            .filter(
                p ->
                    p.get("necesidad") instanceof Map<?, ?> nec
                        && descripcionNecesidad.equals(nec.get("descripcion")))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError("Propuesta no encontrada para " + descripcionNecesidad));

    List<UUID> donaciones =
        ((List<Map<String, Object>>) propuesta.get("fragmentaciones"))
            .stream()
                .map(f -> (Map<String, Object>) f.get("donacionIndependiente"))
                .map(di -> UUID.fromString((String) di.get("id")))
                .toList();
    Assertions.assertFalse(donaciones.isEmpty(), "La propuesta no tiene fragmentaciones");

    UUID propuestaId = UUID.fromString((String) propuesta.get("id"));
    donacionesClient.actualizarEstadoPropuesta(propuestaId, "APROBADA").then().statusCode(200);
    return donaciones;
  }

  /**
   * Atajo: lleva las donaciones a ENTREGADA por REST, como un transportista, sin pasar por
   * Logística. El camino real (eventos de Logística) lo cubre {@code FullDistributedDonationE2EIT}.
   */
  protected static void entregarPorRest(List<UUID> donacionesIndependientes) {
    for (UUID di : donacionesIndependientes) {
      for (String estado : List.of("LISTA_PARA_ENTREGAR", "EN_TRASLADO", "ENTREGADA")) {
        donacionesClient
            .cambiarEstadoDonacionIndependiente(di, estado, "TRANSPORTISTA")
            .then()
            .statusCode(200);
      }
    }
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
