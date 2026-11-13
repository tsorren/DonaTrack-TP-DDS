package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.InfrastructureException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.comunicaciones.EventoImportacionTerminadaV1;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.personas.HumanaInputDTO;
import grupo5.donaciones.dto.personas.HumanaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaInputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.fixtures.AlmacenamientoFixtures;
import grupo5.donaciones.fixtures.PadronFixtures;
import grupo5.donaciones.infrastructure.LectorDonantesCSV;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.entities.donantes.MotivoErrorFila;
import grupo5.donaciones.models.entities.personas.Humana;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.entities.personas.TipoDocumento;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.models.ports.CargadorDonantes;
import grupo5.donaciones.models.ports.CargadorDonantes.FilaPadron;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.services.impl.ImportadorService;
import grupo5.donaciones.services.impl.ValidadorPersonaDuplicada;
import grupo5.donaciones.services.mappers.DireccionMapper;
import grupo5.donaciones.services.mappers.MedioDeContactoMapper;
import grupo5.donaciones.services.mappers.PersonaMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImportadorServiceTest {

  private IArchivoDonantesRepository archivoRepository;
  private CargadorDonantes lectorCSV;
  private final StoragePort storage = new StorageEnMemoria();
  private PersonaMapper personaMapper;
  private MedioDeContactoMapper medioDeContactoMapper;
  private ValidadorPersonaDuplicada validadorDuplicados;
  private IPersonasService personaService;
  private IDonantesService donantesService;
  private IDonacionesEventPublisher publisher;
  private ImportadorService service;

  @BeforeEach
  void setUp() {
    archivoRepository = mock(IArchivoDonantesRepository.class);
    lectorCSV = mock(CargadorDonantes.class);
    medioDeContactoMapper = new MedioDeContactoMapper();
    personaMapper = new PersonaMapper(new DireccionMapper(), medioDeContactoMapper);
    validadorDuplicados = mock(ValidadorPersonaDuplicada.class);
    personaService = mock(IPersonasService.class);
    donantesService = mock(IDonantesService.class);
    publisher = mock(IDonacionesEventPublisher.class);

    service =
        new ImportadorService(
            archivoRepository,
            lectorCSV,
            personaMapper,
            medioDeContactoMapper,
            validadorDuplicados,
            personaService,
            donantesService,
            storage,
            AlmacenamientoFixtures.propiedades(),
            publisher);
  }

  @Test
  void procesarImportacionAsincronica_conArchivoYFilasValidas_creaDonantesYFinaliza() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

    Map<String, String> fila =
        Map.of("TipoPersona", "HUMANA", "Nombre", "Juan", "Apellido", "Perez");
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, fila));

    when(validadorDuplicados.buscarDuplicado(any(Persona.class))).thenReturn(Optional.empty());

    PersonaOutputDTO outputDTO =
        new HumanaOutputDTO(
            TipoPersona.HUMANA,
            UUID.randomUUID(),
            TipoDocumento.DNI,
            "12345678",
            null,
            List.of(),
            "Juan",
            "Perez",
            null,
            null);
    when(personaService.crearPersona(any(PersonaInputDTO.class))).thenReturn(outputDTO);

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    verify(personaService).crearPersona(any(PersonaInputDTO.class));
    verify(donantesService).crearDonante(any(DonanteInputDTO.class));
    // Al pasar a PROCESANDO, al conocer el total y al terminar
    verify(archivoRepository, times(3)).save(archivo);
    assertEquals(1, archivo.getTotalFilas());
    assertEquals(1, archivo.getCreadas());
    assertEquals(1, archivo.getProcesadas());
    verify(archivoRepository, never()).guardarFilasConError(any());
  }

  /**
   * R11-03: el CSV no trae representante y {@code Juridica} exige uno (ERR-VAL-104). El alta de una
   * empresa nueva lleva el de relleno del mapeo y pasa por el mismo mapeo que usa {@code
   * PersonasService.crearPersona}.
   */
  @Test
  void procesarImportacionAsincronica_conEmpresaNueva_laCreaConSuRepresentanteDeRelleno() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    Map<String, String> fila =
        Map.of(
            "TipoPersona", "JURIDICA",
            "Nombre/Razón Social", "Distribuidora Ejemplo S.A.",
            "TipoDoc", "CUIT",
            "Documento", "30000000001",
            "Email", "contacto@ejemplo.test");
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, fila));
    when(validadorDuplicados.buscarDuplicado(any(Persona.class))).thenReturn(Optional.empty());
    UUID creadaId = UUID.randomUUID();
    when(personaService.crearPersona(any(PersonaInputDTO.class)))
        .thenAnswer(
            pedido -> {
              Persona entidad = personaMapper.toEntity(pedido.getArgument(0));
              Juridica juridica = (Juridica) entidad;
              return new JuridicaOutputDTO(
                  TipoPersona.JURIDICA,
                  creadaId,
                  juridica.getTipoDocumento(),
                  juridica.getDocumento(),
                  null,
                  List.of(),
                  juridica.getRazonSocial(),
                  juridica.getTipo(),
                  juridica.getRubro(),
                  List.of());
            });

    service.procesarImportacionAsincronica(archivoId);

    ArgumentCaptor<PersonaInputDTO> alta = ArgumentCaptor.captor();
    verify(personaService).crearPersona(alta.capture());
    JuridicaInputDTO empresa = assertInstanceOf(JuridicaInputDTO.class, alta.getValue());
    assertEquals("Distribuidora Ejemplo S.A.", empresa.razonSocial());
    assertEquals(1, empresa.representantes().size());
    assertEquals("Representante", empresa.representantes().getFirst().nombre());
    assertEquals("Legal", empresa.representantes().getFirst().apellido());
    verify(donantesService).crearDonante(new DonanteInputDTO(creadaId));
    verify(archivoRepository, never()).guardarFilasConError(any());
    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    assertEquals(1, archivo.getCreadas());
    assertEquals(0, archivo.getConError());
  }

  @Test
  void procesarImportacionAsincronica_conPersonaDuplicada_actualizaPersona() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

    Map<String, String> fila =
        Map.of("TipoPersona", "HUMANA", "Nombre", "Juan", "Apellido", "Perez");
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, fila));

    Humana personaExistente = grupo5.donaciones.fixtures.PersonaMother.humanaValida();

    when(validadorDuplicados.buscarDuplicado(any(Persona.class)))
        .thenReturn(Optional.of((Persona) personaExistente));

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    verify(personaService).actualizarParcial(any(UUID.class), any(PersonaInputDTO.class));
    verify(archivoRepository, times(3)).save(archivo);
    assertEquals(1, archivo.getActualizadas());
    assertEquals(0, archivo.getCreadas());
  }

  @Test
  void procesarImportacionAsincronica_cuandoFallaLector_marcaComoError() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    when(lectorCSV.contarFilas(any(InputStream.class)))
        .thenThrow(new RuntimeException("Error de I/O"));

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.ERROR, archivo.getEstado());
    verify(archivoRepository, times(2)).save(archivo);
  }

  @Test
  void procesarImportacionAsincronica_sinElPadronEnElStorage_marcaComoErrorSinLeer() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = new Archivo(UUID.randomUUID() + ".csv");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.ERROR, archivo.getEstado());
    verify(lectorCSV, never()).contarFilas(any(InputStream.class));
    verify(lectorCSV, never()).leerPorTandas(any(InputStream.class), anyInt(), any());
  }

  @Test
  void procesarImportacionAsincronica_conFilasQueFallan_cuentaYGuardaCadaErrorConSuLineaYMotivo() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    Map<String, String> conflicto = Map.of("TipoPersona", "HUMANA", "Nombre", "Conflicto");
    Map<String, String> dadoDeBaja = Map.of("TipoPersona", "HUMANA", "Nombre", "Baja");
    Map<String, String> invalida = Map.of("TipoPersona", "HUMANA", "Nombre", "Invalida");
    Map<String, String> rota = Map.of("TipoPersona", "HUMANA", "Nombre", "Rota");
    PadronFixtures.entregar(
        lectorCSV,
        new FilaPadron(2, null),
        new FilaPadron(3, conflicto),
        new FilaPadron(5, dadoDeBaja),
        new FilaPadron(6, invalida),
        new FilaPadron(7, rota));
    Humana existente = grupo5.donaciones.fixtures.PersonaMother.humanaValida();
    when(validadorDuplicados.buscarDuplicado(any(Persona.class)))
        .thenAnswer(
            inv -> {
              Humana buscada = inv.getArgument(0);
              return switch (buscada.getNombre()) {
                case "Conflicto" ->
                    throw new BusinessStateException(ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO);
                case "Baja" -> Optional.of((Persona) existente);
                default -> Optional.empty();
              };
            });
    when(personaService.crearPersona(any(PersonaInputDTO.class)))
        .thenAnswer(
            inv -> {
              HumanaInputDTO input = inv.getArgument(0);
              if ("Invalida".equals(input.nombre())) {
                throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
              }
              throw new IllegalStateException("falla inesperada");
            });
    when(donantesService.estaDadoDeBaja(existente.getId())).thenReturn(true);

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
    assertEquals(5, archivo.getTotalFilas());
    assertEquals(5, archivo.getConError());
    assertEquals(5, archivo.getProcesadas());
    ArgumentCaptor<List<FilaConError>> errores = ArgumentCaptor.captor();
    verify(archivoRepository).guardarFilasConError(errores.capture());
    assertEquals(
        List.of(
            new FilaConError(archivo.getId(), 2, MotivoErrorFila.FORMATO_INVALIDO, null),
            new FilaConError(
                archivo.getId(),
                3,
                MotivoErrorFila.PERSONA_DUPLICADA_EN_CONFLICTO,
                ErrorCatalog.PERSONA_DUPLICADA_EN_CONFLICTO.getCode()),
            new FilaConError(archivo.getId(), 5, MotivoErrorFila.DONANTE_DADO_DE_BAJA, null),
            new FilaConError(
                archivo.getId(),
                6,
                MotivoErrorFila.DATOS_INVALIDOS,
                ErrorCatalog.ARGUMENTO_INVALIDO.getCode()),
            new FilaConError(archivo.getId(), 7, MotivoErrorFila.ERROR_INESPERADO, null)),
        errores.getValue());
  }

  @Test
  void procesarImportacionAsincronica_conMuchasFilas_guardaElAvancePorTandas() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    int total = 2 * ImportadorService.TAMANIO_TANDA_PROGRESO + 1;
    List<FilaPadron> filas =
        java.util.stream.IntStream.rangeClosed(2, total + 1)
            .mapToObj(linea -> new FilaPadron(linea, null))
            .toList();
    PadronFixtures.entregar(lectorCSV, filas);

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(total, archivo.getConError());
    // Dos tandas completas y el resto al terminar
    verify(archivoRepository, times(3)).guardarFilasConError(any());
    // PROCESANDO, total, dos tandas y el final
    verify(archivoRepository, times(5)).save(archivo);
  }

  @Test
  void procesarImportacionAsincronica_conFechaDeNacimientoIlegible_laCuentaComoDatosInvalidos() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    Map<String, String> fila =
        Map.of("TipoPersona", "HUMANA", "Nombre", "Juan Perez", "FechaNacimiento", "1990-13-45");
    PadronFixtures.entregar(lectorCSV, new FilaPadron(4, fila));

    service.procesarImportacionAsincronica(archivoId);

    verify(archivoRepository)
        .guardarFilasConError(
            List.of(new FilaConError(archivo.getId(), 4, MotivoErrorFila.DATOS_INVALIDOS, null)));
    verify(personaService, never()).crearPersona(any(PersonaInputDTO.class));
  }

  @Test
  void procesarImportacionAsincronica_siNoPuedeGuardarLasFilasConError_terminaEnError() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, null));
    org.mockito.Mockito.doThrow(new IllegalStateException("base caída"))
        .when(archivoRepository)
        .guardarFilasConError(any());

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.ERROR, archivo.getEstado());
    assertEquals(1, archivo.getConError());
    // PROCESANDO, total y el ERROR final: el avance con errores no llegó a guardarse
    verify(archivoRepository, times(3)).save(archivo);
  }

  /**
   * C3 (R11-B08): cada tanda se aplica y se guarda antes de leer la siguiente. Si la lectura se
   * corta en la tercera, las dos primeras quedan importadas y el archivo termina en ERROR con sus
   * números.
   */
  @Test
  void
      procesarImportacionAsincronica_siLaLecturaFallaEnLaTerceraTanda_lasDosPrimerasQuedanAplicadas() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    int tanda = ImportadorService.TAMANIO_TANDA_PROGRESO;
    when(lectorCSV.contarFilas(any(InputStream.class))).thenReturn(3 * tanda);
    doAnswer(
            pedido -> {
              Consumer<List<FilaPadron>> alLeerTanda = pedido.getArgument(2);
              alLeerTanda.accept(filasValidas(2, tanda));
              alLeerTanda.accept(filasValidas(2 + tanda, tanda));
              throw new InfrastructureException(
                  ErrorCatalog.CSV_READ_ERROR, new Exception("corte"));
            })
        .when(lectorCSV)
        .leerPorTandas(any(InputStream.class), anyInt(), any());
    when(validadorDuplicados.buscarDuplicado(any(Persona.class))).thenReturn(Optional.empty());
    when(personaService.crearPersona(any(PersonaInputDTO.class))).thenReturn(humanaCreada());

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.ERROR, archivo.getEstado());
    assertEquals(3 * tanda, archivo.getTotalFilas());
    assertEquals(2 * tanda, archivo.getProcesadas());
    assertEquals(2 * tanda, archivo.getCreadas());
    verify(personaService, times(2 * tanda)).crearPersona(any(PersonaInputDTO.class));
    verify(lectorCSV).leerPorTandas(any(InputStream.class), eq(tanda), any());
    // PROCESANDO, total, el avance de cada tanda leída y el ERROR final
    verify(archivoRepository, times(5)).save(archivo);
  }

  /**
   * C5 (R11-03, R11-B08): una empresa leída del CSV de verdad, con el lector real, llega al alta
   * con el representante de relleno.
   */
  @Test
  void procesarImportacionAsincronica_conEmpresaLeidaDelCsvReal_llegaAlAltaConSuRepresentante() {
    ImportadorService conLectorReal =
        new ImportadorService(
            archivoRepository,
            new LectorDonantesCSV(),
            personaMapper,
            medioDeContactoMapper,
            validadorDuplicados,
            personaService,
            donantesService,
            storage,
            AlmacenamientoFixtures.propiedades(),
            publisher);
    UUID archivoId = UUID.randomUUID();
    Archivo archivo =
        archivoEnStorage(
            "\uFEFFTipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono\n"
                + "JURIDICA,CUIT,30-52235350-3,Distribuidora Ejemplo S.A.,"
                + "contacto@ejemplo.test,+54 11 4000-0000\n");
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    when(validadorDuplicados.buscarDuplicado(any(Persona.class))).thenReturn(Optional.empty());
    when(personaService.crearPersona(any(PersonaInputDTO.class))).thenReturn(humanaCreada());

    conLectorReal.procesarImportacionAsincronica(archivoId);

    ArgumentCaptor<PersonaInputDTO> alta = ArgumentCaptor.captor();
    verify(personaService).crearPersona(alta.capture());
    JuridicaInputDTO empresa = assertInstanceOf(JuridicaInputDTO.class, alta.getValue());
    assertEquals("Distribuidora Ejemplo S.A.", empresa.razonSocial());
    assertEquals(1, empresa.representantes().size());
    assertEquals(EstadoArchivo.PROCESADO, archivo.getEstado());
    assertEquals(1, archivo.getTotalFilas());
    assertEquals(1, archivo.getCreadas());
  }

  /** Si la segunda pasada lee otra cantidad que la cuenta previa, el total queda con la real. */
  @Test
  void procesarImportacionAsincronica_siLaCuentaPreviaNoCoincide_terminaConLaCuentaReal() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, null), new FilaPadron(3, null));
    when(lectorCSV.contarFilas(any(InputStream.class))).thenReturn(5);

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
    assertEquals(2, archivo.getTotalFilas());
    assertEquals(2, archivo.getProcesadas());
  }

  /** C1 (R11-30): al terminar sin errores se avisa una sola vez, con las cifras y sin el nombre. */
  @Test
  void procesarImportacionAsincronica_alTerminarSinErrores_publicaImportacionTerminadaUnaVez() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo =
        archivoEnStorageConNombre(
            "padron-con-datos.csv", java.time.LocalDateTime.of(2026, 11, 12, 18, 30));
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    PadronFixtures.entregar(
        lectorCSV, new FilaPadron(2, Map.of("TipoPersona", "HUMANA", "Nombre", "Ana")));
    when(validadorDuplicados.buscarDuplicado(any(Persona.class))).thenReturn(Optional.empty());
    when(personaService.crearPersona(any(PersonaInputDTO.class))).thenReturn(humanaCreada());

    service.procesarImportacionAsincronica(archivoId);

    EventoImportacionTerminadaV1 evento = eventoPublicado();
    assertEquals(archivo.getId(), evento.archivoId());
    assertEquals(EventoImportacionTerminadaV1.TERMINADA, evento.resultado());
    assertEquals(1, evento.totalFilas());
    assertEquals(1, evento.procesadas());
    assertEquals(1, evento.creadas());
    assertEquals(0, evento.actualizadas());
    assertEquals(0, evento.conError());
    assertEquals(java.time.LocalDateTime.of(2026, 11, 12, 18, 30), evento.subidoEn());
    assertNotNull(evento.fecha());
    org.assertj.core.api.Assertions.assertThat(evento.toString())
        .doesNotContain("padron-con-datos");
  }

  /** C2 (R11-30): con filas con error, el resultado lo dice y lleva cuántas. */
  @Test
  void procesarImportacionAsincronica_conFilasConError_publicaTerminadaConErrores() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, null), new FilaPadron(3, null));

    service.procesarImportacionAsincronica(archivoId);

    EventoImportacionTerminadaV1 evento = eventoPublicado();
    assertEquals(EventoImportacionTerminadaV1.TERMINADA_CON_ERRORES, evento.resultado());
    assertEquals(2, evento.conError());
    assertEquals(2, evento.procesadas());
    assertNull(evento.subidoEn());
  }

  /** C3 (R11-30): si se corta, se avisa CORTADA con las filas que se leyeron. */
  @Test
  void procesarImportacionAsincronica_siSeCorta_publicaCortadaConLasFilasLeidas() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    when(lectorCSV.contarFilas(any(InputStream.class)))
        .thenThrow(new RuntimeException("Error de I/O"));

    service.procesarImportacionAsincronica(archivoId);

    EventoImportacionTerminadaV1 evento = eventoPublicado();
    assertEquals(EventoImportacionTerminadaV1.CORTADA, evento.resultado());
    assertNull(evento.totalFilas());
    assertEquals(0, evento.procesadas());
  }

  /** Si no se puede guardar el estado final, no se avisa nada (spec, invariantes). */
  @Test
  void procesarImportacionAsincronica_siNoPuedeGuardarElError_noPublica() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    org.mockito.Mockito.doThrow(new IllegalStateException("base caída"))
        .when(archivoRepository)
        .save(archivo);

    org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class, () -> service.procesarImportacionAsincronica(archivoId));

    verify(publisher, never()).publicarImportacionTerminada(any());
  }

  /** Si el aviso no sale, la importación igual queda terminada: no pasa a ERROR. */
  @Test
  void procesarImportacionAsincronica_siFallaElAviso_elArchivoQuedaProcesado() {
    UUID archivoId = UUID.randomUUID();
    Archivo archivo = nuevoArchivoEnStorage();
    when(archivoRepository.findById(archivoId)).thenReturn(Optional.of(archivo));
    PadronFixtures.entregar(lectorCSV, new FilaPadron(2, null));
    org.mockito.Mockito.doThrow(new IllegalStateException("broker caído"))
        .when(publisher)
        .publicarImportacionTerminada(any());

    service.procesarImportacionAsincronica(archivoId);

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, archivo.getEstado());
    verify(publisher, times(1)).publicarImportacionTerminada(any());
  }

  private EventoImportacionTerminadaV1 eventoPublicado() {
    ArgumentCaptor<EventoImportacionTerminadaV1> captor =
        ArgumentCaptor.forClass(EventoImportacionTerminadaV1.class);
    verify(publisher, times(1)).publicarImportacionTerminada(captor.capture());
    return captor.getValue();
  }

  private Archivo archivoEnStorageConNombre(String nombre, java.time.LocalDateTime subidoEn) {
    String clave = UUID.randomUUID() + ".csv";
    storage.guardar(
        AlmacenamientoFixtures.BUCKET_PADRONES,
        clave,
        new ByteArrayInputStream(new byte[] {1}),
        1,
        "text/csv");
    return new Archivo(clave, nombre, subidoEn);
  }

  private static List<FilaPadron> filasValidas(int primeraLinea, int cantidad) {
    return IntStream.range(primeraLinea, primeraLinea + cantidad)
        .mapToObj(
            linea ->
                new FilaPadron(
                    linea, Map.of("TipoPersona", "HUMANA", "Nombre", "Persona " + linea)))
        .toList();
  }

  private static PersonaOutputDTO humanaCreada() {
    return new HumanaOutputDTO(
        TipoPersona.HUMANA,
        UUID.randomUUID(),
        TipoDocumento.DNI,
        "12345678",
        null,
        List.of(),
        "Juan",
        "Perez",
        null,
        null);
  }

  private Archivo archivoEnStorage(String csv) {
    String clave = UUID.randomUUID() + ".csv";
    byte[] contenido = csv.getBytes(StandardCharsets.UTF_8);
    storage.guardar(
        AlmacenamientoFixtures.BUCKET_PADRONES,
        clave,
        new ByteArrayInputStream(contenido),
        contenido.length,
        "text/csv");
    return new Archivo(clave);
  }

  /** Un padrón guardado en el storage: el path del archivo es su clave. */
  private Archivo nuevoArchivoEnStorage() {
    String clave = UUID.randomUUID() + ".csv";
    storage.guardar(
        AlmacenamientoFixtures.BUCKET_PADRONES,
        clave,
        new ByteArrayInputStream(new byte[] {1}),
        1,
        "text/csv");
    return new Archivo(clave);
  }
}
