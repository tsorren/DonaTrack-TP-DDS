package grupo5.cliente.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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

  /**
   * La regex anterior de NOMBRE, con grupo repetido (Sonar S5998); la nueva debe aceptar lo mismo.
   */
  private static final String NOMBRE_ANTERIOR = "(?:publico|donante|entidad|admin)(?:-[a-z]+)+";

  private final Pantallas pantallas =
      new Pantallas(new DefaultResourceLoader(), null, null, null, null);

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

  @Test
  void elNombreNuevoEquivaleAlAnteriorEnLasPlantillasReales() throws IOException {
    List<String> nombres;
    try (Stream<Path> archivos = Files.list(Path.of("src/main/resources/templates"))) {
      nombres =
          archivos
              .map(p -> p.getFileName().toString())
              .filter(n -> n.endsWith(".html"))
              .map(n -> n.substring(0, n.length() - ".html".length()))
              .sorted()
              .toList();
    }
    assertThat(nombres).isNotEmpty().contains("admin-inicio", "estilos");
    for (String nombre : nombres) {
      assertThat(nombre.matches(Pantallas.NOMBRE))
          .as(nombre)
          .isEqualTo(nombre.matches(NOMBRE_ANTERIOR));
    }
    assertThat("admin-inicio".matches(Pantallas.NOMBRE)).isTrue();
    assertThat("estilos".matches(Pantallas.NOMBRE)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "admin",
        "admin-",
        "admin-Inicio",
        "admin-a1",
        "publico-x-",
        "otro-inicio",
        "donante-donaciones-detalle"
      })
  void elNombreNuevoEquivaleAlAnteriorEnCasosSueltos(String nombre) {
    assertThat(nombre.matches(Pantallas.NOMBRE)).isEqualTo(nombre.matches(NOMBRE_ANTERIOR));
  }

  @Test
  void laDiferenciaAceptadaSonLosGuionesDobles() {
    assertThat("admin--x".matches(Pantallas.NOMBRE)).isTrue();
    assertThat("admin--x".matches(NOMBRE_ANTERIOR)).isFalse();
  }
}
