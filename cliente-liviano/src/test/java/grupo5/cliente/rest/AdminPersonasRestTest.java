package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import grupo5.cliente.cuentas.SolicitudesDeEntidad.Pendiente;
import grupo5.cliente.puertos.PersonaNueva;
import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import grupo5.cliente.vistas.AdminEntidadesVista;
import grupo5.cliente.vistas.AdminEntidadesVista.Entidad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST del admin contra respuestas de ejemplo de Donaciones (plan E6, §F1). */
class AdminPersonasRestTest {

  private static final String DONACIONES = "http://donaciones";
  private static final UUID ARCHIVO = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
  private static final UUID PERSONA = UUID.fromString("00000000-0000-4000-8000-000000000009");
  private static final MediaType JSON = MediaType.APPLICATION_JSON;

  /** Una persona activa, una organización dada de baja y una empresa, con campos que no se usan. */
  private static final String TRES_DONANTES =
      """
      [
        {"idDonante": "00000000-0000-4000-8000-000000000001",
         "personaId": "00000000-0000-4000-8000-000000000001", "activo": true, "mostrarNombre": true,
         "persona": {"tipo": "HUMANA", "id": "00000000-0000-4000-8000-000000000001",
                     "nombre": "Ana", "apellido": "Ejemplo", "tipoDocumento": "DNI",
                     "documento": "20000001", "mediosDeContacto": []}},
        {"idDonante": "00000000-0000-4000-8000-000000000002", "activo": false,
         "persona": {"tipo": "JURIDICA", "razonSocial": "Fundación Ejemplo Sur",
                     "tipoJuridico": "ONG", "tipoDocumento": "CUIT", "documento": "30-00000002-0"}},
        {"idDonante": "00000000-0000-4000-8000-000000000003", "activo": true,
         "persona": {"tipo": "JURIDICA", "razonSocial": "Distribuidora Ejemplo SA",
                     "tipoJuridico": "EMPRESA"}}
      ]
      """;

