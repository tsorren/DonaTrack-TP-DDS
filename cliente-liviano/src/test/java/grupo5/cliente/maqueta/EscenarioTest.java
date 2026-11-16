package grupo5.cliente.maqueta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

class EscenarioTest {

  @AfterEach
  void limpiar() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void sinParametroHayDatos() {
    assertThat(Escenario.de(null)).isEqualTo(Escenario.CON_DATOS);
    assertThat(Escenario.de(" ")).isEqualTo(Escenario.CON_DATOS);
  }

  @Test
  void leeElEscenarioDeLaUrl() {
    MockHttpServletRequest pedido = new MockHttpServletRequest();
    pedido.setParameter("escenario", "sin-permiso");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(pedido));

    assertThat(Escenario.actual()).isEqualTo(Escenario.SIN_PERMISO);
  }

  @Test
  void fueraDeUnPedidoHayDatos() {
    assertThat(Escenario.actual()).isEqualTo(Escenario.CON_DATOS);
  }

  @Test
  void unEscenarioDesconocidoEsUnError() {
    assertThatThrownBy(() -> Escenario.de("vacia"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("vacia");
  }
}
