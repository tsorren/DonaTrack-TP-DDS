package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Entidad;
import grupo5.cliente.vistas.DonanteEntidadesVista.Pedido;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Entidades del donante contra respuestas de ejemplo de Donaciones. */
class DonanteEntidadesRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String COMEDOR = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String HOGAR = "6f1c2c1e-0000-4000-8000-0000000000e2";
  private static final String LEGUMBRES = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-000000000001";

  private MockRestServiceServer apis;
  private DonanteEntidadesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonanteEntidadesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class), hoy);
  }

  @Test
  void armaLaPaginaConLoQuePideCadaEntidadYSiYaLeDono() {
    HttpHeaders total = new HttpHeaders();
    total.add("X-Total-Count", "13");
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?nombre=comedor&page=1&size=6"))
        .andRespond(
            withSuccess(
                    "[{\"id\": \"%s\", \"razonSocial\": \"Comedor Barrio Ejemplo\"},"
                            .formatted(COMEDOR)
                        + " {\"id\": \"%s\", \"razonSocial\": \"Hogar Ejemplo\"}]".formatted(HOGAR),
                    MediaType.APPLICATION_JSON)
                .headers(total));
    apis.expect(requestTo("http://donaciones/api/subcategorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"nombre\": \"Legumbres\", \"categoria\":".formatted(LEGUMBRES)
                    + " {\"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\"}}]",
                MediaType.APPLICATION_JSON));
    apis.expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&donanteId=" + DONANTE))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"entidadBeneficiariaId\": \"%s\", \"cantidad\": 1}]"
                    .formatted(DONACION, COMEDOR),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + COMEDOR))
        .andRespond(
            withSuccess(
                "[{\"descripcion\": \"Lentejas secas\", \"cantidadNecesitada\": 40,"
                    + " \"idSubcategoria\": \"%s\", \"fechaFin\": \"2026-10-20\"}]"
                        .formatted(LEGUMBRES),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + HOGAR))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteEntidadesVista vista = adaptador.entidades(DONANTE, " comedor ", 2);

    apis.verify();
    assertEquals(13, vista.total());
    assertEquals(3, vista.paginas());
    assertEquals(3, vista.dibujadas());
    assertEquals("hoy, 15 de octubre, 16:25", vista.actualizado());
    Entidad comedor = vista.entidades().getFirst();
    assertEquals(List.of(DONACION), comedor.ya());
    Pedido lentejas = comedor.pide().getFirst();
    assertEquals("cat-alimentos", lentejas.cat());
    assertEquals("Piden 40 kg", lentejas.falta());
    assertEquals("Hasta el 20 de octubre", lentejas.cuando());
    assertNull(lentejas.recibido());
    assertEquals(List.of(), vista.entidades().get(1).ya());
  }

  @Test
  void sinSubcategoriasLosPedidosQuedanSinCategoria() {
    apis.expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=6"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"razonSocial\": \"Hogar Ejemplo\"}]".formatted(HOGAR),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/subcategorias")).andRespond(withServerError());
    apis.expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&donanteId=" + DONANTE))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + HOGAR))
        .andRespond(
            withSuccess(
                "[{\"descripcion\": \"Frazadas\", \"cantidadNecesitada\": 20}]",
                MediaType.APPLICATION_JSON));

    DonanteEntidadesVista vista = adaptador.entidades(DONANTE, "", 1);

    Pedido frazadas = vista.entidades().getFirst().pide().getFirst();
    assertNull(frazadas.cat());
    assertEquals("Piden 20", frazadas.falta());
    assertEquals("Sin fecha de cierre", frazadas.cuando());
    assertEquals(1, vista.total());
  }
}
