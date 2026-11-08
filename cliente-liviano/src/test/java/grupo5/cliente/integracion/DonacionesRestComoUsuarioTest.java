package grupo5.cliente.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import grupo5.cliente.puertos.DonacionesComoUsuario.DonacionDelDonante;
import grupo5.cliente.puertos.DonacionesComoUsuario.Juridica;
import grupo5.cliente.puertos.DonacionesComoUsuario.NecesidadDeEntidad;
import grupo5.cliente.vistas.EstadoEntrega;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DonacionesRestComoUsuarioTest {

  private static final String BASE = "http://donaciones";
  private static final UUID PERSONA = UUID.randomUUID();

  private final RestClient.Builder base = RestClient.builder().baseUrl(BASE);
  private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(base).build();
  private final DonacionesRestComoUsuario donaciones = new DonacionesRestComoUsuario(base.build());

  @Test
  void lasDonacionesDelDonanteSeFiltranPorSuPersonaYSeLeenConSuEstado() {
    String json =
        """
        [{"id":"%s","descripcion":"Arroz","estadoActual":"EN_TRASLADO",
          "fechaRegistro":"2026-10-14T10:00:00","cantidad":10,"historial":[],"items":[]}]"""
            .formatted(UUID.randomUUID());
    servidor
        .expect(requestTo(BASE + "/donaciones-independientes?donanteId=" + PERSONA))
        .andRespond(withSuccess(json, APPLICATION_JSON));

    DonacionDelDonante donacion = donaciones.donacionesDe(PERSONA).get(0);

    assertThat(donacion.descripcion()).isEqualTo("Arroz");
    assertThat(donacion.cantidad()).isEqualTo(10);
    assertThat(donacion.estado()).isEqualTo(EstadoEntrega.EN_CAMINO);
  }

  @Test
  void unEstadoSinCaminoNiResultadoEsPendiente() {
    var donacion = new DonacionDelDonante(PERSONA, "Arroz", "EN_DEPOSITO", null, 1);

    assertThat(donacion.estado()).isEqualTo(EstadoEntrega.PENDIENTE);
  }

  @Test
  void lasNecesidadesDeLaEntidadSeFiltranPorSuPersona() {
    String json =
        """
        [{"id":"%s","tipo":"EXTRAORDINARIA","idEntidad":"%s","cantidadNecesitada":40,
          "descripcion":"Lentejas","estaSatisfecha":false,"fechaFin":"2026-10-31"}]"""
            .formatted(UUID.randomUUID(), PERSONA);
    servidor
        .expect(requestTo(BASE + "/api/necesidades?entidadId=" + PERSONA))
        .andRespond(withSuccess(json, APPLICATION_JSON));

    NecesidadDeEntidad necesidad = donaciones.necesidadesDe(PERSONA).get(0);

    assertThat(necesidad.descripcion()).isEqualTo("Lentejas");
    assertThat(necesidad.cantidadNecesitada()).isEqualTo(40);
    assertThat(necesidad.fechaFin()).isEqualTo(LocalDate.of(2026, 10, 31));
  }

  @Test
  void lasJuridicasSeLeenEnUnaSolaLlamadaConSuRazonSocial() {
    String json =
        """
        [{"tipo":"JURIDICA","id":"%s","razonSocial":"Merendero Oeste","documento":"30-1",
          "mediosDeContacto":[],"representantes":[]}]"""
            .formatted(PERSONA);
    servidor
        .expect(requestTo(BASE + "/api/personas?tipo=JURIDICA"))
        .andRespond(withSuccess(json, APPLICATION_JSON));

    assertThat(donaciones.juridicas()).containsExactly(new Juridica(PERSONA, "Merendero Oeste"));
    servidor.verify();
  }

  @Test
  void registrarUnaEntidadEnviaLaJuridica() {
    servidor
        .expect(requestTo(BASE + "/api/entidades"))
        .andExpect(method(POST))
        .andExpect(jsonPath("$.juridicaId").value(PERSONA.toString()))
        .andRespond(withStatus(HttpStatus.CREATED));

    donaciones.registrarEntidad(PERSONA);

    servidor.verify();
  }
}
