package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoDTO;
import grupo5.donaciones.dto.comunicaciones.RegistrarDonanteRequest;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.infrastructure.clients.IncentivosFeignClient;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.services.mappers.DonanteMapper;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DonantesServiceTest {

  @Mock private IDonantesRepository donantesRepository;

  @Mock private DonanteMapper donanteMapper;

  @Mock private IncentivosFeignClient incentivosFeignClient;
  @Mock private NotificacionesFeignClient notificacionesFeignClient;

  @InjectMocks private DonantesService donantesService;

  private Donante donante;
  private DonanteInputDTO donanteInputDTO;
  private DonanteOutputDTO donanteOutputDTO;
  private UUID donanteId;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockitoAnnotations.openMocks(this);
    donanteId = UUID.randomUUID();

    donante = new Donante();
    Field idField = Donante.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(donante, donanteId);

    donanteInputDTO = new DonanteInputDTO(UUID.randomUUID());
    donanteOutputDTO = new DonanteOutputDTO(donanteId, null);
  }

  @Test
  void testCrearDonante() {
    // Arrange
    Humana humana = new Humana("Juan", "Perez", java.time.LocalDate.of(1990, 1, 1));
    donante.setPersona(humana);

    when(donanteMapper.toEntity(donanteInputDTO)).thenReturn(donante);
    when(donantesRepository.save(donante)).thenReturn(donante);
    when(donanteMapper.toOutputDTO(donante)).thenReturn(donanteOutputDTO);

    // Act
    DonanteOutputDTO resultado = donantesService.crearDonante(donanteInputDTO);

    // Assert
    assertNotNull(resultado);
    assertEquals(donanteId, resultado.idDonante());
    verify(donanteMapper, times(1)).toEntity(donanteInputDTO);
    verify(donantesRepository, times(1)).save(donante);
    verify(donanteMapper, times(1)).toOutputDTO(donante);
    verify(incentivosFeignClient, times(1))
        .registrarDonante(eq(donante.getId()), any(RegistrarDonanteRequest.class));
    verify(notificacionesFeignClient, times(1)).enviarEvento(any(EventoDonanteRegistradoDTO.class));
  }

  @Test
  void testEliminarDonante() {
    // Arrange
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    // Act
    donantesService.eliminarDonante(donanteId);

    // Assert
    verify(donantesRepository, times(1)).delete(donante);
    verify(incentivosFeignClient, times(1)).darDeBaja(donanteId);
  }

  @Test
  void testObtenerPorId_cuandoExiste() {
    // Arrange
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));
    when(donanteMapper.toOutputDTO(donante)).thenReturn(donanteOutputDTO);

    // Act
    DonanteOutputDTO resultado = donantesService.obtenerPorId(donanteId);

    // Assert
    assertNotNull(resultado);
    assertEquals(donanteId, resultado.idDonante());
  }

  @Test
  void testObtenerPorId_cuandoNoExiste_debeLanzarExcepcion() {
    // Arrange
    UUID idInexistente = UUID.randomUUID();
    when(donantesRepository.findById(idInexistente)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        RecursoNoEncontradoException.class,
        () -> {
          donantesService.obtenerPorId(idInexistente);
        });
  }
}
