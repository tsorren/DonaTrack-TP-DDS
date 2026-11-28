package grupo5.donaciones.graphql.comun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

class TopeDePaginaTest {

  private final TopeDePagina tope = new TopeDePagina(50);

  @Test
  void sinPaginaNiTamanio_esLaPrimeraDe20() {
    assertThat(tope.validar(null, null)).isEqualTo(new PedidoDePagina(0, 20));
  }

  @Test
  void aceptaHastaElTope() {
    assertThat(tope.validar(3, 50)).isEqualTo(new PedidoDePagina(3, 50));
    assertThat(tope.validar(0, 1)).isEqualTo(new PedidoDePagina(0, 1));
  }

  @Test
  void fueraDeRangoEsArgumentoInvalido() {
    assertInvalido(0, 51);
    assertInvalido(0, 0);
    assertInvalido(-1, 20);
  }

  @Test
  void elTopeEsElQueSeConfigura() {
    TopeDePagina chico = new TopeDePagina(5);

    assertThat(chico.tamanioMaximo()).isEqualTo(5);
    assertThat(chico.validar(0, 5).tamanio()).isEqualTo(5);
    assertThatThrownBy(() -> chico.validar(0, 6)).isInstanceOf(ValidationException.class);
  }

  @Test
  void unTopeMenorQueUnoNoArranca() {
    assertThatThrownBy(() -> new TopeDePagina(0)).isInstanceOf(IllegalArgumentException.class);
  }

  private void assertInvalido(int pagina, int tamanio) {
    assertThatThrownBy(() -> tope.validar(pagina, tamanio))
        .isInstanceOfSatisfying(
            ValidationException.class,
            ex -> assertThat(ex.getError()).isEqualTo(ErrorCatalog.ARGUMENTO_INVALIDO));
  }
}
