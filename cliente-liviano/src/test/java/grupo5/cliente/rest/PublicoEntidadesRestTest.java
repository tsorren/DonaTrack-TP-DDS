package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Entidades y necesidades (público) contra respuestas de ejemplo de Donaciones. */
class PublicoEntidadesRestTest {

  private static final String DONACIONES = "http://donaciones";
  private static final String COMEDOR = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String PARROQUIA = "6f1c2c1e-0000-4000-8000-0000000000e2";
  private static final String SIN_PEDIDOS = "6f1c2c1e-0000-4000-8000-0000000000e3";
  private static final String LEGUMBRES = "6f1c2c1e-0000-4000-8000-0000000000c1";

  private MockRestServiceServer apis;
  private PublicoEntidadesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    Clock hoy = Clock.fixed(Instant.parse("2026-11-10T17:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new PublicoEntidadesRest(
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class), hoy);
  }

  private void entidadesYNecesidades() {
    HttpHeaders total = new HttpHeaders();
    total.add("X-Total-Count", "46");
    apis.expect(requestTo(DONACIONES + "/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                    """
                    [{"id": "%s", "razonSocial": "Comedor Barrio Ejemplo", "rubro": "Comedor"},
                     {"id": "%s", "razonSocial": "Parroquia Ejemplo", "rubro": "Acción social"},
                     {"id": "%s", "razonSocial": "Club Ejemplo", "rubro": "Club de barrio"}]
                    """
                        .formatted(COMEDOR, PARROQUIA, SIN_PEDIDOS),
                    MediaType.APPLICATION_JSON)
                .headers(total));
    apis.expect(requestTo(DONACIONES + "/api/necesidades/publicas"))
        .andRespond(
            withSuccess(
                """
                [{"id": "6f1c2c1e-0000-4000-8000-0000000000f1", "idEntidad": "%s",
                  "idSubcategoria": "%s", "descripcion": "Lentejas secas",
                  "cantidadNecesitada": 40, "tipo": "RECURRENTE", "fechaFin": "2026-11-30"},
                 {"id": "6f1c2c1e-0000-4000-8000-0000000000f2", "idEntidad": "%s",
                  "descripcion": "Frazadas", "cantidadNecesitada": 20,
                  "tipo": "EXTRAORDINARIA", "fechaFin": null}]
                """
                    .formatted(COMEDOR, LEGUMBRES, PARROQUIA),
                MediaType.APPLICATION_JSON));
  }

  @Test
  void conSesionLosPedidosTraenCategoriaYUnidad() {
    entidadesYNecesidades();
    apis.expect(requestTo(DONACIONES + "/api/subcategorias"))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "nombre": "Legumbres",
                  "categoria": {"nombre": "Alimentos", "unidad": "KILOGRAMO"}}]
                """
                    .formatted(LEGUMBRES),
                MediaType.APPLICATION_JSON));

    PublicoEntidadesVista vista = adaptador.entidades();

    apis.verify();
    assertTrue(vista.conCategorias());
    assertFalse(vista.conEmergencias());
    assertTrue(vista.nota().startsWith("Todavía no mostramos"));
    Entidad comedor = vista.entidades().getFirst();
    assertEquals("Comedor", comedor.tipo());
    assertEquals("alimentos", comedor.cat());
    Pedido lentejas = comedor.pedidos().getFirst();
    assertEquals("alimentos", lentejas.cat());
    assertEquals("Piden 40 kg", lentejas.falta());
    assertEquals("Se repite; este período termina el 30 de noviembre", lentejas.cuando());
    assertEquals("recurrente", lentejas.tipo());
    assertEquals(40, lentejas.total());
  }

  @Test
  void sinSesionLaPaginaSigueYLosPedidosVanSinCategoria() {
    entidadesYNecesidades();
    apis.expect(requestTo(DONACIONES + "/api/subcategorias"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    PublicoEntidadesVista vista = adaptador.entidades();

    apis.verify();
    assertFalse(vista.conCategorias());
    assertNull(vista.entidades().getFirst().cat());
    assertNull(vista.entidades().getFirst().pedidos().getFirst().cat());
    assertEquals("Piden 40", vista.entidades().getFirst().pedidos().getFirst().falta());
  }

  @Test
  void soloSeMuestranLasEntidadesConPedidosYElTotalEsElDeLasActivas() {
    entidadesYNecesidades();
    apis.expect(requestTo(DONACIONES + "/api/subcategorias"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    PublicoEntidadesVista vista = adaptador.entidades();

    assertEquals(2, vista.entidades().size());
    assertEquals(46, vista.total());
    assertEquals("10 de noviembre de 2026", vista.fecha());
    assertNull(vista.periodo());
    Entidad parroquia = vista.entidades().get(1);
    assertEquals("Otra", parroquia.tipo());
    assertEquals(PARROQUIA, parroquia.clave());
    assertNull(parroquia.numero());
    assertNull(parroquia.cubiertas());
    assertNull(parroquia.mision());
    Pedido frazadas = parroquia.pedidos().getFirst();
    assertEquals("Sin fecha de cierre", frazadas.cuando());
    assertEquals("extraordinaria", frazadas.tipo());
    assertNull(frazadas.faltan());
    assertFalse(frazadas.emergencia());
  }

  @Test
  void sinEntidadesConPedidosLaListaVa() {
    apis.expect(requestTo(DONACIONES + "/api/entidades/publicas?page=0&size=100"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(DONACIONES + "/api/necesidades/publicas"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    apis.expect(requestTo(DONACIONES + "/api/subcategorias"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    PublicoEntidadesVista vista = adaptador.entidades();

    assertTrue(vista.entidades().isEmpty());
    assertEquals(0, vista.total());
  }
}
