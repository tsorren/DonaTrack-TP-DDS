package grupo5.donaciones.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClaveSeguraTest {

  @Test
  void clavesIguales_coinciden() {
    assertTrue(ClaveSegura.coinciden("clave-sintetica", "clave-sintetica"));
  }

  @Test
  void clavesDistintasOdeDistintoLargo_noCoinciden() {
    assertFalse(ClaveSegura.coinciden("clave-sintetica", "clave-sintetic0"));
    assertFalse(ClaveSegura.coinciden("corta", "una-clave-bastante-mas-larga"));
  }

  @Test
  void nulos_noCoinciden() {
    assertFalse(ClaveSegura.coinciden(null, "clave"));
    assertFalse(ClaveSegura.coinciden("clave", null));
    assertFalse(ClaveSegura.coinciden(null, null));
  }
}
