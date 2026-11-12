package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El ícono de una categoría se elige por su nombre, siempre igual, y nunca queda sin ninguno. */
class ComunIconoTest {

  private static final Path SPRITE = Path.of("src/main/resources/templates/iconos.html");

  @ParameterizedTest
  @CsvSource({
    "Alimentos,cat-alimentos",
    "Ropa,cat-ropa",
    "Vestimenta,cat-ropa",
    "Muebles,cat-muebles",
    "Útiles escolares,cat-escolares",
    "Higiene,cat-higiene",
    "Juguetes,cat-juguetes",
    "Blanquería,cat-blanqueria",
    "Electrodomésticos,cat-electrodomesticos",
    "HIGIENE,cat-higiene",
  })
  void cadaCategoriaDelCatalogoTieneElSuyo(String nombre, String esperado) {
    assertEquals(esperado, Comun.icono(nombre));
  }

  @Test
  void unNombreConDosPistasSiempreDaElMismoIcono() {
    // "Muebles Escolares" nombra dos íconos; gana el que va primero en la lista, no el azar.
    for (int i = 0; i < 50; i++) {
      assertEquals("cat-muebles", Comun.icono("Muebles Escolares"));
    }
  }

  @Test
  void unaCategoriaSinIconoPropioMuestraElGenerico() {
    assertEquals("cat-generico", Comun.icono("Herramientas"));
    assertEquals("cat-generico", Comun.icono(""));
  }

  @Test
  void todoIconoQueSeEligeEstaDibujadoEnElSprite() throws Exception {
    String sprite = Files.readString(SPRITE, StandardCharsets.UTF_8);
    List<String> nombres =
        List.of(
            "Alimentos",
            "Ropa",
            "Muebles",
            "Escolares",
            "Higiene",
            "Juguetes",
            "Blanquería",
            "Electrodomésticos",
            "Herramientas");
    for (String nombre : nombres) {
      String id = Comun.icono(nombre);
      assertTrue(sprite.contains("<symbol id=\"" + id + "\""), "falta el símbolo " + id);
    }
  }
}
