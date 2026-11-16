package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.vistas.CampanitaVista;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** La campanita (R11-29, R11-30) contra respuestas de ejemplo de Notificaciones. */
class CampanitaRestTest {

  private static final UUID PERSONA = UUID.fromString("00000000-0000-4000-8000-000000000011");
  private static final UUID ADMINISTRACION =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final String SERVICIO = "http://notificaciones/api/notificaciones/";
  private static final String RECIENTES = "?leida=false&orden=recientes&page=0&size=5";

  private static final String UNA = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String OTRA = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000d1";
  private static final String ARCHIVO = "6f1c2c1e-0000-4000-8000-0000000000f1";

  private static final String DOS_DEL_DONANTE =
      """
      [
        {"id": "%1$s", "mensaje": "Tu donación de Fideos fue asignada a Comedor Barrio Ejemplo",
         "estado": "ENVIADA", "fechaCreacion": "2026-10-15T16:02:30", "fechaLectura": null,
         "tipoEvento": "DONACION_ASIGNADA", "referenciaId": "%3$s"},
        {"id": "%2$s", "mensaje": "Una de antes, sin tipo", "estado": "ENVIADA",
         "fechaCreacion": "2026-10-14T12:15:00", "fechaLectura": null}
      ]
      """
          .formatted(UNA, OTRA, DONACION);

  private final HttpServletRequest pedido = new MockHttpServletRequest();
  private MockRestServiceServer notificaciones;
  private CampanitaRest adaptador;

  /** Es donante; como entidad, no ingresó. */
  private static final class UnDonante implements CuentaActual {

    @Override
    public Optional<UUID> donante(HttpServletRequest pedido) {
      return Optional.of(PERSONA);
    }