  private MockRestServiceServer donaciones;
  private AdminPersonasRest adaptador;
  private final StaticListableBeanFactory cuentas = new StaticListableBeanFactory();

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    donaciones = MockRestServiceServer.bindTo(builder).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new AdminPersonasRest(
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class),
            reloj,
            cuentas.getBeanProvider(SolicitudesDeEntidad.class));
  }

  /** Una activa y una dada de baja; la activa con una necesidad vigente de alimentos. */
  private static final String DOS_ENTIDADES =
      """
      [
        {"id": "00000000-0000-4000-8000-0000000000e1", "activo": true,
         "juridica": {"tipo": "JURIDICA", "razonSocial": "Merendero Ejemplo Sur", "rubro": "Merendero",
                      "tipoJuridico": "ONG",
                      "direccion": {"calle": "Calle Ejemplo", "altura": 10, "localidad": "Ejemplo Sur"},
                      "mediosDeContacto": [{"tipo": "CORREO", "direccionCorreo": "sur@ejemplo.org"}],
                      "representantes": [{"tipo": "HUMANA", "nombre": "Raúl", "apellido": "Ejemplo"}]}},
        {"id": "00000000-0000-4000-8000-0000000000e2", "activo": false,
         "juridica": {"tipo": "JURIDICA", "razonSocial": "Club Ejemplo Norte",
                      "tipoJuridico": "INSTITUCION"}}
      ]
      """;

  @Test
  void lasEntidadesSalenPorNombreConSusNecesidadesYLasQuePidenSumarseConSusDatos() {
    UUID juridica = UUID.fromString("00000000-0000-4000-8000-0000000000e3");
    SolicitudesDeEntidad solicitudes = mock(SolicitudesDeEntidad.class);
    when(solicitudes.pendientes())
        .thenReturn(
            List.of(
                new Pendiente("cuenta-1", "Hogar Ejemplo Norte", "hogar@ejemplo.org", juridica)));
    cuentas.addBean("solicitudes", solicitudes);
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades"))
        .andRespond(withSuccess(DOS_ENTIDADES, JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/necesidades?estado=VIGENTE"))
        .andRespond(
            withSuccess(
                """
                [{"id": "00000000-0000-4000-8000-0000000000f1",
                  "idEntidad": "00000000-0000-4000-8000-0000000000e1",
                  "idSubcategoria": "00000000-0000-4000-8000-0000000000c1",
                  "descripcion": "Leche en polvo", "cantidadNecesitada": 40,
                  "unidad": "KILOGRAMO", "fechaLimite": "2026-12-31", "estado": "VIGENTE"}]
                """,
                JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/subcategorias"))
        .andRespond(
            withSuccess(
                """
                [{"id": "00000000-0000-4000-8000-0000000000c1", "nombre": "Leche",
                  "categoria": {"nombre": "Alimentos"}}]
                """,
                JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/personas?tipo=JURIDICA"))
        .andRespond(
            withSuccess(
                """
                [{"tipo": "JURIDICA", "id": "00000000-0000-4000-8000-0000000000e3",
                  "razonSocial": "Asociación Civil Hogar Ejemplo Norte", "tipoJuridico": "ONG",
                  "tipoDocumento": "CUIT", "documento": "30-00000011-0",
                  "direccion": {"calle": "Calle Ejemplo", "altura": 1234,
                                "localidad": "Localidad Ejemplo", "provincia": "Buenos Aires"}}]
                """,
                JSON));

    AdminEntidadesVista vista = adaptador.entidades();

    assertEquals(1, vista.activas());
    assertEquals(
        List.of("Club Ejemplo Norte", "Hogar Ejemplo Norte", "Merendero Ejemplo Sur"),
        vista.todas().stream().map(Entidad::nombre).toList());
    Entidad club = vista.todas().get(0);
    assertEquals("inactiva", club.estado());
    assertEquals("Institución", club.tipo());
    assertNull(club.baja());
    Entidad hogar = vista.todas().get(1);
    assertEquals("cuenta-1", hogar.clave());
    assertEquals("pendiente", hogar.estado());
    assertEquals("30-00000011-0", hogar.cuit());
    assertEquals("Calle Ejemplo 1234, Localidad Ejemplo, Buenos Aires", hogar.direccion());
    assertNull(hogar.cuando());
    assertNull(hogar.constancia());
    Entidad merendero = vista.todas().get(2);
    assertEquals("activa", merendero.estado());
    assertEquals("Merendero", merendero.tipo());
    assertEquals("Ejemplo Sur", merendero.localidad());
    assertEquals("Raúl Ejemplo", merendero.responsable());
    assertEquals("sur@ejemplo.org", merendero.correo());
    assertEquals("alimentos", merendero.cat());
    assertEquals(
        new AdminEntidadesVista.Necesidad(
            "Leche en polvo", "alimentos", "Pide 40 kg, hasta el 31 de diciembre"),
        merendero.necesidades().get(0));
    assertNull(merendero.alta());
    assertNull(merendero.entregas());
  }

  @Test
  void sinSolicitudesNiNecesidadesNoPideMasDeLoQueHace() {
    donaciones.expect(requestTo(DONACIONES + "/api/entidades")).andRespond(withSuccess("[]", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/necesidades?estado=VIGENTE"))
        .andRespond(withSuccess("[]", JSON));

    assertEquals(List.of(), adaptador.entidades().todas());
    donaciones.verify();
  }

  @Test
  void aprobarHabilitaLaCuentaYDevuelveElNombre() {
    SolicitudesDeEntidad solicitudes = mock(SolicitudesDeEntidad.class);
    when(solicitudes.pendientes())
        .thenReturn(
            List.of(new Pendiente("cuenta-1", "Hogar Ejemplo Norte", "h@ejemplo.org", null)));
    cuentas.addBean("solicitudes", solicitudes);

    assertEquals("Hogar Ejemplo Norte", adaptador.aprobarEntidad("cuenta-1"));
    verify(solicitudes).aprobar("cuenta-1");
  }

  @Test
  void losDonantesSalenConSuTipoSuDocumentoYSuEstadoSinInventarLoQueNoHay() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes"))
        .andRespond(withSuccess(TRES_DONANTES, MediaType.APPLICATION_JSON));

    AdminDonantesVista vista = adaptador.donantes();

    assertEquals(2, vista.activos());
    assertEquals("10 de noviembre de 2026, 18:30", vista.al());
    AdminDonantesVista.Donante ana = vista.todos().get(0);
    assertEquals("Ana Ejemplo", ana.nombre());
    assertEquals("persona", ana.tipo());
    assertEquals("DNI 20000001", ana.documento());
    assertEquals("activo", ana.estado());
    assertNull(ana.alta());
    assertNull(ana.dona());
    assertEquals("organizacion", vista.todos().get(1).tipo());
    assertEquals("baja", vista.todos().get(1).estado());
    assertEquals("empresa", vista.todos().get(2).tipo());
    assertEquals("", vista.todos().get(2).documento());
    List<String> claves = vista.todos().stream().map(AdminDonantesVista.Donante::clave).toList();
    assertEquals(claves, vista.ordenNuevos());
    assertEquals(claves.reversed(), vista.ordenAntiguos());
    assertEquals(List.of(claves.get(0), claves.get(2), claves.get(1)), vista.ordenNombre());
  }

  @Test
  void elCsvViajaEnLaParteArchivoYSuAvanceDiceQuePantallaLeToca() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(Matchers.containsString("name=\"archivo\"")))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"path\": \"x.csv\", \"estado\": \"PENDIENTE\","
                    + " \"procesadas\": 0, \"creadas\": 0, \"actualizadas\": 0, \"conError\": 0}",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"PROCESANDO\", \"totalFilas\": null,"
                    + " \"procesadas\": 8400, \"creadas\": 8000, \"actualizadas\": 300,"
                    + " \"conError\": 100}",
                MediaType.APPLICATION_JSON));

    UUID id =
        adaptador.importar(
            new MockMultipartFile("archivo", "donantes.csv", "text/csv", "NOMBRE\nAna".getBytes()));
    Importacion avance = adaptador.importacion(id, "donantes.csv");

    assertEquals(ARCHIVO, id);
    assertEquals("importando", avance.accion());
    assertEquals("donantes.csv", avance.archivo());
    assertNull(avance.total());
    assertEquals("8.400", avance.procesadas());
    assertEquals(
        "/admin-donantes-importado?archivo=" + ARCHIVO + "&nombre=donantes.csv", avance.avance());
  }

  @Test
  void unaImportacionTerminadaTraeSuResultadoYLasFilasConErrorSinFechaDeFin() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"PROCESADO_CON_ERRORES\","
                    + " \"totalFilas\": 20000, \"procesadas\": 20000, \"creadas\": 19100,"
                    + " \"actualizadas\": 774, \"conError\": 126}",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/api/donantes/archivos/"
                    + ARCHIVO
                    + "/filas-con-error?page=0&size=20"))
        .andRespond(
            withSuccess(
                "[{\"linea\": 15, \"motivo\": \"Falta el apellido.\", \"codigo\": \"ERR-VAL-1\"}]",
                MediaType.APPLICATION_JSON));

    Importacion resultado = adaptador.importacion(ARCHIVO, null);

    assertEquals("importado", resultado.accion());
    assertNull(resultado.terminada());
    assertEquals(
        "20.000 filas leídas: 19.100 donantes nuevos, 774 actualizados y 126 filas con error.",
        resultado.resultado());
    assertEquals("126", resultado.conError());
    assertEquals("Fila 15", resultado.filas().get(0).fila());
    assertEquals("Falta el apellido.", resultado.filas().get(0).que());
  }

  @Test
  void registrarDonanteCreaLaPersonaYDespuesElDonanteConSuId() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/personas"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                    {"tipo": "HUMANA", "nombre": "Ana", "apellido": "Ejemplo",
                     "mediosDeContacto": [{"tipo": "CORREO", "direccionCorreo": "ana@ejemplo.org"}]}
                    """))
        .andRespond(withSuccess("{\"tipo\": \"HUMANA\", \"id\": \"" + PERSONA + "\"}", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"idPersona\": \"" + PERSONA + "\"}"))
        .andRespond(withSuccess("{}", JSON));

    adaptador.registrarDonante(new PersonaNueva.Humana("Ana", "Ejemplo", "ana@ejemplo.org"));

    donaciones.verify();
  }

  @Test
  void registrarEntidadCreaLaJuridicaYDespuesLaEntidadConSuId() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/personas"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                    {"tipo": "JURIDICA", "razonSocial": "Escuela Ejemplo N.º 5",
                     "tipoJuridico": "INSTITUCION",
                     "direccion": {"calle": "Calle Ejemplo", "altura": 123},
                     "representantes": [{"nombre": "Clara", "apellido": "Ejemplo"}]}
                    """))
        .andRespond(withSuccess("{\"tipo\": \"JURIDICA\", \"id\": \"" + PERSONA + "\"}", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{\"juridicaId\": \"" + PERSONA + "\"}"))
        .andRespond(withSuccess("{}", JSON));

    adaptador.registrarEntidad(
        new PersonaNueva.Juridica(
            "Escuela Ejemplo N.º 5",
            "INSTITUCION",
            null,
            new PersonaNueva.Direccion("Calle Ejemplo", 123, null, null, null, "Argentina"),
            new PersonaNueva.Humana("Clara", "Ejemplo", "escuela5@ejemplo.org"),
            "escuela5@ejemplo.org"));

    donaciones.verify();
  }

  @Test
  void unArchivoQueNoSePudoLeerLlevaASuError() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"ERROR\", \"procesadas\": 0,"
                    + " \"creadas\": 0, \"actualizadas\": 0, \"conError\": 0}",
                MediaType.APPLICATION_JSON));

    assertEquals("importar-error", adaptador.importacion(ARCHIVO, "x.csv").accion());
    assertNull(adaptador.importacion(null, null));
  }
}
