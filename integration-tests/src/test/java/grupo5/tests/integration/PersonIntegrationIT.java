package grupo5.tests.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class PersonIntegrationIT extends BaseIT {

  @Test
  void testCrearPersonaHumanaYVerificarReplicacion() {
    String nombre = "Carlos";
    String dni = TestIdGenerator.randomDni();
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana().conNombre(nombre).conDocumento(dni).build();

    UUID personaId = donacionesClient.crearPersonaOk(persona);
    assertNotNull(personaId);

    // Esperar replicación síncrona/asíncrona en notificaciones-service
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);

    notificacionesClient
        .obtenerPersona(personaId)
        .then()
        .statusCode(200)
        .body("denominacion", equalTo(nombre + " Perez"));
  }

  @Test
  void testCrearPersonaJuridicaYVerificarReplicacion() {
    String razonSocial = TestIdGenerator.uniqueName("Comedor Solidario");
    PersonaTestDTO persona = PersonaTestDataBuilder.juridica().conRazonSocial(razonSocial).build();

    UUID personaId = donacionesClient.crearPersonaOk(persona);
    assertNotNull(personaId);

    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);

    notificacionesClient
        .obtenerPersona(personaId)
        .then()
        .statusCode(200)
        .body("denominacion", equalTo(razonSocial));
  }
}
