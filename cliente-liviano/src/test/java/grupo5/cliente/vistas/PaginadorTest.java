package grupo5.cliente.vistas;

import static org.assertj.core.api.Assertions.assertThat;

import grupo5.cliente.vistas.Paginador.Casilla;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El paginador del sitio (R11-11, ficha docs/interfaz/flujos/r11-p1-paginador.md, §4.1): siempre la
 * primera y la última, tres seguidas alrededor de la actual y un "…" que nunca esconde una sola.
 */
class PaginadorTest {

  /** La ventana como texto: la actual entre paréntesis, el hueco como "…". */
  private static String ventana(int pagina, int paginas) {
    Paginador p = Paginador.de("Páginas", pagina, paginas, n -> "?pagina=" + n, false);
    return String.join(
        " ",
        p.tramos().stream()
            .map(c -> c.hueco() ? "…" : c.actual() ? "(" + c.numero() + ")" : "" + c.numero())
            .toList());
  }

  @Test
  void laVentanaTienePrimeraUltimaYTresSeguidas() {
    assertThat(ventana(1, 9)).isEqualTo("(1) 2 3 … 9");
    assertThat(ventana(2, 9)).isEqualTo("1 (2) 3 … 9");
    assertThat(ventana(4, 9)).isEqualTo("1 2 3 (4) 5 … 9");
    assertThat(ventana(5, 9)).isEqualTo("1 … 4 (5) 6 … 9");
    assertThat(ventana(9, 9)).isEqualTo("1 … 7 8 (9)");
  }

  @Test
  void conPocasPaginasVanTodasSinHueco() {
    assertThat(ventana(2, 3)).isEqualTo("1 (2) 3");
    assertThat(ventana(1, 2)).isEqualTo("(1) 2");
    assertThat(ventana(1, 5)).isEqualTo("(1) 2 3 4 5");
  }

  @Test
  void entreLasFlechasNuncaHayMasDeSieteCasillas() {
    for (int pagina = 1; pagina <= 62; pagina++) {
      assertThat(Paginador.de("P", pagina, 62, n -> "x", false).tramos())
          .hasSizeLessThanOrEqualTo(7);
    }
  }

  @Test
  void losExtremosNoSonEnlacesYLaActualTampoco() {
    Paginador primera = Paginador.de("P", 1, 9, n -> "?pagina=" + n, true);
    Paginador medio = Paginador.de("P", 5, 9, n -> "?pagina=" + n, true);
    Paginador ultima = Paginador.de("P", 9, 9, n -> "?pagina=" + n, true);

    assertThat(primera.primera()).isNull();
    assertThat(primera.anterior()).isNull();
    assertThat(primera.siguiente()).isEqualTo("?pagina=2");
    assertThat(primera.ultima()).isEqualTo("?pagina=9");
    assertThat(medio.primera()).isEqualTo("?pagina=1");
    assertThat(medio.anterior()).isEqualTo("?pagina=4");
    assertThat(medio.tramos()).contains(new Casilla(5, null), new Casilla(6, "?pagina=6"));
    assertThat(ultima.siguiente()).isNull();
    assertThat(ultima.ultima()).isNull();
    assertThat(ultima.cambio()).isTrue();
  }

  @Test
  void conUnaSolaPaginaNoHayPaginadorYFueraDeRangoSeCorrige() {
    assertThat(Paginador.de("P", 1, 1, n -> "x", false)).isNull();
    assertThat(Paginador.de("P", 1, 0, n -> "x", false)).isNull();
    assertThat(Paginador.de("P", 40, 9, n -> "x", false).pagina()).isEqualTo(9);
    assertThat(Paginador.de("P", 0, 9, n -> "x", false).pagina()).isEqualTo(1);
    assertThat(List.of(Paginador.de("P", 0, 9, n -> "x", false).tramos().get(0)))
        .containsExactly(new Casilla(1, null));
  }
}
