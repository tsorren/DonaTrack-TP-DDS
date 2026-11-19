package grupo5.donaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El healthcheck de Swarm consulta liveness sin token, así que tiene que responder aunque la
 * validación del token esté encendida. El resto de la salud sigue pidiendo token.
 */
@SpringBootTest(properties = "donatrack.seguridad.habilitada=true")
@AutoConfigureMockMvc
class SaludDonacionesContextTests {

  // Con la seguridad encendida hace falta un decodificador de tokens. Estos pedidos no llevan
  // token.
  @MockitoBean private JwtDecoder tokens;

  @Autowired private MockMvc mvc;

  @Test
  void livenessRespondeUpSinToken() throws Exception {
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void readinessRespondeUpSinToken() throws Exception {
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void laSaludGeneralSigueRespondiendoSinToken() throws Exception {
    // UP (200) o DOWN (503) según haya RabbitMQ: lo que importa es que no pida token (401)
    mvc.perform(get("/actuator/health"))
        .andExpect(resultado -> assertThat(resultado.getResponse().getStatus()).isIn(200, 503));
  }

  @Test
  void losComponentesDeLaSaludPidenToken() throws Exception {
    mvc.perform(get("/actuator/health/db")).andExpect(status().isUnauthorized());
  }
}
