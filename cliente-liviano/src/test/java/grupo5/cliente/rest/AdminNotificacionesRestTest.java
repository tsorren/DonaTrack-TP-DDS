package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.AdminNotificacionesVista;
import grupo5.cliente.vistas.AdminNotificacionesVista.Aviso;
import grupo5.cliente.vistas.ConsultaBandeja;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * La bandeja real de la Administración (R11-30, spec R11-p2-campanita C17): la de {@code
 * cliente.notificaciones.administracion}, de a 20, con las categorías del admin y marcar contra
 * Notificaciones.
 */
class AdminNotificacionesRestTest {

  private static final UUID ADMINISTRACION = UUID.fromString(AvisosRest.ADMINISTRACION);
  private static final String IMPORTACION = "6f1c2c1e-0000-4000-8000-0000000000a1";
  private static final String CORTADA = "6f1c2c1e-0000-4000-8000-0000000000a2";
  private static final String CONTACTO = "6f1c2c1e-0000-4000-8000-0000000000a3";
  private static final String ARCHIVO = "7d1a2b3c-0000-4000-8000-000000000001";
  private static final String BASE = "http://notificaciones/api/notificaciones/";
  private static final String PRIMERA = BASE + "persona/" + ADMINISTRACION + "?page=0&size=20";
  private static final String CONTEO = BASE + "persona/" + ADMINISTRACION + "/no-leidas/conteo";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  // "Ahora" es el 12 de noviembre de 2026, 18:45 en Buenos Aires.
  private final AdminNotificacionesRest adaptador =
      new AdminNotificacionesRest(
          ClientesHttp.cliente(builder, "http://notificaciones", NotificacionesApi.class),
          ADMINISTRACION,
          Clock.fixed(Instant.parse("2026-11-12T21:45:10Z"), ZoneOffset.UTC));

  private static HttpHeaders total(long total) {
    HttpHeaders cabeceras = new HttpHeaders();
    cabeceras.add("X-Total-Count", String.valueOf(total));
    return cabeceras;
  }

  private void conteo(long sinLeer) {
    servidor
        .expect(requestTo(CONTEO))
        .andRespond(withSuccess("{\"noLeidas\": " + sinLeer + "}", MediaType.APPLICATION_JSON));
  }

  @Test
  void pideLaBandejaDeLaAdministracionDeA20ConLasCategoriasDelAdmin() {
    servidor
        .expect(requestTo(PRIMERA))
        .andRespond(
            withSuccess(
                    """
                    [{"id": "%s", "mensaje": "La importación de donantes terminó.",
                      "fechaCreacion": "2026-11-12T18:40:30", "fechaLectura": null,
                      "tipoEvento": "IMPORTACION_TERMINADA", "referenciaId": "%s"},
                     {"id": "%s", "mensaje": "La importación de donantes se cortó.",
                      "fechaCreacion": "2026-11-11T10:00:00", "fechaLectura": null,
                      "tipoEvento": "IMPORTACION_CORTADA", "referenciaId": "%s"},
                     {"id": "%s", "mensaje": "Nuevo mensaje de contacto Nº 12, sobre el sitio.",
                      "fechaCreacion": "2026-11-10T09:00:00", "fechaLectura": "2026-11-10T10:00:00",
                      "tipoEvento": "MENSAJE_CONTACTO"},
                     {"id": null, "mensaje": "Sin id", "fechaCreacion": "2026-11-01T10:00:00"}]
                    """
                        .formatted(IMPORTACION, ARCHIVO, CORTADA, ARCHIVO, CONTACTO),
                    MediaType.APPLICATION_JSON)
                .headers(total(45)));
    conteo(2);

    AdminNotificacionesVista vista = adaptador.bandeja(ConsultaBandeja.primera());

    servidor.verify();
    assertEquals("2026-11-12T18:45", vista.ahora());
    assertEquals("12 de noviembre de 2026, 18:45", vista.actualizado());
    assertEquals(
        List.of("", "importaciones", "contacto", "otras"),
        vista.temas().stream().map(t -> t.valor()).toList());
    assertEquals(45, vista.pagina().total());
    assertEquals(3, vista.pagina().paginas());
    assertEquals(2, vista.sinLeer());
    assertEquals(3, vista.pagina().filas().size());

    Aviso terminada = vista.pagina().filas().get(0);
    assertEquals("importaciones", terminada.tema());
    assertEquals("Terminó una importación", terminada.titulo());
    assertEquals("2026-11-12T18:40", terminada.fecha());
    assertFalse(terminada.leida());
    assertEquals("/admin-donantes-importado?archivo=" + ARCHIVO, terminada.destino());
    assertEquals("Ver el resultado", terminada.vinculo());

    Aviso cortada = vista.pagina().filas().get(1);
    assertEquals("Una importación se cortó", cortada.titulo());
    assertEquals("Ver qué pasó", cortada.vinculo());

    Aviso contacto = vista.pagina().filas().get(2);
    assertEquals("contacto", contacto.tema());
    assertEquals("Nuevo mensaje de contacto", contacto.titulo());
    assertTrue(contacto.leida());
    // No hay pantalla de mensajes de contacto: el aviso no tiene vínculo.
    assertNull(contacto.destino());
    assertNull(contacto.vinculo());
    assertNull(contacto.boton());
  }

