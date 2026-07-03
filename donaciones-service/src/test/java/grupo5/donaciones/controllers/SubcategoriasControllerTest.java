package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.donaciones.controllers.impl.SubcategoriasController;
import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.services.ISubcategoriasService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SubcategoriasControllerTest {

  private MockMvc mockMvc;

  @Mock private ISubcategoriasService subcategoriasService;

  @InjectMocks private SubcategoriasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void crear_DeberiaRetornarCreatedYDto() throws Exception {
    UUID categoriaId = UUID.randomUUID();
    SubcategoriaInputDTO input = new SubcategoriaInputDTO("Fideos", categoriaId, List.of());
    UUID id = UUID.randomUUID();
    SubcategoriaOutputDTO output = new SubcategoriaOutputDTO(id, "Fideos", null, List.of());

    when(subcategoriasService.crear(any(SubcategoriaInputDTO.class))).thenReturn(output);

    mockMvc
        .perform(
            post("/api/subcategorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Fideos"));
  }

  @Test
  void agregarAlias_DeberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    AliasSubcategoriaInputDTO input = new AliasSubcategoriaInputDTO("tallarines");
    SubcategoriaOutputDTO output = new SubcategoriaOutputDTO(id, "Fideos", null, List.of());

    when(subcategoriasService.agregarAlias(eq(id), any(AliasSubcategoriaInputDTO.class)))
        .thenReturn(output);

    mockMvc
        .perform(
            post("/api/subcategorias/" + id + "/aliases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Fideos"));
  }
}
