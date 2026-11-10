package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.DetalleDonacionVista.Noticia;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El detalle de una donación contra respuestas de ejemplo de Donaciones y Logística. */
class DetalleDonacionRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String FIDEOS = "6f1c2c1e-0000-4000-8000-000000000001";
  private static final String UTILES = "6f1c2c1e-0000-4000-8000-000000000002";
  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000f1";
  private static final String LISTA =
      "http://donaciones/donaciones-independientes?donanteId=" + DONANTE;
  private static final String COMEDOR = "Comedor Barrio Ejemplo";

  /** Una entregada (la primera) y una que no pudo llegar, con su motivo. */
  private static final String DOS =
      """
      [
        {"id": "%1$s", "descripcion": "Fideos secos", "estadoActual": "Entregada",
         "fechaRegistro": "2026-10-01T10:00:00", "cantidad": 8, "entidadBeneficiariaId": "%3$s",
         "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"},
         "historial": [
           {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-10-02T12:00:00"},
           {"estadoNuevo": "Entregada", "timestamp": "2026-10-05T17:20:00"},
           {"estadoNuevo": "ListaParaEntregar", "timestamp": "2026-10-03T09:00:00"},
           {"estadoNuevo": "EnTraslado", "timestamp": "2026-10-05T10:00:00"}]},
        {"id": "%2$s", "descripcion": "Útiles escolares", "estadoActual": "EntregaFallida",
         "fechaRegistro": "2026-10-08T10:00:00", "cantidad": 2, "entidadBeneficiariaId": "%3$s",
         "historial": [
           {"estadoNuevo": "EnTraslado", "timestamp": "2026-10-11T09:00:00"},
           {"estadoNuevo": "EntregaFallida", "timestamp": "2026-10-11T16:10:00",
            "justificacion": "el camino estaba cortado"}]}
      ]
      """
          .formatted(FIDEOS, UTILES, ENTIDAD);

  private MockRestServiceServer apis;
  private DonacionesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    // "Hoy" es el 12 de octubre: la noticia del 11 dice "Ayer".
    Clock hoy = Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            hoy);
    apis.expect(requestTo(LISTA)).andRespond(withSuccess(DOS, MediaType.APPLICATION_JSON));
  }

  private void conLaEntidad() {
    apis.expect(requestTo("http://donaciones/api/entidades/publicas"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"" + COMEDOR + "\"}]",
                MediaType.APPLICATION_JSON));
  }

  @Test
  void unaEntregadaTraeLaFotoDeLogisticaYTodasSusEtapasHechas() {
    conLaEntidad();
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donaciones/" + FIDEOS))
        .andRespond(
            withSuccess("{\"idEntrega\": \"" + ENTREGA + "\"}", MediaType.APPLICATION_JSON));
    apis.expect(requestTo("http://logistica/api/entregas/" + ENTREGA))
        .andRespond(
            withSuccess(
                "{\"fotoRecepcionUrl\": \"https://fotos.ejemplo/a.jpg\","
                    + " \"fotosRecepcion\": [\"https://fotos.ejemplo/a.jpg\"]}",
                MediaType.APPLICATION_JSON));

    DetalleDonacionVista detalle = adaptador.detalle(DONANTE, FIDEOS).orElseThrow();

    apis.verify();
    assertEquals(1, detalle.donacion().numero());
    assertEquals("5 de octubre de 2026", detalle.llego());
    assertEquals("https://fotos.ejemplo/a.jpg", detalle.foto());
    assertEquals(
        COMEDOR + " confirmó la entrega el 5 de octubre y subió esta foto.", detalle.pieFoto());
    assertNull(detalle.problema());
    assertEquals("Distribuidora Ejemplo SA", detalle.figuraComo());
    assertEquals(
        List.of(
            new PasoRecorrido("En depósito", "1 de octubre", Avance.HECHO),
            new PasoRecorrido("Asignada", "2 de octubre", Avance.HECHO),
            new PasoRecorrido("Lista para entregar", "3 de octubre", Avance.HECHO),
            new PasoRecorrido("En camino", "5 de octubre", Avance.HECHO),
            new PasoRecorrido("Entregada", "5 de octubre", Avance.HECHO)),
        detalle.pasos());
    assertEquals(
        List.of(
            new Noticia(
                "5 de octubre, 17:20",
                "2026-10-05T17:20",
                COMEDOR + " confirmó que recibió tu donación y sumó una foto."),
            new Noticia(
                "5 de octubre, 10:00",
                "2026-10-05T10:00",
                "El camión salió a entregar tu donación a " + COMEDOR + "."),
            new Noticia(
                "3 de octubre, 9:00", "2026-10-03T09:00", "Tu donación quedó lista para entregar."),
            new Noticia(
                "2 de octubre, 12:00",
                "2026-10-02T12:00",
                "Asignamos tu donación a una necesidad de " + COMEDOR + ".")),
        detalle.noticias());
  }

  @Test
  void unaEntregadaSinEntregaEnLogisticaDiceQueSeConfirmoSinFoto() {
    conLaEntidad();
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donaciones/" + FIDEOS))
        .andRespond(withResourceNotFound());

    DetalleDonacionVista detalle = adaptador.detalle(DONANTE, FIDEOS).orElseThrow();

    assertNull(detalle.foto());
    assertEquals(COMEDOR + " confirmó la entrega el 5 de octubre.", detalle.pieFoto());
    assertEquals(
        COMEDOR + " confirmó que recibió tu donación.", detalle.noticias().getFirst().texto());
  }

  @Test
  void conLogisticaCaidaLaEntregadaSeMuestraSinFoto() {
    conLaEntidad();
    apis.expect(requestTo("http://logistica/api/entregas/seguimiento/donaciones/" + FIDEOS))
        .andRespond(withServerError());

    DetalleDonacionVista detalle = adaptador.detalle(DONANTE, FIDEOS).orElseThrow();

    assertNull(detalle.foto());
    assertEquals(COMEDOR + " confirmó la entrega el 5 de octubre.", detalle.pieFoto());
  }

  @Test
  void unaQueNoPudoLlegarMuestraElMotivoYSeQuedaEnCamino() {
    conLaEntidad();

    DetalleDonacionVista detalle = adaptador.detalle(DONANTE, UTILES).orElseThrow();

    apis.verify();
    assertEquals(2, detalle.donacion().numero());
    assertEquals("No pudo llegar a " + COMEDOR + ".", detalle.problema().titulo());
    assertEquals(
        "El camino estaba cortado. Última noticia: 11 de octubre, 16:10.",
        detalle.problema().texto());
    assertEquals(
        new PasoRecorrido("En camino", "11 de octubre: no pudo llegar", Avance.ACTUAL),
        detalle.pasos().get(3));
    assertEquals(Avance.PENDIENTE, detalle.pasos().get(4).avance());
    assertEquals(
        new Noticia(
            "Ayer, 16:10",
            "2026-10-11T16:10",
            "El camión no pudo llegar a " + COMEDOR + ": el camino estaba cortado."),
        detalle.noticias().getFirst());
    assertNull(detalle.foto());
    assertNull(detalle.pieFoto());
    // Sin quienDono (o con el nombre oculto), la ficha no muestra "Figura como".
    assertNull(detalle.figuraComo());
  }

  @Test
  void unaDonacionQueNoEsDelDonanteNoSeMuestra() {
    Optional<DetalleDonacionVista> detalle =
        adaptador.detalle(DONANTE, "6f1c2c1e-0000-4000-8000-000000000099");

    apis.verify();
    assertTrue(detalle.isEmpty());
  }
}
