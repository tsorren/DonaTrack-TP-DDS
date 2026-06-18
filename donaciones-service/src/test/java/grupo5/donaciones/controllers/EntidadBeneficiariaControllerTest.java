package grupo5.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.services.IEntidadBeneficiariaService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EntidadBeneficiariaControllerTest {
  private MockMvc mockMvc;

  private IEntidadBeneficiariaService service;

  @BeforeEach
  void setUp() {
    service = mock(IEntidadBeneficiariaService.class);

    mockMvc = MockMvcBuilders.standaloneSetup(new EntidadBeneficiariaController(service)).build();
  }

  @Test
  void crearEntidad_debeRetornarCreated() throws Exception {

    UUID juridicaId = UUID.randomUUID();

    when(service.crearEntidad(any())).thenReturn(mock(EntidadBeneficiariaOutputDTO.class));

    mockMvc
        .perform(
            post("/api/entidades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                          "juridicaId":"%s"
                                        }
                                        """
                        .formatted(juridicaId)))
        .andExpect(status().isCreated());
  }

  @Test
  void obtenerEntidad_debeRetornarOk() throws Exception {

    UUID id = UUID.randomUUID();

    when(service.obtenerEntidad(id)).thenReturn(mock(EntidadBeneficiariaOutputDTO.class));

    mockMvc.perform(get("/api/entidades/" + id)).andExpect(status().isOk());
  }
}
