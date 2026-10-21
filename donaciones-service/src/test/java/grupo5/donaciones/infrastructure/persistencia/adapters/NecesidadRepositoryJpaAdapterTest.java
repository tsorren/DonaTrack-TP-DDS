package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import java.time.Period;
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
@Import({NecesidadesPersistenciaMapper.class, NecesidadRepositoryJpaAdapter.class})
// Sin transacción de test: en producción los adapters no son transaccionales, cada operación del
// repo corre en su propia transacción y el mapeo a dominio ocurre afuera.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class NecesidadRepositoryJpaAdapterTest {

  // Usuario por defecto del contenedor: Flyway crea el esquema `donaciones`, así que el test
  // valida entidades contra migraciones (ddl-auto=validate) sin depender de roles ni contraseñas.
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private INecesidadesRepository repository;

  @BeforeEach
  void limpiar() {
    repository.deleteAll();
  }

  @Test
  void guardarYLeer_necesidadExtraordinaria_conservaSusCampos() {
    UUID entidadId = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 80);

    repository.save(original);
    Necesidad leida = repository.findById(original.getId()).orElseThrow();

    assertEquals(original.getId(), leida.getId());
    assertEquals(entidadId, leida.getEntidadId());
    assertEquals(subcategoriaId, leida.getSubcategoriaId());
    assertEquals(80, leida.getCantidadNecesitada());
    assertEquals(original.getDescripcion(), leida.getDescripcion());
    assertEquals(original.getFechaInicio(), leida.getFechaInicio());
    assertTrue(leida.isActiva());
    assertInstanceOf(NecesidadExtraordinaria.class, leida);
  }

  @Test
  void guardarYLeer_necesidadRecurrente_conservaPeriodicidadYPeriodos() {
    NecesidadRecurrente original =
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), UUID.randomUUID(), 50);

    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(Period.ofDays(7), leida.getPeriodo());
    assertEquals(1, leida.getPeriodos().size());
    assertEquals(50, leida.getPeriodos().getFirst().cantidadObjetivo());
    assertEquals(
        original.getPeriodos().getFirst().fechaFin(), leida.getPeriodos().getFirst().fechaFin());
  }

  @Test
  void guardar_alRenovarElPeriodo_agregaElNuevoPeriodoSinDuplicarLosAnteriores() {
    NecesidadRecurrente original =
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), UUID.randomUUID(), 50);
    repository.save(original);

    original.generarNuevoPeriodo();
    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(2, leida.getPeriodos().size());
  }

  @Test
  void buscarNecesidadesPorEntidad_devuelveSoloLasDeEsaEntidad() {
    UUID entidadA = UUID.randomUUID();
    UUID entidadB = UUID.randomUUID();
    UUID subcategoriaId = UUID.randomUUID();
    repository.save(NecesidadMother.extraordinaria(entidadA, subcategoriaId, 10));
    repository.save(NecesidadMother.recurrenteSemanal(entidadA, subcategoriaId, 20));
    repository.save(NecesidadMother.extraordinaria(entidadB, subcategoriaId, 30));

    List<Necesidad> deA = repository.buscarNecesidadesPorEntidad(entidadA);

    assertEquals(2, deA.size());
    assertTrue(deA.stream().allMatch(n -> entidadA.equals(n.getEntidadId())));
  }

  @Test
  void guardar_necesidadDesactivada_persisteElEstadoInactivo() {
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 10);
    repository.save(original);

    original.desactivar();
    repository.save(original);

    assertFalse(repository.findById(original.getId()).orElseThrow().isActiva());
  }
}
