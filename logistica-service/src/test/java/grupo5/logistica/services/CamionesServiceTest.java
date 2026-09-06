package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.camiones.CambioEstadoCamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionRequestDTO;
import grupo5.logistica.dto.camiones.CamionResponseDTO;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import grupo5.logistica.models.entities.camiones.SolicitudNuevoCamion;
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.services.impl.CamionesService;
import grupo5.logistica.services.mappers.CamionMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CamionesServiceTest {

  private ICamionRepository camionRepository;
  private CamionMapper camionMapper;
  private CamionesService camionesService;

  @BeforeEach
  void setUp() {
    camionRepository = mock(ICamionRepository.class);
    camionMapper = mock(CamionMapper.class);
    camionesService =
        new CamionesService(
            camionRepository, camionMapper, new ValidadorPatentes(camionRepository));
  }

  // ===================== crear() =====================

  @Test
  void crear_deberiaGuardarYDevolverDTO_cuandoDatosValidos() {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 10f, 2f, 5000f);
    SolicitudNuevoCamion solicitud = new SolicitudNuevoCamion("AB123CD", 10f, 2f, 5000f, List.of());
    CamionResponseDTO responseDTO =
        new CamionResponseDTO(
            UUID.randomUUID(), "AB123CD", 10f, 2f, 5000f, EstadoCamion.DISPONIBLE, null);

    when(camionMapper.toSolicitud(request, List.of())).thenReturn(solicitud);
    when(camionMapper.toResponseDTO(any(Camion.class))).thenReturn(responseDTO);

    CamionResponseDTO resultado = camionesService.crear(request);

    verify(camionRepository).save(any(Camion.class));
    assertEquals("AB123CD", resultado.patente());
    assertEquals(EstadoCamion.DISPONIBLE, resultado.estado());
  }

  @Test
  void crear_deberiaLanzarExcepcion_cuandoPatenteConFormatoInvalido() {
    CamionRequestDTO request = new CamionRequestDTO("INVALIDA", 10f, 2f, 5000f);

    assertThrows(ValidationException.class, () -> camionesService.crear(request));
    verify(camionRepository, never()).save(any());
  }

  @Test
  void crear_deberiaLanzarExcepcion_cuandoPatenteDuplicada() {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 10f, 2f, 5000f);
    when(camionRepository.findByPatente("AB123CD")).thenReturn(Optional.of(mock(Camion.class)));

    assertThrows(BusinessStateException.class, () -> camionesService.crear(request));
    verify(camionRepository, never()).save(any());
  }

  @Test
  void crear_deberiaLanzarExcepcion_cuandoPatenteConFormatoEquivalenteYaExiste() {
    CamionRequestDTO request = new CamionRequestDTO("ab-123-cd", 10f, 2f, 5000f);

    when(camionRepository.findByPatente("AB123CD")).thenReturn(Optional.of(mock(Camion.class)));

    assertThrows(BusinessStateException.class, () -> camionesService.crear(request));
    verify(camionRepository, never()).save(any());
  }

  // ===================== consultarTodos() =====================

  @Test
  void consultarTodos_deberiaConsultarSoloActivos() {
    Camion disponible = mock(Camion.class);

    when(camionRepository.findActivos()).thenReturn(List.of(disponible));

    CamionResponseDTO dto =
        new CamionResponseDTO(
            UUID.randomUUID(), "AB123CD", 10f, 2f, 5000f, EstadoCamion.DISPONIBLE, null);
    when(camionMapper.toResponseDTO(disponible)).thenReturn(dto);

    List<CamionResponseDTO> resultado = camionesService.consultarTodos();

    assertEquals(1, resultado.size());
    assertEquals(EstadoCamion.DISPONIBLE, resultado.getFirst().estado());
  }

  @Test
  void consultarTodos_deberiaRetornarListaVacia_cuandoNoHayCamiones() {
    when(camionRepository.findActivos()).thenReturn(List.of());

    List<CamionResponseDTO> resultado = camionesService.consultarTodos();

    assertTrue(resultado.isEmpty());
  }

  // ===================== consultarPorId() =====================

  @Test
  void consultarPorId_deberiaRetornarDTO_cuandoCamionExiste() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);
    CamionResponseDTO dto =
        new CamionResponseDTO(id, "AB123CD", 10f, 2f, 5000f, EstadoCamion.DISPONIBLE, null);

    when(camion.getEstado()).thenReturn(EstadoCamion.DISPONIBLE);
    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));
    when(camionMapper.toResponseDTO(camion)).thenReturn(dto);

    CamionResponseDTO resultado = camionesService.consultarPorId(id);

    assertEquals(id, resultado.id());
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoCamionNoExiste() {
    UUID id = UUID.randomUUID();
    when(camionRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.consultarPorId(id));
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoCamionEstaDeshabilitado() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);

    when(camion.getEstado()).thenReturn(EstadoCamion.DESHABILITADO);
    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.consultarPorId(id));
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaHabilitar_cuandoCamionEstaDeshabilitado() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.DISPONIBLE, null);

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));
    when(camionMapper.toResponseDTO(camion))
        .thenReturn(
            new CamionResponseDTO(id, "AB123CD", 10f, 2f, 5000f, EstadoCamion.DISPONIBLE, null));

    camionesService.cambiarEstado(id, request);

    verify(camion).habilitar();
    verify(camionRepository).save(camion);
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoSeIntentaPasarAEnRuta() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.EN_RUTA, null);

    when(camion.getEstado()).thenReturn(EstadoCamion.DISPONIBLE);
    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    assertThrows(ValidationException.class, () -> camionesService.cambiarEstado(id, request));
    verify(camionRepository, never()).save(any());
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoCamionNoExiste() {
    UUID id = UUID.randomUUID();
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.DESHABILITADO, null);

    when(camionRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class, () -> camionesService.cambiarEstado(id, request));
  }

  // ===================== darDeBaja() =====================

  @Test
  void darDeBaja_deberiaDeshabilitar_cuandoCamionEstaDisponible() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);

    when(camion.getEstado()).thenReturn(EstadoCamion.DISPONIBLE);
    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    camionesService.darDeBaja(id);

    verify(camion).deshabilitar();
    verify(camionRepository).save(camion);
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_cuandoCamionYaEstaDeshabilitado() {
    UUID id = UUID.randomUUID();
    Camion camion = mock(Camion.class);

    when(camion.getEstado()).thenReturn(EstadoCamion.DESHABILITADO);
    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.darDeBaja(id));
    verify(camion, never()).deshabilitar();
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_cuandoCamionNoExiste() {
    UUID id = UUID.randomUUID();
    when(camionRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.darDeBaja(id));
  }
}
