package grupo5.donaciones.controllers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.direcciones.DireccionInputDTO;
import grupo5.donaciones.dto.direcciones.DireccionOutputDTO;
import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ContadoresPublicosDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.dto.donaciones.outputs.ResumenAdminDTO;
import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import grupo5.donaciones.services.ParametrosListado;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DonacionesControllerTest extends AbstractDonacionesWebMvcTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void cargarDonacion_deberiaRetornarStatusCreated() throws Exception {
    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    ItemDonacionInputDTO itemInput =
        new ItemDonacionInputDTO("Arroz 1kg", null, null, null, 1.0, 0.002, 10);
    DonacionInputDTO input =
        new DonacionInputDTO(
            UUID.randomUUID(),
            "descripcion",
            List.of(itemInput),
            "Deposito Central",
            dirDTO,
            LocalDateTime.now());

    DireccionOutputDTO dirOut =
        new DireccionOutputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionOutputDTO output =
        new DonacionOutputDTO(
            UUID.randomUUID(),
            new grupo5.donaciones.dto.donaciones.outputs.DonanteResumenDTO(
                UUID.randomUUID(), UUID.randomUUID(), null),
            List.of(),
            "descripcion",
            LocalDateTime.of(2026, Month.JUNE, 18, 0, 0),
            dirOut,
            EstadoDonacion.CARGADA,
            List.of());

    when(donacionesService.cargarDonacion(any())).thenReturn(output);

    mockMvc
        .perform(
            post("/api/donaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("X-Trace-Id"));
  }

  @Test
  void cargarDonacion_conItemsVacios_deberiaRetornarBadRequest() throws Exception {
    DireccionInputDTO dirDTO =
        new DireccionInputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionInputDTO input =
        new DonacionInputDTO(
            UUID.randomUUID(),
            "descripcion",
            List.of(),
            "Deposito Central",
            dirDTO,
            LocalDateTime.now());

    mockMvc
        .perform(
            post("/api/donaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"))
        .andExpect(jsonPath("$.errors[0].field").value("items"));
  }

  @Test
  void listarDonaciones_devuelveLaPaginaConElTotalEnElHeader_R1101() throws Exception {
    UUID donanteId = UUID.randomUUID();
    when(donacionesService.buscar(any(), any())).thenReturn(new Pagina<>(List.of(), 57));

    mockMvc
        .perform(
            get("/api/donaciones")
                .param("page", "2")
                .param("size", "10")
                .param("q", "arroz")
                .param("orden", "ANTIGUAS")
                .param("donanteId", donanteId.toString())
                .param("desde", "2026-11-01")
                .param("hasta", "2026-11-30"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "57"))
        .andExpect(jsonPath("$").isArray());

    verify(donacionesService)
        .buscar(
            new FiltroDonaciones(donanteId, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)),
            new ParametrosListado(2, 10, "arroz", "ANTIGUAS"));
  }

  @Test
  void listarDonaciones_conSizeFueraDeRango_devuelve400() throws Exception {
    when(donacionesService.buscar(any(), any()))
        .thenThrow(new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO));

    mockMvc
        .perform(get("/api/donaciones").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ERR-CSR-003"));
  }

  @Test
  void obtenerResumen_devuelveElResumenDelAdministrador_R1101() throws Exception {
    when(resumenAdminService.calcular())
        .thenReturn(
            new ResumenAdminDTO(
                Map.of("EN_DEPOSITO", 3L),
                new ResumenAdminDTO.EntregadasEnElMes("2026-11", 2),
                new ResumenAdminDTO.PorVencer(7, 0, null),
                new ResumenAdminDTO.Donantes(10, 8),
                4,
                1,
                OffsetDateTime.of(2026, 11, 11, 12, 0, 0, 0, ZoneOffset.UTC)));

    mockMvc
        .perform(get("/api/donaciones/resumen"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.donacionesPorEstado.EN_DEPOSITO").value(3))
        .andExpect(jsonPath("$.donantes.activos").value(8))
        .andExpect(jsonPath("$.propuestasPendientes").value(1));
  }

  @Test
  void obtenerDonacion_deberiaRetornarStatusOk() throws Exception {
    UUID id = UUID.randomUUID();
    DireccionOutputDTO dirOut =
        new DireccionOutputDTO(
            "Calle Falsa", 123, null, null, "1000", "CABA", "Buenos Aires", "Argentina");
    DonacionOutputDTO output =
        new DonacionOutputDTO(
            id,
            new grupo5.donaciones.dto.donaciones.outputs.DonanteResumenDTO(
                UUID.randomUUID(), UUID.randomUUID(), null),
            List.of(),
            "descripcion",
            LocalDateTime.of(2026, Month.JUNE, 18, 0, 0),
            dirOut,
            EstadoDonacion.CARGADA,
            List.of());

    when(donacionesService.obtenerDonacion(id)).thenReturn(output);

    mockMvc.perform(get("/api/donaciones/{id}", id)).andExpect(status().isOk());
  }

  @Test
  void obtenerContadoresPublicos_devuelveLosTresTotalesYLaFecha() throws Exception {
    when(contadoresPublicosService.calcular())
        .thenReturn(
            new ContadoresPublicosDTO(
                86, 412, 23, OffsetDateTime.of(2026, 11, 8, 18, 30, 0, 0, ZoneOffset.ofHours(-3))));

    mockMvc
        .perform(get("/api/donaciones/contadores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.donantesActivos").value(86))
        .andExpect(jsonPath("$.donacionesRealizadas").value(412))
        .andExpect(jsonPath("$.beneficiariosActivos").value(23))
        .andExpect(jsonPath("$.calculadoEn").value("2026-11-08T18:30:00-03:00"));
  }
}
