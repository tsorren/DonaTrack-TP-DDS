package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.DonanteMisionesVista;
import grupo5.cliente.vistas.DonanteMisionesVista.Categoria;
import grupo5.cliente.vistas.DonanteMisionesVista.Mision;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Misiones e insignias contra respuestas de ejemplo de Incentivos y Donaciones. */
class DonanteMisionesRestTest {

  private static final UUID DONANTE = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000aa");
  private static final String DONACIONES = "http://donaciones";
  private static final String INCENTIVOS = "http://incentivos";
  private static final String BASE = INCENTIVOS + "/api/incentivos/donantes/" + DONANTE;

  private MockRestServiceServer apis;
  private DonanteMisionesRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    apis = MockRestServiceServer.bindTo(builder).build();
    Clock hoy = Clock.fixed(Instant.parse("2026-11-10T17:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new DonanteMisionesRest(
            ClientesHttp.cliente(builder, INCENTIVOS, IncentivosApi.class),
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class),
            hoy);
  }

  private void donanteDe(String tipo) {
    apis.expect(requestTo(DONACIONES + "/api/donantes/" + DONANTE))
        .andRespond(
            withSuccess(
                "{\"idDonante\": \"%s\", \"persona\": {\"tipo\": \"%s\"}}".formatted(DONANTE, tipo),
                MediaType.APPLICATION_JSON));
  }

  private static String mision(
      String nombre, String categoria, int progreso, int objetivo, String fecha) {
    return """
        {"nombre": "Racha", "descripcion": "Realiza una donacion durante N meses consecutivos",
         "categoria": "%s", "progresoActual": %d, "objetivo": %d, "completada": %s,
         "fechaCompletada": %s, "insignia": {"nombre": "%s", "descripcion": "%s pide algo"}}
        """
        .formatted(
            categoria,
            progreso,
            objetivo,
            fecha != null,
            fecha == null ? "null" : "\"" + fecha + "\"",
            nombre,
            nombre);
  }

  @Test
  void laPrimeraSinCompletarEstaEnCursoLaSiguienteEsperaYLasDeMasAdelanteNoSeAbren() {
    donanteDe("HUMANA");
    apis.expect(requestTo(BASE))
        .andRespond(
            withSuccess(
                """
                {"donanteId": "%s", "nombre": "Lucía Pérez", "categoria": "SOSTENEDOR",
                 "fechaRegistro": "2026-06-01", "misionesCompletadas": 2}
                """
                    .formatted(DONANTE),
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/misiones"))
        .andRespond(
            withSuccess(
                "["
                    + String.join(
                        ",",
                        mision("Racha Inicial", "COLABORADOR", 3, 3, "2026-08-03"),
                        mision("Explorador", "COLABORADOR", 3, 3, "2026-09-02"),
                        mision("Gran Aporte", "SOSTENEDOR", 12, 50, null),
                        mision("Constancia", "SOSTENEDOR", 0, 6, null),
                        mision("Impacto Real", "TRANSFORMADOR", 0, 10, null))
                    + "]",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/ascensos"))
        .andRespond(
            withSuccess(
                """
                [{"categoriaAnterior": "COLABORADOR", "categoriaNueva": "SOSTENEDOR",
                  "fecha": "2026-09-02"}]
                """,
                MediaType.APPLICATION_JSON));

    DonanteMisionesVista vista = adaptador.misiones(DONANTE);

    apis.verify();
    assertTrue(vista.persona());
    assertFalse(vista.nueva());
    assertEquals("Lucía", vista.nombre());
    assertTrue(vista.nota().startsWith("Todavía no mostramos"));
    assertEquals("Sostenedor", vista.categoria());
    assertEquals("2 de septiembre de 2026", vista.desde());
    assertEquals(2, vista.completadas());
    assertEquals("10 de noviembre de 2026, 14:30", vista.actualizado());
    List<Mision> m = vista.misiones();
    assertEquals(
        List.of("completa", "completa", "curso", "espera", "cerrada"),
        m.stream().map(Mision::estado).toList());
    assertEquals("3 de agosto de 2026", m.getFirst().fecha());
    assertEquals("Colaborador", m.getFirst().categoria());
    assertEquals("Gran Aporte", m.get(2).nombre());
    assertEquals("Gran Aporte pide algo", m.get(2).pide());
    assertEquals("12 de 50", m.get(2).cuenta());
    assertEquals(12, m.get(2).progreso());
    assertEquals(50, m.get(2).objetivo());
    assertNull(m.get(2).nota());
    assertEquals("Gran Aporte", m.get(3).espera());
    assertEquals(
        List.of(
            new Categoria("Colaborador", "completa", null, null),
            new Categoria("Sostenedor", "tu categoría", "Transformador", null),
            new Categoria("Transformador", "se abre después", null, "Sostenedor")),
        vista.categorias());
    assertNull(m.getFirst().historia());
    assertEquals(
        "Te sumaste a DonaTrack. Esta planta llega con tu cuenta.", vista.bienvenida().hizo());
    assertEquals("1 de junio de 2026", vista.bienvenida().hitos().getFirst().fecha());
    assertNull(vista.bienvenida().donacion());
  }

  @Test
  void sinMisionesCompletasEsNuevaYDesdeCuandoEsLaFechaDeAlta() {
    donanteDe("HUMANA");
    apis.expect(requestTo(BASE))
        .andRespond(
            withSuccess(
                "{\"nombre\": \"  \", \"categoria\": \"COLABORADOR\","
                    + " \"fechaRegistro\": \"2026-11-01\"}",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/misiones"))
        .andRespond(
            withSuccess(
                "["
                    + String.join(
                        ",",
                        mision("Racha Inicial", "COLABORADOR", 0, 3, null),
                        mision("Explorador", "COLABORADOR", 0, 3, null),
                        mision("Gran Aporte", "SOSTENEDOR", 0, 50, null))
                    + "]",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/ascensos"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteMisionesVista vista = adaptador.misiones(DONANTE);

    assertTrue(vista.nueva());
    assertNull(vista.nombre());
    assertEquals("1 de noviembre de 2026", vista.desde());
    assertEquals(
        List.of("curso", "espera", "cerrada"),
        vista.misiones().stream().map(Mision::estado).toList());
    assertEquals("0 de 3", vista.misiones().getFirst().cuenta());
    assertEquals("Racha Inicial", vista.misiones().get(1).espera());
    assertEquals("tu categoría", vista.categorias().getFirst().estado());
  }

  @Test
  void conTodasCompletasTodasLasCategoriasEstanCompletas() {
    donanteDe("HUMANA");
    apis.expect(requestTo(BASE))
        .andRespond(
            withSuccess(
                "{\"nombre\": \"Ana\", \"categoria\": \"TRANSFORMADOR\"}",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/misiones"))
        .andRespond(
            withSuccess(
                "[" + mision("Racha Inicial", "COLABORADOR", 3, 3, "2026-08-03") + "]",
                MediaType.APPLICATION_JSON));
    apis.expect(requestTo(BASE + "/ascensos"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    DonanteMisionesVista vista = adaptador.misiones(DONANTE);

    assertEquals(List.of("completa"), vista.categorias().stream().map(Categoria::estado).toList());
    assertNull(vista.desde());
  }

  @Test
  void unaEmpresaNoTieneMisionesYNoSeLeePedeANadaMasDeIncentivos() {
    donanteDe("JURIDICA");

    DonanteMisionesVista vista = adaptador.misiones(DONANTE);

    apis.verify();
    assertFalse(vista.persona());
    assertTrue(vista.misiones().isEmpty());
  }
}
