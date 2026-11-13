package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.DonanteEntidadesPuerto.Consulta;
import grupo5.cliente.vistas.DonanteEntidadesVista;
import grupo5.cliente.vistas.DonanteEntidadesVista.Categoria;
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

/**
 * Entidades del donante contra respuestas de ejemplo de Donaciones: la búsqueda, la categoría y "Ya
 * doné" se mandan al servicio (R11-12), que da el total y la marca de cada una.
 */
class DonanteEntidadesRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String COMEDOR = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String HOGAR = "6f1c2c1e-0000-4000-8000-0000000000e2";
  private static final String LEGUMBRES = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String HIGIENE = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final String JABON = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String ENTIDADES =
      "http://donaciones/api/donantes/" + DONANTE + "/entidades";

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

  private void conSubcategorias() {
    apis.expect(requestTo("http://donaciones/api/subcategorias"))
        .andRespond(
            withSuccess(
                ("[{\"id\": \"%s\", \"nombre\": \"Legumbres\", \"categoria\":"
                        + " {\"id\": \"%s\", \"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\"}},"
                        + " {\"id\": \"%s\", \"nombre\": \"Jabón\", \"categoria\":"
                        + " {\"id\": \"%s\", \"nombre\": \"Higiene\", \"unidad\": \"UNIDADES\"}}]")
                    .formatted(LEGUMBRES, ALIMENTOS, JABON, HIGIENE),
                MediaType.APPLICATION_JSON));
  }

  private static HttpHeaders total(int total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  @Test
  void armaLaPaginaConLoQuePideCadaEntidadYLaMarcaDelServicio() {
    conSubcategorias();
    apis.expect(requestTo(ENTIDADES + "?nombre=comedor&page=1&size=6"))
        .andRespond(
            withSuccess(
                    ("[{\"id\": \"%s\", \"razonSocial\": \"Comedor Barrio Ejemplo\","
                            + " \"yaLeDono\": true},"
                            + " {\"id\": \"%s\", \"razonSocial\": \"Hogar Ejemplo\","
                            + " \"yaLeDono\": false}]")
                        .formatted(COMEDOR, HOGAR),
                    MediaType.APPLICATION_JSON)
                .headers(total(13)));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + COMEDOR))
        .andRespond(
            withSuccess(
                "[{\"descripcion\": \"Lentejas secas\", \"cantidadNecesitada\": 40,"
                    + " \"idSubcategoria\": \"%s\", \"fechaFin\": \"2026-10-20\"}]"
                        .formatted(LEGUMBRES),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + HOGAR))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteEntidadesVista vista =
        adaptador.entidades(DONANTE, new Consulta(" comedor ", null, false, 2));

    apis.verify();
    assertEquals(13, vista.total());
    assertEquals(2, vista.pagina());
    assertEquals(3, vista.paginas());
    assertEquals(3, vista.dibujadas());
    assertEquals("hoy, 15 de octubre, 16:25", vista.actualizado());
    // Las categorías reales, por nombre, también las que no tienen ícono propio
    assertEquals(
        List.of(
            new Categoria(ALIMENTOS, "Alimentos", "cat-alimentos"),
            new Categoria(HIGIENE, "Higiene", "cat-higiene")),
        vista.categorias());
    Entidad comedor = vista.entidades().getFirst();
    assertTrue(comedor.ya());
    Pedido lentejas = comedor.pide().getFirst();
    assertEquals(ALIMENTOS, lentejas.categoria());
    assertEquals("cat-alimentos", lentejas.cat());
    assertEquals("Piden 40 kg", lentejas.falta());
    assertEquals("Hasta el 20 de octubre", lentejas.cuando());
    assertNull(lentejas.recibido());
    assertFalse(vista.entidades().get(1).ya());
  }

  /** B4 y B5: los filtros van al servicio y no se pide ni las públicas ni las donaciones. */
  @Test
  void mandaLaCategoriaYSoloDonadasAlServicio() {
    conSubcategorias();
    apis.expect(
            requestTo(
                ENTIDADES
                    + "?nombre=comedor&categoriaId="
                    + HIGIENE
                    + "&soloDonadas=true&page=1&size=6"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(7)));

    DonanteEntidadesVista vista =
        adaptador.entidades(DONANTE, new Consulta("comedor", HIGIENE, true, 2));

    apis.verify();
    assertEquals(7, vista.total());
    assertEquals(2, vista.paginas());
  }

  @Test
  void unaCategoriaQueNoEstaNoSeManda() {
    conSubcategorias();
    apis.expect(requestTo(ENTIDADES + "?page=0&size=6"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(0)));

    adaptador.entidades(DONANTE, new Consulta("", "alimentos", false, 1));

    apis.verify();
  }

  /** Regla general 3: una página más allá de la última muestra la última. */
  @Test
  void unaPaginaMasAllaDeLaUltimaPideLaUltima() {
    conSubcategorias();
    apis.expect(requestTo(ENTIDADES + "?soloDonadas=true&page=8&size=6"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(3)));
    apis.expect(requestTo(ENTIDADES + "?soloDonadas=true&page=0&size=6"))
        .andRespond(
            withSuccess(
                    "[{\"id\": \"%s\", \"razonSocial\": \"Hogar Ejemplo\", \"yaLeDono\": true}]"
                        .formatted(HOGAR),
                    MediaType.APPLICATION_JSON)
                .headers(total(3)));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + HOGAR))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteEntidadesVista vista = adaptador.entidades(DONANTE, new Consulta("", null, true, 9));

    apis.verify();
    assertEquals(1, vista.pagina());
    assertEquals(1, vista.paginas());
  }

  @Test
  void sinSubcategoriasLosPedidosQuedanSinCategoria() {
    apis.expect(requestTo("http://donaciones/api/subcategorias")).andRespond(withServerError());
    apis.expect(requestTo(ENTIDADES + "?page=0&size=6"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"razonSocial\": \"Hogar Ejemplo\", \"yaLeDono\": false}]"
                    .formatted(HOGAR),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://donaciones/api/necesidades/publicas?entidadId=" + HOGAR))
        .andRespond(
            withSuccess(
                "[{\"descripcion\": \"Frazadas\", \"cantidadNecesitada\": 20}]",
                MediaType.APPLICATION_JSON));

    DonanteEntidadesVista vista = adaptador.entidades(DONANTE, new Consulta("", null, false, 1));

    Pedido frazadas = vista.entidades().getFirst().pide().getFirst();
    assertNull(frazadas.categoria());
    assertNull(frazadas.cat());
    assertEquals("Piden 20", frazadas.falta());
    assertEquals("Sin fecha de cierre", frazadas.cuando());
    assertEquals(1, vista.total());
    assertEquals(List.of(), vista.categorias());
  }
}
