package grupo5.cliente.rest;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.EntidadConfirmarPuerto.Acta;
import grupo5.cliente.vistas.EntidadEntregaVista;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Confirmar recepción contra respuestas de Logística y Donaciones. */
class EntidadConfirmarRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String ENTREGA = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000d1";
  private static final String NECESIDAD = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String LOGISTICA = "http://logistica/api/entregas/";

  private final RestClient.Builder deLogistica = RestClient.builder();
  private final MockRestServiceServer logistica = MockRestServiceServer.bindTo(deLogistica).build();
  private final RestClient.Builder deDonaciones = RestClient.builder();
  private final MockRestServiceServer donaciones =
      MockRestServiceServer.bindTo(deDonaciones).build();
  // "Hoy" es el 8 de noviembre de 2026 en Buenos Aires.
  private final EntidadConfirmarRest adaptador =
      new EntidadConfirmarRest(
          ClientesHttp.cliente(deLogistica, "http://logistica", LogisticaApi.class),
          ClientesHttp.cliente(deDonaciones, "http://donaciones", DonacionesApi.class),
          Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC));

  @Test
  void esperanLasEnTrasladoConLoQueLlevanYLaNecesidadQueCubren() {
    logistica
        .expect(requestTo(LOGISTICA + "seguimiento/beneficiarios/" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_TRASLADO",
                  "horaSalida": "2026-11-08T17:30:00Z", "horaArribo": "2026-11-08T21:20:00Z"},
                 {"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000a2", "idDonacion": "%s",
                  "estadoEntrega": "PENDIENTE"},
                 {"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000a3", "idDonacion": "%s",
                  "estadoEntrega": "ENTREGADA", "horaArribo": "2026-11-07T15:00:00Z"}]
                """
                    .formatted(ENTREGA, DONACION, DONACION, DONACION),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/donaciones-independientes?entidadId=" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "descripcion": "Lentejas secas", "estadoActual": "EnTraslado",
                  "items": [{"bien": {"categoria": {"id": "%s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
                  "cantidad": 15, "necesidadId": "%s",
                  "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}}]
                """
                    .formatted(DONACION, NECESIDAD, NECESIDAD),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/necesidades/" + NECESIDAD))
        .andRespond(
            withSuccess(
                "{\"descripcion\": \"Lentejas secas\", \"cantidadNecesitada\": 40,"
                    + " \"unidad\": \"KILOGRAMO\", \"fechaLimite\": \"2026-11-30\"}",
                MediaType.APPLICATION_JSON));

    List<EntidadEntregaVista> entregas = adaptador.entregas(ENTIDAD);

    logistica.verify();
    donaciones.verify();
    assertEquals(1, entregas.size());
    EntidadEntregaVista e = entregas.getFirst();
    assertEquals(ENTREGA, e.clave());
    assertEquals("lentejas secas", e.nombre());
    assertEquals("Lentejas secas, 15 kg", e.titulo());
    assertEquals("8 de noviembre, 14:30", e.salio());
    assertNull(e.llego());
    assertEquals("Salió del depósito el 8 de noviembre a las 14:30.", e.llegoFrase());
    assertEquals("cat-alimentos", e.categoria());
    assertEquals("Lentejas secas, 40 kg, hasta el 30 de noviembre.", e.pedido());
    assertNull(e.recibido());
    assertEquals("Distribuidora Ejemplo SA", e.dono());
    assertEquals("empresa", e.donoTipo());
    assertEquals(15, e.salida());
    assertEquals("kg", e.unidad());
    assertEquals(true, e.decimales());
    assertNull(e.remito());
    assertEquals("8 de noviembre de 2026", adaptador.hoy());
  }

  @Test
  void sinNingunaEnLaPuertaNoPideLasDonaciones() {
    logistica
        .expect(requestTo(LOGISTICA + "seguimiento/beneficiarios/" + ENTIDAD))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertFalse(adaptador.entregas(ENTIDAD).iterator().hasNext());
    donaciones.verify();
  }

  @Test
  void confirmarSubeLasFotosYDejaLaEntregaEntregadaConElProblema() {
    logistica
        .expect(requestTo(LOGISTICA + ENTREGA + "/fotos"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, startsWith("multipart/form-data")))
        .andRespond(
            withSuccess(
                "[{\"url\": \"https://fotos.ejemplo/1.jpg\"}]", MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(LOGISTICA + ENTREGA + "/estado"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(
            content()
                .json(
                    """
                    {"estado": "ENTREGADA", "actor": "%s",
                     "fotosRecepcion": ["https://fotos.ejemplo/1.jpg"],
                     "fotoRecepcionUrl": "https://fotos.ejemplo/1.jpg",
                     "numeroRemito": "0001-00004521",
                     "reporte": {"motivo": "INCOMPLETA", "detalle": "Dos bolsas rotas.",
                                 "cantidadRecibida": 12.5}}
                    """
                        .formatted(ENTIDAD)))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    MockMultipartFile foto =
        new MockMultipartFile("fotos", "acta.jpg", "image/jpeg", new byte[] {1, 2, 3});

    adaptador.confirmar(
        ENTIDAD,
        ENTREGA,
        new Acta(List.of(foto), " 0001-00004521 ", "incompleta", 12.5, "Dos bolsas rotas."));

    logistica.verify();
  }
}
