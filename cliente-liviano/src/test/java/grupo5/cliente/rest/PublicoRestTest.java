package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.PublicoCifra;
import grupo5.cliente.vistas.PublicoEntregas;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    Clock noviembre = Clock.fixed(Instant.parse("2026-11-10T17:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new PublicoRest(ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class), noviembre);
  }

  @Test
  void lasEntregasSalenDeLasDestacadasDelMesConSuFotoYLasDeLaSemanaMarcadas() {
    servidor
        .expect(requestTo(DONACIONES + "/api/donaciones/destacadas?periodo=MES&limite=50"))
        .andRespond(
            withSuccess(
                """
                [{"id": "00000000-0000-4000-8000-000000000001", "categoria": "Muebles",
                  "subcategoria": "Sillas", "cantidad": 4, "unidad": "UNIDADES",
                  "entidadId": "00000000-0000-4000-8000-000000000002",
                  "entidad": "Biblioteca Popular Ejemplo",
                  "fechaEntrega": "2026-10-20T10:00:00", "fotos": ["https://fotos/1.jpg"]},
                 {"id": "00000000-0000-4000-8000-000000000003", "categoria": "Alimentos",
                  "subcategoria": "Arroz", "cantidad": 5, "unidad": "KILOGRAMO",
                  "entidadId": "00000000-0000-4000-8000-000000000004",
                  "entidad": "Comedor Barrio Ejemplo",
                  "fechaEntrega": "2026-11-09T12:00:00", "fotos": ["https://fotos/2.jpg"]},
                 {"id": "00000000-0000-4000-8000-000000000005", "categoria": "Otra",
                  "subcategoria": null, "cantidad": null, "unidad": null,
                  "entidad": "Club Ejemplo Norte", "fechaEntrega": "2026-11-01T12:00:00",
                  "fotos": []}]
                """,
                MediaType.APPLICATION_JSON));

    PublicoEntregas entregas = adaptador.entregasConfirmadas();

    assertThat(entregas.lista())
        .extracting(PublicoEntregas.Entrega::que)
        .containsExactly("Arroz, 5 kg", "Sillas, 4 unidades");
    PublicoEntregas.Entrega arroz = entregas.lista().getFirst();
    assertThat(arroz.cat()).isEqualTo("alimentos");
    assertThat(arroz.lugar()).isEqualTo("Comedor Barrio Ejemplo");
    assertThat(arroz.dia()).isEqualTo("9 de noviembre");
    assertThat(arroz.anio()).isEqualTo("2026");
    assertThat(arroz.semana()).isTrue();
    assertThat(arroz.foto()).isEqualTo("https://fotos/2.jpg");
    assertThat(entregas.lista().get(1).semana()).isFalse();
    assertThat(entregas.rangoMes()).isEqualTo("del 11 de octubre al 10 de noviembre de 2026");
    assertThat(entregas.rangoSemana()).isEqualTo("del 3 al 10 de noviembre de 2026");
    assertThat(entregas.actualizado()).isEqualTo("10 de noviembre de 2026, 14:30");
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
