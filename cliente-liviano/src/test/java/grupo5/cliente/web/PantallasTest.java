package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

/**
 * El nombre que llega en la dirección no arma una ruta al disco (Sonar S2083): solo se reconoce una
 * plantilla de la lista, y lo demás da 404.
 */
class PantallasTest {

  private final Pantallas pantallas = new Pantallas(new DefaultResourceLoader(), null, null);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../templates/publico-portada",
        "../../application",
        "publico-../publico-portada",
        "publico-portada/../publico-portada",
        "reales/../publico-portada",
        "..\\templates\\publico-portada",
        "publico-portada%2F..%2Fpublico-portada"
      })
  void unaRutaConSaltosNoEsUnaPantallaYDa404(String pedido) {
    assertThat(pantallas.existe(pedido)).isFalse();
    assertThatThrownBy(() -> pantallas.mostrar(pedido, new ExtendedModelMap()))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            falla -> assertThat(falla.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @ParameterizedTest
  @ValueSource(strings = {"publico-portada", "donante-inicio", "admin-asignaciones"})
  void lasPlantillasDibujadasExisten(String pantalla) {
    assertThat(pantallas.existe(pantalla)).isTrue();
  }
}
