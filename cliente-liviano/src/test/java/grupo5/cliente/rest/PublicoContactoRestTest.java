package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.ContactoRechazado;
import grupo5.cliente.puertos.PublicoContactoPuerto.Mensaje;
import grupo5.cliente.puertos.PublicoContactoPuerto.Motivo;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Contacto contra el contrato de HB-21 (SPEC-08), con respuestas de ejemplo de Notificaciones. */
class PublicoContactoRestTest {

  private static final String CONTACTO = "http://notificaciones/api/contacto";
  private static final UUID CLAVE = UUID.fromString("00000000-0000-4000-8000-000000000021");
  private static final Mensaje MENSAJE =
      new Mensaje("SITIO", "No cargó el mapa.", "a1b2c3d4e5f6", "ana@ejemplo.test", CLAVE);

  private MockRestServiceServer servidor;
  private PublicoContactoRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
    adaptador =
        new PublicoContactoRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class));
  }

  @Test
  void mandaElMensajeConLaIpDelNavegadorYDevuelveSuNumero() {
    servidor
        .expect(requestTo(CONTACTO))
        .andExpect(method(POST))
        .andExpect(header("X-Forwarded-For", "203.0.113.7"))
        .andExpect(
            content()
                .json(
                    """
                    {"tema": "SITIO", "texto": "No cargó el mapa.", "codigoError": "a1b2c3d4e5f6",
                     "correo": "ana@ejemplo.test", "claveEnvio": "%s"}
                    """
                        .formatted(CLAVE)))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"numero\": 4821}"));

    assertThat(adaptador.enviar(MENSAJE, "203.0.113.7")).isEqualTo(4821);
  }

  @Test
  void laMismaClaveOtraVezDevuelveElNumeroDelPrimero() {
    servidor
        .expect(requestTo(CONTACTO))
        .andRespond(withSuccess("{\"numero\": 4821}", MediaType.APPLICATION_JSON));

    assertThat(adaptador.enviar(MENSAJE, "203.0.113.7")).isEqualTo(4821);
  }

  @Test
  void elLimiteYLosDatosQueNoValidanSonRechazosQueLaPantallaExplica() {
    servidor.expect(requestTo(CONTACTO)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    servidor.expect(requestTo(CONTACTO)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThatThrownBy(() -> adaptador.enviar(MENSAJE, "203.0.113.7"))
        .isInstanceOfSatisfying(
            ContactoRechazado.class, r -> assertThat(r.motivo()).isEqualTo(Motivo.LIMITE));
    assertThatThrownBy(() -> adaptador.enviar(MENSAJE, "203.0.113.7"))
        .isInstanceOfSatisfying(
            ContactoRechazado.class, r -> assertThat(r.motivo()).isEqualTo(Motivo.INVALIDO));
  }

  @Test
  void conNotificacionesCaidoLaFallaVaALasPaginasDeError() {
    servidor.expect(requestTo(CONTACTO)).andRespond(withServerError());

    assertThatThrownBy(() -> adaptador.enviar(MENSAJE, "203.0.113.7"))
        .isInstanceOf(RestClientException.class)
        .isNotInstanceOf(ContactoRechazado.class);
  }

  @Test
  void cadaFormularioLlevaUnaClaveNuevaYNoHayNumeroDeEjemplo() {
    assertThat(adaptador.nuevaClave()).isNotNull().isNotEqualTo(adaptador.nuevaClave());
    assertThat(adaptador.numeroDeEjemplo()).isNull();
  }
}
