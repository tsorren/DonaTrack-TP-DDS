package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Las formas nuevas de las interfaces que declara el esqueleto de la conexión de pantallas
 * (SPEC-08), una vez cada una: filtros que viajan solo si se cargan, el total en una cabecera,
 * multipart, cabeceras propias, respuestas sin cuerpo y los records con campos que faltan.
 */
class ContratoDeLasApisTest {

  private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000021");
  private static final String DONACIONES = "http://donaciones";
  private static final String LOGISTICA = "http://logistica";
  private static final String INCENTIVOS = "http://incentivos";
  private static final String NOTIFICACIONES = "http://notificaciones";

  private MockRestServiceServer servidor;
  private DonacionesApi donaciones;
  private LogisticaApi logistica;
  private IncentivosApi incentivos;
  private NotificacionesApi notificaciones;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    servidor = MockRestServiceServer.bindTo(builder).build();
    donaciones = ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class);
    logistica = ClientesHttp.cliente(builder, LOGISTICA, LogisticaApi.class);
    incentivos = ClientesHttp.cliente(builder, INCENTIVOS, IncentivosApi.class);
    notificaciones = ClientesHttp.cliente(builder, NOTIFICACIONES, NotificacionesApi.class);
  }

  @Test
  void losFiltrosViajanSoloSiSeCarganYLaDonacionLeeQuienDono() {
    servidor
        .expect(requestTo(startsWith(DONACIONES + "/donaciones-independientes?")))
        .andExpect(queryParam("estado", "ENTREGADA"))
        .andExpect(queryParam("necesidadId", ID.toString()))
        .andRespond(
            withSuccess(
                "[{\"id\": \""
                    + ID
                    + "\", \"cantidad\": 3, \"estadoActual\": \"Entregada\","
                    + " \"quienDono\": {\"tipo\": \"HUMANA\", \"nombreVisible\": null}}]",
                MediaType.APPLICATION_JSON));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("estado", "ENTREGADA");
    filtros.add("necesidadId", ID.toString());

    List<DonacionesApi.Donacion> lista = donaciones.pagina(filtros).getBody();

    servidor.verify();
    assertThat(lista)
        .singleElement()
        .satisfies(d -> assertThat(d.quienDono().tipo()).isEqualTo("HUMANA"));
    assertThat(lista.getFirst().necesidadId()).isNull();
  }

  @Test
  void lasEntidadesPublicasTraenElTotalEnLaCabecera() {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", "42");
    servidor
        .expect(requestTo(DONACIONES + "/api/entidades/publicas?page=0"))
        .andRespond(
            withSuccess("[{\"razonSocial\": \"Comedor Ejemplo\"}]", MediaType.APPLICATION_JSON)
                .headers(cabeceras));
    MultiValueMap<String, String> filtros = new LinkedMultiValueMap<>();
    filtros.add("page", "0");

    ResponseEntity<List<DonacionesApi.EntidadPublica>> respuesta =
        donaciones.entidadesPublicas(filtros);

    assertThat(respuesta.getHeaders().getFirst("X-Total-Count")).isEqualTo("42");
    assertThat(respuesta.getBody())
        .singleElement()
        .extracting("razonSocial")
        .isEqualTo("Comedor Ejemplo");
  }

  @Test
  void lasFotosDelActaVanEnMultipart() {
    servidor
        .expect(requestTo(LOGISTICA + "/api/entregas/" + ID + "/fotos"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, startsWith("multipart/form-data")))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("[{\"url\": \"https://fotos.ejemplo/1.jpg\"}]"));
    MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
    partes.add(
        "foto",
        new ByteArrayResource(new byte[] {1, 2, 3}) {
          @Override
          public String getFilename() {
            return "acta.jpg";
          }
        });

    List<LogisticaApi.FotoSubida> fotos = logistica.subirFotos(ID, partes);

    assertThat(fotos).singleElement().extracting("url").isEqualTo("https://fotos.ejemplo/1.jpg");
  }

  @Test
  void elCambioDeEstadoLlevaElActorYElContactoLaIp() {
    servidor
        .expect(requestTo(DONACIONES + "/donaciones-independientes/" + ID + "/estado"))
        .andExpect(method(HttpMethod.PATCH))
        .andExpect(header("X-Actor", "admin"))
        .andExpect(jsonPath("$.estado").value("VENCIDA"))
        .andRespond(
            withSuccess("{\"id\": \"" + ID + "\", \"cantidad\": 1}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(NOTIFICACIONES + "/api/contacto"))
        .andExpect(header("X-Forwarded-For", "203.0.113.7"))
        .andExpect(jsonPath("$.tema").value("SITIO"))
        .andExpect(jsonPath("$.claveEnvio").value(ID.toString()))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"numero\": 4821}"));

    donaciones.cambiarEstado(
        ID, "admin", new DonacionesApi.Cambiar("VENCIDA", "Venció", null, null, null, null));
    NotificacionesApi.MensajeRecibido recibido =
        notificaciones.contacto(
            "203.0.113.7",
            new NotificacionesApi.MensajeDeContacto(
                "SITIO", "No anduvo", null, "a@ejemplo.test", ID));

    servidor.verify();
    assertThat(recibido.numero()).isEqualTo(4821);
  }

  @Test
  void lasRespuestasSinCuerpoNoFallan() {
    servidor
        .expect(requestTo(DONACIONES + "/api/necesidades/" + ID))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withNoContent());
    servidor
        .expect(requestTo(INCENTIVOS + "/api/incentivos/ranking/ultimo"))
        .andRespond(withNoContent());
    servidor
        .expect(requestTo(LOGISTICA + "/api/camiones?incluirDeshabilitados=true"))
        .andRespond(withSuccess("[{\"patente\": \"AB123CD\"}]", MediaType.APPLICATION_JSON));

    donaciones.darDeBajaNecesidad(ID);
    IncentivosApi.RankingMensual ultimo = incentivos.ultimoRanking();
    List<LogisticaApi.Camion> camiones = logistica.camiones(true);

    servidor.verify();
    assertThat(ultimo).isNull();
    assertThat(camiones).singleElement().satisfies(c -> assertThat(c.capacidadKG()).isNull());
  }

  @Test
  void elAltaDeUnaDonacionMandaLosCamposObligatorios() {
    servidor
        .expect(requestTo(DONACIONES + "/api/donaciones"))
        .andExpect(jsonPath("$.idDonante").value(ID.toString()))
        .andExpect(jsonPath("$.descripcion").value("Alimentos"))
        .andExpect(jsonPath("$.items[0].descripcionBien").value("Arroz"))
        .andExpect(jsonPath("$.items[0].cantidad").value(10))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andRespond(withSuccess("{\"id\": \"" + ID + "\"}", MediaType.APPLICATION_JSON));

    donaciones.registrarDonacion(
        new DonacionesApi.DonacionNueva(
            ID,
            "Alimentos",
            List.of(new DonacionesApi.ItemNuevo("Arroz", null, null, null, null, null, 10)),
            "Depósito Ejemplo",
            null,
            null,
            null));

    servidor.verify();
  }
}
