package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.infrastructure.persistencia.mapper.ArchivosPersistenciaMapper;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.EstadoArchivo;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
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

    archivo.finalizarProcesamiento(0);
    repository.save(archivo);
    assertEquals(EstadoArchivo.PROCESADO, estadoPersistido(archivo));
  }

  @Test
  void guardar_alFinalizarConErrores_persisteProcesadoConErrores() {
    Archivo archivo = new Archivo("/tmp/b.csv");
    archivo.marcarComoProcesando();
    archivo.finalizarProcesamiento(3);

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
}
