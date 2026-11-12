package grupo5.donaciones.models.repositories.listados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextoBuscadoTest {

  @Test
  void sinTildes_encuentraConTildes() {
    assertTrue(TextoBuscado.contiene("María Gómez", "gomez"));
  }

  @Test
  void conTildesYMayusculas_encuentraSinTildes() {
    assertTrue(TextoBuscado.contiene("Gomez", "GÓMEZ"));
  }

  @Test
  void laEnieSeComparaComoN() {
    assertTrue(TextoBuscado.contiene("Asociación Peña", "pena"));
  }

  @Test
  void esContieneNoIgual() {
    assertTrue(TextoBuscado.contiene("Fundación Esperanza", "esper"));
    assertFalse(TextoBuscado.contiene("Fundación Esperanza", "comedor"));
  }

  @Test
  void buscadoNuloNoFiltra_yCampoNuloNoCoincide() {
    assertTrue(TextoBuscado.contiene(null, null));
    assertFalse(TextoBuscado.contiene(null, "ana"));
  }

  @Test
  void normalizar_quitaTildesYPasaAMinusculas() {
    assertEquals("jose perez", TextoBuscado.normalizar("José PÉREZ"));
    assertNull(TextoBuscado.normalizar(null));
  }
}
