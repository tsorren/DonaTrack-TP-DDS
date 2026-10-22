package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
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
  @Autowired private JdbcTemplate jdbc;

  // Con la FK de V6 la necesidad tiene que apuntar a una subcategoría que exista
  private UUID subcategoriaId;

  @BeforeEach
  void limpiar() {
    repository.deleteAll();
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");
    subcategoriaId = crearSubcategoria();
  }

  private UUID crearSubcategoria() {
    UUID categoriaId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.categoria (id, nombre, con_uso, con_vencimiento, tipo_unidad)"
            + " VALUES (?, 'Alimentos', false, true, 'UNIDADES')",
        categoriaId);
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.subcategoria (id, id_categoria, nombre) VALUES (?, ?, 'Fideos')",
        id,
        categoriaId);
    return id;
  }

  @Test
  void guardarYLeer_necesidadExtraordinaria_conservaSusCampos() {
    UUID entidadId = UUID.randomUUID();
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
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), subcategoriaId, 50);

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
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), subcategoriaId, 50);
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
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10);
    repository.save(original);

    original.desactivar();
    repository.save(original);

    assertFalse(repository.findById(original.getId()).orElseThrow().isActiva());
  }

  @Test
  void guardarYLeer_necesidadMensual_conservaElMesSinConvertirloEnTreintaDias() {
    NecesidadRecurrente original = NecesidadMother.recurrenteMensual(subcategoriaId, 50);

    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(Period.ofMonths(1), leida.getPeriodo());
  }

  @Test
  void guardarYLeer_periodoConAniosYMeses_noPierdeLosAnios() {
    NecesidadRecurrente original =
        NecesidadMother.recurrenteConFecha(
            subcategoriaId, 50, NecesidadMother.FECHA_INICIO, Period.of(1, 2, 0));

    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(Period.of(1, 2, 0), leida.getPeriodo());
    assertEquals(
        "P1Y2M",
        jdbc.queryForObject(
            "SELECT periodo FROM donaciones.necesidad WHERE id = ?",
            String.class,
            original.getId()));
  }

  @Test
  void guardar_conUnaCopiaLeidaAntesDeOtroGuardado_fallaPorModificacionConcurrente() {
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10);
    repository.save(original);
    Necesidad copiaA = repository.findById(original.getId()).orElseThrow();
    Necesidad copiaB = repository.findById(original.getId()).orElseThrow();

    copiaA.actualizar("Gana A", 20);
    repository.save(copiaA);
    copiaB.actualizar("Pierde B", 30);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> repository.save(copiaB));
    assertEquals(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE, ex.getError());
    assertEquals("Gana A", repository.findById(original.getId()).orElseThrow().getDescripcion());
  }

  @Test
  void guardar_elMismoObjetoDosVeces_noEsConflicto() {
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10);
    repository.save(original);

    original.actualizar("Primera edición", 20);
    repository.save(original);
    original.actualizar("Segunda edición", 30);
    repository.save(original);

    assertEquals(
        "Segunda edición", repository.findById(original.getId()).orElseThrow().getDescripcion());
  }

  @Test
  void guardar_conSubcategoriaInexistente_fallaPorClaveForanea() {
    NecesidadExtraordinaria huerfana =
        NecesidadMother.extraordinaria(UUID.randomUUID(), UUID.randomUUID(), 10);

    assertThrows(DataIntegrityViolationException.class, () -> repository.save(huerfana));
  }

  @Test
  void existsBySubcategoriaId_indicaSiHayNecesidadesQueLaUsan() {
    repository.save(NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10));

    assertTrue(repository.existsBySubcategoriaId(subcategoriaId));
    assertFalse(repository.existsBySubcategoriaId(crearSubcategoria()));
  }

  @Test
  void findByActivaTrueAndRecurrenteTrue_filtraEnLaBase() {
    NecesidadRecurrente activa =
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), subcategoriaId, 10);
    NecesidadRecurrente inactiva =
        NecesidadMother.recurrenteSemanal(UUID.randomUUID(), subcategoriaId, 10);
    inactiva.desactivar();
    repository.save(activa);
    repository.save(inactiva);
    repository.save(NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10));

    List<Necesidad> resultado = repository.findByActivaTrueAndRecurrenteTrue();

    assertEquals(List.of(activa.getId()), resultado.stream().map(Necesidad::getId).toList());
  }
}
