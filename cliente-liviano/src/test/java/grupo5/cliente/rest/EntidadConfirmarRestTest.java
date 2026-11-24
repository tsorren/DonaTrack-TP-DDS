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
  private static final String LEGUMBRES = "6f1c2c1e-0000-4000-8000-0000000000c5";
  private static final String LOGISTICA = "http://logistica/api/entregas/";

  /** R11-43: de Donaciones, solo la donación del acta, si es de la entidad y espera. */
  private static final String LA_PEDIDA =
      "http://donaciones/donaciones-independientes?entidadId="
          + ENTIDAD
          + "&id="
          + DONACION
          + "&estado=EN_PUERTA&estado=EN_TRASLADO&orden=ATENCION&size=1";

  /** R11-43: y una página de 2 de las que esperan, la puerta primero, para ofrecer la que sigue. */
  private static final String LA_OTRA =
      "http://donaciones/donaciones-independientes?entidadId="
          + ENTIDAD
          + "&estado=EN_PUERTA&estado=EN_TRASLADO&orden=ATENCION&size=2";

  /**
   * ORG-12: la confirmada, en Donaciones, ENTREGADA o, si todavía no se enteró, en la puerta o en
   * traslado.
   */
  private static final String LA_CONFIRMADA =
      "http://donaciones/donaciones-independientes?entidadId="
          + ENTIDAD
          + "&id="
          + DONACION
          + "&estado=ENTREGADA&estado=EN_PUERTA&estado=EN_TRASLADO&orden=ATENCION&size=1";

  /** R11-43: de Logística, solo las entregas de esas donaciones. */
  private static String esperando(String... donaciones) {
    return deLaDonacion(donaciones) + "&estado=EN_PUERTA&estado=EN_TRASLADO";
  }

  private static String deLaDonacion(String... donaciones) {
    StringBuilder direccion =
        new StringBuilder(LOGISTICA + "seguimiento/beneficiarios/" + ENTIDAD + "?");
    for (String donacion : donaciones) {
      direccion.append("idDonacion=").append(donacion).append('&');
    }
    return direccion.append("size=100").toString();
  }

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
        .expect(requestTo(esperando(DONACION)))
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
    String lentejas =
        """
        [{"id": "%s", "descripcion": "Lentejas secas", "estadoActual": "EnTraslado",
          "items": [{"bien": {"categoria": {"id": "%s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
          "cantidad": 15, "necesidadId": "%s",
          "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}}]
        """
            .formatted(DONACION, NECESIDAD, NECESIDAD);
    donaciones
        .expect(requestTo(LA_PEDIDA))
        .andRespond(withSuccess(lentejas, MediaType.APPLICATION_JSON));
    // La única que espera es la pedida: no hay otra para ofrecer
    donaciones
        .expect(requestTo(LA_OTRA))
        .andRespond(withSuccess(lentejas, MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/necesidades/" + NECESIDAD))
        .andRespond(
            withSuccess(
                "{\"descripcion\": \"Para el almuerzo.\", \"cantidadNecesitada\": 40,"
                    + " \"idSubcategoria\": \""
                    + LEGUMBRES
                    + "\","
                    + " \"unidad\": \"KILOGRAMO\", \"fechaLimite\": \"2026-11-30\"}",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/subcategorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + LEGUMBRES + "\", \"nombre\": \"Lentejas secas\"}]",
                MediaType.APPLICATION_JSON));

    List<EntidadEntregaVista> entregas = adaptador.entregas(ENTIDAD, DONACION);

    logistica.verify();
    donaciones.verify();
    assertEquals(1, entregas.size());
    EntidadEntregaVista e = entregas.getFirst();
    // La clave es la donación, como en Donaciones recibidas (HB-22)
    assertEquals(DONACION, e.clave());
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
    assertNull(e.registrada());
    assertNull(e.asignada());
    assertNull(e.preparada());
    assertEquals("8 de noviembre de 2026", adaptador.hoy());
  }

  @Test
  void siLaPedidaNoEsperaOEsDeOtraEntidadNoPideNadaMas() {
    donaciones
        .expect(requestTo(LA_PEDIDA))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertFalse(adaptador.entregas(ENTIDAD, DONACION).iterator().hasNext());
    donaciones.verify();
    logistica.verify();
  }

  @Test
  void sinUnaClaveQueSeaUnIdNoLlamaANingunServicio() {
    assertFalse(adaptador.entregas(ENTIDAD, null).iterator().hasNext());
    assertFalse(adaptador.entregas(ENTIDAD, "lentejas").iterator().hasNext());
    donaciones.verify();
    logistica.verify();
  }

  @Test
  void confirmarSubeLasFotosYDejaLaEntregaEntregadaConElProblema() {
    logistica
        .expect(requestTo(deLaDonacion(DONACION)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_PUERTA"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
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

    // Con la clave de la donación, confirma la entrega que la lleva
    adaptador.confirmar(
        ENTIDAD,
        DONACION,
        new Acta(List.of(foto), " 0001-00004521 ", "incompleta", 12.5, "Dos bolsas rotas."));

    logistica.verify();
  }

  @Test
  void primeroLasQueLlegaronALaPuertaConLaHoraDeLlegadaDelHistorial() {
    String otraDonacion = "6f1c2c1e-0000-4000-8000-0000000000d2";
    logistica
        .expect(requestTo(esperando(DONACION, otraDonacion)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "6f1c2c1e-0000-4000-8000-0000000000a2", "idDonacion": "%s",
                  "estadoEntrega": "EN_TRASLADO", "horaSalida": "2026-11-08T17:30:00Z"},
                 {"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_PUERTA",
                  "horaSalida": "2026-11-08T12:00:00Z"}]
                """
                    .formatted(otraDonacion, ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    String lentejasEnLaPuerta =
        """
        {"id": "%s", "descripcion": "Lentejas secas", "estadoActual": "EN_PUERTA",
          "cantidad": 15,
          "historial": [{"estadoNuevo": "EnPuerta", "timestamp": "2026-11-08T18:20:00"}]}
        """
            .formatted(DONACION);
    donaciones
        .expect(requestTo(LA_PEDIDA))
        .andRespond(withSuccess("[" + lentejasEnLaPuerta + "]", MediaType.APPLICATION_JSON));
    // De la página de 2, la que no es la pedida
    donaciones
        .expect(requestTo(LA_OTRA))
        .andRespond(
            withSuccess(
                "["
                    + lentejasEnLaPuerta
                    + ", {\"id\": \""
                    + otraDonacion
                    + "\", \"descripcion\": \"Arroz\", \"estadoActual\": \"EN_TRASLADO\","
                    + " \"cantidad\": 5}]",
                MediaType.APPLICATION_JSON));

    List<EntidadEntregaVista> entregas = adaptador.entregas(ENTIDAD, DONACION);

    assertEquals(
        List.of(DONACION, otraDonacion),
        entregas.stream().map(EntidadEntregaVista::clave).toList());
    EntidadEntregaVista enLaPuerta = entregas.getFirst();
    assertEquals("8 de noviembre, 18:20", enLaPuerta.llego());
    // Sin el cambio a ASIGNADA ni a LISTA en el historial, sin fecha: no cae en el registro
    assertNull(enLaPuerta.asignada());
    assertNull(enLaPuerta.preparada());
    assertEquals("Llegó a tu puerta el 8 de noviembre a las 18:20.", enLaPuerta.llegoFrase());
    assertNull(entregas.get(1).llego());
    assertEquals("Salió del depósito el 8 de noviembre a las 14:30.", entregas.get(1).llegoFrase());
  }

  @Test
  void laConfirmadaTraeElRemitoLaFotoCuandoYElProblemaDeLogistica() {
    donaciones
        .expect(requestTo(LA_CONFIRMADA))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "descripcion": "Lentejas secas", "estadoActual": "ENTREGADA",
                  "items": [{"bien": {"categoria": {"id": "%s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
                  "cantidad": 15, "fechaRegistro": "2026-11-05T10:00:00",
                  "historial": [
                    {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-06T11:00:00"},
                    {"estadoNuevo": "ListaParaEntregar", "timestamp": "2026-11-07T09:10:00"},
                    {"estadoNuevo": "EnPuerta", "timestamp": "2026-11-08T18:20:00"}]}]
                """
                    .formatted(DONACION, NECESIDAD),
                MediaType.APPLICATION_JSON));
    // Ninguna otra espera: no hay nada para ofrecer después
    donaciones.expect(requestTo(LA_OTRA)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(deLaDonacion(DONACION)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "ENTREGADA",
                  "horaSalida": "2026-11-08T17:30:00Z"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(LOGISTICA + ENTREGA))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "estadoActual": "ENTREGADA", "numeroRemito": " 0001-00004521 ",
                 "fotosRecepcion": ["https://fotos.ejemplo/1.jpg", "https://fotos.ejemplo/2.jpg"],
                 "historialEstado": [
                   {"estadoAnterior": "EN_TRASLADO", "estadoNuevo": "EN_PUERTA",
                    "timestamp": "2026-11-08T21:20:00Z"},
                   {"estadoAnterior": "EN_PUERTA", "estadoNuevo": "ENTREGADA",
                    "timestamp": "2026-11-08T21:45:00Z"}],
                 "reporte": {"motivo": "INCOMPLETA", "detalle": " Dos bolsas rotas. ",
                             "cantidadRecibida": 12.5}}
                """
                    .formatted(ENTREGA),
                MediaType.APPLICATION_JSON));

    List<EntidadEntregaVista> vistas = adaptador.confirmada(ENTIDAD, DONACION, null);

    donaciones.verify();
    logistica.verify();
    assertEquals(1, vistas.size());
    EntidadEntregaVista e = vistas.getFirst();
    assertEquals(DONACION, e.clave());
    assertEquals("Lentejas secas, 15 kg", e.titulo());
    assertEquals("0001-00004521", e.remito());
    assertEquals("https://fotos.ejemplo/1.jpg", e.foto());
    assertEquals("8 de noviembre de 2026, 18:45", e.confirmo());
    assertEquals("Llegó a tu puerta el 8 de noviembre a las 18:20.", e.llegoFrase());
    assertEquals("12,5 de 15 kg", e.llegoMenos());
    assertEquals("Dos bolsas rotas.", e.detalle());
    // El motivo asentado en Logística, como lo nombra la pantalla
    assertEquals("incompleta", e.problema());
    // Los tres primeros pasos, del historial de la donación (U3)
    assertEquals("5 de noviembre, 10:00", e.registrada());
    assertEquals("6 de noviembre, 11:00", e.asignada());
    assertEquals("7 de noviembre, 9:10", e.preparada());
  }

  @Test
  void unaFotoQueNoEsUnaDireccionWebNoLlegaALaPantalla() {
    donaciones
        .expect(requestTo(LA_CONFIRMADA))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + DONACION + "\", \"descripcion\": \"Arroz\", \"cantidad\": 5}]",
                MediaType.APPLICATION_JSON));
    donaciones.expect(requestTo(LA_OTRA)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(deLaDonacion(DONACION)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "ENTREGADA"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(LOGISTICA + ENTREGA))
        .andRespond(
            withSuccess(
                "{\"fotoRecepcionUrl\": \"javascript:alert(1)\"}", MediaType.APPLICATION_JSON));

    EntidadEntregaVista e = adaptador.confirmada(ENTIDAD, DONACION, null).getFirst();

    assertNull(e.foto());
    assertNull(e.remito());
    assertNull(e.confirmo());
    assertNull(e.llegoMenos());
  }

  /**
   * Confirmada desde el traslado, sin pasar por la puerta: no hay hora de llegada (no cae en la
   * fecha de registro) y, sin reporte, no hay problema aunque la dirección traiga uno.
   */
  @Test
  void unaConfirmadaDesdeElTrasladoNoInventaLaLlegadaNiElProblema() {
    donaciones
        .expect(requestTo(LA_CONFIRMADA))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "descripcion": "Arroz", "cantidad": 5,
                  "fechaRegistro": "2026-11-05T10:00:00",
                  "historial": [
                    {"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T14:30:00"}]}]
                """
                    .formatted(DONACION),
                MediaType.APPLICATION_JSON));
    donaciones.expect(requestTo(LA_OTRA)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(deLaDonacion(DONACION)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "ENTREGADA",
                  "horaSalida": "2026-11-08T17:30:00Z"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(LOGISTICA + ENTREGA))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    EntidadEntregaVista e = adaptador.confirmada(ENTIDAD, DONACION, "danada").getFirst();

    assertNull(e.llego());
    assertEquals("Salió del depósito el 8 de noviembre a las 14:30.", e.llegoFrase());
    assertNull(e.problema());
  }

  @Test
  void siLaPedidaTodaviaNoEstaEntregadaNoHayNadaAsentado() {
    donaciones
        .expect(requestTo(LA_CONFIRMADA))
        .andRespond(
            withSuccess(
                "[{\"id\": \"" + DONACION + "\", \"descripcion\": \"Arroz\", \"cantidad\": 5}]",
                MediaType.APPLICATION_JSON));
    donaciones.expect(requestTo(LA_OTRA)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(deLaDonacion(DONACION)))
        .andRespond(
            withSuccess(
                """
                [{"idEntrega": "%s", "idDonacion": "%s", "estadoEntrega": "EN_PUERTA"}]
                """
                    .formatted(ENTREGA, DONACION),
                MediaType.APPLICATION_JSON));

    assertFalse(adaptador.confirmada(ENTIDAD, DONACION, null).iterator().hasNext());
    donaciones.verify();
    logistica.verify();
  }

  @Test
  void laConfirmadaSinUnaClaveQueSeaUnIdNoLlamaANingunServicio() {
    assertFalse(adaptador.confirmada(ENTIDAD, "lentejas", null).iterator().hasNext());
    donaciones.verify();
    logistica.verify();
  }
}
