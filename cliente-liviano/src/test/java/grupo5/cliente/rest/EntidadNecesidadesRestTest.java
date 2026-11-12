package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.EntidadNecesidadesVista;
import grupo5.cliente.vistas.EntidadNecesidadesVista.Necesidad;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Necesidades de la entidad contra respuestas de Donaciones. */
class EntidadNecesidadesRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String LENTEJAS = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String SILLAS = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String FRAZADAS = "6f1c2c1e-0000-4000-8000-0000000000b3";
  private static final String LEGUMBRES = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String MESAS = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String API = "http://donaciones/api/";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  // "Hoy" es el 8 de noviembre de 2026 en Buenos Aires.
  private final EntidadNecesidadesRest adaptador =
      new EntidadNecesidadesRest(
          ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class),
          Clock.fixed(Instant.parse("2026-11-08T21:30:00Z"), ZoneOffset.UTC));

  @Test
  void lasVigentesConSusEntregasQuienDonoYSusPeriodos() {
    servidor
        .expect(requestTo(API + "necesidades?entidadId=" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "tipo": "RECURRENTE", "idSubcategoria": "%s", "cantidadNecesitada": 40,
                  "descripcion": "Para el almuerzo.", "estado": "VIGENTE", "version": 3,
                  "periodicidad": "MENSUAL", "unidad": "KILOGRAMO",
                  "ultimoCambio": {"cantidadAnterior": 30, "fecha": "2026-11-08"}},
                 {"id": "%s", "tipo": "EXTRAORDINARIA", "idSubcategoria": "%s", "cantidadNecesitada": 6,
                  "descripcion": "Para el comedor.", "estado": "VIGENTE", "version": 1,
                  "fechaLimite": "2026-11-20", "unidad": "UNIDADES"},
                 {"id": "%s", "tipo": "EXTRAORDINARIA", "cantidadNecesitada": 20,
                  "descripcion": "Frazadas", "estado": "DADA_DE_BAJA", "unidad": "UNIDADES"}]
                """
                    .formatted(LENTEJAS, LEGUMBRES, SILLAS, MESAS, FRAZADAS),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(API + "subcategorias"))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "nombre": "Lentejas secas", "categoria": {"nombre": "Alimentos"}, "unidad": "KILOGRAMO"},
                 {"id": "%s", "nombre": "Mesas y sillas", "categoria": {"nombre": "Muebles"}, "unidad": "UNIDADES"}]
                """
                    .formatted(LEGUMBRES, MESAS),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(
            requestTo("http://donaciones/donaciones-independientes?size=100&entidadId=" + ENTIDAD))
        .andRespond(
            withSuccess(
                """
                [{"id": "6f1c2c1e-0000-4000-8000-000000000001", "descripcion": "Lentejas",
                  "estadoActual": "Entregada", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "Entregada", "timestamp": "2026-11-05T17:00:00"}],
                  "cantidad": 10, "necesidadId": "%s",
                  "quienDono": {"tipo": "JURIDICA", "nombreVisible": "Distribuidora Ejemplo SA"}},
                 {"id": "6f1c2c1e-0000-4000-8000-000000000002", "descripcion": "Lentejas",
                  "estadoActual": "EnTraslado", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "EnTraslado", "timestamp": "2026-11-08T09:15:00"}],
                  "cantidad": 5, "necesidadId": "%s"},
                 {"id": "6f1c2c1e-0000-4000-8000-000000000003", "descripcion": "Mesa",
                  "estadoActual": "EntregaFallida", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "EntregaFallida", "timestamp": "2026-11-07T16:10:00"}],
                  "cantidad": 1, "necesidadId": "%s"},
                 {"id": "6f1c2c1e-0000-4000-8000-000000000004", "descripcion": "Silla",
                  "estadoActual": "EN_PUERTA", "fechaRegistro": "2026-11-01T10:00:00",
                  "historial": [{"estadoNuevo": "EN_PUERTA", "timestamp": "2026-11-08T18:20:00"}],
                  "cantidad": 2, "necesidadId": "%s"}]
                """
                    .formatted(LENTEJAS, LENTEJAS, SILLAS, SILLAS),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(API + "necesidades/" + LENTEJAS + "/periodos"))
        .andRespond(
            withSuccess(
                """
                [{"inicio": "2026-10-01", "fin": "2026-10-31", "objetivo": 30, "recibido": 12,
                  "resultado": "INCOMPLETO"},
                 {"inicio": "2026-11-01", "fin": "2026-11-30", "objetivo": 40, "recibido": 10,
                  "resultado": "EN_CURSO"}]
                """,
                MediaType.APPLICATION_JSON));

    EntidadNecesidadesVista vista = adaptador.necesidades(ENTIDAD, "");

    servidor.verify();
    assertEquals("8 de noviembre de 2026", vista.al());
    assertEquals("2026-11-08", vista.hoy());
    assertEquals(2, vista.todas().size());
    Necesidad lentejas = vista.todas().getFirst();
    assertEquals("necesidad-" + LENTEJAS, lentejas.clave());
    assertEquals(3L, lentejas.version());
    assertEquals("Lentejas secas", lentejas.nombre());
    assertEquals("Alimentos", lentejas.rubro());
    assertEquals("Noviembre, hasta el 30", lentejas.plazo());
    assertEquals("faltan 22 días", lentejas.faltan());
    assertEquals("Cada mes", lentejas.repite());
    assertEquals(10, lentejas.valor());
    assertEquals(40, lentejas.max());
    assertEquals("Hoy pasó de 30 a 40 kg.", lentejas.cambio());
    assertEquals("Cambiada hoy", lentejas.cambiada());
    assertEquals("Para el almuerzo.", lentejas.motivo());
    assertEquals(1, lentejas.camino());
    assertEquals("en-camino", lentejas.entregas().getFirst().tono());
    assertEquals(
        "Salió del depósito el 8 de noviembre a las 9:15.", lentejas.entregas().getFirst().texto());
    assertEquals("Distribuidora Ejemplo SA", lentejas.donaron().getFirst().quien());
    assertEquals("empresa", lentejas.donaron().getFirst().tipo());
    assertEquals("5 de noviembre", lentejas.donaron().getFirst().cuando());
    assertEquals("Desde octubre de 2026", lentejas.desde());
    assertEquals(
        List.of("curso", "parte"), lentejas.periodos().stream().map(p -> p.tono()).toList());

    Necesidad sillas = vista.todas().get(1);
    assertEquals("cat-muebles", sillas.categoria());
    assertEquals("Hasta el 20 de noviembre", sillas.plazo());
    assertNull(sillas.valor());
    assertEquals(6, sillas.max());
    assertEquals(1, sillas.problema());
    assertEquals("necesidad-" + SILLAS, vista.conProblema());
    // HB-22: la que llegó a la puerta lleva su sello y el acta de su donación
    assertEquals(1, sillas.puerta());
    assertEquals(0, sillas.camino());
    var enLaPuerta = sillas.entregas().get(1);
    assertEquals("puerta", enLaPuerta.tono());
    assertEquals(
        "Llegó a tu puerta el 8 de noviembre a las 18:20. Espera tu confirmación.",
        enLaPuerta.texto());
    assertEquals("6f1c2c1e-0000-4000-8000-000000000004", enLaPuerta.confirmar());
    assertEquals("6f1c2c1e-0000-4000-8000-000000000004", vista.aConfirmar());

    assertEquals("Frazadas · 20 unidades", vista.terminadas().getFirst().titulo());
    assertEquals("La diste de baja.", vista.terminadas().getFirst().texto());
    assertEquals(
        List.of("Alimentos", "Muebles"), vista.catalogo().stream().map(g -> g.nombre()).toList());
    assertEquals(LEGUMBRES, vista.catalogo().getFirst().opciones().getFirst().id());
    assertEquals("Lentejas secas (kg)", vista.catalogo().getFirst().opciones().getFirst().texto());
  }

  @Test
  void publicarCambiarConLaVersionYDarDeBaja() {
    servidor
        .expect(requestTo(API + "necesidades"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
                    {"tipo": "EXTRAORDINARIA", "idEntidad": "%s", "idSubcategoria": "%s",
                     "cantidadNecesitada": 6, "descripcion": "Para el comedor.",
                     "fechaLimite": "2026-11-20"}
                    """
                        .formatted(ENTIDAD, MESAS)))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(API + "necesidades/" + LENTEJAS))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                {"id": "%s", "tipo": "RECURRENTE", "idEntidad": "%s", "idSubcategoria": "%s",
                 "cantidadNecesitada": 40, "descripcion": "Para el almuerzo.", "version": 4,
                 "periodicidad": "MENSUAL"}
                """
                    .formatted(LENTEJAS, ENTIDAD, LEGUMBRES),
                MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(API + "necesidades/" + LENTEJAS))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(
            content()
                .json(
                    """
                    {"tipo": "RECURRENTE", "cantidadNecesitada": 45,
                     "descripcion": "Para el almuerzo y la cena.", "version": 3,
                     "periodicidad": "MENSUAL"}
                    """))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    servidor
        .expect(requestTo(API + "necesidades/" + SILLAS))
        .andExpect(method(HttpMethod.DELETE))
        .andRespond(withSuccess());

    adaptador.publicar(
        ENTIDAD, UUID.fromString(MESAS), 6, null, LocalDate.of(2026, 11, 20), "Para el comedor.");
    adaptador.cambiar(UUID.fromString(LENTEJAS), 3, 45, "Para el almuerzo y la cena.");
    adaptador.darDeBaja(UUID.fromString(SILLAS));

    servidor.verify();
  }
}
