package grupo5.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** La allowlist del gateway ({@code "MÉTODO /ruta"}) como las rutas sin token de la seguridad. */
class ReglasDeAccesoGatewayTest {

  private static ReglasDeAccesoGateway reglasCon(List<String> rutasPublicas) {
    return new ReglasDeAccesoGateway(new GatewayProperties(null, rutasPublicas));
  }

  @Test
  void devuelveSoloLaRutaDeCadaEntrada() {
    ReglasDeAccesoGateway reglas =
        reglasCon(List.of("GET /api/donaciones/contadores", "GET /api/entregas/mapa"));

    assertThat(reglas.rutasPublicas())
        .containsExactly("/api/donaciones/contadores", "/api/entregas/mapa");
  }

  @Test
  void abreLaRutaDeUnPostComoElContactoDeLaPantallaPublica() {
    assertThat(reglasCon(List.of("POST /api/contacto")).rutasPublicas())
        .containsExactly("/api/contacto");
  }

  @Test
  void laMismaRutaConDosMetodosNoSeRepite() {
    assertThat(reglasCon(List.of("GET /api/x", "POST /api/x")).rutasPublicas())
        .containsExactly("/api/x");
  }

  @Test
  void toleraEspaciosDeMas() {
    assertThat(reglasCon(List.of("  GET   /api/entregas/mapa  ")).rutasPublicas())
        .containsExactly("/api/entregas/mapa");
  }

  @Test
  void sinAllowlistNoAbreNingunaRuta() {
    assertThat(reglasCon(null).rutasPublicas()).isEmpty();
    assertThat(reglasCon(List.of()).rutasPublicas()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "FETCH /api/x",
        "get /api/x",
        "/api/donaciones/contadores",
        "GET api/x",
        "GET /api/x extra",
        ""
      })
  void unaEntradaMalEscritaCortaElArranque(String entrada) {
    List<String> rutas = List.of(entrada);

    assertThatThrownBy(() -> reglasCon(rutas))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MÉTODO /ruta");
  }

  @Test
  void unaEntradaMalaCortaAunqueHayaBuenas() {
    List<String> rutas = List.of("GET /api/ok", "FETCH /api/mal");

    assertThatThrownBy(() -> reglasCon(rutas)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void laListaDevueltaNoSePuedeModificar() {
    List<String> publicas = reglasCon(List.of("GET /api/entregas/mapa")).rutasPublicas();

    assertThatThrownBy(() -> publicas.add("/api/otra"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
