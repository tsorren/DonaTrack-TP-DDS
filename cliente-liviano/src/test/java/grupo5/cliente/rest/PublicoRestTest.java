package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.PublicoCifra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Las páginas públicas contra respuestas de ejemplo de Donaciones (plan E6, §F1). */
class PublicoRestTest {

  private static final String DONACIONES = "http://donaciones";

  private MockRestServiceServer servidor;
  private PublicoRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
    adaptador = new PublicoRest(ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class));
  }

  @Test
  void lasCifrasSalenDeLosContadoresEscritasComoEnLaArgentina() {
    servidor
        .expect(requestTo(DONACIONES + "/api/donaciones/contadores"))
        .andRespond(
            withSuccess(
                """
                {"donantesActivos": 318, "donacionesRealizadas": 1284,
                 "beneficiariosActivos": 46, "calculadoEn": "2026-11-10T12:00:00-03:00"}
                """,
                MediaType.APPLICATION_JSON));

    assertThat(adaptador.cifrasDePortada())
        .containsExactly(
            new PublicoCifra("1.284", "Entregas confirmadas"),
            new PublicoCifra("46", "Entidades en el mapa"),
            new PublicoCifra("318", "Donantes con al menos una donación"));
  }

  @Test
  void unContadorQueFaltaEsCeroYNosotrosMuestraLasMismas() {
    servidor
        .expect(requestTo(DONACIONES + "/api/donaciones/contadores"))
        .andRespond(withSuccess("{\"donantesActivos\": 2}", MediaType.APPLICATION_JSON));

    assertThat(adaptador.cifrasDeNosotros())
        .extracting(PublicoCifra::n)
        .containsExactly("0", "0", "2");
  }

  @Test
  void conDonacionesCaidoLaFallaLlegaAlController() {
    servidor
        .expect(requestTo(DONACIONES + "/api/donaciones/contadores"))
        .andRespond(withServerError());

    assertThatThrownBy(adaptador::cifrasDePortada).isInstanceOf(RestClientException.class);
  }
}
