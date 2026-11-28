package grupo5.donaciones.graphql.comun;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import grupo5.donaciones.dto.Pagina;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginaGraphQlTest {

  @Test
  void mapeaLosElementosYConservaElTotalYElPedido() {
    Pagina<Integer> pagina = new Pagina<>(List.of(1, 2), 7);

    PaginaGraphQl<String> resultado =
        PaginaGraphQl.de(pagina, new PedidoDePagina(3, 2), n -> "n" + n);

    assertThat(resultado.elementos()).containsExactly("n1", "n2");
    assertThat(resultado.total()).isEqualTo(7);
    assertThat(resultado.pagina()).isEqualTo(3);
    assertThat(resultado.tamanio()).isEqualTo(2);
  }

  @Test
  void losElementosNoSePuedenCambiarDesdeAfuera() {
    List<String> elementos = new ArrayList<>(List.of("a"));
    PaginaGraphQl<String> pagina = new PaginaGraphQl<>(elementos, 1, 0, 20);

    elementos.add("b");

    assertThat(pagina.elementos()).containsExactly("a");
    assertThatThrownBy(() -> pagina.elementos().add("c"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
