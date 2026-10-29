package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.BienMother;
import grupo5.donaciones.fixtures.DonacionMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.DireccionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesIndependientesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonacionesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.mapper.PropuestasPersistenciaMapper;
import grupo5.donaciones.models.entities.donaciones.Bien;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.ItemDonacionIndependiente;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.PosibleFragmentacion;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import java.time.temporal.ChronoUnit;
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

/** Paso 9: propuestas de asignación y sus posibles fragmentaciones. */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  DireccionesPersistenciaMapper.class,
  DonacionesPersistenciaMapper.class,
  DonacionRepositoryJpaAdapter.class,
  ItemsNormalizadosPersistenciaMapper.class,
  ItemDonacionNormalizadoRepositoryJpaAdapter.class,
  NecesidadesPersistenciaMapper.class,
  NecesidadRepositoryJpaAdapter.class,
  DonacionesIndependientesPersistenciaMapper.class,
  DonacionIndependienteRepositoryJpaAdapter.class,
  PropuestasPersistenciaMapper.class,
  PropuestaRepositoryJpaAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class PropuestasRepositoryJpaTest {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Autowired private IPropuestasRepository propuestas;
  @Autowired private IDonacionesIndependientesRepository independientes;
  @Autowired private IDonacionesRepository donaciones;
  @Autowired private IItemDonacionNormalizadoRepository itemsNormalizados;
  @Autowired private INecesidadesRepository necesidades;
  @Autowired private JdbcTemplate jdbc;

  private NecesidadExtraordinaria necesidad;
  private DonacionIndependiente donacion;
  private DonacionIndependiente otraDonacion;

  @BeforeEach
  void preparar() {
    propuestas.deleteAll();
    independientes.deleteAll();
    itemsNormalizados.deleteAll();
    donaciones.deleteAll();
    necesidades.deleteAll();
    jdbc.update("DELETE FROM donaciones.deposito");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.entidad_beneficiaria");
    jdbc.update("DELETE FROM donaciones.persona");
    jdbc.update("DELETE FROM donaciones.direccion");
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");
    UUID subcategoriaId = crearSubcategoria();
    necesidad = NecesidadMother.extraordinaria(crearEntidad(), subcategoriaId, 80);
    necesidades.save(necesidad);

    Bien fideos = BienMother.alimentoPerecedero("Fideos secos");
    Donacion original = DonacionMother.segmentada(crearDonante(), fideos, 30);
    donaciones.save(original);
    ItemDonacionNormalizado normalizado =
        itemsNormalizados.save(
            new ItemDonacionNormalizado(
                original.getId(), BienMother.aceptado(fideos, subcategoriaId), 30));
    donacion = independiente(normalizado, 20);
    otraDonacion = independiente(normalizado, 10);
    independientes.saveAll(List.of(donacion, otraDonacion));
  }

  private static DonacionIndependiente independiente(
      ItemDonacionNormalizado normalizado, int cantidad) {
    return new DonacionIndependiente(
        normalizado.getDonacionOriginalId(),
        List.of(
            new ItemDonacionIndependiente(normalizado.getId(), normalizado.getBien(), cantidad)));
  }

  private UUID crearDonante() {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", id);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')", id);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", id);
    return id;
  }

  private UUID crearEntidad() {
    UUID id = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'JURIDICA')", id);
    jdbc.update(
        "INSERT INTO donaciones.juridica (id, razon_social, tipo_juridico, rubro)"
            + " VALUES (?, 'Comedor', 'ONG', 'Social')",
        id);
    jdbc.update("INSERT INTO donaciones.entidad_beneficiaria (id, activo) VALUES (?, true)", id);
    return id;
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

  private Propuesta propuestaCon(DonacionIndependiente... fragmentadas) {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(necesidad.getId());
    for (DonacionIndependiente d : fragmentadas) {
      propuesta.agregarFragmentacion(d, 5);
    }
    return propuesta;
  }

  private int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM donaciones." + tabla, Integer.class);
  }

  @Test
  void guardarYLeer_propuestaPendiente_conservaNecesidadEstadoFechaYFragmentaciones() {
    Propuesta original = propuestaCon(donacion, otraDonacion);

    propuestas.save(original);
    Propuesta leida = propuestas.findById(original.getId()).orElseThrow();

    assertEquals(necesidad.getId(), leida.getNecesidadQueSatisfaceId());
    assertEquals(EstadoPropuesta.PENDIENTE, leida.getEstado());
    assertEquals(
        original.getFechaCreacion().truncatedTo(ChronoUnit.MILLIS),
        leida.getFechaCreacion().truncatedTo(ChronoUnit.MILLIS));
    assertEquals(
        List.of(donacion.getId(), otraDonacion.getId()).stream().sorted().toList(),
        leida.getPosiblesFragmentaciones().stream()
            .map(PosibleFragmentacion::getDonacionOriginalId)
            .sorted()
            .toList());
    assertTrue(
        leida.getPosiblesFragmentaciones().stream().allMatch(f -> f.getCantidadNecesaria() == 5));
    assertTrue(leida.getDomainEvents().isEmpty());
  }

  @Test
  void guardar_alAprobar_persisteElEstadoSinDuplicarFragmentaciones() {
    Propuesta propuesta = propuestaCon(donacion);
    propuestas.save(propuesta);

    Propuesta leida = propuestas.findById(propuesta.getId()).orElseThrow();
    leida.aceptar("admin@donatrack.org");
    propuestas.save(leida);

    assertEquals(
        EstadoPropuesta.APROBADA, propuestas.findById(propuesta.getId()).orElseThrow().getEstado());
    assertEquals(1, contar("posible_fragmentacion"));
  }

  @Test
  void guardar_unaDonacionEnDosPropuestas_noChocaPorClave() {
    // D-23: con la PK del DER (id de la donación) la segunda propuesta fallaba
    propuestas.saveAll(List.of(propuestaCon(donacion), propuestaCon(donacion)));

    assertEquals(2, contar("posible_fragmentacion"));
  }

  @Test
  void guardar_fragmentacionDeUnaDonacionInexistente_fallaPorClaveForanea() {
    Propuesta huerfana = propuestaCon(donacion);
    DonacionIndependiente noGuardada =
        new DonacionIndependiente(
            donacion.getDonacionOriginalId(), List.copyOf(donacion.getItems()));
    huerfana.agregarFragmentacion(noGuardada, 3);

    assertThrows(DataIntegrityViolationException.class, () -> propuestas.save(huerfana));
  }

  @Test
  void borrar_donacionIndependienteEnUnaPropuesta_loRechazaLaBase() {
    propuestas.save(propuestaCon(donacion));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "DELETE FROM donaciones.donacion_independiente WHERE id = ?", donacion.getId()));
  }

  @Test
  void borrar_propuesta_borraSusFragmentaciones() {
    Propuesta propuesta = propuestaCon(donacion, otraDonacion);
    propuestas.save(propuesta);

    propuestas.delete(propuesta);

    assertEquals(0, contar("posible_fragmentacion"));
  }

  @Test
  void insertar_cantidadCero_loRechazaLaBase() {
    Propuesta propuesta = propuestaCon(donacion);
    propuestas.save(propuesta);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> jdbc.update("UPDATE donaciones.posible_fragmentacion SET cantidad = 0"));
  }
}
