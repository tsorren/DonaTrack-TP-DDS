package grupo5.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donantes.ArchivoDonantesSubido;
import grupo5.donaciones.dto.donantes.ArchivoOutputDTO;
import grupo5.donaciones.dto.donantes.FilaConErrorDTO;
import grupo5.donaciones.dto.donantes.FiltroArchivos;
import grupo5.donaciones.fixtures.AlmacenamientoFixtures;
import grupo5.donaciones.infrastructure.almacenamiento.StorageEnMemoria;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.entities.donantes.MotivoErrorFila;
import grupo5.donaciones.models.ports.StoragePort;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import grupo5.donaciones.models.repositories.impl.ArchivoDonantesRepositoryEnMemoria;
import grupo5.donaciones.services.impl.ArchivoDonantesService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

class ArchivoDonantesServiceTest {

  private static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

  /** 15 de octubre de 2026, 23:30 en Buenos Aires (02:30 del 16 en UTC). */
  private static final Clock RELOJ =
      Clock.fixed(
          ZonedDateTime.of(2026, 10, 15, 23, 30, 0, 0, BUENOS_AIRES).toInstant(), BUENOS_AIRES);

  private static final byte[] PADRON =
      "TipoPersona,Nombre\nHumana,Ana\n".getBytes(StandardCharsets.UTF_8);

  private IArchivoDonantesRepository archivoRepository;
  private IImportadorService importadorService;
  private StoragePort storage;
  private ArchivoDonantesService service;

  @BeforeEach
  void setUp() {
    archivoRepository = mock(IArchivoDonantesRepository.class);
    importadorService = mock(IImportadorService.class);
    storage = new StorageEnMemoria();
    service =
        new ArchivoDonantesService(
            archivoRepository,
            importadorService,
            storage,
            AlmacenamientoFixtures.propiedades(),
            RELOJ);
  }

  @Test
  void cargarArchivoDonantes_deberiaGuardarElPadronConClavePropiaYDispararLaImportacion()
      throws Exception {
    ArchivoOutputDTO resultado = service.cargarArchivoDonantes(subido("padron.csv", PADRON));

    // La clave es un UUID generado por el servicio, nunca el nombre del cliente
    assertTrue(resultado.path().matches("[0-9a-f-]{36}\\.csv"), resultado.path());
    assertEquals(EstadoArchivo.PENDIENTE.toString(), resultado.estado());
    try (InputStream guardado =
        storage.abrir(AlmacenamientoFixtures.BUCKET_PADRONES, resultado.path())) {
      assertArrayEquals(PADRON, guardado.readAllBytes());
    }
    verify(archivoRepository).save(any(Archivo.class));
    verify(importadorService).procesarImportacionAsincronica(resultado.id());
  }

  @Test
  void cargarArchivoDonantes_conNombreMaliciosoNoDeberiaUsarloComoClave() {
    ArchivoOutputDTO resultado =
        service.cargarArchivoDonantes(subido("../../etc/passwd.csv", PADRON));

    assertTrue(resultado.path().matches("[0-9a-f-]{36}\\.csv"), resultado.path());
  }

