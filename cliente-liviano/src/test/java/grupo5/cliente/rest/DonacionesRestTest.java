package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import grupo5.cliente.vistas.MisDonacionesVista.Categoria;
import grupo5.cliente.vistas.MisDonacionesVista.Donacion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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
        .expect(requestTo("http://donaciones/donaciones-independientes?donanteId=" + DONANTE))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    // El donante no puede leer una entidad por id: una sola llamada a las públicas, para todas.
    donaciones
        .expect(once(), requestTo("http://donaciones/api/entidades/publicas"))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ENTIDAD
                    + "\", \"razonSocial\": \"Comedor Barrio Ejemplo\", \"rubro\": \"x\"}]",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ALIMENTOS
                    + "\", \"nombre\": \"Alimentos\", \"unidad\": \"KILOGRAMO\"}]",
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
        .expect(requestTo("http://donaciones/donaciones-independientes?donanteId=" + DONANTE))
        .andRespond(withSuccess(CUATRO, MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
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
        .expect(requestTo("http://donaciones/donaciones-independientes?donanteId=" + DONANTE))
        .andRespond(withSuccess(TRES, MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo("http://donaciones/api/entidades/publicas"))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));
    donaciones
        .expect(requestTo("http://donaciones/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    DonacionesRest adaptador =
        new DonacionesRest(
            ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            Clock.systemUTC());

    List<Donacion> libro = adaptador.misDonaciones(DONANTE).donaciones();

    donaciones.verify();
    // Tiene destino aunque no se sepa el nombre; la que no tiene, sigue sin destino.
    assertEquals("una entidad", libro.get(1).destino());
    assertNull(libro.get(0).destino());
    assertEquals(3, libro.size());
  }
}
