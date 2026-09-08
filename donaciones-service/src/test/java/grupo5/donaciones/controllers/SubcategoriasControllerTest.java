package grupo5.donaciones.controllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.CommonLibAutoConfiguration;
import grupo5.common.logging.LoggingAutoConfiguration;
import grupo5.donaciones.controllers.impl.SubcategoriasController;
import grupo5.donaciones.dto.categorias.AliasSubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.services.ISubcategoriasService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SubcategoriasController.class)
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
class SubcategoriasControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ISubcategoriasService subcategoriasService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void crear_DeberiaRetornarCreatedYDto() throws Exception {
    UUID categoriaId = UUID.randomUUID();
    SubcategoriaInputDTO input = DTOFixtures.subcategoriaInput("Fideos", categoriaId);
    UUID id = UUID.randomUUID();
    SubcategoriaOutputDTO output = new SubcategoriaOutputDTO(id, "Fideos", null, List.of());

    when(subcategoriasService.crear(any(SubcategoriaInputDTO.class))).thenReturn(output);

    mockMvc
        .perform(
            post("/api/subcategorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Fideos"));
  }

  @Test
  void crear_conNombreVacio_DeberiaRetornarBadRequest() throws Exception {
    UUID categoriaId = UUID.randomUUID();
    SubcategoriaInputDTO input = DTOFixtures.subcategoriaInput("", categoriaId);

    mockMvc
        .perform(
            post("/api/subcategorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("nombre"));
  }

  @Test
  void agregarAlias_DeberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    AliasSubcategoriaInputDTO input = DTOFixtures.aliasSubcategoriaInput("tallarines");
    SubcategoriaOutputDTO output = new SubcategoriaOutputDTO(id, "Fideos", null, List.of());

    when(subcategoriasService.agregarAlias(eq(id), any(AliasSubcategoriaInputDTO.class)))
        .thenReturn(output);

    mockMvc
        .perform(
            post("/api/subcategorias/" + id + "/aliases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Fideos"));
  }
}
