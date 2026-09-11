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
import grupo5.logistica.models.entities.camiones.ValidadorPatentes;
import grupo5.logistica.models.repositories.ICamionRepository;
import grupo5.logistica.services.impl.CamionesService;
import grupo5.logistica.services.mappers.CamionMapper;
import grupo5.logistica.testutils.CamionMother;
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
    camionMapper = new CamionMapper();
    camionesService =
        new CamionesService(
            camionRepository, camionMapper, new ValidadorPatentes(camionRepository));
  }

  // ===================== crear() =====================

  @Test
  void crear_deberiaGuardarYDevolverDTO_cuandoDatosValidos() {
    CamionRequestDTO request = new CamionRequestDTO("AB123CD", 10f, 2f, 5000f);

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
    Camion disponible = CamionMother.disponible();

    when(camionRepository.findActivos()).thenReturn(List.of(disponible));

    List<CamionResponseDTO> resultado = camionesService.consultarTodos();

    assertEquals(1, resultado.size());
    assertEquals(EstadoCamion.DISPONIBLE, resultado.getFirst().estado());
    assertEquals(disponible.getPatente(), resultado.getFirst().patente());
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
    Camion camion = CamionMother.disponible();
    UUID id = camion.getId();

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    CamionResponseDTO resultado = camionesService.consultarPorId(id);

    assertEquals(id, resultado.id());
    assertEquals(camion.getPatente(), resultado.patente());
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoCamionNoExiste() {
    UUID id = UUID.randomUUID();
    when(camionRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.consultarPorId(id));
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoCamionEstaDeshabilitado() {
    Camion camion = CamionMother.disponible();
    camion.deshabilitar();
    UUID id = camion.getId();

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.consultarPorId(id));
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaHabilitar_cuandoCamionEstaDeshabilitado() {
    Camion camion = CamionMother.disponible();
    camion.deshabilitar();
    UUID id = camion.getId();
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.DISPONIBLE, null);

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    CamionResponseDTO resultado = camionesService.cambiarEstado(id, request);

    assertEquals(EstadoCamion.DISPONIBLE, camion.getEstado());
    assertEquals(EstadoCamion.DISPONIBLE, resultado.estado());
    verify(camionRepository).save(camion);
  }

  @Test
  void cambiarEstado_deberiaLanzarExcepcion_cuandoSeIntentaPasarAEnRuta() {
    Camion camion = CamionMother.disponible();
    UUID id = camion.getId();
    CambioEstadoCamionRequestDTO request =
        new CambioEstadoCamionRequestDTO(EstadoCamion.EN_RUTA, null);

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
    Camion camion = CamionMother.disponible();
    UUID id = camion.getId();

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    camionesService.darDeBaja(id);

    assertEquals(EstadoCamion.DESHABILITADO, camion.getEstado());
    verify(camionRepository).save(camion);
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_cuandoCamionYaEstaDeshabilitado() {
    Camion camion = CamionMother.disponible();
    camion.deshabilitar();
    UUID id = camion.getId();

    when(camionRepository.findById(id)).thenReturn(Optional.of(camion));

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.darDeBaja(id));
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_cuandoCamionNoExiste() {
    UUID id = UUID.randomUUID();
    when(camionRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> camionesService.darDeBaja(id));
  }
}
