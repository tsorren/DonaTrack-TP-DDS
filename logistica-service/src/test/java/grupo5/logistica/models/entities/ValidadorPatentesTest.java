package grupo5.logistica.models.entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorPatentesTest {

  @ParameterizedTest
  @ValueSource(strings = {"ABC123", "AB123CD", " ab-123-cd "})
  void validarAceptaPatentesValidas(String patente) {
    assertDoesNotThrow(() -> ValidadorPatentes.validar(patente, List.of()));
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", "123ABC", "AB12CD"})
  void validarRechazaFormatosInvalidos(String patente) {
    assertThrows(ValidationException.class, () -> ValidadorPatentes.validar(patente, List.of()));
  }

  @Test
  void validarRechazaPatenteNula() {
    assertThrows(ValidationException.class, () -> ValidadorPatentes.validar(null, List.of()));
  }

  @Test
  void validarRechazaDuplicadosAunqueElFormatoVisualDifiera() {
    assertThrows(
        BusinessStateException.class,
        () -> ValidadorPatentes.validar("ab-123-cd", List.of("AB123CD")));
  }
}
