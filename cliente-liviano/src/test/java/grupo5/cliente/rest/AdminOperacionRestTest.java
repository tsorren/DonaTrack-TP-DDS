package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.vistas.AdminCamionesVista;
import grupo5.cliente.vistas.AdminCamionesVista.Camion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  private MockRestServiceServer logistica;
  private AdminOperacionRest adaptador;

  @BeforeEach
  void armar() {
    RestClient.Builder builder = RestClient.builder();
    logistica = MockRestServiceServer.bindTo(builder).build();
    Clock reloj = Clock.fixed(Instant.parse("2026-11-10T21:30:00Z"), ZoneOffset.UTC);
    adaptador =
        new AdminOperacionRest(
            ClientesHttp.cliente(builder, "http://logistica", LogisticaApi.class), reloj);
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
}
