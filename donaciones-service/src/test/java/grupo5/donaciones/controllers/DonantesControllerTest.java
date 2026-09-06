package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import grupo5.common.handlers.GlobalExceptionHandler;
import grupo5.common.logging.TraceResponseHeaderFilter;
import grupo5.donaciones.controllers.impl.DonantesController;
import grupo5.donaciones.dto.donantes.ArchivoInputDTO;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.models.entities.personas.Genero;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.IArchivoDonantesService;
import grupo5.donaciones.services.IDonantesService;
import java.time.LocalDate;
import java.time.Month;
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
class DonantesControllerTest {

  private MockMvc mockMvc;

  @Mock private IDonantesService donantesService;
  @Mock private IArchivoDonantesService archivoDonantesService;

  @InjectMocks private DonantesController controller;

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
  void crearDonante_DeberiaRetornarCreatedYDto() throws Exception {
    UUID personaId = UUID.randomUUID();
    UUID donanteId = UUID.randomUUID();
    DonanteInputDTO input = new DonanteInputDTO(personaId);
    HumanaOutputDTO personaOutput =
        new HumanaOutputDTO(
            TipoPersona.HUMANA,
            personaId,
            TipoDocumento.DNI,
            "12345678",
            null,
            List.of(),
            "Juan",
            "Pérez",
            Genero.HOMBRE,
            LocalDate.of(1990, Month.JANUARY, 1));
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, personaOutput);

    when(donantesService.crearDonante(any(DonanteInputDTO.class))).thenReturn(output);

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"))
        .andExpect(jsonPath("$.idDonante").value(donanteId.toString()));
  }

  @Test
  void crearDonante_conIdPersonaNulo_DeberiaRetornarBadRequest() throws Exception {
    DonanteInputDTO input = new DonanteInputDTO(null);

    mockMvc
        .perform(
            post("/api/donantes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("idPersona"));
  }

  @Test
  void listarDonantes_DeberiaRetornarOkYLista() throws Exception {
    UUID donanteId = UUID.randomUUID();
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, null);

    when(donantesService.listarDonantesPorContacto(null)).thenReturn(List.of(output));

    mockMvc
        .perform(get("/api/donantes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size()").value(1));
  }

  @Test
  void obtenerDonante_DeberiaRetornarOkYDto() throws Exception {
    UUID donanteId = UUID.randomUUID();
    DonanteOutputDTO output = new DonanteOutputDTO(donanteId, null);

    when(donantesService.obtenerPorId(donanteId)).thenReturn(output);

    mockMvc
        .perform(get("/api/donantes/{id}", donanteId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idDonante").value(donanteId.toString()));
  }

  @Test
  void eliminarDonante_DeberiaRetornarNoContent() throws Exception {
    UUID donanteId = UUID.randomUUID();

    mockMvc.perform(delete("/api/donantes/{id}", donanteId)).andExpect(status().isNoContent());
  }

  @Test
  void cargarArchivoDonantes_conPathValido_DeberiaRetornarAccepted() throws Exception {
    ArchivoInputDTO input = new ArchivoInputDTO("/ruta/donantes.csv");
    ArchivoOutputDTO output =
        new ArchivoOutputDTO(UUID.randomUUID(), "/ruta/donantes.csv", "PENDIENTE");

    when(archivoDonantesService.cargarArchivoDonantes(any(ArchivoInputDTO.class)))
        .thenReturn(output);

    mockMvc
        .perform(
            post("/api/donantes/archivos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.estado").value("PENDIENTE"));
  }

  @Test
  void cargarArchivoDonantes_conPathVacio_DeberiaRetornarBadRequest() throws Exception {
    ArchivoInputDTO input = new ArchivoInputDTO("");

    mockMvc
        .perform(
            post("/api/donantes/archivos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("path"));
  }

  @Test
  void obtenerArchivoDonantes_DeberiaRetornarOkYDto() throws Exception {
    UUID archivoId = UUID.randomUUID();
    ArchivoOutputDTO output = new ArchivoOutputDTO(archivoId, "/ruta/donantes.csv", "PROCESADO");

    when(archivoDonantesService.obtenerPorId(archivoId)).thenReturn(output);

    mockMvc
        .perform(get("/api/donantes/archivos/{id}", archivoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(archivoId.toString()))
        .andExpect(jsonPath("$.estado").value("PROCESADO"));
  }
}
