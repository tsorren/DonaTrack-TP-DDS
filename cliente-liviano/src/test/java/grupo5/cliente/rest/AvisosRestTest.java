package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.CuentaActual;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El conteo de sin leer de la cabecera contra respuestas de ejemplo de Notificaciones. */
class AvisosRestTest {

  private static final UUID PERSONA = UUID.fromString("00000000-0000-4000-8000-000000000011");
  private static final String CONTEO =
      "http://notificaciones/api/notificaciones/persona/" + PERSONA + "/no-leidas/conteo";

  private static final UUID ADMINISTRACION =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final HttpServletRequest pedido = new MockHttpServletRequest();
  private MockRestServiceServer notificaciones;
  private AvisosRest adaptador;

  /** La misma persona es donante y entidad: lo que cambia es el área que se pide. */
  private static final class UnaPersona implements CuentaActual {

    @Override
    public Optional<UUID> donante(HttpServletRequest pedido) {
      return Optional.of(PERSONA);
    }

    @Override
    public Optional<UUID> entidad(HttpServletRequest pedido) {
      return Optional.of(PERSONA);
    }
  }

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    notificaciones = MockRestServiceServer.bindTo(builder).build();
    adaptador =
        new AvisosRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
            new UnaPersona(),
            ADMINISTRACION);
  }

  @Test
  void elDonanteYLaEntidadVenLasSuyas() {
    notificaciones
        .expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 5}", MediaType.APPLICATION_JSON));
    notificaciones
        .expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": 0}", MediaType.APPLICATION_JSON));

    assertThat(adaptador.sinLeer("donante", pedido)).isEqualTo(5);
    assertThat(adaptador.sinLeer("entidad", pedido)).isZero();
    notificaciones.verify();
  }

  /** C8 (R11-30): la administración cuenta las de la bandeja compartida de la Administración. */
  @Test
  void laAdministracionVeLasDeLaBandejaDeLaAdministracion() {
    notificaciones
        .expect(
            requestTo(
                "http://notificaciones/api/notificaciones/persona/"
                    + ADMINISTRACION
                    + "/no-leidas/conteo"))
        .andRespond(withSuccess("{\"noLeidas\": 2}", MediaType.APPLICATION_JSON));

    assertThat(adaptador.sinLeer("admin", pedido)).isEqualTo(2);
    notificaciones.verify();
  }

  @Test
  void unAreaDesconocidaNoSePide() {
    assertThat(adaptador.sinLeer("publico", pedido)).isZero();
    notificaciones.verify();
  }

  @Test
  void conNotificacionesCaidoLaCabeceraQuedaSinNumero() {
    notificaciones.expect(requestTo(CONTEO)).andRespond(withServerError());

    assertThat(adaptador.sinLeer("donante", pedido)).isZero();
  }
}
