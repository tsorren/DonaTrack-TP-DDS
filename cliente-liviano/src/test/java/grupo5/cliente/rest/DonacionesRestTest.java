package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.DonacionesPuerto.Consulta;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * El adaptador REST de Mis donaciones contra respuestas de ejemplo de Donaciones (plan E6, §F1).
 */
class DonacionesRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String ENTIDAD = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String ALIMENTOS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String PRIMERA =
      "http://donaciones/donaciones-independientes?donanteId=" + DONANTE + "&page=0&size=12";

  /**
   * Tres donaciones desordenadas, dos para la misma entidad. Los estados llegan como los escribe el
   * mapper ("EnTraslado") y como el enum del contrato ("EN_DEPOSITO"). Sobran campos.
   */
  private static final String TRES =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-000000000001", "descripcion": "Fideos secos",
         "estadoActual": "Entregada", "fechaRegistro": "2026-10-01T10:00:00",
         "historial": [
           {"estadoNuevo": "EnTraslado", "timestamp": "2026-10-05T10:00:00", "actor": "x"},
           {"estadoNuevo": "Entregada", "timestamp": "2026-10-05T17:20:00", "actor": "x"}],
         "items": [{"cantidad": 8, "bien": {"bien": {"descripcion": "Fideos"},
           "categoria": {"id": "%2$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 8, "entidadBeneficiariaId": "%1$s"},
        {"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Arroz largo fino",
         "estadoActual": "EN_DEPOSITO", "fechaRegistro": "2026-10-14T09:00:00",
         "historial": [], "items": [], "cantidad": 10, "entidadBeneficiariaId": null},
        {"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Lentejas secas",
         "estadoActual": "EnTraslado", "fechaRegistro": "2026-10-12T10:00:00",
         "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-10-15T14:30:00"}],
         "items": [{"cantidad": 15, "bien": {
           "categoria": {"id": "%2$s", "nombre": "Alimentos", "unidad": "KILOGRAMO"}}}],
         "cantidad": 15, "entidadBeneficiariaId": "%1$s"}
      ]
      """
          .formatted(ENTIDAD, ALIMENTOS);

  @Test
  void armaElLibroConNumeroDestinoEstadoYNotaDeCadaDonacion() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ALIMENTOS
                    + "\", \"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\"}]",
                MediaType.APPLICATION_JSON));
    // Una página: la primera, de 12; sin X-Total-Count, el total es lo que vino.
    donaciones.expect(requestTo(PRIMERA)).andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    // El donante no puede leer una entidad por id: una sola llamada a las públicas, para todas.
    donaciones
        .expect(once(), requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ENTIDAD
                    + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\", \"rubro\": \"x\"}]",
                MediaType.APPLICATION_JSON));
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            hoy);

    MisDonacionesVista vista = adaptador.misDonaciones(DONANTE);

    donaciones.verify();
    List<Donacion> libro = vista.donaciones();
    assertEquals(List.of(3L, 2L, 1L), libro.stream().map(Donacion::numero).toList());

    Donacion arroz = libro.get(0);
    assertEquals(EstadoEntrega.PENDIENTE, arroz.estado());
    assertEquals("En el depósito, espera destino", arroz.nota());
    assertNull(arroz.destino());
    assertNull(arroz.categoria());
    assertEquals("10", arroz.cantidad());

    Donacion lentejas = libro.get(1);
    assertEquals(EstadoEntrega.EN_CAMINO, lentejas.estado());
    assertEquals("Salió el 15 de octubre, 14:30", lentejas.nota());
    assertEquals("Comedor Barrio Ejemplo", lentejas.destino());
    assertEquals("15 kg", lentejas.cantidad());
    assertEquals("cat-alimentos", lentejas.icono());
    assertEquals(ALIMENTOS, lentejas.categoria());
    assertEquals("12 de octubre de 2026", lentejas.donada());

    Donacion fideos = libro.get(2);
    assertEquals(EstadoEntrega.ENTREGADA, fideos.estado());
    assertEquals("Entrega confirmada el 5 de octubre", fideos.nota());

    assertEquals(List.of(new Categoria(ALIMENTOS, "Alimentos")), vista.categorias());
    assertEquals("hoy, 15 de octubre, 16:25", vista.actualizado());
  }

  @Test
  void elMesElEstadoLaCategoriaYLaPaginaLosResuelveDonacionesYElTotalVieneDeLaCabecera() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ALIMENTOS
                    + "\", \"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\"}]",
                MediaType.APPLICATION_JSON));
    // 25 en octubre, en curso y de alimentos: la página 2 de 12 trae las 13 a 24.
    donaciones
        .expect(
            requestTo(
                "http://donaciones/donaciones-independientes?donanteId="
                    + DONANTE
                    + "&desde=2026-10-01&hasta=2026-10-31&estado=EN_TRASLADO&estado=EN_PUERTA"
                    + "&categoriaId="
                    + ALIMENTOS
                    + "&page=1&size=12"))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON).header("X-Total-Count", "25"));
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            hoy);

    MisDonacionesVista vista =
        adaptador.misDonaciones(
            DONANTE, new Consulta(YearMonth.of(2026, 10), EstadoEntrega.EN_CAMINO, ALIMENTOS, 2));

    donaciones.verify();
    assertEquals(25, vista.total());
    assertEquals(2, vista.pagina());
    assertEquals(3, vista.paginas());
    assertEquals("Arroz largo fino", vista.donaciones().getFirst().titulo());
    // El número sigue la posición entre las 25: la página 2 empieza en la 13.ª más reciente.
    assertEquals(
        List.of(13L, 12L, 11L), vista.donaciones().stream().map(Donacion::numero).toList());
  }

  @Test
  void unaCategoriaQueNoEstaNoFiltraYUnaPaginaDeMasPideLaUltima() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(PRIMERA.replace("page=0", "page=8")))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header("X-Total-Count", "15"));
    donaciones
        .expect(requestTo(PRIMERA.replace("page=0", "page=1")))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON).header("X-Total-Count", "15"));
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            Clock.systemUTC());

    MisDonacionesVista vista =
        adaptador.misDonaciones(DONANTE, new Consulta(null, null, "no-es-una-categoria", 9));

    donaciones.verify();
    assertEquals(15, vista.total());
    assertEquals(2, vista.pagina());
    assertEquals(2, vista.paginas());
    assertEquals(3, vista.donaciones().size());
  }

  /** B6 (RF-05): con una subcategoría se manda solo esa; la categoría elegida es la suya. */
  @Test
  void laSubcategoriaSeMandaSolaYSuCategoriaQuedaElegida() {
    String arroz = "6f1c2c1e-0000-4000-8000-0000000000d1";
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(
            withSuccess(
                ("[{\"id\": \"%s\", \"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\","
                        + " \"subcategorias\": [{\"id\": \"%s\", \"nombre\": \"Arroz\"}]}]")
                    .formatted(ALIMENTOS, arroz),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(PRIMERA.replace("&page=0", "&subcategoriaId=" + arroz + "&page=0")))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header("X-Total-Count", "0"));
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            Clock.systemUTC());

    Consulta consulta = new Consulta(null, null, "otra-cosa", arroz, 1);
    MisDonacionesVista vista = adaptador.misDonaciones(DONANTE, consulta);

    donaciones.verify();
    assertEquals(ALIMENTOS, consulta.categoriaEn(vista.categorias()));
    assertEquals(
        List.of(new Categoria(arroz, "Arroz")), vista.categorias().getFirst().subcategorias());
  }

  /** Los otros cuatro estados, sin historial, sin ítems y con un ítem sin bien: ninguno falla. */
  private static final String CUATRO =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-000000000011", "descripcion": "A",
         "estadoActual": "AsignacionRealizada", "fechaRegistro": "2026-10-04T10:00:00",
         "historial": null, "items": null, "cantidad": 1},
        {"id": "6f1c2c1e-0000-4000-8000-000000000012", "descripcion": "B",
         "estadoActual": "ListaParaEntregar", "fechaRegistro": "2026-10-03T10:00:00",
         "items": [{"bien": null}], "cantidad": 2},
        {"id": "6f1c2c1e-0000-4000-8000-000000000013", "descripcion": "C",
         "estadoActual": "EntregaFallida", "fechaRegistro": "2026-10-02T10:00:00",
         "historial": [{"estadoNuevo": "EntregaFallida", "timestamp": "2026-10-11T16:10:00"}],
         "cantidad": 3},
        {"id": "6f1c2c1e-0000-4000-8000-000000000014", "descripcion": "D",
         "estadoActual": "Vencida", "fechaRegistro": "2026-10-01T10:00:00", "cantidad": 4}
      ]
      """;

  @Test
  void losDemasEstadosYLosDatosQueFaltanNoRompenLaLista() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(PRIMERA))
        .andRespond(withSuccess(CUATRO, MediaType.APPLICATION_JSON));
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            Clock.systemUTC());

    List<Donacion> libro = adaptador.misDonaciones(DONANTE).donaciones();

    donaciones.verify();
    assertEquals(
        List.of(
            EstadoEntrega.PENDIENTE,
            EstadoEntrega.PENDIENTE,
            EstadoEntrega.CON_PROBLEMA,
            EstadoEntrega.CON_PROBLEMA),
        libro.stream().map(Donacion::estado).toList());
    assertEquals(
        List.of(
            "Asignada, espera el camión",
            "Lista para entregar, espera el camión",
            "No pudo llegar el 11 de octubre",
            "Venció en el depósito el 1 de octubre"),
        libro.stream().map(Donacion::nota).toList());
    assertNull(libro.get(1).categoria());
  }

  @Test
  void sinLaListaDeEntidadesLasAsignadasVanAUnaEntidad() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    donaciones.expect(requestTo(PRIMERA)).andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            Clock.systemUTC());

    List<Donacion> libro = adaptador.misDonaciones(DONANTE).donaciones();

    donaciones.verify();
    // Tiene destino aunque no se sepa el nombre; la que no tiene, sigue sin destino.
    assertEquals("una organización", libro.get(1).destino());
    assertNull(libro.get(0).destino());
    assertEquals(3, libro.size());
  }

  @Test
  void laQueLlegoALaEntidadSigueEnCaminoParaElDonanteConSuNota() {
    assertEquals(EstadoEntrega.EN_CAMINO, DonacionesRest.estadoEntrega(Comun.PUERTA));
    assertEquals(
        "Llegó a la organización el 8 de noviembre, 18:20",
        DonacionesRest.nota(Comun.PUERTA, java.time.LocalDateTime.of(2026, 11, 8, 18, 20)));
    assertEquals(
        "El camión llegó a Comedor Ejemplo. Falta que confirme la recepción.",
        DonacionesRest.textoDeNoticia(
            new DonacionesApi.Cambio(
                "EN_PUERTA", java.time.LocalDateTime.of(2026, 11, 8, 18, 20), null, null, null),
            "Comedor Ejemplo",
            false));
  }

  /** Una página llena de 100 entidades públicas que no son las buscadas. */
  private static String otrasCien() {
    return IntStream.range(0, DonacionesApi.TOPE)
        .mapToObj(
            i -> "{\"id\": \"" + UUID.randomUUID() + "\", \"razonSocial\": \"Otra " + i + "\"}")
        .collect(Collectors.joining(",", "[", "]"));
  }

  private static List<DonacionesApi.Donacion> conDestino(UUID entidad) {
    return List.of(
        new DonacionesApi.Donacion(
            UUID.randomUUID(),
            "Fideos",
            "ENTREGADA",
            null,
            List.of(),
            List.of(),
            1,
            entidad,
            null,
            null,
            null,
            null));
  }

  @Test
  void elNombreDeUnaEntidadMasAllaDeLaPrimeraPaginaSeBuscaEnLaSiguiente() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(otrasCien(), MediaType.APPLICATION_JSON).header("X-Total-Count", "150"));
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas?page=1&size=100"))
        .andRespond(
            withSuccess(
                    "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                    MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "150"));
    DonacionesApi api = ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class);

    Map<UUID, String> nombres =
        DonacionesRest.nombresDeEntidades(api, conDestino(UUID.fromString(ENTIDAD)));

    donaciones.verify();
    assertEquals("Comedor Barrio Ejemplo", nombres.get(UUID.fromString(ENTIDAD)));
  }

  @Test
  void conTodasLasEntidadesYaNombradasNoSePidenMasPaginas() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    donaciones
        .expect(once(), requestTo("http://donaciones/api/entidades/publicas?page=0&size=100"))
        .andRespond(
            withSuccess(
                    "[{\"id\": \"" + ENTIDAD + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\"}]",
                    MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "900"));
    DonacionesApi api = ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class);

    Map<UUID, String> nombres =
        DonacionesRest.nombresDeEntidades(api, conDestino(UUID.fromString(ENTIDAD)));

    donaciones.verify();
    assertEquals("Comedor Barrio Ejemplo", nombres.get(UUID.fromString(ENTIDAD)));
  }

  @Test
  void pasadoElTopeDePaginasLaEntidadQuedaSinNombre() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(builder).build();
    for (int pagina = 0; pagina < DonacionesRest.PAGINAS_DE_ENTIDADES; pagina++) {
      donaciones
          .expect(
              requestTo("http://donaciones/api/entidades/publicas?page=" + pagina + "&size=100"))
          .andRespond(
              withSuccess(otrasCien(), MediaType.APPLICATION_JSON).header("X-Total-Count", "5000"));
    }
    DonacionesApi api = ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class);

    Map<UUID, String> nombres =
        DonacionesRest.nombresDeEntidades(api, conDestino(UUID.fromString(ENTIDAD)));

    donaciones.verify();
    assertEquals(DonacionesRest.SIN_NOMBRE, nombres.get(UUID.fromString(ENTIDAD)));
  }
}
