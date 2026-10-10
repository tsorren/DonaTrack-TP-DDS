package grupo5.logistica.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.logistica.dto.choferes.CambioEstadoChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferRequestDTO;
import grupo5.logistica.dto.choferes.ChoferResponseDTO;
import grupo5.logistica.models.entities.choferes.Chofer;
import grupo5.logistica.models.entities.choferes.EstadoChofer;
import grupo5.logistica.models.repositories.IChoferesRepository;
import grupo5.logistica.services.impl.ChoferService;
import grupo5.logistica.services.mappers.ChoferMapper;
import grupo5.logistica.testutils.ChoferMother;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChoferesServiceTest {

  private IChoferesRepository choferesRepository;
  private ChoferMapper choferMapper;
  private ChoferService choferService;

  @BeforeEach
  void setUp() {
    choferesRepository = mock(IChoferesRepository.class);
    choferMapper = new ChoferMapper();

    choferService = new ChoferService(choferesRepository, choferMapper);
  }

  // ===================== crear() =====================

  @Test
  void crear_deberiaGuardarYDevolverDTO_cuandoDatosValidos() {
    ChoferRequestDTO request = new ChoferRequestDTO("Juan", "Perez", "LIC123", "1122334455");

    ChoferResponseDTO resultado = choferService.crear(request);

    verify(choferesRepository).save(any(Chofer.class));
    assertEquals("Juan", resultado.nombre());
    assertEquals("Perez", resultado.apellido());
    assertEquals("LIC123", resultado.licencia());
    assertEquals(EstadoChofer.DISPONIBLE, resultado.estado());
  }

  // ===================== consultarTodos() =====================

  @Test
  void consultarTodos_deberiaConsultarSoloActivos() {
    Chofer disponible = ChoferMother.disponible();

    when(choferesRepository.findActivos()).thenReturn(List.of(disponible));

    List<ChoferResponseDTO> resultado = choferService.consultarTodos();

    assertEquals(1, resultado.size());
    assertEquals(disponible.getNombre(), resultado.getFirst().nombre());
    assertEquals(disponible.getId(), resultado.getFirst().id());
  }

  @Test
  void consultarTodos_deberiaRetornarListaVacia_cuandoNoHayChoferes() {
    when(choferesRepository.findActivos()).thenReturn(List.of());

    List<ChoferResponseDTO> resultado = choferService.consultarTodos();

    assertTrue(resultado.isEmpty());
  }

  // ===================== consultarPorId() =====================

  @Test
  void consultarPorId_deberiaRetornarChofer_cuandoExiste() {
    Chofer chofer = ChoferMother.disponible();
    UUID id = chofer.getId();

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    ChoferResponseDTO resultado = choferService.consultarPorId(id);

    assertEquals(id, resultado.id());
    assertEquals(chofer.getNombre(), resultado.nombre());
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoNoExiste() {
    UUID id = UUID.randomUUID();

    when(choferesRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.consultarPorId(id));
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoEstaDeshabilitado() {
    Chofer chofer = ChoferMother.disponible();
    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    UUID id = chofer.getId();

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.consultarPorId(id));
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaHabilitarChofer_cuandoEstaDeshabilitado() {
    Chofer chofer = ChoferMother.disponible();
    chofer.cambiarEstado(EstadoChofer.DESHABILITADO);
    UUID id = chofer.getId();

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DISPONIBLE, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    ChoferResponseDTO resultado = choferService.cambiarEstado(id, request);

    assertEquals(EstadoChofer.DISPONIBLE, chofer.getEstado());
    assertEquals(EstadoChofer.DISPONIBLE, resultado.estado());
    verify(choferesRepository).save(chofer);
  }

  @Test
  void cambiarEstado_deberiaDeshabilitarChofer() {
    Chofer chofer = ChoferMother.disponible();
    UUID id = chofer.getId();

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DESHABILITADO, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    ChoferResponseDTO resultado = choferService.cambiarEstado(id, request);

    assertEquals(EstadoChofer.DESHABILITADO, chofer.getEstado());
    assertEquals(EstadoChofer.DESHABILITADO, resultado.estado());
    verify(choferesRepository).save(chofer);
  }

  @Test
  void cambiarEstado_deberiaRechazarEstadoEnRuta() {
    Chofer chofer = ChoferMother.disponible();
    UUID id = chofer.getId();

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.EN_RUTA, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    assertThrows(ValidationException.class, () -> choferService.cambiarEstado(id, request));

    verify(choferesRepository, never()).save(any());
  }

  // ===================== darDeBaja() =====================

  @Test
  void darDeBaja_deberiaDeshabilitarChoferDisponible() {
    Chofer chofer = ChoferMother.disponible();
    UUID id = chofer.getId();

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    choferService.darDeBaja(id);

    assertEquals(EstadoChofer.DESHABILITADO, chofer.getEstado());
    verify(choferesRepository).save(chofer);
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_siNoExiste() {
    UUID id = UUID.randomUUID();

    when(choferesRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.darDeBaja(id));
  }
}
