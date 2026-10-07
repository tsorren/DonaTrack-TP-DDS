package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.config.ApiKeyFilter;
import grupo5.donaciones.controllers.impl.LogisticaCallbackController;
import grupo5.donaciones.dto.logistica.AvisoProveedorRequestDTO;
import grupo5.donaciones.services.IAvisosProveedorService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogisticaCallbackController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class, ApiKeyFilter.class})
@TestPropertySource(
    properties = "donatrack.logistica.proveedor.externo.callback-api-key=clave-sintetica-externo")
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("donaciones-webmvc-context")
class LogisticaCallbackControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IAvisosProveedorService avisosProveedorService;

  private static String avisoValido(UUID donacionId) {
    return """
        {"tipo":"ENTREGA_EXITOSA","entregaId":"%s","donacionIndependienteId":"%s","patenteCamion":"AB123CD"}
        """
        .formatted(UUID.randomUUID(), donacionId);
  }

  @Test
  void avisoValido_respondeAcceptedYDelegaEnElServicio() throws Exception {
    mockMvc
        .perform(
            post("/api/logistica/proveedores/externo/avisos")
                .header("X-API-Key", "clave-sintetica-externo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(avisoValido(UUID.randomUUID())))
        .andExpect(status().isAccepted());

    verify(avisosProveedorService)
        .registrarAviso(eq("externo"), any(AvisoProveedorRequestDTO.class));
  }

  @Test
  void avisoSinTipo_respondeBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/logistica/proveedores/externo/avisos")
                .header("X-API-Key", "clave-sintetica-externo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entregaId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isBadRequest());

    verify(avisosProveedorService, never()).registrarAviso(any(), any());
  }

  @Test
  void donacionQueNoEsDelProveedor_respondeNotFound() throws Exception {
    UUID donacionId = UUID.randomUUID();
    doThrow(new RecursoNoEncontradoException(donacionId))
        .when(avisosProveedorService)
        .registrarAviso(eq("externo"), any(AvisoProveedorRequestDTO.class));

    mockMvc
        .perform(
            post("/api/logistica/proveedores/externo/avisos")
                .header("X-API-Key", "clave-sintetica-externo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(avisoValido(donacionId)))
        .andExpect(status().isNotFound());
  }

  @Test
  void avisoSinApiKey_respondeUnauthorizedYNoLlegaAlServicio() throws Exception {
    mockMvc
        .perform(
            post("/api/logistica/proveedores/externo/avisos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(avisoValido(UUID.randomUUID())))
        .andExpect(status().isUnauthorized());

    verify(avisosProveedorService, never()).registrarAviso(any(), any());
  }

  @Test
  void avisoConApiKeyDeOtroProveedor_respondeUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/logistica/proveedores/externo/avisos")
                .header("X-API-Key", "clave-de-otro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(avisoValido(UUID.randomUUID())))
        .andExpect(status().isUnauthorized());

    verify(avisosProveedorService, never()).registrarAviso(any(), any());
  }
}
