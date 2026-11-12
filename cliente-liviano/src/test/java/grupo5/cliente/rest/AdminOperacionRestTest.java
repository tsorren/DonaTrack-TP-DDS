package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.DirectorioDeAdministradores;
import grupo5.cliente.vistas.AdminAsignacionesVista;
import grupo5.cliente.vistas.AdminAsignacionesVista.Cuentas;
import grupo5.cliente.vistas.AdminAsignacionesVista.Decidida;
import grupo5.cliente.vistas.AdminAsignacionesVista.DonacionEnJuego;
import grupo5.cliente.vistas.AdminAsignacionesVista.Propuesta;
import grupo5.cliente.vistas.AdminAsignacionesVista.Sugerencia;
import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import grupo5.cliente.vistas.AdminDonacionesRegistroVista;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import grupo5.cliente.vistas.ConsultaDecididas;
import grupo5.cliente.vistas.ConsultaDonaciones;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de la operación del admin contra respuestas de ejemplo (plan E6, §F1). */
class AdminOperacionRestTest {

  private static final String CAMIONES = "http://logistica/api/camiones";
  private static final String ID = "6f1c2c1e-0000-4000-8000-0000000000c1";

  /** Un camión en ruta, uno chico disponible y uno deshabilitado (HB-23). */
  private static final String FLOTA =
      """
      [
        {"id": "6f1c2c1e-0000-4000-8000-0000000000c1", "patente": "AB 123 CD",
         "capacidadVolumen": 18.0, "altura": 3.2, "capacidadKG": 3500.0, "estado": "EN_RUTA",
         "rutaId": "6f1c2c1e-0000-4000-8000-0000000000f1"},
        {"id": "6f1c2c1e-0000-4000-8000-0000000000c2", "patente": "AD 789 GH",
         "capacidadVolumen": 9.5, "altura": 2.5, "capacidadKG": 1500.0, "estado": "DISPONIBLE",
         "rutaId": null},
        {"id": "6f1c2c1e-0000-4000-8000-0000000000c3", "patente": "AF 345 KL",
         "capacidadVolumen": 12.0, "altura": 2.8, "capacidadKG": 2000.0,
         "estado": "DESHABILITADO", "rutaId": null}
      ]
      """;

  private static final String DONACIONES = "http://donaciones";
  private static final String N1 = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String N2 = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final String E1 = "6f1c2c1e-0000-4000-8000-0000000000e1";
  private static final String P1 = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String P2 = "6f1c2c1e-0000-4000-8000-0000000000b2";

