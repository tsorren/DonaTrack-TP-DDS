package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.infrastructure.persistencia.mapper.ArchivosPersistenciaMapper;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.entities.donantes.MotivoErrorFila;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ArchivosPersistenciaMapper.class, ArchivoDonantesRepositoryJpaAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class ArchivoDonantesRepositoryJpaAdapterTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IArchivoDonantesRepository repository;

  @BeforeEach
  void limpiar() {
    repository.deleteAll();
  }

  private EstadoArchivo estadoPersistido(Archivo archivo) {
    return repository.findById(archivo.getId()).orElseThrow().getEstado();
  }

  @Test
  void guardarYLeer_archivoNuevo_quedaPendienteConSuPath() {
    Archivo original = new Archivo("/tmp/donantes.csv");

    repository.save(original);
    Archivo leido = repository.findById(original.getId()).orElseThrow();

    assertEquals(original.getId(), leido.getId());
    assertEquals("/tmp/donantes.csv", leido.getPath());
    assertEquals(EstadoArchivo.PENDIENTE, leido.getEstado());
  }

  @Test
  void guardar_seguidoDeCadaTransicion_persisteElEstadoVigente() {
    Archivo archivo = new Archivo("/tmp/a.csv");
    repository.save(archivo);

    archivo.marcarComoProcesando();
    repository.save(archivo);
    assertEquals(EstadoArchivo.PROCESANDO, estadoPersistido(archivo));

    archivo.finalizarProcesamiento();
    repository.save(archivo);
    assertEquals(EstadoArchivo.PROCESADO, estadoPersistido(archivo));
  }

  @Test
  void guardar_alFinalizarConErrores_persisteProcesadoConErrores() {
    Archivo archivo = new Archivo("/tmp/b.csv");
    archivo.marcarComoProcesando();
    archivo.registrarError();
    archivo.finalizarProcesamiento();

    repository.save(archivo);

    assertEquals(EstadoArchivo.PROCESADO_CON_ERRORES, estadoPersistido(archivo));
  }

  @Test
  void guardar_alMarcarError_persisteError() {
    Archivo archivo = new Archivo("/tmp/c.csv");
    archivo.marcarComoError();

    repository.save(archivo);

    assertEquals(EstadoArchivo.ERROR, estadoPersistido(archivo));
  }

  @Test
  void guardar_conAvance_persisteLosContadores() {
    Archivo archivo = new Archivo("/tmp/d.csv");
    archivo.iniciarConteo(4);
    archivo.registrarCreada();
    archivo.registrarActualizada();
    archivo.registrarError();

    repository.save(archivo);
    Archivo leido = repository.findById(archivo.getId()).orElseThrow();

    assertEquals(4, leido.getTotalFilas());
    assertEquals(1, leido.getCreadas());
    assertEquals(1, leido.getActualizadas());
    assertEquals(1, leido.getConError());
    assertEquals(3, leido.getProcesadas());
  }

  @Test
  void buscarFilasConError_devuelveLasDelArchivoOrdenadasPorLineaYPaginadas() {
    Archivo archivo = new Archivo("/tmp/e.csv");
    Archivo otro = new Archivo("/tmp/f.csv");
    repository.save(archivo);
    repository.save(otro);
    UUID id = archivo.getId();
    repository.guardarFilasConError(
        List.of(
            new FilaConError(id, 7, MotivoErrorFila.DATOS_INVALIDOS, "ERR-CSR-003"),
            new FilaConError(id, 3, MotivoErrorFila.FORMATO_INVALIDO, null),
            new FilaConError(otro.getId(), 2, MotivoErrorFila.ERROR_INESPERADO, null),
            new FilaConError(id, 5, MotivoErrorFila.DONANTE_DADO_DE_BAJA, null)));

    List<FilaConError> primera = repository.buscarFilasConError(id, 0, 2);
    List<FilaConError> segunda = repository.buscarFilasConError(id, 1, 2);

    assertEquals(List.of(3, 5), primera.stream().map(FilaConError::linea).toList());
    assertEquals(MotivoErrorFila.FORMATO_INVALIDO, primera.get(0).motivo());
    assertNull(primera.get(0).codigo());
    assertEquals(1, segunda.size());
    assertEquals(7, segunda.get(0).linea());
    assertEquals("ERR-CSR-003", segunda.get(0).codigo());
  }

  @Test
  void deleteAll_borraTambienLasFilasConError() {
    Archivo archivo = new Archivo("/tmp/g.csv");
    repository.save(archivo);
    repository.guardarFilasConError(
        List.of(new FilaConError(archivo.getId(), 2, MotivoErrorFila.FORMATO_INVALIDO, null)));

    repository.deleteAll();

    assertTrue(repository.buscarFilasConError(archivo.getId(), 0, 10).isEmpty());
  }
}
