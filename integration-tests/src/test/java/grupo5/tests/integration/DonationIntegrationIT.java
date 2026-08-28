package grupo5.tests.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.builders.PersonaTestDataBuilder;
import grupo5.tests.dto.DonacionTestDTO;
import grupo5.tests.dto.PersonaTestDTO;
import grupo5.tests.utils.PollingUtils;
import grupo5.tests.utils.TestIdGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class DonationIntegrationIT extends BaseIT {

  @Test
  void testCrearDonacion() {
    // 1. Crear persona
    PersonaTestDTO persona =
        PersonaTestDataBuilder.humana()
            .conNombre("Maria")
            .conDocumento(TestIdGenerator.randomDni())
            .conEmail(TestIdGenerator.randomEmail("maria"))
            .build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);

    // 2. Esperar replicación y crear donante
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);

    // 3. Crear donación con ítem aislado
    DonacionTestDTO donacion =
        DonacionTestDataBuilder.deRopa(TestIdGenerator.uniqueItemName("pantalon it"), 5)
            .conDonante(donanteId)
            .build();

    UUID donacionId = donacionesClient.crearDonacionOk(donacion);
    assertNotNull(donacionId);
  }
}
