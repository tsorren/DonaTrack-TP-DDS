package grupo5.donaciones.models.entities.donantes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DonanteParserTest {

  private DonanteParser parser;

  @BeforeEach
  void setUp() {
    parser = new DonanteParser();
  }

  @Test
  void procesarLinea_conLineaValida_retornaMapa() {
    String[] cabecera = {"TipoPersona", "Nombre", "Email"};
    String linea = "Humana,Juan,juan@example.com";

    Map<String, String> resultado = parser.procesarLinea(linea, cabecera);

    assertEquals(3, resultado.size());
    assertEquals("Humana", resultado.get("TipoPersona"));
    assertEquals("Juan", resultado.get("Nombre"));
    assertEquals("juan@example.com", resultado.get("Email"));
  }

  @Test
  void procesarLinea_conColumnasMenoresACabecera_lanzaIllegalArgumentException() {
    String[] cabecera = {"TipoPersona", "Nombre", "Email"};
    String linea = "Humana,Juan";

    assertThrows(IllegalArgumentException.class, () -> parser.procesarLinea(linea, cabecera));
  }

  @Test
  void procesarLinea_conLineaNula_lanzaIllegalArgumentException() {
    String[] cabecera = {"TipoPersona", "Nombre"};

    assertThrows(IllegalArgumentException.class, () -> parser.procesarLinea(null, cabecera));
  }

  @Test
  void procesarLinea_conCabeceraNula_lanzaIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> parser.procesarLinea("Humana,Juan", null));
  }
}
