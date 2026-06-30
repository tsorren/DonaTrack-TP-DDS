package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.controllers.impl.CategoriasController;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.services.ICategoriasService;
import grupo5.donaciones.utils.MockMvcTestUtils;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class CategoriasControllerTest {

  private MockMvc mockMvc;

  @Mock private ICategoriasService categoriasService;

  @InjectMocks private CategoriasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcTestUtils.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void crear_DeberiaRetornarCreatedYDto() throws Exception {
    CategoriaInputDTO input = new CategoriaInputDTO("Alimentos", false, true, Unidad.KILOGRAMO);
    UUID id = UUID.randomUUID();
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasService.crear(any(CategoriaInputDTO.class))).thenReturn(output);

    mockMvc
        .perform(
            post("/api/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Alimentos"))
        .andExpect(jsonPath("$.unidad").value("KILOGRAMO"));
  }

  @Test
  void obtenerTodas_DeberiaRetornarOkYLista() throws Exception {
    UUID id = UUID.randomUUID();
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasService.obtenerTodas()).thenReturn(List.of(output));

    mockMvc
        .perform(get("/api/categorias"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1))
        .andExpect(jsonPath("$[0].nombre").value("Alimentos"));
  }
}
