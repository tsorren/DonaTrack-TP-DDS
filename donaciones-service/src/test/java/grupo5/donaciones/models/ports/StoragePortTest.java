package grupo5.donaciones.models.ports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StoragePortTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "3f2b9c1e-7a4d-4e8f-9b0a-1c2d3e4f5a6b.csv",
        "fotos/3f2b9c1e-7a4d-4e8f-9b0a-1c2d3e4f5a6b.jpg",
        "a_b-c.d"
      })
  void deberiaAceptarClavesGeneradasPorElServicio(String clave) {
    assertEquals(clave, StoragePort.validarClave(clave));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        "../padron.csv",
        "fotos/../../etc/passwd",
        "..",
        "a..b",
        "/absoluta.csv",
        "fotos//doble.jpg",
        "fotos/",
        "con\\barra-invertida",
        ".oculto",
        "con espacio.csv",
        "salto\nde-linea"
      })
  void deberiaRechazarClavesQuePuedenEscaparDelBucket(String clave) {
    assertThrows(IllegalArgumentException.class, () -> StoragePort.validarClave(clave));
  }

  @Test
  void deberiaRechazarClavesDemasiadoLargas() {
    String clave = "a".repeat(513);
    assertThrows(IllegalArgumentException.class, () -> StoragePort.validarClave(clave));
  }
}
