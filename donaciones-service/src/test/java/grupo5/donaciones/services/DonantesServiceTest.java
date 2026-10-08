package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteDadoDeBajaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoV1;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
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
  @Mock private IDonacionesEventPublisher eventPublisher;
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
        new DonantesService(donantesRepository, donanteMapper, personasRepository, eventPublisher);
  }

  @Test
  void testCrearDonante() throws Exception {
    // Arrange
    Humana humana =
        new Humana("Juan", "Perez", java.time.LocalDate.of(1990, java.time.Month.JANUARY, 1));
    donanteInputDTO = new DonanteInputDTO(humana.getId());

    when(personasRepository.existsById(humana.getId())).thenReturn(true);
    when(personasRepository.findById(humana.getId())).thenReturn(Optional.of(humana));
    when(donantesRepository.findById(humana.getId())).thenReturn(Optional.empty());
    when(donantesRepository.save(any(Donante.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    ResultadoRegistro<DonanteOutputDTO> resultado = donantesService.crearDonante(donanteInputDTO);

    // Assert
    assertTrue(resultado.creado());
    assertNotNull(resultado.recurso());
    assertEquals(humana.getId(), resultado.recurso().idDonante());
    assertTrue(resultado.recurso().activo());
    verify(donantesRepository, times(1)).save(any(Donante.class));
    verify(eventPublisher, times(1))
        .publicarDonanteRegistrado(any(EventoDonanteRegistradoV1.class));
  }

  @Test
  void crearDonante_cuandoLaPersonaYaEsDonanteActivo_noDuplicaNiPublicaEvento() {
    Humana humana = personaConDonanteExistente();
    Donante existente = new Donante(humana.getId());
    when(donantesRepository.findById(humana.getId())).thenReturn(Optional.of(existente));

    ResultadoRegistro<DonanteOutputDTO> resultado =
        donantesService.crearDonante(new DonanteInputDTO(humana.getId()));

    assertFalse(resultado.creado());
    assertEquals(humana.getId(), resultado.recurso().idDonante());
    verify(donantesRepository, never()).save(any(Donante.class));
    verify(eventPublisher, never()).publicarDonanteRegistrado(any());
  }

  @Test
  void crearDonante_cuandoElDonanteEstabaDeBaja_loReactivaYPublicaEvento() {
    Humana humana = personaConDonanteExistente();
    Donante dadoDeBaja = new Donante(humana.getId());
    dadoDeBaja.darDeBaja();
    when(donantesRepository.findById(humana.getId())).thenReturn(Optional.of(dadoDeBaja));
    when(donantesRepository.save(any(Donante.class))).thenAnswer(inv -> inv.getArgument(0));

    ResultadoRegistro<DonanteOutputDTO> resultado =
        donantesService.crearDonante(new DonanteInputDTO(humana.getId()));

    assertFalse(resultado.creado());
    assertTrue(dadoDeBaja.estaActivo());
    verify(donantesRepository).save(dadoDeBaja);
    verify(eventPublisher, times(1))
        .publicarDonanteRegistrado(any(EventoDonanteRegistradoV1.class));
  }

  @Test
  void crearDonante_conPersonaInexistente_lanzaRecursoNoEncontrado() {
    UUID personaId = UUID.randomUUID();
    when(donantesRepository.findById(personaId)).thenReturn(Optional.empty());
    when(personasRepository.existsById(personaId)).thenReturn(false);

    assertThrows(
        RecursoNoEncontradoException.class,
        () -> donantesService.crearDonante(new DonanteInputDTO(personaId)));
    verify(eventPublisher, never()).publicarDonanteRegistrado(any());
  }

  @Test
  void registrarSiNoExiste_sinDonante_loCreaYPublicaEvento() {
    Humana humana = personaConDonanteExistente();
    when(donantesRepository.findById(humana.getId())).thenReturn(Optional.empty());
    when(personasRepository.existsById(humana.getId())).thenReturn(true);
    when(donantesRepository.save(any(Donante.class))).thenAnswer(inv -> inv.getArgument(0));

    assertEquals(EstadoRegistroDonante.CREADO, donantesService.registrarSiNoExiste(humana.getId()));
    verify(eventPublisher, times(1))
        .publicarDonanteRegistrado(any(EventoDonanteRegistradoV1.class));
  }

  @Test
  void registrarSiNoExiste_conDonanteActivo_noHaceNada() {
    Humana humana = personaConDonanteExistente();
    when(donantesRepository.findById(humana.getId()))
        .thenReturn(Optional.of(new Donante(humana.getId())));

    assertEquals(
        EstadoRegistroDonante.YA_REGISTRADO, donantesService.registrarSiNoExiste(humana.getId()));
    verify(donantesRepository, never()).save(any(Donante.class));
    verify(eventPublisher, never()).publicarDonanteRegistrado(any());
  }

  @Test
  void registrarSiNoExiste_conDonanteDeBaja_noLoReactiva() {
    Humana humana = personaConDonanteExistente();
    Donante deBaja = new Donante(humana.getId());
    deBaja.darDeBaja();
    when(donantesRepository.findById(humana.getId())).thenReturn(Optional.of(deBaja));

    assertEquals(
        EstadoRegistroDonante.DADO_DE_BAJA, donantesService.registrarSiNoExiste(humana.getId()));
    assertFalse(deBaja.estaActivo());
    verify(donantesRepository, never()).save(any(Donante.class));
    verify(eventPublisher, never()).publicarDonanteRegistrado(any());
  }

  private Humana personaConDonanteExistente() {
    Humana humana =
        new Humana("Ana", "Lopez", java.time.LocalDate.of(1992, java.time.Month.MARCH, 3));
    when(personasRepository.findById(humana.getId())).thenReturn(Optional.of(humana));
    return humana;
  }

  @Test
  void eliminarDonante_daDeBajaSinBorrarYPublicaElEvento() {
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    donantesService.eliminarDonante(donanteId);

    assertFalse(donante.estaActivo());
    verify(donantesRepository).save(donante);
    verify(donantesRepository, never()).delete(any(Donante.class));
    verify(eventPublisher, times(1))
        .publicarDonanteDadoDeBaja(any(EventoDonanteDadoDeBajaV1.class));
  }

  @Test
  void eliminarDonante_yaDadoDeBaja_noRepiteElEvento() {
    donante.darDeBaja();
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    donantesService.eliminarDonante(donanteId);

    verify(donantesRepository, never()).save(any(Donante.class));
    verify(eventPublisher, never()).publicarDonanteDadoDeBaja(any());
  }

  @Test
  void eliminarDonante_inexistente_lanzaRecursoNoEncontrado() {
    UUID idInexistente = UUID.randomUUID();
    when(donantesRepository.findById(idInexistente)).thenReturn(Optional.empty());

    assertThrows(
        RecursoNoEncontradoException.class, () -> donantesService.eliminarDonante(idInexistente));
  }

  @Test
  void darDeBajaSiExiste_conDonanteActivo_loDaDeBajaYPublica() {
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    donantesService.darDeBajaSiExiste(donanteId);

    assertFalse(donante.estaActivo());
    verify(eventPublisher, times(1))
        .publicarDonanteDadoDeBaja(any(EventoDonanteDadoDeBajaV1.class));
  }

  @Test
  void darDeBajaSiExiste_sinDonante_noHaceNada() {
    UUID personaSinRol = UUID.randomUUID();
    when(donantesRepository.findById(personaSinRol)).thenReturn(Optional.empty());

    donantesService.darDeBajaSiExiste(personaSinRol);

    verify(donantesRepository, never()).save(any(Donante.class));
    verify(eventPublisher, never()).publicarDonanteDadoDeBaja(any());
  }

  @Test
  void obtenerPorId_deUnDonanteDeBaja_loDevuelveMarcadoInactivo() {
    donante.darDeBaja();
    when(donantesRepository.findById(donanteId)).thenReturn(Optional.of(donante));

    assertFalse(donantesService.obtenerPorId(donanteId).activo());
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
