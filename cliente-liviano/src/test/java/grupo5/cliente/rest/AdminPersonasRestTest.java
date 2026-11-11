package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.AdminDonantesVista;
import grupo5.cliente.vistas.AdminDonantesVista.Importacion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST del admin contra respuestas de ejemplo de Donaciones (plan E6, §F1). */
class AdminPersonasRestTest {

  private static final String DONACIONES = "http://donaciones";
  private static final UUID ARCHIVO = UUID.fromString("00000000-0000-4000-8000-0000000000aa");

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

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    donaciones = MockRestServiceServer.bindTo(builder).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new AdminPersonasRest(
            ClientesHttp.cliente(builder, DONACIONES, DonacionesApi.class), reloj);
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
