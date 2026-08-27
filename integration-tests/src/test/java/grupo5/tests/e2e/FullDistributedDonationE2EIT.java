package grupo5.tests.e2e;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.NecesidadTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.NecesidadTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class FullDistributedDonationE2EIT extends BaseIT {

  @Test
  @SuppressWarnings("unchecked")
  void testFlujoCompletoDistribuidoConLogisticaYRabbitMQ() {
    // 1. Crear Donante (Persona Humana)
    PersonaTestDTO personaDonante =
        PersonaTestDataBuilder.humana()
            .conNombre("Roberto")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("roberto"))
            .build();
    UUID donantePersonaId = donacionesClient.crearPersonaOk(personaDonante);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, donantePersonaId);
    UUID donanteId = donacionesClient.crearDonanteOk(donantePersonaId);

    // 2. Crear Entidad Beneficiaria (Persona Jurídica)
    PersonaTestDTO personaJuridica =
        PersonaTestDataBuilder.juridica()
            .conRazonSocial(TestIdGenerator.uniqueName("Comedor Esperanza"))
            .build();
    UUID benefPersonaId = donacionesClient.crearPersonaOk(personaJuridica);
    UUID entidadId = donacionesClient.crearEntidadOk(benefPersonaId);

    // 3. Crear Donación con ítem único
    String itemBien = TestIdGenerator.uniqueItemName("arroz dist");
    DonacionTestDTO donacion =
        DonacionTestDataBuilder.deAlimento(itemBien, 10)
            .conDonante(donanteId)
            .conDescripcion("Donación distribuida de " + itemBien)
            .build();
    UUID donacionId = donacionesClient.crearDonacionOk(donacion);
    assertNotNull(donacionId);

    // Esperar normalización y segmentación asíncrona
    PollingUtils.esperarDonacionSegmentada(donacionesClient, donacionId);

    // 4. Obtener subcategoría "No Perecederos" y crear Necesidad
    Response subcatResp = donacionesClient.obtenerSubcategorias();
    subcatResp.then().statusCode(200);
    UUID subcategoryId =
        UUID.fromString(subcatResp.path("find { it.nombre == 'No Perecederos' }.id"));

    String descNecesidad = "Necesidad para " + itemBien;
    NecesidadTestDTO necesidad =
        NecesidadTestDataBuilder.extraordinaria(entidadId, subcategoryId, 10)
            .conDescripcion(descNecesidad)
            .build();
    donacionesClient.crearNecesidadOk(necesidad);

    // 5. Ejecutar Matching
    Response ejecucionResp = donacionesClient.ejecutarAsignacion();
    ejecucionResp.then().statusCode(201);

    List<Map<String, Object>> propuestas = JsonPath.from(ejecucionResp.asString()).get();
    assertFalse(propuestas.isEmpty());

    Map<String, Object> propuesta = null;
    for (Map<String, Object> p : propuestas) {
      Map<String, Object> nec = (Map<String, Object>) p.get("necesidad");
      if (nec != null && descNecesidad.equals(nec.get("descripcion"))) {
        propuesta = p;
        break;
      }
    }

    assertNotNull(propuesta, "Propuesta no encontrada para " + descNecesidad);
    UUID propuestaId = UUID.fromString((String) propuesta.get("id"));

    List<Map<String, Object>> frags = (List<Map<String, Object>>) propuesta.get("fragmentaciones");
    assertNotNull(frags);
    assertFalse(frags.isEmpty());
    Map<String, Object> di = (Map<String, Object>) frags.getFirst().get("donacionIndependiente");
    assertNotNull(di);
    UUID donacionIndependienteId = UUID.fromString((String) di.get("id"));

    // 6. Aprobar propuesta en donaciones-service
    // Esto dispara la llamada asíncrona Feign hacia logistica-service (POST /api/entregas)
    donacionesClient.actualizarEstadoPropuesta(propuestaId, "APROBADA").then().statusCode(200);

    // 7. Esperar y verificar que logistica-service registre la entrega
    UUID entregaId =
        PollingUtils.esperarEntregaCreadaParaDonacion(logisticaClient, donacionIndependienteId);
    assertNotNull(entregaId, "logistica-service debería haber recibido la entrega");

    logisticaClient
        .obtenerEntrega(entregaId)
        .then()
        .statusCode(200)
        .body("estadoActual", equalTo("PENDIENTE"));

    // 8. Transiciones de estado de la donación independiente hacia entrega exitosa
    donacionesClient
        .cambiarEstadoDonacionIndependiente(
            donacionIndependienteId, "LISTA_PARA_ENTREGAR", "TRANSPORTISTA")
        .then()
        .statusCode(200);

    donacionesClient
        .cambiarEstadoDonacionIndependiente(donacionIndependienteId, "EN_TRASLADO", "TRANSPORTISTA")
        .then()
        .statusCode(200);

    donacionesClient
        .cambiarEstadoDonacionIndependiente(donacionIndependienteId, "ENTREGADA", "TRANSPORTISTA")
        .then()
        .statusCode(200);

    // 9. Verificar side-effects downstream (Incentivos y Notificaciones)
    PollingUtils.esperarTotalDonacionesExitosas(incentivosClient, donanteId, 0);
    PollingUtils.esperarMinimoNotificaciones(notificacionesClient, donantePersonaId, 3);
  }
}
