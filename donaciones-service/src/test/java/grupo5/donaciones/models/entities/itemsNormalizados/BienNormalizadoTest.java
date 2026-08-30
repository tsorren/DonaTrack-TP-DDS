package grupo5.donaciones.models.entities.itemsNormalizados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.Bien;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BienNormalizadoTest {

  @Test
  void crearAceptado_conVencimientoNoPermitido_debeMantenerLaValidacion() {
    Bien bien = new Bien("alimento", null, LocalDate.now().plusDays(1), null, 1.0, 1.0);

    UUID normalizadoId = UUID.randomUUID();

    ValidationException exception =
        assertThrows(
            ValidationException.class,
            () ->
                new BienNormalizado(
                    bien, normalizadoId, 1.0, EstadoNormalizacion.ACEPTADO, false, false));

    assertEquals(ErrorCatalog.BIEN_VENCIMIENTO_NO_PERMITIDO, exception.getError());
  }
}