    @Override
    public Optional<UUID> entidad(HttpServletRequest pedido) {
      return Optional.empty();
    }
  }

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    notificaciones = MockRestServiceServer.bindTo(builder).build();
    // 19:25 en UTC son las 16:25 en la Argentina.
    Clock hoy = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador =
        new CampanitaRest(
            ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
            new UnDonante(),
            ADMINISTRACION,
            hoy);
  }

  private static HttpHeaders total(long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  private static String una(String id, String tipo, String referencia) {
    return """
        {"id": "%s", "mensaje": "Texto", "estado": "ENVIADA",
         "fechaCreacion": "2026-10-15T15:00:00", "fechaLectura": null,
         "tipoEvento": "%s", "referenciaId": "%s"}
        """
        .formatted(id, tipo, referencia);
  }

  /** C9: pide solo las 5 sin leer más nuevas; la cifra es el total de sin leer, no las filas. */
  @Test
  void pideLasCincoSinLeerMasNuevasYLaCifraEsElTotal() {
    notificaciones
        .expect(requestTo(SERVICIO + "persona/" + PERSONA + RECIENTES))
        .andRespond(withSuccess(DOS_DEL_DONANTE, MediaType.APPLICATION_JSON).headers(total(12)));

    CampanitaVista vista = adaptador.recientes("donante", pedido);

    notificaciones.verify();
    assertThat(vista.ahora()).isEqualTo("2026-10-15T16:25");
    assertThat(vista.sinLeer()).isEqualTo(12);
    assertThat(vista.filas()).hasSize(2);
    assertThat(vista.filas().get(0).id()).isEqualTo(UNA);
    assertThat(vista.filas().get(0).titulo())
        .isEqualTo(DonanteNotificacionesRest.titulo("DONACION_ASIGNADA"));
    assertThat(vista.filas().get(0).fecha()).isEqualTo("2026-10-15T16:02");
    assertThat(vista.filas().get(1).titulo()).isEqualTo("Novedad");
  }

  /** C8 y C10 (R11-30): la administración lee la bandeja compartida de la Administración. */
  @Test
  void laAdministracionLeeSuBandejaConSusTitulos() {
    String cuerpo =
        "["
            + una(UNA, "IMPORTACION_TERMINADA", ARCHIVO)
            + ","
            + una(OTRA, "IMPORTACION_CORTADA", ARCHIVO)
            + "]";
    notificaciones
        .expect(requestTo(SERVICIO + "persona/" + ADMINISTRACION + RECIENTES))
        .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON).headers(total(2)));

    CampanitaVista vista = adaptador.recientes("admin", pedido);

    notificaciones.verify();
    assertThat(vista.filas())
        .extracting(CampanitaVista.Fila::titulo)
        .containsExactly("Terminó una importación", "Una importación se cortó");
    assertThat(vista.sinLeer()).isEqualTo(2);
  }

  /** Sin cuenta del área, nada que pedir: la campanita queda vacía sin llamar al servicio. */
  @Test
  void sinCuentaDelAreaNoSePide() {
    CampanitaVista vista = adaptador.recientes("entidad", pedido);

    notificaciones.verify();
    assertThat(vista.filas()).isEmpty();
    assertThat(vista.sinLeer()).isZero();
  }

  /** C12: abrir la marca como leída y lleva al detalle de la donación. */
  @Test
  void abrirLaMarcaLeidaYLlevaALaDonacion() {
    notificaciones
        .expect(requestTo(SERVICIO + UNA + "/leida?leida=true"))
        .andExpect(method(HttpMethod.PATCH))
        .andRespond(
            withSuccess(una(UNA, "DONACION_ASIGNADA", DONACION), MediaType.APPLICATION_JSON));

    assertThat(adaptador.abrir("donante", UNA, pedido))
        .isEqualTo("/donante-donaciones-detalle?donacion=" + DONACION);
    notificaciones.verify();
  }

  /** C13: la de importación lleva a la página de esa importación, terminada o cortada. */
  @Test
  void abrirUnaDeImportacionLlevaALaImportacion() {
    notificaciones
        .expect(requestTo(SERVICIO + UNA + "/leida?leida=true"))
        .andRespond(
            withSuccess(una(UNA, "IMPORTACION_CORTADA", ARCHIVO), MediaType.APPLICATION_JSON));

    assertThat(adaptador.abrir("admin", UNA, pedido))
        .isEqualTo("/admin-donantes-importado?archivo=" + ARCHIVO);
    notificaciones.verify();
  }

  /** La de la organización lleva a sus donaciones recibidas, en el estado de su categoría. */
  @Test
  void abrirUnaDeLaOrganizacionLlevaAlEstadoDeSuCategoria() {
    notificaciones
        .expect(requestTo(SERVICIO + UNA + "/leida?leida=true"))
        .andRespond(
            withSuccess(una(UNA, "DONACION_ASIGNADA", DONACION), MediaType.APPLICATION_JSON));

    assertThat(adaptador.abrir("entidad", UNA, pedido))
        .isEqualTo(
            EntidadNotificacionesRest.destino(EntidadNotificacionesRest.tema("DONACION_ASIGNADA")));
    notificaciones.verify();
  }

  /** C14: un id que no es UUID no se manda; sin respuesta, la bandeja del área. */
  @Test
  void unIdInvalidoOUnaFallaLlevanALaBandeja() {
    notificaciones
        .expect(requestTo(SERVICIO + OTRA + "/leida?leida=true"))
        .andRespond(withServerError());

    assertThat(adaptador.abrir("donante", "https://otro.sitio", pedido))
        .isEqualTo("/donante-notificaciones");
    assertThat(adaptador.abrir("donante", OTRA, pedido)).isEqualTo("/donante-notificaciones");
    notificaciones.verify();
  }

  /** El de contacto, sin referencia, lleva a la bandeja de la administración. */
  @Test
  void sinReferenciaLlevaALaBandeja() {
    NotificacionesApi.Notificacion contacto =
        new NotificacionesApi.Notificacion(
            UUID.fromString(UNA), "Texto", "ENVIADA", null, null, "MENSAJE_CONTACTO", null);

    assertThat(CampanitaRest.destino("admin", contacto)).isEqualTo("/admin-notificaciones");
  }
}
