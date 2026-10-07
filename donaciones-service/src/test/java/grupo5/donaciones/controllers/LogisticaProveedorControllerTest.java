package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.config.ApiKeyFilter;
import grupo5.donaciones.controllers.impl.LogisticaProveedorController;
import grupo5.donaciones.dto.logistica.ProveedorLogisticaDTO;
import grupo5.donaciones.services.IAdministracionProveedoresService;
import java.util.List;
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

@WebMvcTest(LogisticaProveedorController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class, ApiKeyFilter.class})
@TestPropertySource(
    properties = {
      "donatrack.logistica.admin-api-key=clave-sintetica-admin",
      "donatrack.logistica.proveedor.externo.callback-api-key=clave-sintetica-externo"
    })
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("donaciones-webmvc-context")
class LogisticaProveedorControllerTest {

  private static final String CLAVE_ADMIN = "clave-sintetica-admin";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IAdministracionProveedoresService administracionService;

  private static final List<ProveedorLogisticaDTO> PROVEEDORES =
      List.of(
          new ProveedorLogisticaDTO("donatrack", "amqp", true, false),
          new ProveedorLogisticaDTO("externo", "http", true, true));

  @Test
  void listar_conClaveDeAdministracion_devuelveLosProveedores() throws Exception {
    when(administracionService.listarProveedores()).thenReturn(PROVEEDORES);

    mockMvc
        .perform(get("/api/logistica/proveedores").header("X-API-Key", CLAVE_ADMIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("donatrack"))
        .andExpect(jsonPath("$[0].transporte").value("amqp"))
        .andExpect(jsonPath("$[1].preferido").value(true));
  }

  @Test
  void listar_sinClave_da401() throws Exception {
    mockMvc.perform(get("/api/logistica/proveedores")).andExpect(status().isUnauthorized());

    verify(administracionService, never()).listarProveedores();
  }

  @Test
  void listar_conLaClaveDeUnProveedor_da401() throws Exception {
    mockMvc
        .perform(get("/api/logistica/proveedores").header("X-API-Key", "clave-sintetica-externo"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cambiarPreferido_conClaveDeAdministracion_devuelveLaListaActualizada() throws Exception {
    when(administracionService.cambiarProveedorPreferido("externo")).thenReturn(PROVEEDORES);

    mockMvc
        .perform(
            put("/api/logistica/proveedor-preferido")
                .header("X-API-Key", CLAVE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proveedorId\":\"externo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[1].id").value("externo"));
  }

  @Test
  void cambiarPreferido_sinClave_da401YNoCambiaNada() throws Exception {
    mockMvc
        .perform(
            put("/api/logistica/proveedor-preferido")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proveedorId\":\"externo\"}"))
        .andExpect(status().isUnauthorized());

    verify(administracionService, never()).cambiarProveedorPreferido(any());
  }

  @Test
  void cambiarPreferido_sinProveedor_da400() throws Exception {
    mockMvc
        .perform(
            put("/api/logistica/proveedor-preferido")
                .header("X-API-Key", CLAVE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proveedorId\":\" \"}"))
        .andExpect(status().isBadRequest());

    verify(administracionService, never()).cambiarProveedorPreferido(any());
  }

  @Test
  void cambiarPreferido_conProveedorNoConfigurado_da400() throws Exception {
    when(administracionService.cambiarProveedorPreferido("fantasma"))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(
            put("/api/logistica/proveedor-preferido")
                .header("X-API-Key", CLAVE_ADMIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proveedorId\":\"fantasma\"}"))
        .andExpect(status().isBadRequest());
  }
}
