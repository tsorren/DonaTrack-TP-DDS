package grupo5.cliente.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParamCount;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.AdminActividadPuerto.AccionDelSitio;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.puertos.DirectorioDeAdministradores.Administrador;
import grupo5.cliente.vistas.AdminActividadVista;
import grupo5.cliente.vistas.AdminActividadVista.Fila;
import grupo5.cliente.vistas.ConsultaActividad;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * El registro de actividad desde Donaciones (R11-26, spec C10 a C13): los filtros traducidos y de a
 * 20, la búsqueda por administrador acotada a 21, los nombres leídos una vez por cuenta y la
 * anotación que nunca falla.
 */
class AdminActividadRestTest {

  private static final String DONACIONES = "http://donaciones";
  private static final String ACTIVIDAD = DONACIONES + "/api/actividad";
  private static final String ANA = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String BRUNO = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String BORRADA = "6f1c2c1e-0000-4000-8000-0000000000c3";
  private static final String ARCHIVO = "6f1c2c1e-0000-4000-8000-0000000000d4";
  private static final String DONACION = "6f1c2c1e-0000-4000-8000-0000000000e5";
  private static final LocalDate HOY = LocalDate.of(2026, 11, 12);

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer api = MockRestServiceServer.bindTo(builder).build();
  private final DirectorioDeAdministradores directorio = mock(DirectorioDeAdministradores.class);
  private final AdminActividadRest adaptador =
      new AdminActividadRest(
          ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class),
          directorio,
          Clock.fixed(Instant.parse("2026-11-12T21:34:00Z"), ZoneOffset.UTC));

  private static Administrador cuenta(String id, String nombre, String apellido, String correo) {
    return new Administrador(id, nombre, apellido, correo, HOY, true);
  }

  private static String fila(String autor, String accion, String objeto, String detalle) {
    return """
        {"id":"%s","ocurrioEn":"2026-11-12T18:20:00","autor":"%s","accion":"%s",
         "objetoId":%s,"detalle":%s}"""
        .formatted(
            java.util.UUID.randomUUID(),
            autor,
            accion,
            objeto == null ? "null" : "\"" + objeto + "\"",
            detalle == null ? "null" : "\"" + detalle + "\"");
  }

  private static HttpHeaders total(long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  private static ConsultaActividad consulta(
      String buscar, String autor, String accion, String fecha) {
    return ConsultaActividad.de(buscar, autor, accion, fecha, null, null, 1, HOY);
  }

  // C10: de a 20, con el grupo traducido a sus acciones, la fecha a desde y hasta, y yo a la sesión
  @Test
  void pideDeA20ConLosFiltrosTraducidos() {
    api.expect(requestTo(startsWith(ACTIVIDAD)))
        .andExpect(method(GET))
        .andExpect(queryParam("autor", ANA))
        .andExpect(
            queryParam("accion", "CAMION_AGREGADO", "CAMION_DESHABILITADO", "CAMION_HABILITADO"))
        .andExpect(queryParam("desde", "2026-11-06"))
        .andExpect(queryParam("hasta", "2026-11-12"))
        .andExpect(queryParam("page", "0"))
        .andExpect(queryParam("size", "20"))
        .andRespond(withSuccess("[]", APPLICATION_JSON).headers(total(0)));

    AdminActividadVista vista =
        adaptador.actividad(consulta("", "yo", "camiones", "7-dias"), ANA, null);

    assertThat(vista.acciones().total()).isZero();
    assertThat(vista.filtrada()).isTrue();
    assertThat(vista.al()).isEqualTo("12 de noviembre de 2026, 18:34");
    api.verify();
  }

  @Test
  void sinFiltrosPideSoloLaPaginaYElTotalEsElDeDonaciones() {
    api.expect(requestTo(ACTIVIDAD + "?page=0&size=20"))
        .andExpect(queryParamCount(2))
        .andRespond(
            withSuccess("[" + fila(BRUNO, "DONACION_REGISTRADA", "x", null) + "]", APPLICATION_JSON)
                .headers(total(57)));
    when(directorio.cuenta(BRUNO))
        .thenReturn(Optional.of(cuenta(BRUNO, "Bruno", "Díaz", "bruno.diaz@ejemplo.org")));

    AdminActividadVista vista = adaptador.actividad(ConsultaActividad.primera(), ANA, null);

    assertThat(vista.acciones().total()).isEqualTo(57);
    assertThat(vista.acciones().paginas()).isEqualTo(3);
    assertThat(vista.filtrada()).isFalse();
    api.verify();
  }

  // C11: la búsqueda pide 21 cuentas y la actividad de las que coinciden
  @Test
  void buscarPorAdministradorPideLaActividadDeLasCuentasQueCoinciden() {
    when(directorio.buscar("bruno", 21))
        .thenReturn(List.of(cuenta(BRUNO, "Bruno", "Díaz", "bruno.diaz@ejemplo.org")));
    api.expect(requestTo(startsWith(ACTIVIDAD)))
        .andExpect(queryParam("autor", BRUNO))
        .andRespond(withSuccess("[]", APPLICATION_JSON).headers(total(0)));

    AdminActividadVista vista = adaptador.actividad(consulta("bruno", "", "", ""), ANA, null);

    assertThat(vista.avisos()).isEmpty();
    assertThat(vista.filtrada()).isTrue();
    api.verify();
  }

  @Test
  void sinCoincidenciasOConMasDe20AvisaYNoFiltraPorCuenta() {
    when(directorio.buscar("zoe", 21)).thenReturn(List.of());
    List<Administrador> muchas =
        IntStream.range(0, 21)
            .mapToObj(i -> cuenta("cuenta-" + i, "Ana", "N" + i, "a" + i + "@ejemplo.org"))
            .toList();
    when(directorio.buscar("a", 21)).thenReturn(muchas);
    api.expect(ExpectedCount.times(2), requestTo(ACTIVIDAD + "?page=0&size=20"))
        .andRespond(withSuccess("[]", APPLICATION_JSON).headers(total(0)));

    AdminActividadVista ninguna = adaptador.actividad(consulta("zoe", "", "", ""), ANA, null);
    AdminActividadVista demasiadas = adaptador.actividad(consulta("a", "", "", ""), ANA, null);

    assertThat(ninguna.avisos())
        .containsExactly(
            "Ninguna cuenta de administración coincide con «zoe». Te mostramos la actividad de"
                + " todos.");
    assertThat(ninguna.filtrada()).isFalse();
    assertThat(demasiadas.avisos())
        .containsExactly(
            "Hay más de 20 cuentas de administración que coinciden con «a». Escribí más del"
                + " nombre o el correo para encontrarla.");
    api.verify();
  }

  @Test
  void laCuentaElegidaQueNoEstaEntreLasBuscadasNoPideNada() {
    when(directorio.buscar("bruno", 21))
        .thenReturn(List.of(cuenta(BRUNO, "Bruno", "Díaz", "bruno.diaz@ejemplo.org")));
    api.expect(ExpectedCount.never(), requestTo(startsWith(ACTIVIDAD)));

    AdminActividadVista vista = adaptador.actividad(consulta("bruno", "yo", "", ""), ANA, null);

    assertThat(vista.acciones().total()).isZero();
    assertThat(vista.filtrada()).isTrue();
    api.verify();
  }

  // C12: cada cuenta distinta se lee una vez; las frases de la ficha; nunca un identificador
  @Test
  void cadaCuentaSeLeeUnaVezYCadaAccionSeDiceConSusPalabras() {
    when(directorio.cuenta(ANA))
        .thenReturn(Optional.of(cuenta(ANA, "Ana", "Pérez", "ana.perez@ejemplo.org")));
    when(directorio.cuenta(BRUNO))
        .thenReturn(Optional.of(cuenta(BRUNO, null, null, "bruno.diaz@ejemplo.org")));
    when(directorio.cuenta(BORRADA)).thenReturn(Optional.empty());
    api.expect(requestTo(startsWith(ACTIVIDAD)))
        .andRespond(
            withSuccess(
                    "["
                        + String.join(
                            ",",
                            fila(ANA, "PROPUESTA_APROBADA", "p", "Comedor Barrio Ejemplo"),
                            fila(BRUNO, "IMPORTACION_SUBIDA", ARCHIVO, "donantes-noviembre.csv"),
                            fila(BRUNO, "DONACION_VENCIDA", DONACION, null),
                            fila(ANA, "ADMINISTRADOR_DADO_DE_ALTA", BRUNO, null),
                            fila(BORRADA, "ASIGNACION_CORRIDA", null, "3"),
                            fila(ANA, "ASIGNACION_CORRIDA", null, "1"),
                            fila(ANA, "CONTRASENA_PROVISORIA_NUEVA", BORRADA, null),
                            fila(ANA, "DONANTE_REGISTRADO", "d", null),
                            fila(ANA, "CAMION_DESHABILITADO", "c", "AB 123 CD"))
                        + "]",
                    APPLICATION_JSON)
                .headers(total(9)));

    List<Fila> filas =
        adaptador.actividad(ConsultaActividad.primera(), ANA, null).acciones().filas();

    assertThat(filas)
        .extracting(Fila::quien)
        .containsExactly(
            "Ana Pérez (vos)",
            "bruno.diaz@ejemplo.org",
            "bruno.diaz@ejemplo.org",
            "Ana Pérez (vos)",
            "Una cuenta de administración que ya no existe",
            "Ana Pérez (vos)",
            "Ana Pérez (vos)",
            "Ana Pérez (vos)",
            "Ana Pérez (vos)");
    assertThat(filas)
        .extracting(Fila::que)
        .containsExactly(
            "Aprobó la propuesta para Comedor Barrio Ejemplo.",
            "Subió el archivo donantes-noviembre.csv para importar.",
            "Marcó una donación como vencida.",
            "Dio de alta a bruno.diaz@ejemplo.org como administrador.",
            "Corrió la asignación: 3 propuestas nuevas.",
            "Corrió la asignación: 1 propuesta nueva.",
            "Generó otra contraseña provisoria para una cuenta de administración que ya no existe.",
            "Registró a una persona donante.",
            "Deshabilitó el camión AB 123 CD.");
    assertThat(filas.get(1).enlace()).isEqualTo("/admin-donantes-importado?archivo=" + ARCHIVO);
    assertThat(filas.get(1).lectorEnlace())
        .isEqualTo("Ver la importación de donantes-noviembre.csv");
    assertThat(filas.get(2).enlace()).isEqualTo("/admin-donaciones-detalle?donacion=" + DONACION);
    assertThat(filas.get(0).enlace()).isNull();
    assertThat(filas.get(4).filtrarPor()).isNull();
    assertThat(filas.get(0).filtrarPor()).isEqualTo(ANA);
    assertThat(filas.get(0).cuando()).isEqualTo("12 nov 2026, 18:20");
    verify(directorio, times(1)).cuenta(ANA);
    verify(directorio, times(1)).cuenta(BRUNO);
    verify(directorio, times(1)).cuenta(BORRADA);
  }

  @Test
  void siElProveedorDeIngresoNoRespondeLaListaSaleSinNombresYConElAviso() {
    when(directorio.cuenta(anyString())).thenThrow(new RestClientException("no responde"));
    api.expect(requestTo(startsWith(ACTIVIDAD)))
        .andRespond(
            withSuccess(
                    "["
                        + fila(BRUNO, "DONACION_REGISTRADA", "x", null)
                        + ","
                        + fila(ANA, "DONACION_REGISTRADA", "y", null)
                        + "]",
                    APPLICATION_JSON)
                .headers(total(2)));

    AdminActividadVista vista = adaptador.actividad(ConsultaActividad.primera(), ANA, null);

    assertThat(vista.acciones().filas())
        .extracting(Fila::quien)
        .containsExactly("Una cuenta de administración", "Vos");
    assertThat(vista.acciones().filas()).extracting(Fila::filtrarPor).containsExactly(BRUNO, ANA);
    assertThat(vista.avisos()).containsExactly(AdminActividadRest.SIN_NOMBRES);
    verify(directorio, times(1)).cuenta(anyString());
  }

  @Test
  void siLaBusquedaNoRespondeAvisaYPideSinEseFiltro() {
    when(directorio.buscar("bruno", 21)).thenThrow(new RestClientException("no responde"));
    api.expect(requestTo(ACTIVIDAD + "?page=0&size=20"))
        .andRespond(withSuccess("[]", APPLICATION_JSON).headers(total(0)));

    AdminActividadVista vista = adaptador.actividad(consulta("bruno", "", "", ""), ANA, null);

    assertThat(vista.avisos()).containsExactly(AdminActividadRest.SIN_NOMBRES);
    api.verify();
  }

  @Test
  void laPastillaDeUnaCuentaDiceSuNombre() {
    when(directorio.cuenta(BRUNO))
        .thenReturn(Optional.of(cuenta(BRUNO, "Bruno", "Díaz", "bruno.diaz@ejemplo.org")));
    api.expect(requestTo(startsWith(ACTIVIDAD)))
        .andExpect(queryParam("autor", BRUNO))
        .andRespond(withSuccess("[]", APPLICATION_JSON).headers(total(0)));

    AdminActividadVista vista = adaptador.actividad(consulta("", BRUNO, "", ""), ANA, null);

    assertThat(vista.quienes().getLast().texto()).isEqualTo("De: Bruno Díaz");
    api.verify();
  }

  // C13: la anotación lleva la acción y lo que tocó; si falla, no se propaga
  @Test
  void anotarMandaLaAccionYNuncaFalla() {
    api.expect(requestTo(ACTIVIDAD))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.accion").value("CAMION_AGREGADO"))
        .andExpect(jsonPath("$.objetoId").value("c1"))
        .andExpect(jsonPath("$.detalle").value("AB 123 CD"))
        .andExpect(jsonPath("$.autor").doesNotExist())
        .andRespond(withStatus(HttpStatus.CREATED));
    api.expect(requestTo(ACTIVIDAD)).andRespond(withServerError());

    adaptador.anotar(AccionDelSitio.CAMION_AGREGADO, "c1", "AB 123 CD");
    assertThatCode(() -> adaptador.anotar(AccionDelSitio.ADMINISTRADOR_DADO_DE_ALTA, BRUNO, null))
        .doesNotThrowAnyException();
    api.verify();
    verify(directorio, never()).buscar(anyString(), org.mockito.ArgumentMatchers.anyInt());
  }
}
