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
            archivoRepository, importadorService, storage, AlmacenamientoFixtures.propiedades());
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

  private ArchivoDonantesService conRepositorio(IArchivoDonantesRepository repo) {
    return new ArchivoDonantesService(
        repo, importadorService, storage, AlmacenamientoFixtures.propiedades());
  }

  private static ArchivoDonantesSubido subido(String nombre, byte[] contenido) {
    return new ArchivoDonantesSubido(nombre, contenido.length, new ByteArrayInputStream(contenido));
  }
}