  @Test
  void cargarArchivoDonantes_conExtensionEnMayusculasDeberiaAceptarlo() {
    ArchivoOutputDTO resultado = service.cargarArchivoDonantes(subido("PADRON.CSV", PADRON));

    assertEquals(EstadoArchivo.PENDIENTE.toString(), resultado.estado());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"padron.xlsx", "padron", "padron.csv.exe"})
  void cargarArchivoDonantes_sinExtensionCsvDeberiaRechazarlo(String nombre) {
    ValidationException ex =
        assertThrows(
            ValidationException.class, () -> service.cargarArchivoDonantes(subido(nombre, PADRON)));

    assertEquals(ErrorCatalog.ARCHIVO_DONANTES_INVALIDO, ex.getError());
    verifyNoInteractions(archivoRepository, importadorService);
  }

  @Test
  void cargarArchivoDonantes_vacioDeberiaRechazarlo() {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.cargarArchivoDonantes(subido("padron.csv", new byte[0])));

    assertEquals(ErrorCatalog.ARCHIVO_DONANTES_INVALIDO, ex.getError());
  }

  @Test
  void cargarArchivoDonantes_mayorAlMaximoDeberiaRechazarloSinGuardar() {
    long demasiado = DataSize.ofMegabytes(50).toBytes() + 1;
    ArchivoDonantesSubido grande =
        new ArchivoDonantesSubido("padron.csv", demasiado, new ByteArrayInputStream(PADRON));

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.cargarArchivoDonantes(grande));

    assertEquals(ErrorCatalog.ARCHIVO_DONANTES_TAMANIO_EXCEDIDO, ex.getError());
    verifyNoInteractions(archivoRepository, importadorService);
  }

  @Test
  void obtenerPorId_informaElProgresoDelPadron() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    Archivo archivo = new Archivo("x.csv");
    archivo.iniciarConteo(3);
    archivo.registrarCreada();
    archivo.registrarError();
    repo.save(archivo);

    ArchivoOutputDTO dto = conRepositorio(repo).obtenerPorId(archivo.getId());

    assertEquals(3, dto.totalFilas());
    assertEquals(2, dto.procesadas());
    assertEquals(1, dto.creadas());
    assertEquals(0, dto.actualizadas());
    assertEquals(1, dto.conError());
  }

  @Test
  void obtenerFilasConError_devuelveLaPaginaPedidaYElTotalDelArchivo() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    Archivo archivo = new Archivo("x.csv");
    archivo.registrarError();
    archivo.registrarError();
    archivo.registrarError();
    repo.save(archivo);
    UUID id = archivo.getId();
    repo.guardarFilasConError(
        List.of(
            new FilaConError(id, 8, MotivoErrorFila.DATOS_INVALIDOS, "ERR-CSR-003"),
            new FilaConError(id, 2, MotivoErrorFila.FORMATO_INVALIDO, null)));
    repo.guardarFilasConError(
        List.of(new FilaConError(id, 4, MotivoErrorFila.DONANTE_DADO_DE_BAJA, null)));

    Pagina<FilaConErrorDTO> pagina = conRepositorio(repo).obtenerFilasConError(id, 1, 2);

    assertEquals(3, pagina.total());
    assertEquals(
        List.of(new FilaConErrorDTO(8, "DATOS_INVALIDOS", "ERR-CSR-003")), pagina.elementos());
  }

  @Test
  void obtenerFilasConError_sinPaginado_usaLaPrimeraPaginaDeVeinte() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    Archivo archivo = new Archivo("x.csv");
    repo.save(archivo);
    repo.guardarFilasConError(
        IntStream.rangeClosed(2, 30)
            .mapToObj(
                l -> new FilaConError(archivo.getId(), l, MotivoErrorFila.FORMATO_INVALIDO, null))
            .toList());

    Pagina<FilaConErrorDTO> pagina =
        conRepositorio(repo).obtenerFilasConError(archivo.getId(), null, null);

    assertEquals(20, pagina.elementos().size());
    assertEquals(2, pagina.elementos().get(0).linea());
  }

  @Test
  void obtenerFilasConError_deUnArchivoQueNoExiste_lanzaNoEncontrado() {
    ArchivoDonantesService conRepo = conRepositorio(new ArchivoDonantesRepositoryEnMemoria());
    UUID id = UUID.randomUUID();

    assertThrows(RecursoNoEncontradoException.class, () -> conRepo.obtenerFilasConError(id, 0, 20));
  }

  @ParameterizedTest
  @CsvSource({"-1, 20", "0, 0", "0, 101"})
  void obtenerFilasConError_conPaginadoFueraDeRango_rechazaSinConsultar(int page, int size) {
    ValidationException ex =
        assertThrows(
            ValidationException.class,
            () -> service.obtenerFilasConError(UUID.randomUUID(), page, size));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    verifyNoInteractions(archivoRepository);
  }

  @Test
  void repositorioEnMemoria_alBorrarElArchivo_borraSusFilasConError() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    Archivo archivo = new Archivo("x.csv");
    repo.save(archivo);
    repo.guardarFilasConError(
        List.of(new FilaConError(archivo.getId(), 2, MotivoErrorFila.FORMATO_INVALIDO, null)));

    repo.delete(archivo);

    assertTrue(repo.buscarFilasConError(archivo.getId(), 0, 20).isEmpty());
  }

  // --- R11-20: la lista de importaciones ---------------------------------------------------------

  @Test
  void cargarArchivoDonantes_guardaElNombreSinLaRutaYLaHoraDelReloj_C7() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();

    ArchivoOutputDTO subido =
        conRepositorio(repo)
            .cargarArchivoDonantes(subido("C:\\datos\\donantes-octubre.csv", PADRON));
    ArchivoOutputDTO leido = conRepositorio(repo).obtenerPorId(subido.id());

    assertEquals("donantes-octubre.csv", subido.nombre());
    assertEquals(LocalDateTime.of(2026, 10, 15, 23, 30), subido.subidoEn());
    assertEquals("donantes-octubre.csv", leido.nombre());
    assertEquals(subido.subidoEn(), leido.subidoEn());
  }

  @Test
  void listar_sinParametros_devuelveLasVeinteMasRecientesYElTotal_C1() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    LocalDateTime base = LocalDateTime.of(2026, 10, 1, 10, 0);
    IntStream.range(0, 25)
        .forEach(i -> repo.save(new Archivo(i + ".csv", "padron-" + i + ".csv", base.plusDays(i))));

    Pagina<ArchivoOutputDTO> pagina =
        conRepositorio(repo).listar(FiltroArchivos.ninguno(), ParametrosListado.primeraPagina());

    assertEquals(25, pagina.total());
    assertEquals(20, pagina.elementos().size());
    assertEquals("padron-24.csv", pagina.elementos().get(0).nombre());
    assertEquals("padron-5.csv", pagina.elementos().get(19).nombre());
  }

  @Test
  void listar_buscaEnElNombreSinTildesNiMayusculas_C2() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    repo.save(new Archivo("a.csv", "padrón-octubre.csv", LocalDateTime.of(2026, 10, 1, 9, 0)));
    repo.save(new Archivo("b.csv", "donantes.csv", LocalDateTime.of(2026, 10, 2, 9, 0)));

    Pagina<ArchivoOutputDTO> pagina =
        conRepositorio(repo)
            .listar(FiltroArchivos.ninguno(), new ParametrosListado(null, null, "PADRON", null));

    assertEquals(1, pagina.total());
    assertEquals("padrón-octubre.csv", pagina.elementos().get(0).nombre());
  }

  @Test
  void listar_conVariosEstados_devuelveSoloEsosYSuTotal_C3() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    LocalDateTime cuando = LocalDateTime.of(2026, 10, 1, 9, 0);
    repo.save(new Archivo("p.csv", "pendiente.csv", cuando));
    Archivo procesando = new Archivo("q.csv", "procesando.csv", cuando.plusHours(1));
    procesando.marcarComoProcesando();
    repo.save(procesando);
    IntStream.range(0, 3)
        .forEach(
            i -> {
              Archivo terminado = new Archivo(i + ".csv", "terminado.csv", cuando.minusDays(i));
              terminado.marcarComoProcesando();
              terminado.finalizarProcesamiento();
              repo.save(terminado);
            });

    Pagina<ArchivoOutputDTO> pagina =
        conRepositorio(repo)
            .listar(
                new FiltroArchivos(
                    List.of(EstadoArchivo.PENDIENTE, EstadoArchivo.PROCESANDO), null, null),
                new ParametrosListado(0, 1, null, null));

    assertEquals(2, pagina.total());
    assertEquals(1, pagina.elementos().size());
    assertEquals("PROCESANDO", pagina.elementos().get(0).estado());
  }

  @Test
  void listar_lasFechasSonDiasInclusivosEnHoraArgentina_C4() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    conRepositorio(repo).cargarArchivoDonantes(subido("tarde.csv", PADRON));
    ArchivoDonantesService conRepo = conRepositorio(repo);
    LocalDate dia = LocalDate.of(2026, 10, 15);

    Pagina<ArchivoOutputDTO> elMismoDia =
        conRepo.listar(new FiltroArchivos(null, dia, dia), ParametrosListado.primeraPagina());
    Pagina<ArchivoOutputDTO> elDiaSiguiente =
        conRepo.listar(
            new FiltroArchivos(null, dia.plusDays(1), null), ParametrosListado.primeraPagina());

    assertEquals(1, elMismoDia.total());
    assertEquals(0, elDiaSiguiente.total());
  }

  @Test
  void listar_lasViejasSinNombreNiFechaVanAlFinalYNoEntranEnFiltrosNiBusquedas_C5() {
    ArchivoDonantesRepositoryEnMemoria repo = new ArchivoDonantesRepositoryEnMemoria();
    Archivo vieja = new Archivo("vieja.csv");
    repo.save(vieja);
    repo.save(new Archivo("a.csv", "a.csv", LocalDateTime.of(2026, 10, 1, 9, 0)));
    repo.save(new Archivo("b.csv", "b.csv", LocalDateTime.of(2026, 10, 2, 9, 0)));
    ArchivoDonantesService conRepo = conRepositorio(repo);

    List<ArchivoOutputDTO> recientes =
        conRepo.listar(FiltroArchivos.ninguno(), ParametrosListado.primeraPagina()).elementos();
    List<ArchivoOutputDTO> antiguas =
        conRepo
            .listar(FiltroArchivos.ninguno(), new ParametrosListado(null, null, null, "antiguas"))
            .elementos();
    Pagina<ArchivoOutputDTO> conFecha =
        conRepo.listar(
            new FiltroArchivos(null, LocalDate.of(2026, 1, 1), null),
            ParametrosListado.primeraPagina());
    Pagina<ArchivoOutputDTO> conBusqueda =
        conRepo.listar(FiltroArchivos.ninguno(), new ParametrosListado(null, null, "csv", null));

    assertEquals(
        List.of("b.csv", "a.csv", "vieja.csv"),
        recientes.stream().map(ArchivoOutputDTO::path).toList());
    assertEquals(
        List.of("a.csv", "b.csv", "vieja.csv"),
        antiguas.stream().map(ArchivoOutputDTO::path).toList());
    assertEquals(2, conFecha.total());
    assertEquals(2, conBusqueda.total());
    assertEquals(null, recientes.get(2).nombre());
    assertEquals(null, recientes.get(2).subidoEn());
  }

  @ParameterizedTest
  @CsvSource({"0, 0, ", "0, 101, ", "0, 20, otro"})
  void listar_conParametrosFueraDeRango_rechaza_C6(int page, int size, String orden) {
    FiltroArchivos filtro = FiltroArchivos.ninguno();
    ParametrosListado listado = new ParametrosListado(page, size, null, orden);

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.listar(filtro, listado));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
  }

  @Test
  void listar_conDesdePosteriorAHasta_rechazaSinConsultar_C6() {
    FiltroArchivos filtro =
        new FiltroArchivos(null, LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 15));
    ParametrosListado listado = ParametrosListado.primeraPagina();

    ValidationException ex =
        assertThrows(ValidationException.class, () -> service.listar(filtro, listado));

    assertEquals(ErrorCatalog.ARGUMENTO_INVALIDO, ex.getError());
    verifyNoInteractions(archivoRepository);
  }

  @Test
  void archivo_recortaElNombreA255Caracteres() {
    Archivo archivo = new Archivo("x.csv", "a".repeat(300) + ".csv", null);

    assertEquals(Archivo.LARGO_MAXIMO_NOMBRE, archivo.getNombreOriginal().length());
  }

  private ArchivoDonantesService conRepositorio(IArchivoDonantesRepository repo) {
    return new ArchivoDonantesService(
        repo, importadorService, storage, AlmacenamientoFixtures.propiedades(), RELOJ);
  }

  private static ArchivoDonantesSubido subido(String nombre, byte[] contenido) {
    return new ArchivoDonantesSubido(nombre, contenido.length, new ByteArrayInputStream(contenido));
  }
}
