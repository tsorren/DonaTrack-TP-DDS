package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import grupo5.cliente.vistas.AdminInicioVista;
import grupo5.cliente.vistas.ConsultaDonantes;
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
  private MockRestServiceServer logistica;
  private AdminPersonasRest adaptador;
  private final StaticListableBeanFactory cuentas = new StaticListableBeanFactory();

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    donaciones = MockRestServiceServer.bindTo(builder).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    RestClient.Builder deLogistica = RestClient.builder();
    logistica = MockRestServiceServer.bindTo(deLogistica).build();
    adaptador =
        new AdminPersonasRest(
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class),
            ClientesHttp.cliente(deLogistica, "http://logistica", LogisticaApi.class),
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
        .expect(requestTo(DONACIONES + "/api/entidades?page=0&size=20"))
        .andRespond(withSuccess(DOS_ENTIDADES, JSON).header("X-Total-Count", "2"));
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
        .expect(
            requestTo(
                DONACIONES + "/api/personas?id=00000000-0000-4000-8000-0000000000e3&size=100"))
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
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades/publicas?size=1"))
        .andRespond(withSuccess("[]", JSON).header("X-Total-Count", "57"));

    AdminEntidadesVista vista = adaptador.entidades("", 1);

    donaciones.verify();
    // Las activas, un número del servicio: no se cuentan sobre la página.
    assertEquals(57, vista.activas());
    // Primero las que piden sumarse; después, la página de registradas por nombre.
    assertEquals(
        List.of("Hogar Ejemplo Norte", "Club Ejemplo Norte", "Merendero Ejemplo Sur"),
        vista.todas().stream().map(Entidad::nombre).toList());
    assertEquals(3, vista.enPaginas().total());
    assertEquals(1, vista.enPaginas().registradas().paginas());
    Entidad club = vista.todas().get(1);
    assertEquals("inactiva", club.estado());
    assertEquals("Institución", club.tipo());
    assertNull(club.baja());
    Entidad hogar = vista.todas().get(0);
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
  void sinSolicitudesNiEntidadesNoPideMasDeLoQueHace() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades?page=0&size=20"))
        .andRespond(withSuccess("[]", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades/publicas?size=1"))
        .andRespond(withSuccess("[]", JSON).header("X-Total-Count", "0"));

    assertEquals(List.of(), adaptador.entidades("", 1).todas());
    donaciones.verify();
  }

  @Test
  void laBusquedaVaAlServidorYDespuesDeLaPrimeraPaginaNoVanLasQuePidenSumarse() {
    SolicitudesDeEntidad solicitudes = mock(SolicitudesDeEntidad.class);
    when(solicitudes.pendientes())
        .thenReturn(
            List.of(
                new Pendiente("cuenta-1", "Comedor San José", "c@ejemplo.org", null),
                new Pendiente("cuenta-2", "Hogar Norte", "h@ejemplo.org", null)));
    cuentas.addBean("solicitudes", solicitudes);
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades?nombre=jose&page=1&size=20"))
        .andRespond(withSuccess(DOS_ENTIDADES, JSON).header("X-Total-Count", "22"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/necesidades?estado=VIGENTE"))
        .andRespond(withSuccess("[]", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades/publicas?size=1"))
        .andRespond(withSuccess("[]", JSON).header("X-Total-Count", "30"));

    AdminEntidadesVista vista = adaptador.entidades(" jose ", 2);

    donaciones.verify();
    assertEquals(
        List.of("Club Ejemplo Norte", "Merendero Ejemplo Sur"),
        vista.todas().stream().map(Entidad::nombre).toList());
    // 22 registradas que coinciden y una que pide sumarse ("José", sin distinguir la tilde).
    assertEquals(23, vista.enPaginas().total());
    assertEquals("jose", vista.enPaginas().buscar());
    assertEquals(2, vista.enPaginas().registradas().pagina());
    assertEquals(2, vista.enPaginas().registradas().paginas());
  }

  @Test
  void elInicioCuentaLoQueEsperaDecisionYCadaCifraConLoQueDanLasApis() {
    SolicitudesDeEntidad solicitudes = mock(SolicitudesDeEntidad.class);
    when(solicitudes.pendientes())
        .thenReturn(
            List.of(new Pendiente("cuenta-1", "Hogar Ejemplo Norte", "h@ejemplo.org", null)));
    cuentas.addBean("solicitudes", solicitudes);
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(
            withSuccess(
                """
                {"donacionesPorEstado": {"EN_DEPOSITO": 2, "ASIGNACION_REALIZADA": 1,
                                         "LISTA_PARA_ENTREGAR": 1, "EN_TRASLADO": 1,
                                         "EN_PUERTA": 1, "ENTREGADA": 9},
                 "entregadasEnElMes": {"mes": "2026-11", "cantidad": 1},
                 "porVencer": {"dias": 7, "cantidad": 1,
                               "proxima": {"id": "00000000-0000-4000-8000-0000000000d1",
                                           "descripcion": "Leche en polvo",
                                           "fechaVencimiento": "2026-11-12"}},
                 "donantes": {"total": 20003, "activos": 20001},
                 "entidadesActivas": 1, "propuestasPendientes": 1,
                 "calculadoEn": "2026-11-10T18:30:00-03:00"}
                """,
                JSON));
    logistica
        .expect(requestTo("http://logistica/api/camiones"))
        .andRespond(
            withSuccess(
                """
                [{"patente": "AA000AA", "estado": "EN_RUTA"},
                 {"patente": "AA000AB", "estado": "DISPONIBLE"},
                 {"patente": "AA000AC", "estado": "DISPONIBLE"}]
                """,
                JSON));

    AdminInicioVista inicio = adaptador.inicio();

    assertEquals("Martes 10 de noviembre de 2026", inicio.fecha());
    assertEquals("10 de noviembre de 2026, 18:30", inicio.al());
    assertEquals(
        "Hoy esperan tu decisión 1 organización y 1 propuesta de asignación. Hay 2 donaciones sin"
            + " asignar en el depósito; 1 vence en los próximos 7 días.",
        inicio.resumen());
    assertEquals(1, inicio.decisiones().entidades());
    assertEquals(
        "La primera, Leche en polvo, vence el 12 de noviembre, en 2 días.",
        inicio.decisiones().primeraQueVence());
    // Las propuestas, con el número del resumen: la lista no se pide (DTI-40, ítem 1).
    assertEquals(1, inicio.decisiones().propuestas());
    assertEquals(0, inicio.decisiones().entidadesDeMas());
    assertEquals(2, inicio.decisiones().sinAsignar());
    assertEquals(
        new AdminInicioVista.Cifras(
            2,
            2,
            2,
            0,
            0,
            1,
            "en noviembre, del 1 al 10",
            9,
            1,
            20001,
            20003,
            new AdminInicioVista.Camiones(1, 2)),
        inicio.cifras());
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
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes?orden=nuevos&page=0&size=20"))
        .andRespond(withSuccess(TRES_DONANTES, JSON).header(Paginas.TOTAL, "3"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes?activo=true&size=1"))
        .andRespond(withSuccess("[]", JSON).header(Paginas.TOTAL, "2"));

    AdminDonantesVista vista = adaptador.donantes(ConsultaDonantes.primera());

    assertEquals(2, vista.activos());
    assertEquals("10 de noviembre de 2026, 18:30", vista.al());
    assertEquals(3, vista.donantes().total());
    assertEquals(1, vista.donantes().paginas());
    AdminDonantesVista.Donante ana = vista.donantes().filas().get(0);
    assertEquals("Ana Ejemplo", ana.nombre());
    assertEquals("persona", ana.tipo());
    assertEquals("DNI 20000001", ana.documento());
    assertEquals("activo", ana.estado());
    assertNull(ana.alta());
    assertNull(ana.dona());
    assertEquals("organizacion", vista.donantes().filas().get(1).tipo());
    assertEquals("baja", vista.donantes().filas().get(1).estado());
    assertEquals("empresa", vista.donantes().filas().get(2).tipo());
    assertEquals("", vista.donantes().filas().get(2).documento());
    donaciones.verify();
  }

  @Test
  void laBusquedaYLosFiltrosLosResuelveDonacionesYUnaPaginaDeMasPideLaUltima() {
    UUID alimentos = UUID.fromString("00000000-0000-4000-8000-0000000000c1");
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(
            withSuccess("[{\"id\": \"" + alimentos + "\", \"nombre\": \"Alimentos\"}]", JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/api/donantes?q=jose&tipoJuridico=ONG&activo=false&categoriaId="
                    + alimentos
                    + "&orden=nombre&page=8&size=20"))
        .andRespond(withSuccess("[]", JSON).header(Paginas.TOTAL, "41"));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/api/donantes?q=jose&tipoJuridico=ONG&activo=false&categoriaId="
                    + alimentos
                    + "&orden=nombre&page=2&size=20"))
        .andRespond(withSuccess(TRES_DONANTES, JSON).header(Paginas.TOTAL, "41"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes?activo=true&size=1"))
        .andRespond(withSuccess("[]", JSON).header(Paginas.TOTAL, "2"));

    AdminDonantesVista vista =
        adaptador.donantes(
            new ConsultaDonantes(" jose ", "ong", alimentos.toString(), "baja", "nombre", 9));

    assertEquals(3, vista.donantes().pagina());
    assertEquals(3, vista.donantes().paginas());
    assertEquals(41, vista.donantes().total());
    assertEquals("jose", vista.consulta().buscar());
    donaciones.verify();
  }

  @Test
  void unFiltroQueNoEsDeLasOpcionesSeDescarta() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes?orden=nuevos&page=0&size=20"))
        .andRespond(withSuccess("[]", JSON).header(Paginas.TOTAL, "0"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes?activo=true&size=1"))
        .andRespond(withSuccess("[]", JSON).header(Paginas.TOTAL, "0"));

    AdminDonantesVista vista =
        adaptador.donantes(new ConsultaDonantes("", "marciano", "x", "pendiente", "raro", 1));

    assertEquals(ConsultaDonantes.primera(), vista.consulta());
    donaciones.verify();
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
                    + " \"conError\": 100, \"ultimos\": [{\"nombre\": \"Ana Ejemplo\","
                    + " \"resultado\": \"NUEVO\"}, {\"nombre\": \"Juan Ejemplo\","
                    + " \"resultado\": \"ACTUALIZADO\"}]}",
                MediaType.APPLICATION_JSON));

    UUID id =
        adaptador.importar(
            new MockMultipartFile("archivo", "donantes.csv", "text/csv", "NOMBRE\nAna".getBytes()));
    Importacion avance = adaptador.importacion(id, "donantes.csv", "importando", 1);

    assertEquals(ARCHIVO, id);
    assertEquals("importando", avance.accion());
    assertEquals("leyendo", avance.etapa());
    assertEquals("18:30:00", avance.hora());
    assertEquals("donantes.csv", avance.archivo());
    assertNull(avance.total());
    assertEquals("8.400", avance.procesadas());
    assertEquals(
        "/admin-donantes-importado?archivo=" + ARCHIVO + "&nombre=donantes.csv", avance.avance());
    assertEquals(
        List.of(
            new AdminDonantesVista.Ultimo("Ana Ejemplo", "Nuevo"),
            new AdminDonantesVista.Ultimo("Juan Ejemplo", "Actualizado")),
        avance.ultimos());
    assertNull(avance.filas());
  }

  @Test
  void conElTotalDeFilasLaBarraTieneMedida() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"PROCESANDO\", \"totalFilas\": 20000,"
                    + " \"procesadas\": 8400, \"creadas\": 7950, \"actualizadas\": 410,"
                    + " \"conError\": 40}",
                JSON));

    Importacion avance = adaptador.importacion(ARCHIVO, null, "importando", 1);

    assertEquals("importando", avance.etapa());
    assertEquals("20.000", avance.total());
    assertEquals(8400, avance.leidas());
    assertEquals(20000, avance.totalFilas());
    assertEquals(List.of(), avance.ultimos());
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
                "[{\"linea\": 15, \"motivo\": \"DATOS_INVALIDOS\", \"codigo\": \"ERR-VAL-1\"}]",
                MediaType.APPLICATION_JSON));

    Importacion resultado = adaptador.importacion(ARCHIVO, null, "importado", 1);

    assertEquals("importado", resultado.accion());
    assertEquals("terminada", resultado.etapa());
    assertEquals(
        "20.000 filas leídas: 19.100 donantes nuevos, 774 actualizados y 126 filas con error.",
        resultado.resultado());
    assertEquals("126", resultado.conError());
    assertEquals(126, resultado.filas().total());
    assertEquals(7, resultado.filas().paginas());
    assertEquals("Fila 15", resultado.filas().filas().get(0).fila());
    assertEquals(
        "Hay un dato que no pudimos leer: el tipo de persona, el documento o la fecha.",
        resultado.filas().filas().get(0).que());
  }

  @Test
  void lasFilasConErrorSePidenDeAUnaPaginaYUnaDeMasEsLaUltima() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"PROCESADO_CON_ERRORES\","
                    + " \"totalFilas\": 100, \"procesadas\": 100, \"creadas\": 60,"
                    + " \"actualizadas\": 10, \"conError\": 30}",
                JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/api/donantes/archivos/"
                    + ARCHIVO
                    + "/filas-con-error?page=1&size=20"))
        .andRespond(withSuccess("[{\"linea\": 1021, \"motivo\": \"FORMATO_INVALIDO\"}]", JSON));

    Importacion resultado = adaptador.importacion(ARCHIVO, null, "importado", 5);

    assertEquals(2, resultado.filas().pagina());
    assertEquals("Fila 1.021", resultado.filas().filas().get(0).fila());
    assertEquals(
        "La fila no tiene las mismas columnas que el encabezado.",
        resultado.filas().filas().get(0).que());
    donaciones.verify();
  }

  /** R11-02: ningún código del servicio llega a la columna «Qué pasó». */
  @Test
  void cadaMotivoDeUnaFilaConErrorSeLeeEnPalabrasYUnoDesconocidoComoElInesperado() {
    for (String motivo :
        List.of(
            "FORMATO_INVALIDO",
            "DATOS_INVALIDOS",
            "DONANTE_DADO_DE_BAJA",
            "PERSONA_DUPLICADA_EN_CONFLICTO",
            "ERROR_INESPERADO",
            "OTRO_NUEVO")) {
      String que = AdminPersonasRest.queFallo(motivo);
      assertFalse(que.contains("_"), motivo + " -> " + que);
      assertTrue(que.endsWith("."), que);
    }
    assertEquals(
        "El correo ya es de otro donante, con otro documento.",
        AdminPersonasRest.queFallo("PERSONA_DUPLICADA_EN_CONFLICTO"));
    assertEquals(
        AdminPersonasRest.queFallo("ERROR_INESPERADO"), AdminPersonasRest.queFallo("OTRO_NUEVO"));
    assertEquals(AdminPersonasRest.queFallo("ERROR_INESPERADO"), AdminPersonasRest.queFallo(null));
  }

  @Test
  void sinFilasConErrorNoLasPideYElResultadoNoLasNombra() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"PROCESADO\","
                    + " \"totalFilas\": 2, \"procesadas\": 2, \"creadas\": 2,"
                    + " \"actualizadas\": 0, \"conError\": 0}",
                JSON));

    Importacion resultado = adaptador.importacion(ARCHIVO, null, "importado", 1);

    assertEquals("2 filas leídas: 2 donantes nuevos y 0 actualizados.", resultado.resultado());
    assertEquals(0, resultado.filas().total());
    donaciones.verify();
  }

  @Test
  void unErrorConFilasLeidasEsUnaImportacionCortada() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/archivos/" + ARCHIVO))
        .andRespond(
            withSuccess(
                "{\"id\": \""
                    + ARCHIVO
                    + "\", \"estado\": \"ERROR\", \"procesadas\": 8400,"
                    + " \"creadas\": 7950, \"actualizadas\": 410, \"conError\": 0}",
                JSON));

    Importacion cortada = adaptador.importacion(ARCHIVO, null, "importado", 1);

    assertEquals("importado", cortada.accion());
    assertEquals("cortada", cortada.etapa());
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

    assertEquals(
        "importar-error", adaptador.importacion(ARCHIVO, "x.csv", "importando", 1).accion());
    assertNull(adaptador.importacion(null, null, "importando", 1));
  }
}
