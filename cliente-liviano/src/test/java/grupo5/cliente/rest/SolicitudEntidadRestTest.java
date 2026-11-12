package grupo5.cliente.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.SolicitudEntidadPuerto.DatosRechazados;
import grupo5.cliente.puertos.SolicitudEntidadPuerto.YaAprobada;
import grupo5.cliente.vistas.SolicitudEntidadVista;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** El adaptador REST de la solicitud que espera la aprobación (R11-41) contra Donaciones. */
class SolicitudEntidadRestTest {

  private static final UUID PERSONA = UUID.fromString("6f1c2c1e-0000-4000-8000-0000000000a1");
  private static final String RUTA =
      "http://donaciones/api/personas/" + PERSONA + "/solicitud-entidad";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
  private final SolicitudEntidadRest adaptador =
      new SolicitudEntidadRest(
          ClientesHttp.cliente(builder, "http://donaciones", DonacionesApi.class));

  private static final SolicitudEntidadVista CAMBIOS =
      new SolicitudEntidadVista(
          "Merendero Ejemplo Este",
          "Comedor",
          "institucion",
          "Calle Nueva",
          99,
          "2000",
          "Localidad Nueva",
          "Provincia Nueva",
          "Rosa María Ejemplar",
          "rosa@merendero-ejemplo.test");

  @Test
  void leeLaSolicitudConElTipoDelRegistroYElNombreCompleto() {
    servidor
        .expect(requestTo(RUTA))
        .andRespond(
            withSuccess(
                """
                {"razonSocial": "Merendero Ejemplo Oeste", "rubro": "Merendero", "tipoJuridico": "ONG",
                 "direccion": {"calle": "Calle Ejemplo", "altura": 1234, "codigoPostal": "1000",
                               "localidad": "Localidad Ejemplo", "provincia": "Provincia Ejemplo"},
                 "representante": {"nombre": "Rosa", "apellido": "Ejemplo"},
                 "correo": "rosa@merendero-ejemplo.test"}
                """,
                MediaType.APPLICATION_JSON));

    SolicitudEntidadVista solicitud = adaptador.leer(PERSONA);

    servidor.verify();
    assertEquals("Merendero Ejemplo Oeste", solicitud.entidad());
    assertEquals("asociacion", solicitud.tipo());
    assertEquals("Asociación civil u ONG", solicitud.tipoTexto());
    assertEquals("Rosa Ejemplo", solicitud.representante());
    assertEquals(
        "Calle Ejemplo 1234, Localidad Ejemplo, Provincia Ejemplo · CP 1000",
        solicitud.direccion());
    assertEquals("rosa@merendero-ejemplo.test", solicitud.correo());
  }

  @Test
  void leer_conUn409_esQueYaLaAprobaron() {
    servidor.expect(requestTo(RUTA)).andRespond(withStatus(HttpStatus.CONFLICT));

    assertThrows(YaAprobada.class, () -> adaptador.leer(PERSONA));
  }

  @Test
  void corregir_mandaLosCamposDeDonacionesSinElCorreo() {
    servidor
        .expect(requestTo(RUTA))
        .andExpect(method(PUT))
        .andExpect(
            content()
                .json(
                    """
                    {"razonSocial": "Merendero Ejemplo Este", "rubro": "Comedor",
                     "tipoJuridico": "INSTITUCION",
                     "direccion": {"calle": "Calle Nueva", "altura": 99, "codigoPostal": "2000",
                                   "localidad": "Localidad Nueva", "provincia": "Provincia Nueva"},
                     "representante": {"nombre": "Rosa María", "apellido": "Ejemplar"},
                     "correo": null}
                    """,
                    true))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    adaptador.corregir(PERSONA, CAMBIOS);

    servidor.verify();
  }

  @Test
  void corregir_conUn400_diceQueCamposDelFormularioRevisar() {
    servidor
        .expect(requestTo(RUTA))
        .andRespond(
            withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    """
                    {"code": "ERR-CSR-003", "errors": [
                      {"field": "direccion.calle"}, {"field": "razonSocial"}, {"field": "otro"}]}
                    """));

    DatosRechazados rechazo =
        assertThrows(DatosRechazados.class, () -> adaptador.corregir(PERSONA, CAMBIOS));
    assertEquals(List.of("calle", "entidad"), rechazo.campos());
  }

  @Test
  void corregir_conUn409_esQueYaLaAprobaron() {
    servidor.expect(requestTo(RUTA)).andRespond(withStatus(HttpStatus.CONFLICT));

    assertThrows(YaAprobada.class, () -> adaptador.corregir(PERSONA, CAMBIOS));
  }
}
