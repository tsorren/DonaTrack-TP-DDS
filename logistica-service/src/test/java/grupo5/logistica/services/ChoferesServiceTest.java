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
    choferMapper = mock(ChoferMapper.class);

    choferService = new ChoferService(choferesRepository, choferMapper);
  }

  // ===================== crear() =====================

  @Test
  void crear_deberiaGuardarYDevolverDTO_cuandoDatosValidos() {

    ChoferRequestDTO request = new ChoferRequestDTO("Juan", "Perez", "LIC123", "1122334455");

    Chofer chofer = mock(Chofer.class);

    UUID id = UUID.randomUUID();

    ChoferResponseDTO response =
        new ChoferResponseDTO(
            id, "Juan", "Perez", "LIC123", "1122334455", EstadoChofer.DISPONIBLE, null);

    when(choferMapper.toDomain(request)).thenReturn(chofer);
    when(choferMapper.toResponseDTO(chofer)).thenReturn(response);

    ChoferResponseDTO resultado = choferService.crear(request);

    verify(choferesRepository).save(chofer);
    assertEquals("Juan", resultado.nombre());
    assertEquals(EstadoChofer.DISPONIBLE, resultado.estado());
  }

  // ===================== consultarTodos() =====================

  @Test
  void consultarTodos_deberiaConsultarSoloActivos() {

    Chofer disponible = mock(Chofer.class);

    when(choferesRepository.findActivos()).thenReturn(List.of(disponible));

    ChoferResponseDTO dto =
        new ChoferResponseDTO(
            UUID.randomUUID(), "Juan", "Perez", "LIC123", "112233", EstadoChofer.DISPONIBLE, null);

    when(choferMapper.toResponseDTO(disponible)).thenReturn(dto);

    List<ChoferResponseDTO> resultado = choferService.consultarTodos();

    assertEquals(1, resultado.size());
    assertEquals("Juan", resultado.getFirst().nombre());
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

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    when(chofer.getEstado()).thenReturn(EstadoChofer.DISPONIBLE);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    ChoferResponseDTO dto =
        new ChoferResponseDTO(
            id, "Juan", "Perez", "LIC123", "112233", EstadoChofer.DISPONIBLE, null);

    when(choferMapper.toResponseDTO(chofer)).thenReturn(dto);

    ChoferResponseDTO resultado = choferService.consultarPorId(id);

    assertEquals(id, resultado.id());
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoNoExiste() {

    UUID id = UUID.randomUUID();

    when(choferesRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.consultarPorId(id));
  }

  @Test
  void consultarPorId_deberiaLanzarExcepcion_cuandoEstaDeshabilitado() {

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    when(chofer.getEstado()).thenReturn(EstadoChofer.DESHABILITADO);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.consultarPorId(id));
  }

  // ===================== cambiarEstado() =====================

  @Test
  void cambiarEstado_deberiaHabilitarChofer_cuandoEstaDeshabilitado() {

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DISPONIBLE, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    when(choferMapper.toResponseDTO(chofer))
        .thenReturn(
            new ChoferResponseDTO(
                id, "Juan", "Perez", "LIC", "1122", EstadoChofer.DISPONIBLE, null));

    choferService.cambiarEstado(id, request);

    verify(chofer).cambiarEstado(EstadoChofer.DISPONIBLE);
    verify(choferesRepository).save(chofer);
  }

  @Test
  void cambiarEstado_deberiaDeshabilitarChofer() {

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.DESHABILITADO, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    choferService.cambiarEstado(id, request);

    verify(chofer).cambiarEstado(EstadoChofer.DESHABILITADO);
    verify(choferesRepository).save(chofer);
  }

  @Test
  void cambiarEstado_deberiaRechazarEstadoEnRuta() {

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    CambioEstadoChoferRequestDTO request =
        new CambioEstadoChoferRequestDTO(EstadoChofer.EN_RUTA, null);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));
    doThrow(new ValidationException(null)).when(chofer).cambiarEstado(EstadoChofer.EN_RUTA);

    assertThrows(ValidationException.class, () -> choferService.cambiarEstado(id, request));

    verify(choferesRepository, never()).save(any());
  }

  // ===================== darDeBaja() =====================

  @Test
  void darDeBaja_deberiaDeshabilitarChoferDisponible() {

    UUID id = UUID.randomUUID();

    Chofer chofer = mock(Chofer.class);

    when(chofer.getEstado()).thenReturn(EstadoChofer.DISPONIBLE);

    when(choferesRepository.findById(id)).thenReturn(Optional.of(chofer));

    choferService.darDeBaja(id);

    verify(chofer).cambiarEstado(EstadoChofer.DESHABILITADO);
    verify(choferesRepository).save(chofer);
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcion_siNoExiste() {

    UUID id = UUID.randomUUID();

    when(choferesRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> choferService.darDeBaja(id));
  }
}