  private MockRestServiceServer logistica;
  private MockRestServiceServer donaciones;
  private AdminOperacionRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    logistica = MockRestServiceServer.bindTo(builder).build();
    RestClient.Builder deDonaciones = RestClient.builder();
    donaciones = MockRestServiceServer.bindTo(deDonaciones).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new AdminOperacionRest(
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class),
            ClientesHttp.cliente(deDonaciones, DONACIONES, DonacionesApi.class),
            reloj);
  }

  private static final String ASIGNACIONES = DONACIONES + "/api/asignaciones";
  private static final String RESUMEN_PROPUESTAS = ASIGNACIONES + "/propuestas/resumen";
  private static final String ULTIMA = ASIGNACIONES + "/ejecuciones?page=0&size=1";
  private static final String RECIENTES =
      ASIGNACIONES
          + "/propuestas?page=0&size=3&estado=APROBADA&estado=DESCARTADA"
          + "&orden=decision_reciente";
  private static final String D_ARROZ = "6f1c2c1e-0000-4000-8000-0000000000d1";

  /**
   * Una pendiente vieja sugerida por los dos (las donaciones son de Quienes menos recibieron) y una
   * de hoy, de antes de guardar el algoritmo, que cubre una parte (el donante ocultó su nombre).
   * Las dos ya traen la organización y la unidad: no se piden aparte.
   */
  private static final String LIBRES =
      """
      [
        {"id": "%s", "estado": "PENDIENTE", "fechaCreacion": "2026-10-15T10:00:00",
         "situacion": "LIBRE", "otrasPendientesMismaNecesidad": 0,
         "necesidad": {"id": "%s", "descripcion": "Arroz", "cantidadNecesitada": 25,
                       "entidadNombre": "Comedor Ejemplo", "unidad": "KILOGRAMO"},
         "sugeridaPor": [
           {"algoritmo": "PRIORIDAD_SUBATENDIDOS", "puesto": 1, "deCuantas": 6,
            "eligioLasDonaciones": true},
           {"algoritmo": "COMPATIBILIDAD_SEMANTICA", "puesto": 3, "deCuantas": 10,
            "eligioLasDonaciones": false}],
         "fragmentaciones": [{"cantidadNecesaria": 25, "disponible": 25, "pedidaEnOtras": 0,
           "otrasPropuestas": 0, "donacionIndependiente":
           {"id": "%s", "descripcion": "Arroz", "cantidad": 25,
            "quienDono": {"tipo": "HUMANA", "nombreVisible": "Ana Ejemplo"}}}]},
        {"id": "%s", "estado": "PENDIENTE", "fechaCreacion": "2026-11-10T09:05:00",
         "situacion": "LIBRE", "sugeridaPor": [],
         "necesidad": {"id": "%s", "descripcion": "Leche en polvo", "cantidadNecesitada": 12,
                       "entidadNombre": "Merendero Ejemplo", "unidad": "UNIDADES"},
         "fragmentaciones": [{"cantidadNecesaria": 10, "disponible": 10, "donacionIndependiente":
           {"descripcion": "Leche en polvo", "cantidad": 10,
            "quienDono": {"tipo": "JURIDICA", "nombreVisible": null}}}]}
      ]
      """
          .formatted(P1, N1, D_ARROZ, P2, N2);

  private void resumen(int libres, int enConflicto, int sinStock) {
    donaciones
        .expect(requestTo(RESUMEN_PROPUESTAS))
        .andRespond(
            withSuccess(
                """
                {"pendientes": %d, "libres": %d, "enConflicto": %d, "sinStock": %d}"""
                    .formatted(libres + enConflicto + sinStock, libres, enConflicto, sinStock),
                MediaType.APPLICATION_JSON));
  }

  private void ultima(String cuerpo) {
    donaciones
        .expect(requestTo(ULTIMA))
        .andRespond(withSuccess(cuerpo, MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "3"));
  }

  @Test
  void propuestasPideUnaPaginaSinProblemasElResumenLaUltimaYLasTresDecididas() {
    resumen(12, 2, 1);
    ultima(
        """
        [{"fechaEjecucion": "2026-11-10T00:00:00", "cantidadPropuestasGeneradas": 12}]""");
    donaciones
        .expect(
            requestTo(ASIGNACIONES + "/propuestas?page=0&size=10&estado=PENDIENTE&situacion=LIBRE"))
        .andRespond(withSuccess(LIBRES, MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "12"));
    donaciones
        .expect(requestTo(RECIENTES))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "estado": "APROBADA",
                  "necesidad": {"descripcion": "Fideos", "cantidadNecesitada": 20,
                                "entidadNombre": "Hogar Ejemplo", "unidad": "KILOGRAMO"}}]"""
                    .formatted(P1),
                MediaType.APPLICATION_JSON));

    AdminAsignacionesVista vista = adaptador.asignaciones(ConsultaAsignaciones.primera(), "");

    assertEquals(new Cuentas(15, 12, 3), vista.cuentas());
    assertEquals("hoy, 0:00 · generó 12 propuestas", vista.ultima());
    assertEquals(12, vista.propuestas().total());
    assertEquals(2, vista.propuestas().paginas());
    assertEquals(
        List.of(
            new Propuesta(
                P1,
                "Comedor Ejemplo",
                "Arroz, 25 kg",
                "15 de octubre",
                "Arroz · 25 kg (de Ana Ejemplo)",
                "Cubre la necesidad completa.",
                false,
                List.of(
                    new Sugerencia("Quienes menos recibieron", "1.º de 6"),
                    new Sugerencia("Parecido con lo pedido", "3.º de 10")),
                "Las donaciones son las que eligió Quienes menos recibieron.",
                "libre",
                null,
                null),
            new Propuesta(
                P2,
                "Merendero Ejemplo",
                "Leche en polvo, 12 unidades",
                "hoy, 9:05",
                "Leche en polvo · 10 unidades",
                "Cubre 10 de 12 unidades; el resto sigue pendiente.",
                true,
                List.of(),
                "Sin el dato del algoritmo: se generó antes de que lo guardáramos.",
                "libre",
                null,
                null)),
        vista.propuestas().filas());
    assertEquals(
        List.of(
            new Decidida(
                "Hogar Ejemplo",
                "Fideos, 20 kg",
                "Aprobada",
                null,
                "Se decidió antes de que guardáramos quién y cuándo.",
                null,
                List.of())),
        vista.recientes());
    // Ni una necesidad ni una organización por propuesta, ni listas sin página (A11)
    donaciones.verify();
  }

  @Test
  void elFiltroVerPideLasDelAlgoritmoElegido() {
    resumen(0, 0, 0);
    ultima("[]");
    donaciones
        .expect(
            requestTo(
                ASIGNACIONES
                    + "/propuestas?page=0&size=10&estado=PENDIENTE&situacion=LIBRE"
                    + "&algoritmo=PRIORIDAD_SUBATENDIDOS"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "0"));
    donaciones
        .expect(requestTo(RECIENTES))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    AdminAsignacionesVista vista =
        adaptador.asignaciones(
            ConsultaAsignaciones.de(null, null, "menos-recibieron", null, null, 1), "");

    assertNull(vista.ultima());
    assertEquals("Quienes menos recibieron", vista.algoritmoElegido());
    donaciones.verify();
  }

  @Test
  void conflictosDiceConQuienChocaCadaUnaYLaQueYaNoAlcanzaNoSeAprueba() {
    resumen(0, 2, 1);
    ultima("[]");
    donaciones
        .expect(
            requestTo(
                ASIGNACIONES
                    + "/propuestas?page=0&size=10&estado=PENDIENTE"
                    + "&situacion=EN_CONFLICTO&situacion=SIN_STOCK"))
        .andRespond(
            withSuccess(
                """
                [
                  {"id": "%s", "estado": "PENDIENTE", "situacion": "EN_CONFLICTO",
                   "necesidad": {"descripcion": "Arroz", "cantidadNecesitada": 25,
                                 "entidadNombre": "Comedor Ejemplo", "unidad": "KILOGRAMO"},
                   "fragmentaciones": [{"cantidadNecesaria": 25, "disponible": 25,
                     "pedidaEnOtras": 20, "otrasPropuestas": 1, "donacionIndependiente":
                     {"id": "%s", "descripcion": "Arroz", "cantidad": 25,
                      "quienDono": {"nombreVisible": "Ana Ejemplo"}}}]},
                  {"id": "%s", "estado": "PENDIENTE", "situacion": "EN_CONFLICTO",
                   "otrasPendientesMismaNecesidad": 1,
                   "necesidad": {"descripcion": "Fideos", "cantidadNecesitada": 10,
                                 "entidadNombre": "Escuela Ejemplo", "unidad": "KILOGRAMO"},
                   "fragmentaciones": [{"cantidadNecesaria": 10, "disponible": 10,
                     "donacionIndependiente": {"descripcion": "Fideos", "cantidad": 10}}]},
                  {"id": "6f1c2c1e-0000-4000-8000-0000000000b3", "estado": "PENDIENTE",
                   "situacion": "SIN_STOCK",
                   "necesidad": {"descripcion": "Azúcar", "cantidadNecesitada": 15,
                                 "entidadNombre": "Hogar Ejemplo", "unidad": "KILOGRAMO"},
                   "fragmentaciones": [{"cantidadNecesaria": 15, "disponible": 0,
                     "donacionIndependiente": {"descripcion": "Azúcar", "cantidad": 15,
                      "estadoActual": "AsignacionRealizada"}}]}
                ]"""
                    .formatted(P1, D_ARROZ, P2),
                MediaType.APPLICATION_JSON));

    List<Propuesta> conflictos =
        adaptador
            .asignaciones(ConsultaAsignaciones.de("conflictos", null, null, null, null, 1), "")
            .propuestas()
            .filas();

    assertEquals(
        "Compite por Arroz · 25 kg (de Ana Ejemplo) con 1 propuesta más: alcanza para una.",
        conflictos.get(0).franja());
    assertEquals(D_ARROZ, conflictos.get(0).comparar());
    assertEquals(
        "Cubre la misma necesidad que otra propuesta pendiente. Si aprobás las dos, se asigna de"
            + " más.",
        conflictos.get(1).franja());
    assertNull(conflictos.get(1).comparar());
    assertEquals(
        "Ya no alcanza: Azúcar · 15 kg quedó asignada a otra organización. Podés descartarla.",
        conflictos.get(2).franja());
    assertFalse(conflictos.get(2).aprobable());
    donaciones.verify();
  }

  @Test
  void destinoListaLasDonacionesEnJuegoYLaElegidaTraeTodasLasQueLaPiden() {
    resumen(1, 2, 0);
    ultima("[]");
    donaciones
        .expect(requestTo(ASIGNACIONES + "/donaciones?page=0&size=10"))
        .andRespond(
            withSuccess(
                    """
                [{"donacionIndependiente": {"id": "%s", "descripcion": "Arroz", "cantidad": 25,
                   "fechaRegistro": "2026-11-03T10:00:00",
                   "quienDono": {"nombreVisible": "Ana Ejemplo"}},
                  "disponible": 25, "pedidaEnPendientes": 45, "propuestasPendientes": 2,
                  "alcanza": false}]"""
                        .formatted(D_ARROZ),
                    MediaType.APPLICATION_JSON)
                .header(Paginas.TOTAL, "1"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonacionEnJuego arroz =
        adaptador
            .asignaciones(ConsultaAsignaciones.de("destino", null, null, null, null, 1), "")
            .donaciones()
            .filas()
            .getFirst();
    assertEquals(
        new DonacionEnJuego(
            D_ARROZ,
            "Arroz · 25",
            "Ana Ejemplo",
            "7 días en el depósito",
            2,
            false,
            "La piden 2 propuestas y no alcanza para todas."),
        arroz);
    donaciones.verify();
    donaciones.reset();

    resumen(1, 2, 0);
    ultima("[]");
    donaciones
        .expect(
            requestTo(
                ASIGNACIONES + "/propuestas?page=0&size=20&estado=PENDIENTE&donacion=" + D_ARROZ))
        .andRespond(withSuccess(LIBRES, MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "1"));

    AdminAsignacionesVista elegida =
        adaptador.asignaciones(
            ConsultaAsignaciones.de("destino", null, null, null, D_ARROZ, 1), "");
    assertEquals("La pide 1 propuesta.", elegida.elegida().resumen());
    assertNull(elegida.propuestas().filas().getFirst().comparar());
    donaciones.verify();
  }

  @Test
  void todasLasDecididasSonUnaPaginaDeVeinteSinResumen() {
    donaciones
        .expect(
            requestTo(
                ASIGNACIONES
                    + "/propuestas?page=1&size=20&estado=APROBADA&estado=DESCARTADA"
                    + "&orden=decision_reciente"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "25"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    AdminAsignacionesVista vista =
        adaptador.asignaciones(ConsultaAsignaciones.de(null, "decididas", null, null, null, 2), "");

    assertEquals(25, vista.decididas().total());
    assertNull(vista.cuentas());
    assertEquals(0, vista.sinFecha());
    donaciones.verify();
  }

  // R11-35: la búsqueda, los filtros y el orden van a Donaciones; el motivo y quién decidió vuelven
  @Test
  void todasLasDecididasBuscaYFiltraEnDonaciones() {
    String categoria = "6f1c2c1e-0000-4000-8000-0000000000ca";
    donaciones
        .expect(
            requestTo(
                ASIGNACIONES
                    + "/propuestas?page=0&size=20&estado=DESCARTADA&orden=decision_antigua"
                    + "&q=arroz&categoriaId="
                    + categoria
                    + "&decididaPor=SISTEMA&decididaDesde=2026-11-04&decididaHasta=2026-11-10"))
        .andRespond(
            withSuccess(
                    """
                    [{"id": "%s", "estado": "DESCARTADA", "decididaPor": "SISTEMA",
                      "fechaDecision": "2026-11-09T17:40:00",
                      "motivoDescarte": "  El merendero cerró por vacaciones. ",
                      "necesidad": {"descripcion": "Arroz", "cantidadNecesitada": 10,
                                    "entidadNombre": "Merendero Ejemplo", "unidad": "KILOGRAMO"}},
                     {"id": "%s", "estado": "DESCARTADA",
                      "necesidad": {"descripcion": "Arroz", "cantidadNecesitada": 5,
                                    "entidadNombre": "Comedor Ejemplo", "unidad": "KILOGRAMO"}}]"""
                        .formatted(P1, P2),
                    MediaType.APPLICATION_JSON)
                .header(Paginas.TOTAL, "2"));
    donaciones
        .expect(requestTo(RESUMEN_PROPUESTAS))
        .andRespond(
            withSuccess(
                """
                {"pendientes": 0, "libres": 0, "enConflicto": 0, "sinStock": 0,
                 "decididasSinFecha": 4}""",
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"nombre\": \"Alimentos\"}]".formatted(categoria),
                MediaType.APPLICATION_JSON));

    AdminAsignacionesVista vista =
        adaptador.asignaciones(
            ConsultaAsignaciones.de(null, "decididas", null, null, null, 1)
                .conDecididas(
                    new ConsultaDecididas(
                        "arroz", "descartadas", categoria, "7-dias", "antes", "antiguas", "")),
            "");

    assertEquals(4, vista.sinFecha());
    Decidida primera = vista.decididas().filas().getFirst();
    assertEquals("Se descartó el 9 de noviembre de 2026, 17:40.", primera.quien());
    assertEquals("El merendero cerró por vacaciones.", primera.motivo());
    Decidida segunda = vista.decididas().filas().get(1);
    assertEquals("Se decidió antes de que guardáramos quién y cuándo.", segunda.quien());
    assertEquals("", segunda.motivo());
    assertEquals(
        List.of("", "vos", "antes"), vista.autores().stream().map(o -> o.valor()).toList());
    donaciones.verify();
  }

  @Test
  void correrLaAsignacionDevuelveCuantasPropuestasGenero() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/asignaciones/ejecuciones"))
        .andExpect(method(POST))
        .andRespond(withSuccess("[{}, {}]", MediaType.APPLICATION_JSON));

    assertEquals(2, adaptador.correrAsignacion());
  }

  @Test
  void aprobarMandaElEstadoYCuentaLasQuePasaronAConflictos() {
    resumen(3, 2, 0);
    donaciones
        .expect(requestTo(DONACIONES + "/api/asignaciones/propuestas/" + P1 + "/estado"))
        .andExpect(method(PUT))
        .andExpect(content().json("{\"estado\": \"APROBADA\", \"justificacion\": null}"))
        .andRespond(withSuccess());
    resumen(2, 1, 1);

    assertEquals(1, adaptador.decidirPropuesta(P1, true, " "));
    donaciones.verify();
  }

  @Test
  void descartarSoloMandaElEstadoConElMotivo() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/asignaciones/propuestas/" + P1 + "/estado"))
        .andExpect(method(PUT))
        .andExpect(
            content().json("{\"estado\": \"DESCARTADA\", \"justificacion\": \"Ya no hace falta\"}"))
        .andRespond(withSuccess());

    assertEquals(0, adaptador.decidirPropuesta(P1, false, " Ya no hace falta "));
    donaciones.verify();
  }

  @Test
  void pideTodosLosCamionesConLosDeshabilitadosYNoInventaLoQueLogisticaNoDa() {
    logistica
        .expect(requestTo(CAMIONES + "?incluirDeshabilitados=true"))
        .andRespond(withSuccess(FLOTA, MediaType.APPLICATION_JSON));

    AdminCamionesVista flota = adaptador.camiones("");

    assertEquals("Al 10 de noviembre de 2026, 18:30.", flota.al());
    assertNull(flota.entregas());
    Camion ruta = flota.camiones().get(0);
    assertEquals(ID, ruta.clave());
    assertEquals("ruta", ruta.estado());
    assertEquals("3.500 kg · 18 m³", ruta.capacidad());
    assertEquals("grande", ruta.tamano());
    assertEquals("camion", ruta.forma());
    assertNull(ruta.nombre());
    assertNull(ruta.chofer());
    assertNull(ruta.viaje());
    assertNull(ruta.rendimiento());
    Camion chico = flota.camiones().get(1);
    assertEquals("disponible", chico.estado());
    assertEquals("1.500 kg · 9,5 m³", chico.capacidad());
    assertEquals("chico", chico.tamano());
    assertEquals("furgon", chico.forma());
    Camion parado = flota.camiones().get(2);
    assertEquals("deshabilitado", parado.estado());
    assertEquals("mediano", parado.tamano());
    assertNull(parado.motivo());
    assertFalse(parado.nuevo());
    logistica.verify();
  }

  @Test
  void agregaUnCamionYDevuelveSuClave() {
    logistica
        .expect(requestTo(CAMIONES))
        .andExpect(method(POST))
        .andExpect(
            content()
                .json(
                    "{\"patente\": \"AB 123 CD\", \"capacidadVolumen\": 18.0, \"altura\": 3.2,"
                        + " \"capacidadKG\": 3500.0}"))
        .andRespond(
            withSuccess(
                "{\"id\": \"" + ID + "\", \"patente\": \"AB 123 CD\", \"estado\": \"DISPONIBLE\"}",
                MediaType.APPLICATION_JSON));

    assertEquals(ID, adaptador.agregarCamion("AB 123 CD", 3500f, 18f, 3.2f));
    logistica.verify();
  }

  @Test
  void deshabilitaConElMotivoYHabilitaSinMotivo() {
    String respuesta = "{\"id\": \"" + ID + "\", \"patente\": \"AB 123 CD\"}";
    logistica
        .expect(requestTo(CAMIONES + "/" + ID + "/estado"))
        .andExpect(method(PATCH))
        .andExpect(content().json("{\"estado\": \"DESHABILITADO\", \"motivo\": \"Service\"}"))
        .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
    logistica
        .expect(requestTo(CAMIONES + "/" + ID + "/estado"))
        .andExpect(method(PATCH))
        .andExpect(content().json("{\"estado\": \"DISPONIBLE\", \"motivo\": null}"))
        .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));

    assertEquals("AB 123 CD", adaptador.cambiarCamion(ID, false, " Service "));
    assertEquals("AB 123 CD", adaptador.cambiarCamion(ID, true, null));
    logistica.verify();
  }

  private static final String D1 = "6f1c2c1e-0000-4000-8000-0000000000d1";
  private static final String D2 = "6f1c2c1e-0000-4000-8000-0000000000d2";
  private static final String D3 = "6f1c2c1e-0000-4000-8000-0000000000d3";
  private static final String O1 = "6f1c2c1e-0000-4000-8000-0000000000f1";
  private static final String H1 = "6f1c2c1e-0000-4000-8000-0000000000a9";

  /** El resumen de Donaciones: las cifras de la cabecera, ya contadas allá. */
  private static final String RESUMEN =
      """
      {"donacionesPorEstado": {"EN_DEPOSITO": 1, "ASIGNACION_REALIZADA": 1, "ENTREGADA": 1},
       "entregadasEnElMes": {"mes": "2026-11", "cantidad": 1},
       "porVencer": {"dias": 7, "cantidad": 1,
                     "proxima": {"descripcion": "Leche en polvo", "fechaVencimiento": "2026-11-12"}},
       "donantes": {"total": 3, "activos": 2}, "entidadesActivas": 1, "propuestasPendientes": 0}
      """;

  /**
   * Una en el depósito que vence en 2 días (el donante ocultó su nombre, pero el admin lo ve en la
   * original), una asignada y una entregada este mes.
   */
  private static final String INDEPENDIENTES =
      """
      [
        {"id": "%1$s", "descripcion": "Arroz", "estadoActual": "ENTREGADA",
         "fechaRegistro": "2026-11-01T10:00:00", "cantidad": 25,
         "entidadBeneficiariaId": "%4$s",
         "historial": [
           {"estadoNuevo": "EnDeposito", "timestamp": "2026-11-01T10:00:00", "estadoAnterior": null},
           {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-03T09:00:00",
            "estadoAnterior": "EnDeposito", "actor": "Leo"},
           {"estadoNuevo": "EnTraslado", "timestamp": "2026-11-05T14:30:00",
            "estadoAnterior": "ListaParaEntregar"},
           {"estadoNuevo": "Entregada", "timestamp": "2026-11-05T17:00:00",
            "estadoAnterior": "EnTraslado"}],
         "items": [{"cantidad": 25, "bien": {"categoria": {"id": "%4$s", "nombre": "Alimentos",
           "unidad": "KILOGRAMO"}, "bien": {"descripcion": "Arroz", "estado": "NUEVO"}}}]},
        {"id": "%2$s", "descripcion": "Leche en polvo", "estadoActual": "EnDeposito",
         "fechaRegistro": "2026-11-08T10:00:00", "cantidad": 10, "donacionOriginalId": "%5$s",
         "quienDono": {"tipo": "HUMANA", "nombreVisible": null, "donanteId": "%6$s"},
         "items": [{"cantidad": 10, "bien": {"categoria": {"id": "%4$s", "nombre": "Alimentos",
           "unidad": "UNIDADES"}, "bien": {"descripcion": "Leche en polvo",
           "fechaVencimiento": "2026-11-12", "estado": "NUEVO"}}}]},
        {"id": "%3$s", "descripcion": "Fideos", "estadoActual": "AsignacionRealizada",
         "fechaRegistro": "2026-11-09T10:00:00", "cantidad": 20, "entidadBeneficiariaId": "%4$s",
         "items": []}
      ]
      """
          .formatted(D1, D2, D3, E1, O1, H1);

  @Test
  void lasDonacionesSonUnaPaginaConLosNombresDeQuienesDonaronYLasCifrasDelResumen() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(withSuccess(RESUMEN, MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/donaciones-independientes?orden=recientes&page=0&size=20"))
        .andRespond(
            withSuccess(INDEPENDIENTES, MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "43"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/personas?id=" + H1 + "&size=20"))
        .andRespond(
            withSuccess(
                """
                [{"tipo": "HUMANA", "id": "%s", "nombre": "Ana", "apellido": "Ejemplo"}]"""
                    .formatted(H1),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    AdminDonacionesVista vista = adaptador.donaciones(new ConsultaDonaciones("", false, "", 1), "");

    assertEquals(
        List.of(D1, D2, D3),
        vista.donaciones().filas().stream().map(AdminDonacionesVista.Donacion::clave).toList());
    assertEquals(43, vista.donaciones().total());
    assertEquals(3, vista.donaciones().paginas());
    AdminDonacionesVista.Donacion leche = vista.donaciones().filas().get(1);
    assertEquals("Ana Ejemplo", leche.donante());
    assertEquals("12 de noviembre, en 2 días", leche.vence());
    assertEquals("10 u", leche.corto());
    assertEquals("Nuevo", leche.bien());
    assertEquals(true, leche.prontaAVencer());
    assertEquals("entregada", vista.donaciones().filas().get(0).estado());
    assertEquals("asignada", vista.donaciones().filas().get(2).estado());
    assertTrue(vista.conVence());
    assertNull(vista.porVencer());
    assertEquals(
        "1 en el depósito · 1 asignada · 0 en camino · 1 entrega confirmada en noviembre. Al 10 de"
            + " noviembre de 2026, 18:30.",
        vista.resumen());
    donaciones.verify();
  }

  // R11-34: "vencen pronto" lo filtra y lo ordena Donaciones (venceHasta y orden=vence)
  @Test
  void elEstadoYElOrdenLosResuelveDonacionesYVencenProntoLoFiltraDonaciones() {
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(withSuccess(RESUMEN, MediaType.APPLICATION_JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/donaciones-independientes?estado=EN_TRASLADO&estado=EN_PUERTA"
                    + "&orden=antiguas&page=1&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "21"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(withSuccess(RESUMEN, MediaType.APPLICATION_JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/donaciones-independientes?estado=EN_DEPOSITO&orden=vence"
                    + "&venceHasta=2026-11-17&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "0"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(withSuccess(RESUMEN, MediaType.APPLICATION_JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/donaciones-independientes?estado=EN_DEPOSITO&orden=antiguas"
                    + "&venceHasta=2026-11-17&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "0"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    AdminDonacionesVista enCamino =
        adaptador.donaciones(new ConsultaDonaciones("camino", false, "antigua", 2), "");
    AdminDonacionesVista pronto =
        adaptador.donaciones(new ConsultaDonaciones(null, true, "nombre", 1), "");
    AdminDonacionesVista prontoPorRegistro =
        adaptador.donaciones(new ConsultaDonaciones(null, true, "antigua", 1), "");

    assertEquals(2, enCamino.donaciones().pagina());
    assertEquals("antigua", enCamino.consulta().orden());
    // "nombre" no es un orden de la fuente: queda el de partida, que con "vencen pronto" es vence
    assertEquals("", pronto.consulta().orden());
    assertTrue(pronto.conVence());
    assertNull(pronto.porVencer());
    assertEquals("antigua", prontoPorRegistro.consulta().orden());
    assertEquals(
        List.of("", "antigua", "vence"),
        pronto.criterios().stream().map(AdminDonacionesVista.Criterio::valor).toList());
    donaciones.verify();
  }

  // R11-34: la búsqueda, la categoría, el período de registro y el donante van a Donaciones
  @Test
  void laBandejaDeDonacionesBuscaYFiltraEnDonaciones() {
    String categoria = "6f1c2c1e-0000-4000-8000-0000000000ca";
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/resumen"))
        .andRespond(withSuccess(RESUMEN, MediaType.APPLICATION_JSON));
    donaciones
        .expect(
            requestTo(
                DONACIONES
                    + "/donaciones-independientes?orden=recientes&q=leche&categoriaId="
                    + categoria
                    + "&donanteId="
                    + H1
                    + "&desde=2026-11-04&hasta=2026-11-10&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "0"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/categorias"))
        .andRespond(
            withSuccess(
                "[{\"id\": \"%s\", \"nombre\": \"Alimentos\"}]".formatted(categoria),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/personas?id=" + H1 + "&size=1"))
        .andRespond(
            withSuccess(
                """
                [{"tipo": "HUMANA", "id": "%s", "nombre": "Ana", "apellido": "Ejemplo"}]"""
                    .formatted(H1),
                MediaType.APPLICATION_JSON));

    AdminDonacionesVista vista =
        adaptador.donaciones(
            new ConsultaDonaciones("", false, "", 1, "leche", categoria, "7-dias", H1, ""), "");

    assertEquals("Ana Ejemplo", vista.donante());
    assertEquals(
        List.of(new AdminDonacionesVista.Opcion(categoria, "Alimentos")), vista.categorias());
    assertEquals(0, vista.donaciones().total());
    donaciones.verify();
  }

  @Test
  void elDetalleSePidePorSuIdConSuOriginalYSuEntidad() {
    donaciones
        .expect(requestTo(DONACIONES + "/donaciones-independientes/" + D1))
        .andRespond(
            withSuccess(
                """
                {"id": "%1$s", "descripcion": "Arroz", "estadoActual": "ENTREGADA",
                 "fechaRegistro": "2026-11-01T10:00:00", "cantidad": 25,
                 "entidadBeneficiariaId": "%2$s", "donacionOriginalId": "%3$s",
                 "historial": [
                   {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-03T09:00:00",
                    "estadoAnterior": "EnDeposito", "actor": "Leo"},
                   {"estadoNuevo": "EnTraslado", "timestamp": "2026-11-05T14:30:00",
                    "estadoAnterior": "ListaParaEntregar"},
                   {"estadoNuevo": "Entregada", "timestamp": "2026-11-05T17:00:00",
                    "estadoAnterior": "EnTraslado"}],
                 "items": []}"""
                    .formatted(D1, E1, O1),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donaciones/" + O1))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "donante": {"persona": {"nombre": "Ana", "apellido": "Ejemplo"}},
                 "deposito": {"calle": "Av. Ejemplo", "altura": 1234}}"""
                    .formatted(O1),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/entidades/" + E1))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "juridica": {"razonSocial": "Comedor Ejemplo"}}"""
                    .formatted(E1),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/donaciones-independientes/" + D3))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    AdminDonacionesVista.Donacion detalle = adaptador.donacion(D1, "detalle");

    assertEquals("entregada", detalle.estado());
    assertEquals("Ana Ejemplo", detalle.donante());
    assertEquals("Av. Ejemplo 1234", detalle.deposito());
    assertEquals("Comedor Ejemplo", detalle.entidad());
    assertEquals(
        List.of(
            "1 de noviembre: llegó al depósito de Av. Ejemplo 1234.",
            "3 de noviembre: quedó asignada a Comedor Ejemplo. La aprobó Leo.",
            "5 de noviembre, 14:30: salió en camino.",
            "5 de noviembre: Comedor Ejemplo confirmó que la recibió."),
        detalle.historial());
    assertNull(adaptador.donacion(D3, "detalle"));
    assertNull(adaptador.donacion("no-es-un-id", "detalle"));
    donaciones.verify();
  }

  @Test
  void elHistorialMuestraElNombreDeLaCuentaQueHizoCadaCambio() {
    String cuentaDeAna = "6f1c2c1e-0000-4000-8000-0000000000aa";
    DirectorioDeAdministradores cuentas = mock(DirectorioDeAdministradores.class);
    when(cuentas.cuenta(cuentaDeAna))
        .thenReturn(
            Optional.of(
                new DirectorioDeAdministradores.Administrador(
                    cuentaDeAna, "Ana", "Pérez", "ana.perez@ejemplo.org", null, true)));
    RestClient.Builder deDonaciones = RestClient.builder();
    MockRestServiceServer api = MockRestServiceServer.bindTo(deDonaciones).build();
    AdminOperacionRest conCuentas =
        new AdminOperacionRest(
            null,
            ClientesHttp.cliente(deDonaciones, DONACIONES, DonacionesApi.class),
            Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC),
            RestClient.create(),
            cuentas);
    api.expect(requestTo(DONACIONES + "/donaciones-independientes/" + D1))
        .andRespond(
            withSuccess(
                """
                {"id": "%1$s", "descripcion": "Arroz", "estadoActual": "Vencida",
                 "fechaRegistro": "2026-11-01T10:00:00", "cantidad": 25,
                 "entidadBeneficiariaId": "%2$s", "donacionOriginalId": "%3$s",
                 "historial": [
                   {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-03T09:00:00",
                    "estadoAnterior": "EnDeposito", "actor": "SISTEMA"},
                   {"estadoNuevo": "EnDeposito", "timestamp": "2026-11-04T09:00:00",
                    "estadoAnterior": "AsignacionRealizada"},
                   {"estadoNuevo": "AsignacionRealizada", "timestamp": "2026-11-05T09:00:00",
                    "estadoAnterior": "EnDeposito", "actor": "%4$s"},
                   {"estadoNuevo": "Vencida", "timestamp": "2026-11-06T10:15:00",
                    "estadoAnterior": "EnDeposito", "actor": "%4$s"}],
                 "items": []}"""
                    .formatted(D1, E1, O1, cuentaDeAna),
                MediaType.APPLICATION_JSON));
    api.expect(requestTo(DONACIONES + "/api/donaciones/" + O1))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "donante": {"persona": {"nombre": "Ana", "apellido": "Ejemplo"}},
                 "deposito": {"calle": "Av. Ejemplo", "altura": 1234}}"""
                    .formatted(O1),
                MediaType.APPLICATION_JSON));
    api.expect(requestTo(DONACIONES + "/api/entidades/" + E1))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "juridica": {"razonSocial": "Comedor Ejemplo"}}"""
                    .formatted(E1),
                MediaType.APPLICATION_JSON));

    AdminDonacionesVista.Donacion detalle = conCuentas.donacion(D1, "detalle");

    assertEquals(
        List.of(
            "1 de noviembre: llegó al depósito de Av. Ejemplo 1234.",
            "3 de noviembre: quedó asignada a Comedor Ejemplo.",
            "4 de noviembre: volvió al depósito.",
            "5 de noviembre: quedó asignada a Comedor Ejemplo. La aprobó Ana Pérez.",
            "6 de noviembre, 10:15: quedó vencida; la marcó Ana Pérez. Dejó de estar disponible"
                + " para asignar."),
        detalle.historial());
    // Cada cuenta se busca una sola vez por detalle.
    verify(cuentas, times(1)).cuenta(cuentaDeAna);
    api.verify();
  }

  @Test
  void marcarVencidaMandaElEstadoConElActorYElMotivo() {
    donaciones
        .expect(requestTo(DONACIONES + "/donaciones-independientes/" + D2 + "/estado"))
        .andExpect(method(PATCH))
        .andExpect(header("X-Actor", "Leo Ejemplo"))
        .andExpect(
            content().json("{\"estado\": \"VENCIDA\", \"justificacion\": \"Se rompió el envase\"}"))
        .andRespond(
            withSuccess(
                "{\"id\": \"" + D2 + "\", \"estadoActual\": \"Vencida\", \"cantidad\": 10}",
                MediaType.APPLICATION_JSON));

    adaptador.marcarVencida(D2, "Leo Ejemplo", " Se rompió el envase ");
    donaciones.verify();
  }

  private static final String DONANTES =
      """
      [
        {"idDonante": "%s", "activo": true,
         "persona": {"nombre": "Ana", "apellido": "Ejemplo", "tipoDocumento": "DNI",
                     "documento": "20.000.001"}},
        {"idDonante": "%s", "activo": true,
         "persona": {"razonSocial": "Distribuidora Ejemplo SA", "tipoDocumento": "CUIT",
                     "documento": "30-00000001-0"}},
        {"idDonante": "%s", "activo": false,
         "persona": {"nombre": "Pedro", "apellido": "Ejemplo"}}
      ]
      """
          .formatted(D1, D2, D3);

  @Test
  void registroBuscaEnDonacionesHasta10YTraeAlElegidoPorSuId() {
    donaciones
        .expect(
            requestTo(DONACIONES + "/api/donantes?q=jose&activo=true&orden=nombre&page=0&size=10"))
        .andRespond(withSuccess(DONANTES, MediaType.APPLICATION_JSON).header(Paginas.TOTAL, "37"));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/" + D2))
        .andRespond(
            withSuccess(
                """
                {"idDonante": "%s", "activo": true,
                 "persona": {"razonSocial": "Distribuidora Ejemplo SA", "tipoDocumento": "CUIT",
                             "documento": "30-00000001-0"}}"""
                    .formatted(D2),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "/api/donantes/" + D3))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    AdminDonacionesRegistroVista sinBuscar = adaptador.registro(null, null);
    assertEquals(List.of(), sinBuscar.donantes());
    assertEquals(List.of(), sinBuscar.encontrados());
    assertEquals(0, sinBuscar.coinciden());
    AdminDonacionesRegistroVista buscados = adaptador.registro(" jose ", null);
    assertEquals(3, buscados.encontrados().size());
    assertEquals(37, buscados.coinciden());
    assertEquals(
        List.of(
            new AdminDonacionesRegistroVista.Donante(
                D2, "Distribuidora Ejemplo SA", "CUIT 30-00000001-0")),
        adaptador.registro(null, D2).donantes());
    assertEquals(List.of(), adaptador.registro(null, D3).donantes());
    assertEquals(List.of(), adaptador.registro(null, "no-es-un-id").donantes());
    donaciones.verify();
  }

  @Test
  void registrarSubeLaFotoALaDireccionFirmadaYGuardaSuReferencia() {
    RestClient.Builder deSubidas = RestClient.builder();
    MockRestServiceServer almacenamiento = MockRestServiceServer.bindTo(deSubidas).build();
    RestClient.Builder deDonaciones = RestClient.builder();
    MockRestServiceServer api = MockRestServiceServer.bindTo(deDonaciones).build();
    AdminOperacionRest conFotos =
        new AdminOperacionRest(
            null,
            ClientesHttp.cliente(deDonaciones, DONACIONES, DonacionesApi.class),
            Clock.systemUTC(),
            deSubidas.build());
    api.expect(requestTo(DONACIONES + "/api/donaciones/fotos"))
        .andExpect(method(POST))
        .andExpect(content().json("{\"tipoContenido\": \"image/jpeg\"}"))
        .andRespond(
            withSuccess(
                """
                {"uploadUrl": "http://fotos/subida/1", "metodo": "PUT",
                 "tipoContenido": "image/jpeg", "referencia": "fotos/1.jpg"}""",
                MediaType.APPLICATION_JSON));
    almacenamiento
        .expect(requestTo("http://fotos/subida/1"))
        .andExpect(method(PUT))
        .andExpect(content().bytes(new byte[] {1, 2, 3}))
        .andRespond(withSuccess());
    api.expect(requestTo(DONACIONES + "/api/donaciones"))
        .andExpect(method(POST))
        .andExpect(
            content()
                .json(
                    """
                    {"idDonante": "%s", "descripcion": "Caja de alimentos",
                     "nombreDeposito": "Depósito Central", "fecha": "2026-11-10T00:00:00",
                     "items": [
                       {"descripcionBien": "Arroz", "fotoUrl": "fotos/1.jpg", "cantidad": 10,
                        "estadoBien": "NUEVO", "fechaVencimiento": "2027-01-31"},
                       {"descripcionBien": "Fideos", "fotoUrl": null, "cantidad": 5,
                        "estadoBien": "USADO", "fechaVencimiento": null}]}"""
                        .formatted(D1)))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    conFotos.registrarDonacion(
        new AdminOperacionPuerto.DonacionNueva(
            D1,
            "Caja de alimentos",
            List.of(
                new AdminOperacionPuerto.BienNuevo(
                    "Arroz",
                    10,
                    LocalDate.of(2027, 1, 31),
                    "NUEVO",
                    "image/jpeg",
                    new ByteArrayResource(new byte[] {1, 2, 3})),
                new AdminOperacionPuerto.BienNuevo("Fideos", 5, null, "USADO", null, null)),
            "Depósito Central",
            LocalDate.of(2026, 11, 10)));
    api.verify();
    almacenamiento.verify();
  }
}
