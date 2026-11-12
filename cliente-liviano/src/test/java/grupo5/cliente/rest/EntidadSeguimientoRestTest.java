package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Las noticias de Seguir entregas contra respuestas de Logística. */
class EntidadSeguimientoRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000d1";

  @Test
  void laUltimaActualizacionDeCadaEntregaPorSuDonacion() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer logistica = MockRestServiceServer.bindTo(builder).build();
    logistica
        .expect(requestTo("http://logistica/api/entregas/seguimiento/beneficiarios/" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000a1", "idDonacion": "%s",
                  "estadoEntrega": "EN_CAMINO", "ultimaActualizacion": "2026-11-08T21:25:00Z"},
                 {"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000a2", "estadoEntrega": "PENDIENTE"}]
                """
                    .formatted(DONACION),
                MediaType.APPLICATION_JSON));
    EntidadSeguimientoRest adaptador =
        new EntidadSeguimientoRest(
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class));

    Map<String, String> noticias = adaptador.noticias(ENTIDAD);

    logistica.verify();
    assertEquals(Map.of(DONACION, "Última noticia: 8 de noviembre, 18:25."), noticias);
  }
}
