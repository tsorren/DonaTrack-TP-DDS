package grupo5.donaciones.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReglasDeAccesoDonacionesTest {

  private final ReglasDeAccesoDonaciones reglas = new ReglasDeAccesoDonaciones();

  // SPEC-11, D08: la ruta de GraphQL entra sin token; cada resolver decide con su @PreAuthorize
  @Test
  void laRutaDeGraphQlNoPideToken() {
    assertThat(reglas.rutasPublicas()).contains("/api/donaciones/graphql");
  }

  @Test
  void lasRutasPublicasDeRestSiguenIgual() {
    assertThat(reglas.rutasPublicas())
        .contains(
            "/api/entidades/publicas",
            "/api/necesidades/publicas",
            "/api/donaciones/contadores",
            "/api/donaciones/destacadas")
        .hasSize(5);
  }
}
