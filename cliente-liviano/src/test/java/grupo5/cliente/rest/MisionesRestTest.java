package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.MisionesVista;
import grupo5.cliente.web.TraceIdFiltro;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Pruebas del adaptador REST de misiones e insignias contra el backend de Incentivos (RF-07). */
class MisionesRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-000000000001");
  private static final UUID DONANTE_EMPRESA =
      UUID.fromString("6f1c2c1e-0000-4000-8000-000000000002");

  private static final String PERFIL_URL = "http://incentivos/api/incentivos/donantes/" + DONANTE;
  private static final String MISIONES_URL =
      "http://incentivos/api/incentivos/donantes/" + DONANTE + "/misiones";
  private static final String ASCENSOS_URL =
      "http://incentivos/api/incentivos/donantes/" + DONANTE + "/ascensos";

  private static final String DONANTE_DONACIONES_URL = "http://donaciones/api/donantes/" + DONANTE;
  private static final String DONANTE_EMPRESA_URL =
      "http://donaciones/api/donantes/" + DONANTE_EMPRESA;

  private static final String PERFIL_SOSTENEDOR =
      """
      {
        "donanteId": "6f1c2c1e-0000-4000-8000-000000000001",
        "idPersona": "6f1c2c1e-0000-4000-8000-000000000001",
        "nombre": "Martina Ruiz",
        "categoria": "SOSTENEDOR",
        "fechaRegistro": "2026-06-01",
        "misionesCompletadas": 2,
        "insigniasGanadas": 2
      }
      """;

  private static final String ASCENSOS_SOSTENEDOR =
      """
      [
        {"categoriaAnterior": "COLABORADOR", "categoriaNueva": "SOSTENEDOR", "fecha": "2026-09-02"}
      ]
      """;

  private static final String MISIONES_SEIS =
      """
      [
        {
          "nombre": "Racha Inicial",
          "descripcion": "3 meses consecutivos donando",
          "categoria": "COLABORADOR",
          "progresoActual": 3,
          "objetivo": 3,
          "porcentaje": 100,
          "distanciaAlObjetivo": 0,
          "completada": true,
          "fechaCompletada": "2026-08-03",
          "insignia": {
            "nombre": "Racha Inicial",
            "descripcion": "3 meses consecutivos donando",
            "imagenUrl": "/insignias/racha-colaborador.png",
            "visible": true,
            "fechaObtenida": "2026-08-03"
          }
        },
        {
          "nombre": "Explorador",
          "descripcion": "Donaciones en 3 categorías distintas",
          "categoria": "COLABORADOR",
          "progresoActual": 3,
          "objetivo": 3,
          "porcentaje": 100,
          "distanciaAlObjetivo": 0,
          "completada": true,
          "fechaCompletada": "2026-09-02",
          "insignia": {
            "nombre": "Explorador",
            "descripcion": "Donaciones en 3 categorías distintas",
            "imagenUrl": "/insignias/explorador.png",
            "visible": true,
            "fechaObtenida": "2026-09-02"
          }
        },
        {
          "nombre": "Gran Aporte",
          "descripcion": "Una donación con al menos 50 bienes",
          "categoria": "SOSTENEDOR",
          "progresoActual": 12,
          "objetivo": 50,
          "porcentaje": 24,
          "distanciaAlObjetivo": 38,
          "completada": false,
          "fechaCompletada": null,
          "insignia": {
            "nombre": "Gran Aporte",
            "descripcion": "Una donación con al menos 50 bienes",
            "imagenUrl": "/insignias/gran-aporte.png",
            "visible": true,
            "fechaObtenida": null
          }
        },
        {
          "nombre": "Constancia",
          "descripcion": "6 meses consecutivos donando",
          "categoria": "SOSTENEDOR",
          "progresoActual": 0,
          "objetivo": 6,
          "porcentaje": 0,
          "distanciaAlObjetivo": 6,
          "completada": false,
          "fechaCompletada": null,
          "insignia": {
            "nombre": "Constancia",
            "descripcion": "6 meses consecutivos donando",
            "imagenUrl": "/insignias/constancia.png",
            "visible": true,
            "fechaObtenida": null
          }
        },
        {
          "nombre": "Impacto Real",
          "descripcion": "10 donaciones confirmadas recibidas",
          "categoria": "TRANSFORMADOR",
          "progresoActual": 0,
          "objetivo": 10,
          "porcentaje": 0,
          "distanciaAlObjetivo": 10,
          "completada": false,
          "fechaCompletada": null,
          "insignia": {
            "nombre": "Impacto Real",
            "descripcion": "10 donaciones confirmadas recibidas",
            "imagenUrl": "/insignias/impacto-real.png",
            "visible": true,
            "fechaObtenida": null
          }
        },
        {
          "nombre": "Diversidad Solidaria",
          "descripcion": "Donaciones en 8 categorías distintas",
          "categoria": "TRANSFORMADOR",
          "progresoActual": 0,
          "objetivo": 8,
          "porcentaje": 0,
          "distanciaAlObjetivo": 8,
          "completada": false,
          "fechaCompletada": null,
          "insignia": {
            "nombre": "Diversidad Solidaria",
            "descripcion": "Donaciones en 8 categorías distintas",
            "imagenUrl": "/insignias/diversidad-solidaria.png",
            "visible": true,
            "fechaObtenida": null
          }
        }
      ]
      """;

  private MockRestServiceServer mockIncentivos;
  private MockRestServiceServer mockDonaciones;
  private MisionesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builderIncentivos = RestClient.builder();
    mockIncentivos = MockRestServiceServer.bindTo(builderIncentivos).build();
    IncentivosApi incentivosApi =
        ClientesHttp.cliente(builderIncentivos, "http://incentivos", IncentivosApi.class);

    RestClient.Builder builderDonaciones = RestClient.builder();
    mockDonaciones = MockRestServiceServer.bindTo(builderDonaciones).build();
    DonacionesApi donacionesApi =
        ClientesHttp.cliente(builderDonaciones, "http://donaciones", DonacionesApi.class);

    Clock reloj = Clock.fixed(Instant.parse("2026-10-15T19:25:00Z"), ZoneOffset.UTC);
    adaptador = new MisionesRest(incentivosApi, donacionesApi, reloj);
  }

  @AfterEach
  void limpiar() {
    MDC.remove(TraceIdFiltro.CLAVE);
  }

  @Test
  void mapeaMisionesCategoriasEInsigniasCorrectamente() {
    mockDonaciones
        .expect(requestTo(DONANTE_DONACIONES_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\"" + DONANTE + "\",\"persona\":{\"tipo\":\"HUMANA\"}}",
                MediaType.APPLICATION_JSON));

    mockIncentivos
        .expect(requestTo(PERFIL_URL))
        .andRespond(withSuccess(PERFIL_SOSTENEDOR, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(MISIONES_URL))
        .andRespond(withSuccess(MISIONES_SEIS, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(ASCENSOS_URL))
        .andRespond(withSuccess(ASCENSOS_SOSTENEDOR, MediaType.APPLICATION_JSON));

    MisionesVista vista = adaptador.misiones(DONANTE);

    assertTrue(vista.persona());
    assertFalse(vista.nueva());
    assertEquals(2, vista.obtenidas().size());
    assertEquals(6, vista.misiones().size());
    assertEquals(3, vista.categorias().size());

    assertEquals(
        "Gracias por donar, Martina. Sos donante Sostenedor desde el 2 de septiembre de 2026. Completaste 2 de las 6 misiones. Al 15 de octubre de 2026, 16:25.",
        vista.bajada());

    MisionesVista.Mision m1 = vista.misiones().get(0);
    assertEquals("Racha Inicial", m1.nombre());
    assertEquals("completa", m1.estado());
    assertEquals("3 de agosto de 2026", m1.fecha());

    MisionesVista.Mision m3 = vista.misiones().get(2);
    assertEquals("Gran Aporte", m3.nombre());
    assertEquals("curso", m3.estado());
    assertEquals(12, m3.progreso());
    assertEquals(50, m3.objetivo());
    assertEquals("12 de 50 bienes", m3.cuenta());

    MisionesVista.Mision m4 = vista.misiones().get(3);
    assertEquals("Constancia", m4.nombre());
    assertEquals("espera", m4.estado());
    assertEquals("Gran Aporte", m4.espera());

    MisionesVista.Mision m5 = vista.misiones().get(4);
    assertEquals("Impacto Real", m5.nombre());
    assertEquals("cerrada", m5.estado());

    mockDonaciones.verify();
    mockIncentivos.verify();
  }

  @Test
  void donanteNuevoSinMisionesEsNuevaYComienzaEnColaborador() {
    mockDonaciones
        .expect(requestTo(DONANTE_DONACIONES_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\"" + DONANTE + "\",\"persona\":{\"tipo\":\"HUMANA\"}}",
                MediaType.APPLICATION_JSON));

    String perfilNuevo =
        """
        {
          "donanteId": "6f1c2c1e-0000-4000-8000-000000000001",
          "idPersona": "6f1c2c1e-0000-4000-8000-000000000001",
          "nombre": "Martina",
          "categoria": "COLABORADOR",
          "fechaRegistro": "2026-10-15",
          "misionesCompletadas": 0,
          "insigniasGanadas": 0
        }
        """;

    String misionesVacias =
        """
        [
          {
            "nombre": "Racha Inicial",
            "descripcion": "3 meses consecutivos donando",
            "categoria": "COLABORADOR",
            "progresoActual": 0,
            "objetivo": 3,
            "porcentaje": 0,
            "distanciaAlObjetivo": 3,
            "completada": false,
            "fechaCompletada": null
          }
        ]
        """;

    mockIncentivos
        .expect(requestTo(PERFIL_URL))
        .andRespond(withSuccess(perfilNuevo, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(MISIONES_URL))
        .andRespond(withSuccess(misionesVacias, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(ASCENSOS_URL))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    MisionesVista vista = adaptador.misiones(DONANTE);

    assertTrue(vista.persona());
    assertTrue(vista.nueva());
    assertEquals(0, vista.obtenidas().size());
    assertEquals(
        "Gracias por sumarte, Martina. Sos donante Colaborador. Todavía no completaste ninguna misión. Al 15 de octubre de 2026, 16:25.",
        vista.bajada());
  }

  @Test
  void unaCuentaQueEsPersonaJuridicaDevuelveSinPermiso() {
    mockDonaciones
        .expect(requestTo(DONANTE_EMPRESA_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\""
                    + DONANTE_EMPRESA
                    + "\",\"persona\":{\"tipo\":\"JURIDICA\",\"razonSocial\":\"Acme SA\"}}",
                MediaType.APPLICATION_JSON));

    MisionesVista vista = adaptador.misiones(DONANTE_EMPRESA);

    assertFalse(vista.persona());
    assertTrue(vista.misiones().isEmpty());
    assertTrue(vista.categorias().isEmpty());
    assertNull(vista.bajada());
    mockDonaciones.verify();
  }

  @Test
  void listasVaciasDeMisionesEInsigniasNoGeneraError() {
    mockDonaciones
        .expect(requestTo(DONANTE_DONACIONES_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\"" + DONANTE + "\",\"persona\":{\"tipo\":\"HUMANA\"}}",
                MediaType.APPLICATION_JSON));

    mockIncentivos
        .expect(requestTo(PERFIL_URL))
        .andRespond(withSuccess(PERFIL_SOSTENEDOR, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(MISIONES_URL))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(ASCENSOS_URL))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    MisionesVista vista = adaptador.misiones(DONANTE);

    assertTrue(vista.persona());
    assertTrue(vista.misiones().isEmpty());
    assertEquals(3, vista.categorias().size());
  }

  @Test
  void propagaElTraceIdEnLaLlamadaAIncentivos() {
    MDC.put(TraceIdFiltro.CLAVE, "1234567890abcdef1234567890abcdef");

    mockDonaciones
        .expect(requestTo(DONANTE_DONACIONES_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\"" + DONANTE + "\",\"persona\":{\"tipo\":\"HUMANA\"}}",
                MediaType.APPLICATION_JSON));

    mockIncentivos
        .expect(requestTo(PERFIL_URL))
        .andExpect(header(TraceIdFiltro.CABECERA, "1234567890abcdef1234567890abcdef"))
        .andRespond(withSuccess(PERFIL_SOSTENEDOR, MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(MISIONES_URL))
        .andExpect(header(TraceIdFiltro.CABECERA, "1234567890abcdef1234567890abcdef"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    mockIncentivos
        .expect(requestTo(ASCENSOS_URL))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    adaptador.misiones(DONANTE);

    mockIncentivos.verify();
  }

  @Test
  void unaFallaDeIncentivosLanzaRestClientException() {
    mockDonaciones
        .expect(requestTo(DONANTE_DONACIONES_URL))
        .andRespond(
            withSuccess(
                "{\"idDonante\":\"" + DONANTE + "\",\"persona\":{\"tipo\":\"HUMANA\"}}",
                MediaType.APPLICATION_JSON));

    mockIncentivos.expect(requestTo(PERFIL_URL)).andRespond(withServerError());

    assertThrows(RestClientException.class, () -> adaptador.misiones(DONANTE));
  }

  @Test
  void unaFallaDeDonacionesLanzaRestClientException() {
    mockDonaciones.expect(requestTo(DONANTE_DONACIONES_URL)).andRespond(withServerError());

    assertThrows(RestClientException.class, () -> adaptador.misiones(DONANTE));
  }
}
