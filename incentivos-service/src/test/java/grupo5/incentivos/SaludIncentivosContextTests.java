package grupo5.incentivos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * K3: el healthcheck de Swarm consulta liveness sin token. Incentivos es el servicio cuya salud
 * general incluye RabbitMQ: con el broker caído la salud general da DOWN, pero liveness sigue UP,
 * así una caída corta del broker no reinicia las réplicas.
 */
@SpringBootTest(
    properties = {
      "donatrack.seguridad.habilitada=true",
      // RabbitMQ inaccesible a propósito: nada escucha en el puerto 1
      "spring.rabbitmq.port=1"
    })
@AutoConfigureMockMvc
class SaludIncentivosContextTests {

  // Con la seguridad encendida hace falta un decodificador de tokens; estos pedidos no llevan token
  @MockitoBean private JwtDecoder tokens;

  @MockitoBean private NotificacionesFeignClient notificacionesFeignClient;

  @Autowired private MockMvc mvc;

  @Test
  void conRabbitCaido_livenessSigueUp() throws Exception {
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void conRabbitCaido_laSaludGeneralDaDown() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }

  @Test
  void readinessRespondeSinToken() throws Exception {
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void losComponentesDeLaSaludPidenToken() throws Exception {
    mvc.perform(get("/actuator/health/rabbit")).andExpect(status().isUnauthorized());
  }
}
