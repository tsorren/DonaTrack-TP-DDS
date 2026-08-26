package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.controllers.impl.CategoriasController;
import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.services.ICategoriasService;
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
class CategoriasControllerTest {

  private MockMvc mockMvc;

  @Mock private ICategoriasService categoriasService;

  @InjectMocks private CategoriasController controller;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new TraceResponseHeaderFilter())
            .build();
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
                .header("X-Trace-Id", "trace-test-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Trace-Id", "trace-test-001"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Alimentos"))
        .andExpect(jsonPath("$.unidad").value("KILOGRAMO"));
  }

  @Test
  void crear_conNombreVacio_DeberiaRetornarBadRequestConFieldErrors() throws Exception {
    CategoriaInputDTO input = new CategoriaInputDTO("", false, true, Unidad.KILOGRAMO);

    mockMvc
        .perform(
            post("/api/categorias")
                .header("X-Trace-Id", "trace-test-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("X-Trace-Id", "trace-test-002"))
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("nombre"));
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

  @Test
  void obtener_DeberiaRetornarOkYDto() throws Exception {
    UUID id = UUID.randomUUID();
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasService.obtener(id)).thenReturn(output);

    mockMvc
        .perform(get("/api/categorias/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.nombre").value("Alimentos"));
  }

  @Test
  void modificar_DeberiaRetornarOkYDtoModificado() throws Exception {
    UUID id = UUID.randomUUID();
    CategoriaInputDTO input =
        new CategoriaInputDTO("Alimentos Editados", false, true, Unidad.KILOGRAMO);
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos Editados", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasService.modificar(eq(id), any(CategoriaInputDTO.class))).thenReturn(output);

    mockMvc
        .perform(
            put("/api/categorias/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Alimentos Editados"));
  }

  @Test
  void eliminar_DeberiaRetornarOkYDtoEliminado() throws Exception {
    UUID id = UUID.randomUUID();
    CategoriaOutputDTO output =
        new CategoriaOutputDTO(id, "Alimentos", false, true, Unidad.KILOGRAMO, List.of());

    when(categoriasService.eliminar(id)).thenReturn(output);

    mockMvc
        .perform(delete("/api/categorias/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()));
  }
}
