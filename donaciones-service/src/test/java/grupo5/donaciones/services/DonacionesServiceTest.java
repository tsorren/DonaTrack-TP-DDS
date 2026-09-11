package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.fixtures.DTOFixtures;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.DonanteMother;
import grupo5.donaciones.fixtures.PersonaMother;
import grupo5.donaciones.infrastructure.ProcesadorDeDonaciones;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.events.DonacionCargada;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import grupo5.donaciones.services.impl.DonacionesService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.DonacionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DonacionesServiceTest {

  @Mock private IDonacionesRepository donacionesRepository;
  @Mock private IPersonasRepository personasRepository;
  @Mock private IDonantesRepository donantesRepository;
  @Mock private IDonantesService donantesService;
  @Mock private ProcesadorDeDonaciones procesadorDonaciones;
  @Mock private ApplicationEventPublisher eventPublisher;

  private DonacionMapper mapper;
  private DonacionesService service;

  private Donante donante;
  private Donacion donacion;
  private DonacionInputDTO inputDTO;

  @BeforeEach
  void setUp() {
    PersonaMapper personaMapper =
        new PersonaMapper(new DireccionMapper(), new MedioDeContactoMapper());
    mapper =
        new DonacionMapper(
            new DireccionMapper(), donantesRepository, personasRepository, personaMapper);
    service =
        new DonacionesService(
            donacionesRepository, donantesRepository, mapper, procesadorDonaciones, eventPublisher);

    Humana persona = PersonaMother.juanPerez();
    donante = DonanteMother.paraPersona(persona);
    donacion = DonacionMother.simple(donante.getId());

    inputDTO = DTOFixtures.donacionInput(donante.getId());
  }

  @Test
  void cargarDonacion_cuandoPersonaExiste_deberiaGuardarYRetornarDTO() {
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    when(donacionesRepository.save(any(Donacion.class))).thenAnswer(inv -> inv.getArgument(0));

    DonacionOutputDTO result = service.cargarDonacion(inputDTO);

    assertNotNull(result);
    assertEquals(donante.getId(), result.donante().idDonante());
    verify(donacionesRepository).save(any(Donacion.class));
    verify(eventPublisher, times(1)).publishEvent(any(DonacionCargada.class));
    verify(procesadorDonaciones).procesar(any(Donacion.class));
  }

  @Test
  void cargarDonacion_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.cargarDonacion(inputDTO));
    verify(donacionesRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void listarDonaciones_deberiaRetornarTodasMapeadas() {
    when(donacionesRepository.findAll()).thenReturn(List.of(donacion));
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));

    List<DonacionOutputDTO> result = service.listarDonaciones();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(donacion.getId(), result.getFirst().id());
  }

  @Test
  void obtenerDonacion_cuandoExiste_deberiaRetornarDTO() {
    when(donacionesRepository.findById(donacion.getId())).thenReturn(Optional.of(donacion));
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));

    DonacionOutputDTO result = service.obtenerDonacion(donacion.getId());

    assertNotNull(result);
    assertEquals(donacion.getId(), result.id());
  }

  @Test
  void obtenerDonacion_cuandoNoExiste_deberiaLanzarExcepcion() {
    UUID id = donacion.getId();
    when(donacionesRepository.findById(id)).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerDonacion(id));
  }
}
