package grupo5.tests.integration;

import static org.hamcrest.Matchers.equalTo;

import grupo5.tests.BaseIT;
import grupo5.tests.builders.DonacionTestDataBuilder;
import grupo5.tests.utils.TestIdGenerator;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("integration")
class DonationIntegrationIT extends BaseIT {

  /**
   * El bien no es un alimento, así que la donación no se segmenta y no entra al stock de "No
   * Perecederos" que usan los tests de matching (DIT-04).
   */
  @Test
  void testCrearDonacionQuedaPersistidaConSuDonante() {
    UUID donanteId = crearDonante("Maria").donanteId();

    UUID donacionId =
        donacionesClient.crearDonacionOk(
            DonacionTestDataBuilder.deAlimento(TestIdGenerator.uniqueItemName("pantalon"), 5)
                .conDonante(donanteId)
                .build());

    donacionesClient
        .obtenerDonacion(donacionId)
        .then()
        .statusCode(200)
        .body("id", equalTo(donacionId.toString()))
        .body("donante.idDonante", equalTo(donanteId.toString()));
  }
}
