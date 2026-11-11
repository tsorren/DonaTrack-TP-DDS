package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.EntidadInicioVista;
import grupo5.cliente.vistas.EntidadInicioVista.Necesidad;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST del Inicio de la entidad contra Donaciones. */
class EntidadInicioRestTest {

  private static final UUID ENTIDAD = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000e1");
  private static final String RECURRENTE = "6f1c2c1e-0000-4000-8000-0000000000b1";
  private static final String EXTRAORDINARIA = "6f1c2c1e-0000-4000-8000-0000000000b2";
  private static final String LENTEJAS = "6f1c2c1e-0000-4000-8000-0000000000c1";
  private static final String SILLAS = "6f1c2c1e-0000-4000-8000-0000000000c2";
  private static final String DONACIONES = "http://donaciones/api/";

  @Test
  void lasVigentesConLoRecibidoDelPeriodo() {
    RestClient.Builder deDonaciones = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(deDonaciones).build();
    donaciones
        .expect(requestTo(DONACIONES + "necesidades?entidadId=" + ENTIDAD + "&estado=VIGENTE"))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "tipo": "RECURRENTE", "idSubcategoria": "%s", "cantidadNecesitada": 40,
                  "descripcion": "Lentejas secas", "unidad": "KILOGRAMO"},
                 {"id": "%s", "tipo": "EXTRAORDINARIA", "idSubcategoria": "%s", "cantidadNecesitada": 6,
                  "descripcion": "Sillas", "unidad": "UNIDADES", "fechaLimite": "2026-11-20"}]
                """
                    .formatted(RECURRENTE, LENTEJAS, EXTRAORDINARIA, SILLAS),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "subcategorias"))
        .andRespond(
            withSuccess(
                """
                [{"id": "%s", "nombre": "Legumbres", "categoria": {"nombre": "Alimentos"}},
                 {"id": "%s", "nombre": "Sillas", "categoria": {"nombre": "Muebles"}}]
                """
                    .formatted(LENTEJAS, SILLAS),
                MediaType.APPLICATION_JSON));
    donaciones
        .expect(requestTo(DONACIONES + "necesidades/" + RECURRENTE + "/periodos"))
        .andRespond(
            withSuccess(
                """
                [{"inicio": "2026-10-01", "fin": "2026-10-31", "objetivo": 40, "asignado": 40,
                  "recibido": 40, "resultado": "CUMPLIDO"},
                 {"inicio": "2026-11-01", "fin": "2026-11-30", "objetivo": 40, "asignado": 20,
                  "recibido": 10, "resultado": "EN_CURSO"}]
                """,
                MediaType.APPLICATION_JSON));
    // "Ahora" es el jueves 12 de noviembre de 2026, 18:30 en Buenos Aires.
    EntidadInicioRest adaptador =
        new EntidadInicioRest(
            ClientesHttp.cliente(deDonaciones, "http://donaciones", DonacionesApi.class),
            Clock.fixed(Instant.parse("2026-11-12T21:30:00Z"), ZoneOffset.UTC));

    EntidadInicioVista inicio = adaptador.inicio(ENTIDAD);

    donaciones.verify();
    assertEquals("jueves 12 de noviembre de 2026", inicio.fecha());
    assertEquals("12 de noviembre, 18:30", inicio.al());
    Necesidad lentejas = inicio.necesidades().getFirst();
    assertEquals(
        new Necesidad(
            "Lentejas secas", "cat-alimentos", "10 de 40 kg", 10, 40, "Noviembre, hasta el 30"),
        lentejas);
    Necesidad sillas = inicio.necesidades().get(1);
    assertEquals(
        new Necesidad("Sillas", "cat-muebles", null, null, 6, "Hasta el 20 de noviembre"), sillas);
  }

  @Test
  void sinNecesidadesNoPideLasSubcategorias() {
    RestClient.Builder deDonaciones = RestClient.builder();
    MockRestServiceServer donaciones = MockRestServiceServer.bindTo(deDonaciones).build();
    donaciones
        .expect(requestTo(DONACIONES + "necesidades?entidadId=" + ENTIDAD + "&estado=VIGENTE"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    EntidadInicioRest adaptador =
        new EntidadInicioRest(
            ClientesHttp.cliente(deDonaciones, "http://donaciones", DonacionesApi.class),
            Clock.systemUTC());

    EntidadInicioVista inicio = adaptador.inicio(ENTIDAD);

    donaciones.verify();
    assertEquals(0, inicio.necesidades().size());
  }
}
