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
import grupo5.donaciones.services.impl.DonantesService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.DonanteMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DonantesServiceTest {

  @Mock private IDonantesRepository donantesRepository;
  @Mock private IncentivosFeignClient incentivosFeignClient;
  @Mock private NotificacionesFeignClient notificacionesFeignClient;
  @Mock private grupo5.donaciones.models.repositories.IPersonasRepository personasRepository;

  private DonanteMapper donanteMapper;
  private DonantesService donantesService;

  private Donante donante;
  private DonanteInputDTO donanteInputDTO;
  private UUID donanteId;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IllegalAccessException {
    MockitoAnnotations.openMocks(this);
    donanteId = UUID.randomUUID();

    donante = new Donante(UUID.randomUUID());
    Field idField = Donante.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(donante, donanteId);

    donanteInputDTO = new DonanteInputDTO(UUID.randomUUID());

    donanteMapper =
        new DonanteMapper(
            new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper()),
            personasRepository);
    donantesService =
        new DonantesService(
            donantesRepository,
            donanteMapper,
            incentivosFeignClient,
            notificacionesFeignClient,
            personasRepository);
  }

  @Test
  void testCrearDonante() throws Exception {
    // Arrange
    Humana humana =
        new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    donanteInputDTO = new DonanteInputDTO(humana.getId());

    when(personasRepository.existsById(humana.getId())).thenReturn(true);
    when(personasRepository.findById(humana.getId())).thenReturn(Optional.of(humana));
    when(donantesRepository.save(any(Donante.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    DonanteOutputDTO resultado = donantesService.crearDonante(donanteInputDTO);

    // Assert
    assertNotNull(resultado);
    assertNotNull(resultado.idDonante());
    verify(donantesRepository, times(1)).save(any(Donante.class));
    verify(incentivosFeignClient, times(1))
        .registrarDonante(any(UUID.class), any(RegistrarDonanteRequest.class));
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
