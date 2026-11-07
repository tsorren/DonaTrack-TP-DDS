package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.infrastructure.persistencia.entities.personas.DireccionEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDireccionRepository;
import grupo5.donaciones.models.entities.ubicaciones.Direccion;
import grupo5.donaciones.models.entities.ubicaciones.Localidad;
import grupo5.donaciones.models.entities.ubicaciones.Pais;
import grupo5.donaciones.models.entities.ubicaciones.Provincia;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DireccionesPersistenciaMapper.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class DireccionesPersistenciaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private DireccionesPersistenciaMapper mapper;
  @Autowired private SpringDataDireccionRepository repository;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void limpiar() {
    repository.deleteAll();
  }

  private static Localidad localidad() {
    return new Localidad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
  }

  private static Direccion direccion(String calle, Integer piso, String departamento) {
    return new Direccion(calle, 1234, piso, departamento, "C1000", localidad());
  }

  @Test
  void resolver_dosVecesLaMismaDireccion_compartenLaMismaFila() {
    DireccionEntity primera = mapper.resolver(direccion("Medrano", 3, "B"));
    DireccionEntity segunda = mapper.resolver(direccion("Medrano", 3, "B"));

    assertEquals(primera.getId(), segunda.getId());
    assertEquals(1, repository.count());
  }

  @Test
  void resolver_direccionConOtroPiso_creaOtraFila() {
    DireccionEntity piso3 = mapper.resolver(direccion("Medrano", 3, "B"));
    DireccionEntity piso4 = mapper.resolver(direccion("Medrano", 4, "B"));

    assertNotEquals(piso3.getId(), piso4.getId());
    assertEquals(2, repository.count());
  }

  @Test
  void resolver_sinPisoNiDepartamento_tambienSeComparte() {
    DireccionEntity primera = mapper.resolver(direccion("Medrano", null, null));
    DireccionEntity segunda = mapper.resolver(direccion("Medrano", null, null));

    assertEquals(primera.getId(), segunda.getId());
    assertEquals(1, repository.count());
  }

  @Test
  void resolverYLeer_conservaTodosLosCampos() {
    Direccion original = direccion("Medrano", 3, "B");

    Direccion leida = mapper.toDomain(mapper.resolver(original));

    assertEquals(original, leida);
  }

  @Test
  void insertar_direccionRepetidaPorFueraDelMapper_fallaPorRestriccionUnica() {
    mapper.resolver(direccion("Medrano", null, null));
    UUID idNuevo = UUID.randomUUID();

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO donaciones.direccion"
                    + " (id, calle, altura, codigo_postal, localidad, provincia, pais)"
                    + " VALUES (?, 'Medrano', 1234, 'C1000', 'CABA', 'Buenos Aires', 'Argentina')",
                idNuevo));
  }
}
