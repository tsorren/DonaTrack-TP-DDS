package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donaciones.FiltroDonaciones;
import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.inputs.ItemDonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import grupo5.donaciones.fixtures.AlmacenamientoFixtures;
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
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.OrdenPorFecha;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.impl.DonacionesService;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.DonacionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
            new DireccionMapper(),
            donantesRepository,
            personasRepository,
            personaMapper,
            AlmacenamientoFixtures.fotosBienes());
    service =
        new DonacionesService(
            donacionesRepository,
            donantesRepository,
            mapper,
            procesadorDonaciones,
            eventPublisher,
            AlmacenamientoFixtures.fotosBienes());

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
  void cargarDonacion_conUnaFotoQueNoSeSubio_lanzaFotoNoEncontradaYNoGuarda() {
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));
    DonacionInputDTO conFoto =
        new DonacionInputDTO(
            donante.getId(),
            inputDTO.descripcion(),
            List.of(
                new ItemDonacionInputDTO(
                    "Arroz 1kg",
                    "/fotos-bienes/3f2b9c1e-7a4d-4e8f-9b0a-1c2d3e4f5a6b.jpg",
                    null,
                    null,
                    1.0,
                    0.002,
                    10)),
            inputDTO.nombreDeposito(),
            inputDTO.direccion(),
            inputDTO.fecha());

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.cargarDonacion(conFoto));

    assertEquals(ErrorCatalog.FOTO_BIEN_NO_ENCONTRADA, ex.getError());
    verify(donacionesRepository, never()).save(any());
    verify(procesadorDonaciones, never()).procesar(any());
  }

  @Test
  void cargarDonacion_cuandoElDonanteEstaDeBaja_lanzaDonanteInactivoYNoGuarda() {
    donante.darDeBaja();
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.of(donante));

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.cargarDonacion(inputDTO));

    assertEquals(ErrorCatalog.DONANTE_INACTIVO, ex.getError());
    verify(donacionesRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
    verify(procesadorDonaciones, never()).procesar(any());
  }

  @Test
  void cargarDonacion_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    when(donantesRepository.findById(donante.getId())).thenReturn(Optional.empty());

    assertThrows(RecursoNoEncontradoException.class, () -> service.cargarDonacion(inputDTO));
    verify(donacionesRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void buscar_pasaFiltrosYPaginaAlRepositorioYDevuelveElTotal_R1101() {
    when(donacionesRepository.buscar(any())).thenReturn(new Tramo<>(List.of(donacion), 7));
    when(donantesRepository.findAllById(any())).thenReturn(List.of(donante));
    LocalDate desde = LocalDate.of(2026, 11, 1);
    LocalDate hasta = LocalDate.of(2026, 11, 30);

    Pagina<DonacionOutputDTO> result =
        service.buscar(
            new FiltroDonaciones(donante.getId(), desde, hasta),
            new ParametrosListado(2, 5, "  arroz ", "ANTIGUAS"));

    assertEquals(7, result.total());
    assertEquals(
        List.of(donacion.getId()), result.elementos().stream().map(DonacionOutputDTO::id).toList());
    assertEquals(donante.getId(), result.elementos().getFirst().donante().idDonante());
    ArgumentCaptor<CriterioDonaciones> criterio = ArgumentCaptor.forClass(CriterioDonaciones.class);
    verify(donacionesRepository).buscar(criterio.capture());
    assertEquals(
        new CriterioDonaciones(
            donante.getId(), desde, hasta, "arroz", OrdenPorFecha.ANTIGUAS, new Paginado(2, 5)),
        criterio.getValue());
    // Por lote: una lectura de donantes para toda la página, ninguna por fila
    verify(donantesRepository, never()).findById(any());
  }

  @Test
  void buscar_sinParametros_pideLaPrimeraPaginaDe20DeLasMasRecientes_R1101() {
    when(donacionesRepository.buscar(any())).thenReturn(new Tramo<>(List.of(), 0));

    service.buscar(FiltroDonaciones.ninguno(), ParametrosListado.primeraPagina());

    ArgumentCaptor<CriterioDonaciones> criterio = ArgumentCaptor.forClass(CriterioDonaciones.class);
    verify(donacionesRepository).buscar(criterio.capture());
    assertEquals(OrdenPorFecha.RECIENTES, criterio.getValue().orden());
    assertEquals(new Paginado(0, 20), criterio.getValue().paginado());
  }

  @Test
  void buscar_conDesdePosteriorAHasta_lanza400SinConsultar() {
    FiltroDonaciones invertido =
        new FiltroDonaciones(null, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1));
    ParametrosListado listado = ParametrosListado.primeraPagina();

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.buscar(invertido, listado));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    verify(donacionesRepository, never()).buscar(any());
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
