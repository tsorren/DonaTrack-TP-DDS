package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import grupo5.donaciones.models.entities.propuestas.SituacionPropuesta;
import grupo5.donaciones.models.entities.propuestas.SugerenciaDeAlgoritmo;
import grupo5.donaciones.models.entities.propuestas.TipoAlgoritmo;
import grupo5.donaciones.models.repositories.IDonacionesIndependientesRepository;
import grupo5.donaciones.models.repositories.IDonacionesRepository;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import grupo5.donaciones.models.repositories.listados.CriterioPropuestas;
import grupo5.donaciones.models.repositories.listados.OrdenPropuestas;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.PropuestaEnListado;
import grupo5.donaciones.models.repositories.listados.StockEnPropuestas;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private NecesidadExtraordinaria otraNecesidad;
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
    otraNecesidad = NecesidadMother.extraordinaria(crearEntidad(), subcategoriaId, 80);
    necesidades.save(otraNecesidad);

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
  void guardar_alDecidir_persisteQuienYCuandoDecidio() {
    Propuesta propuesta = propuestaCon(donacion);
    propuestas.save(propuesta);

    Propuesta leida = propuestas.findById(propuesta.getId()).orElseThrow();
    leida.rechazar("sub-de-carla");
    propuestas.save(leida);

    Propuesta decidida = propuestas.findById(propuesta.getId()).orElseThrow();
    assertEquals("sub-de-carla", decidida.getDecididaPor());
    assertEquals(
        leida.getFechaDecision().truncatedTo(ChronoUnit.MILLIS),
        decidida.getFechaDecision().truncatedTo(ChronoUnit.MILLIS));
  }

  @Test
  void guardar_unaPendiente_dejaLaDecisionEnNull() {
    Propuesta propuesta = propuestaCon(donacion);
    propuestas.save(propuesta);

    Propuesta leida = propuestas.findById(propuesta.getId()).orElseThrow();
    assertNull(leida.getDecididaPor());
    assertNull(leida.getFechaDecision());
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

  // --- R11-24: sugerencias, situación, filtros y totales ----------------------------------------

  private Propuesta pidiendo(NecesidadExtraordinaria para, DonacionIndependiente de, int cantidad) {
    Propuesta propuesta = new Propuesta();
    propuesta.asociarNecesidad(para.getId());
    propuesta.agregarFragmentacion(de, cantidad);
    return propuesta;
  }

  private static CriterioPropuestas pendientes(Set<SituacionPropuesta> situaciones) {
    return new CriterioPropuestas(
        Set.of(EstadoPropuesta.PENDIENTE),
        situaciones,
        null,
        false,
        null,
        OrdenPropuestas.PUESTO,
        new Paginado(0, 20));
  }

  private Map<UUID, SituacionPropuesta> situaciones() {
    return propuestas.buscar(pendientes(Set.of())).elementos().stream()
        .collect(Collectors.toMap(p -> p.propuesta().getId(), PropuestaEnListado::situacion));
  }

  // A3
  @Test
  void guardarYLeer_conservaLasSugerenciasTambienAlDecidir() {
    Propuesta propuesta = propuestaCon(donacion);
    propuesta.registrarSugerencia(
        new SugerenciaDeAlgoritmo(TipoAlgoritmo.PRIORIDAD_SUBATENDIDOS, 1, 6, true));
    propuesta.registrarSugerencia(
        new SugerenciaDeAlgoritmo(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA, 3, 10, false));
    propuestas.save(propuesta);

    Propuesta leida = propuestas.findById(propuesta.getId()).orElseThrow();
    assertEquals(Set.copyOf(propuesta.getSugerencias()), Set.copyOf(leida.getSugerencias()));

    leida.aceptar("sub-de-ana");
    propuestas.save(leida);
    assertEquals(
        Set.copyOf(propuesta.getSugerencias()),
        Set.copyOf(propuestas.findById(propuesta.getId()).orElseThrow().getSugerencias()));
  }

  // A3: una propuesta de antes de V24 no tiene sugerencias
  @Test
  void leer_unaPropuestaSinSugerencias_devuelveLaListaVacia() {
    Propuesta propuesta = propuestaCon(donacion);
    propuestas.save(propuesta);

    assertEquals(List.of(), propuestas.findById(propuesta.getId()).orElseThrow().getSugerencias());
  }

  // A5
  @Test
  void buscar_calculaLaSituacionDeCadaPendiente() {
    Propuesta una = pidiendo(necesidad, donacion, 15);
    Propuesta otra = pidiendo(otraNecesidad, donacion, 15);
    Propuesta libre = pidiendo(otraNecesidad, otraDonacion, 5);
    propuestas.saveAll(List.of(una, otra));

    assertEquals(SituacionPropuesta.EN_CONFLICTO, situaciones().get(una.getId()));
    assertEquals(SituacionPropuesta.EN_CONFLICTO, situaciones().get(otra.getId()));

    // Al aprobar una, su donación sale del depósito: la otra ya no alcanza
    Propuesta aprobada = propuestas.findById(una.getId()).orElseThrow();
    aprobada.aceptar("sub-de-ana");
    propuestas.save(aprobada);
    jdbc.update(
        "UPDATE donaciones.donacion_independiente SET estado_actual = 'ASIGNACION_REALIZADA'"
            + " WHERE id = ?",
        donacion.getId());
    assertEquals(SituacionPropuesta.SIN_STOCK, situaciones().get(otra.getId()));

    // Sola, la que pide 5 de 10 queda libre... salvo que otra pendiente cubra su misma necesidad
    propuestas.save(libre);
    assertEquals(SituacionPropuesta.EN_CONFLICTO, situaciones().get(libre.getId()));
    propuestas.delete(propuestas.findById(otra.getId()).orElseThrow());
    assertEquals(SituacionPropuesta.LIBRE, situaciones().get(libre.getId()));
  }

  // A5: lo que piden las otras, por donación, y las decididas sin situación
  @Test
  void buscar_informaLoQueQuedaYLoQuePidenLasOtras() {
    Propuesta una = pidiendo(necesidad, donacion, 15);
    Propuesta otra = pidiendo(otraNecesidad, donacion, 12);
    propuestas.saveAll(List.of(una, otra));

    PropuestaEnListado leida =
        propuestas.buscar(pendientes(Set.of())).elementos().stream()
            .filter(p -> p.propuesta().getId().equals(una.getId()))
            .findFirst()
            .orElseThrow();

    assertEquals(
        new StockEnPropuestas(donacion.getId(), 20, 27, 2),
        leida.porDonacion().get(donacion.getId()));
    assertEquals(0, leida.otrasPendientesMismaNecesidad());
  }

  // A6 y A8
  @Test
  void buscar_filtraEnLaBaseYElResumenCoincideConLosTotales() {
    Propuesta deLaSemantica = pidiendo(necesidad, otraDonacion, 5);
    deLaSemantica.registrarSugerencia(
        new SugerenciaDeAlgoritmo(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA, 2, 2, true));
    Propuesta deLasDos = pidiendo(otraNecesidad, donacion, 5);
    deLasDos.registrarSugerencia(
        new SugerenciaDeAlgoritmo(TipoAlgoritmo.PRIORIDAD_SUBATENDIDOS, 1, 1, true));
    deLasDos.registrarSugerencia(
        new SugerenciaDeAlgoritmo(TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA, 1, 2, false));
    Propuesta descartada = pidiendo(otraNecesidad, donacion, 1);
    descartada.rechazar("sub-de-ana");
    propuestas.saveAll(List.of(deLaSemantica, deLasDos, descartada));

    Tramo<PropuestaEnListado> semantica =
        propuestas.buscar(
            new CriterioPropuestas(
                Set.of(),
                Set.of(),
                TipoAlgoritmo.COMPATIBILIDAD_SEMANTICA,
                false,
                null,
                OrdenPropuestas.PUESTO,
                new Paginado(0, 1)));
    assertEquals(2, semantica.total());
    // En el orden de ese algoritmo: primero su puesto 1
    assertEquals(deLasDos.getId(), semantica.elementos().getFirst().propuesta().getId());

    Tramo<PropuestaEnListado> ambas =
        propuestas.buscar(
            new CriterioPropuestas(
                Set.of(), Set.of(), null, true, null, OrdenPropuestas.PUESTO, new Paginado(0, 20)));
    assertEquals(List.of(deLasDos.getId()), ids(ambas));

    Tramo<PropuestaEnListado> deLaDonacion =
        propuestas.buscar(
            new CriterioPropuestas(
                Set.of(EstadoPropuesta.PENDIENTE),
                Set.of(),
                null,
                false,
                donacion.getId(),
                OrdenPropuestas.PUESTO,
                new Paginado(0, 20)));
    assertEquals(List.of(deLasDos.getId()), ids(deLaDonacion));
    assertNull(
        propuestas
            .buscar(
                new CriterioPropuestas(
                    Set.of(EstadoPropuesta.DESCARTADA),
                    Set.of(),
                    null,
                    false,
                    null,
                    OrdenPropuestas.RECIENTE,
                    new Paginado(0, 20)))
            .elementos()
            .getFirst()
            .situacion());

    Map<SituacionPropuesta, Long> resumen = propuestas.contarPendientesPorSituacion();
    for (SituacionPropuesta s : SituacionPropuesta.values()) {
      assertEquals(propuestas.buscar(pendientes(Set.of(s))).total(), resumen.get(s));
    }
    assertEquals(2L, resumen.values().stream().mapToLong(Long::longValue).sum());
  }

  // A9
  @Test
  void buscarDonacionesEnPendientes_traeSoloLasPedidasYPrimeroLaQueNoAlcanza() {
    propuestas.saveAll(
        List.of(
            pidiendo(necesidad, otraDonacion, 5),
            pidiendo(necesidad, donacion, 15),
            pidiendo(otraNecesidad, donacion, 15)));

    Tramo<StockEnPropuestas> tramo =
        propuestas.buscarDonacionesEnPendientes(null, false, new Paginado(0, 20));

    assertEquals(2, tramo.total());
    assertEquals(
        List.of(
            new StockEnPropuestas(donacion.getId(), 20, 30, 2),
            new StockEnPropuestas(otraDonacion.getId(), 10, 5, 1)),
        tramo.elementos());
    assertEquals(
        List.of(new StockEnPropuestas(donacion.getId(), 20, 30, 2)),
        propuestas.buscarDonacionesEnPendientes(null, true, new Paginado(0, 20)).elementos());
  }

  private static List<UUID> ids(Tramo<PropuestaEnListado> tramo) {
    return tramo.elementos().stream().map(p -> p.propuesta().getId()).toList();
  }
}
