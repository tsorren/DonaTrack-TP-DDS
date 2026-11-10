package grupo5.cliente.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.PersonaNueva.Direccion;
import grupo5.cliente.puertos.PersonaNueva.Humana;
import grupo5.cliente.puertos.PersonaNueva.Juridica;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class PersonasRestTest {

  private static final String BASE = "http://donaciones";
  private static final UUID PERSONA = UUID.randomUUID();

  private final RestClient.Builder base = RestClient.builder().baseUrl(BASE);
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(base).build();
  private final PersonasRest donaciones = new PersonasRest(base.build());

  @Test
  void crearUnaJuridicaEnviaLosDatosQueDonacionesPideYDevuelveElId() {
    Juridica entidad =
        new Juridica(
            "Merendero Oeste",
            "ONG",
            "Merendero",
            new Direccion("Calle 1", 1234, "1708", "Morón", "Buenos Aires", "Argentina"),
            new Humana("Rosa", "Gómez", "rosa@ejemplo.test"),
            "rosa@ejemplo.test");
    servidor
        .expect(requestTo(BASE + "/api/personas"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.tipo").value("JURIDICA"))
        .andExpect(jsonPath("$.razonSocial").value("Merendero Oeste"))
        .andExpect(jsonPath("$.tipoJuridico").value("ONG"))
        .andExpect(jsonPath("$.direccion.codigoPostal").value("1708"))
        .andExpect(jsonPath("$.direccion.pais").value("Argentina"))
        .andExpect(jsonPath("$.mediosDeContacto[0].tipo").value("CORREO"))
        .andExpect(jsonPath("$.mediosDeContacto[0].direccionCorreo").value("rosa@ejemplo.test"))
        .andExpect(jsonPath("$.representantes[0].tipo").value("HUMANA"))
        .andExpect(jsonPath("$.representantes[0].apellido").value("Gómez"))
        .andRespond(
            withSuccess("{\"id\":\"" + PERSONA + "\",\"tipo\":\"JURIDICA\"}", APPLICATION_JSON));

    assertThat(donaciones.crear(entidad)).isEqualTo(PERSONA);
  }

  @Test
  void registrarDonanteUsaElIdDeLaPersona() {
    servidor
        .expect(requestTo(BASE + "/api/donantes"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.idPersona").value(PERSONA.toString()))
        .andRespond(withStatus(HttpStatus.CREATED));

    donaciones.registrarDonante(PERSONA);

    servidor.verify();
  }

  @Test
  void eliminarDaDeBajaLaPersona() {
    servidor
        .expect(requestTo(BASE + "/api/personas/" + PERSONA))
        .andExpect(method(DELETE))
        .andRespond(withNoContent());

    donaciones.eliminar(PERSONA);

    servidor.verify();
  }

  @Test
  void unaRespuestaDeErrorLlegaAQuienLlama() {
    servidor.expect(requestTo(BASE + "/api/donantes")).andRespond(withStatus(HttpStatus.CONFLICT));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> donaciones.registrarDonante(PERSONA))
        .isInstanceOf(HttpClientErrorException.class);
  }
}