  @Test
  void losFiltrosVanAlServicioYOtrasExcluyeImportacionesYContacto() {
    servidor
        .expect(
            requestTo(
                BASE
                    + "persona/"
                    + ADMINISTRACION
                    + "?leida=false&q=donantes&tipo=IMPORTACION_TERMINADA"
                    + "&tipo=IMPORTACION_CORTADA&orden=antiguas&page=1&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(25)));
    conteo(2);
    servidor
        .expect(
            requestTo(
                BASE
                    + "persona/"
                    + ADMINISTRACION
                    + "?excluirTipo=IMPORTACION_TERMINADA&excluirTipo=IMPORTACION_CORTADA"
                    + "&excluirTipo=MENSAJE_CONTACTO&page=0&size=20"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON).headers(total(0)));
    servidor.expect(requestTo(CONTEO)).andRespond(withServerError());

    adaptador.bandeja(new ConsultaBandeja(true, "donantes", "importaciones", true, 2));
    AdminNotificacionesVista otras =
        adaptador.bandeja(new ConsultaBandeja(false, "", "otras", false, 1));

    assertDoesNotThrow(() -> servidor.verify());
    // Sin conteo, la cifra queda en cero: la pantalla no se rompe.
    assertEquals(0, otras.sinLeer());
  }

  @Test
  void sinTipoOConUnTipoQueNoEsDelAdminVaAOtrasNovedades() {
    assertEquals("otras", AdminNotificacionesRest.tema(null));
    assertEquals("otras", AdminNotificacionesRest.tema("DONACION_ASIGNADA"));
    assertEquals("contacto", AdminNotificacionesRest.tema("MENSAJE_CONTACTO"));
  }

  @Test
  void marcarLasElegidasSinNingunaTodasYDejarSinLeerSalteandoLasAjenas() {
    servidor
        .expect(requestTo(BASE + IMPORTACION + "/leida?leida=true"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(BASE + CORTADA + "/leida?leida=true"))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
    servidor
        .expect(requestTo(BASE + "persona/" + ADMINISTRACION + "/marcar-todas-leidas"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess());
    servidor
        .expect(requestTo(BASE + CONTACTO + "/leida?leida=false"))
        .andExpect(method(PATCH))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.marcarLeidas(List.of(UUID.fromString(IMPORTACION), UUID.fromString(CORTADA)));
    adaptador.marcarLeidas(List.of());
    adaptador.dejarSinLeer(UUID.fromString(CONTACTO));

    servidor.verify();
  }
}
