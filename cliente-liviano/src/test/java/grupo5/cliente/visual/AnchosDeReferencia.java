package grupo5.cliente.visual;

import java.util.List;

/**
 * Anchos de pantalla en px. Fuente única; la tabla de docs/interfaz/README.md los explica. Para
 * cambiarlos se edita solo esta clase y esa tabla.
 */
final class AnchosDeReferencia {

  /** Celular, escritorio y pantalla grande: se diseña, se captura y se compara en estos. */
  static final List<Integer> REFERENCIA = List.of(360, 1280, 1920);

  /** Solo para buscar desbordes, sin capturas. Incluye una tableta (768). */
  static final List<Integer> INTERMEDIOS = List.of(414, 600, 768, 1024, 1440);

  /** Alto de la ventana; las capturas son de la página completa. */
  static final int ALTO = 900;

  private AnchosDeReferencia() {}
}
