package grupo5.donaciones.infrastructure.persistencia.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.donaciones.fixtures.DonacionIndependienteMother;
import grupo5.donaciones.fixtures.NecesidadMother;
import grupo5.donaciones.infrastructure.persistencia.mapper.NecesidadesPersistenciaMapper;
import grupo5.donaciones.models.entities.necesidades.EstadoNecesidad;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadExtraordinaria;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import grupo5.donaciones.models.entities.necesidades.PeriodoNecesidad;
import grupo5.donaciones.models.entities.necesidades.TipoNecesidad;
import grupo5.donaciones.models.repositories.INecesidadesRepository;
import grupo5.donaciones.models.repositories.impl.DonacionesIndependientesRepositoryEnMemoria;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.Paginado;
import grupo5.donaciones.models.repositories.listados.Tramo;
import grupo5.donaciones.services.impl.AcumuladosDeAsignacion;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
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

  private static final LocalDate HOY = LocalDate.now(ZoneId.systemDefault());
  private static final Paginado PRIMERA = new Paginado(0, 20);

  @Autowired private INecesidadesRepository repository;
  @Autowired private JdbcTemplate jdbc;

  // Con las FK de V6 y V8 la necesidad apunta a una subcategoría y a una entidad que existan
  private UUID subcategoriaId;
  private UUID entidadId;

  @BeforeEach
  void limpiar() {
    // Las donaciones asignadas de las búsquedas de vigentes apuntan a la necesidad (RESTRICT)
    jdbc.update("DELETE FROM donaciones.item_donacion_independiente");
    jdbc.update("DELETE FROM donaciones.donacion_independiente");
    jdbc.update("DELETE FROM donaciones.item_donacion_normalizado");
    jdbc.update("DELETE FROM donaciones.donacion");
    jdbc.update("DELETE FROM donaciones.donante");
    jdbc.update("DELETE FROM donaciones.humana");
    repository.deleteAll();
    jdbc.update("DELETE FROM donaciones.subcategoria");
    jdbc.update("DELETE FROM donaciones.categoria");
    jdbc.update("DELETE FROM donaciones.entidad_beneficiaria");
    jdbc.update("DELETE FROM donaciones.persona");
    subcategoriaId = crearSubcategoria();
    entidadId = crearEntidad();
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

  @Test
  void guardarYLeer_necesidadExtraordinaria_conservaSusCampos() {
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
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);

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
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    repository.save(original);

    original.generarNuevoPeriodo();
    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(2, leida.getPeriodos().size());
  }

  @Test
  void buscarNecesidadesPorEntidad_devuelveSoloLasDeEsaEntidad() {
    UUID entidadA = entidadId;
    UUID entidadB = crearEntidad();
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
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10);
    repository.save(original);

    original.desactivar();
    repository.save(original);

    assertFalse(repository.findById(original.getId()).orElseThrow().isActiva());
  }

  @Test
  void guardarYLeer_necesidadMensual_conservaElMesSinConvertirloEnTreintaDias() {
    NecesidadRecurrente original = NecesidadMother.recurrenteMensual(subcategoriaId, 50);
    original.asociarAEntidad(entidadId);

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
    original.asociarAEntidad(entidadId);

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
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10);
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
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10);
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
        NecesidadMother.extraordinaria(entidadId, UUID.randomUUID(), 10);

    assertThrows(DataIntegrityViolationException.class, () -> repository.save(huerfana));
  }

  @Test
  void existsBySubcategoriaId_indicaSiHayNecesidadesQueLaUsan() {
    repository.save(NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10));

    assertTrue(repository.existsBySubcategoriaId(subcategoriaId));
    assertFalse(repository.existsBySubcategoriaId(crearSubcategoria()));
  }

  @Test
  void findByActivaTrueAndRecurrenteTrue_filtraEnLaBase() {
    NecesidadRecurrente activa = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 10);
    NecesidadRecurrente inactiva = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 10);
    inactiva.desactivar();
    repository.save(activa);
    repository.save(inactiva);
    repository.save(NecesidadMother.extraordinaria(entidadId, subcategoriaId, 10));

    List<Necesidad> resultado = repository.findByActivaTrueAndRecurrenteTrue();

    assertEquals(List.of(activa.getId()), resultado.stream().map(Necesidad::getId).toList());
  }

  @Test
  void guardar_conEntidadInexistente_fallaPorClaveForanea() {
    NecesidadExtraordinaria huerfana =
        NecesidadMother.extraordinaria(UUID.randomUUID(), subcategoriaId, 10);

    assertThrows(DataIntegrityViolationException.class, () -> repository.save(huerfana));
  }

  @Test
  void guardar_sinEntidad_fallaPorqueEsObligatoria() {
    NecesidadExtraordinaria sinEntidad = NecesidadMother.extraordinaria(subcategoriaId, 10);

    assertThrows(DataIntegrityViolationException.class, () -> repository.save(sinEntidad));
  }

  @Test
  void guardarYLeer_recurrente_conservaElIdDeCadaPeriodoAlRenovar() {
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    repository.save(original);
    original.generarNuevoPeriodo();
    repository.save(original);

    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(
        original.getPeriodos().stream().map(PeriodoNecesidad::id).toList(),
        leida.getPeriodos().stream().map(PeriodoNecesidad::id).toList());
    assertEquals(original.periodoActualId(), leida.periodoActualId());
  }

  @Test
  void leer_recurrente_ordenaLosPeriodosPorFechaFinAunqueSeHayanInsertadoDesordenados() {
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    repository.save(original);
    LocalDate finDelGuardado = original.obtenerPeriodoActual().fechaFin();
    // Se inserta después (queda último en la tabla) un período que termina antes
    UUID anterior = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.periodo_necesidad"
            + " (id, necesidad_id, fecha_inicio, fecha_fin, cantidad_objetivo)"
            + " VALUES (?, ?, ?, ?, 50)",
        anterior,
        original.getId(),
        finDelGuardado.minusDays(30).atStartOfDay(),
        finDelGuardado.minusDays(14).atStartOfDay());

    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    // El período actual es el de mayor fechaFin, no el último que devolvió la tabla
    assertEquals(original.periodoActualId(), leida.periodoActualId());
    assertEquals(anterior, leida.getPeriodos().getFirst().id());
  }

  @Test
  void guardar_dosRenovacionesDelMismoPeriodo_laSegundaSeRechazaYNoSeDuplicaElPeriodo() {
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    repository.save(original);
    NecesidadRecurrente delPlanificadorA =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();
    NecesidadRecurrente delPlanificadorB =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();
    delPlanificadorA.generarNuevoPeriodo();
    delPlanificadorB.generarNuevoPeriodo();

    repository.save(delPlanificadorA);
    BusinessStateException error =
        assertThrows(BusinessStateException.class, () -> repository.save(delPlanificadorB));

    assertEquals(ErrorCatalog.NECESIDAD_MODIFICADA_CONCURRENTEMENTE, error.getError());
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();
    // El período original más uno solo: el de la segunda renovación no se guardó
    assertEquals(2, leida.getPeriodos().size());
  }

  @Test
  void necesidadLeidaDeLaBase_sabeCuantoSeLeAsigno() {
    // El problema que resuelve DTI-06: antes, leída de Postgres volvía con lista vacía (acumulado
    // 0)
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 50);
    repository.save(original);
    DonacionesIndependientesRepositoryEnMemoria donaciones =
        new DonacionesIndependientesRepositoryEnMemoria();
    AcumuladosDeAsignacion acumulados = new AcumuladosDeAsignacion(donaciones);
    donaciones.save(DonacionIndependienteMother.asignada(30, original));

    Necesidad leida = repository.findById(original.getId()).orElseThrow();

    assertEquals(30, acumulados.asignadoA(leida));
    assertFalse(acumulados.estaSatisfecha(leida));
  }

  // ===================== HB-18 (V17) =====================

  @Test
  void guardarYLeer_extraordinaria_conservaFechaLimiteYUltimoCambio() {
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 80);
    original.actualizar(null, 90);

    repository.save(original);
    NecesidadExtraordinaria leida =
        (NecesidadExtraordinaria) repository.findById(original.getId()).orElseThrow();

    assertEquals(NecesidadMother.FECHA_LIMITE, leida.getFechaLimite());
    assertEquals(80, leida.getCantidadAnterior());
    assertEquals(original.getFechaUltimoCambio(), leida.getFechaUltimoCambio());
  }

  @Test
  void leer_extraordinariaConFechaLimiteNula_quedaVigenteYAdmiteAsignaciones() {
    NecesidadExtraordinaria original =
        NecesidadMother.extraordinaria(entidadId, subcategoriaId, 80);
    repository.save(original);
    // Una fila anterior a V17: la columna queda nula
    jdbc.update(
        "UPDATE donaciones.necesidad SET fecha_limite = NULL WHERE id = ?", original.getId());

    NecesidadExtraordinaria leida =
        (NecesidadExtraordinaria) repository.findById(original.getId()).orElseThrow();

    assertNull(leida.getFechaLimite());
    LocalDate dentroDeUnAnio = LocalDate.now().plusYears(1);
    assertEquals(EstadoNecesidad.VIGENTE, leida.estado(0, dentroDeUnAnio));
    assertTrue(leida.admiteAsignaciones(dentroDeUnAnio));
  }

  @Test
  void guardar_cambioDeCantidadEnUnaRecurrente_actualizaSoloElObjetivoDelPeriodoEnCurso() {
    NecesidadRecurrente original = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 40);
    original.generarNuevoPeriodo();
    repository.save(original);
    NecesidadRecurrente leida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    leida.actualizar(null, 45);
    repository.save(leida);
    NecesidadRecurrente releida =
        (NecesidadRecurrente) repository.findById(original.getId()).orElseThrow();

    assertEquals(
        List.of(40, 45),
        releida.getPeriodos().stream().map(PeriodoNecesidad::cantidadObjetivo).toList());
    assertEquals(40, releida.getCantidadAnterior());
  }

  // ===================== SPEC-11, G2: buscarVigentes =====================

  private NecesidadExtraordinaria extraordinaria(String descripcion, UUID subcategoria, int pide) {
    NecesidadExtraordinaria necesidad =
        new NecesidadExtraordinaria(subcategoria, pide, descripcion, NecesidadMother.FECHA_LIMITE);
    necesidad.asociarAEntidad(entidadId);
    repository.save(necesidad);
    return necesidad;
  }

  private UUID categoriaDe(UUID subcategoria) {
    return jdbc.queryForObject(
        "SELECT id_categoria FROM donaciones.subcategoria WHERE id = ?", UUID.class, subcategoria);
  }

  /**
   * Una donación asignada a la necesidad por SQL, con las filas que exigen las FK; {@code periodo}
   * nulo en una extraordinaria. {@code noRecibido} nulo si llegó todo.
   */
  private void asignar(Necesidad necesidad, UUID periodo, int cantidad, Integer noRecibido) {
    UUID donante = UUID.randomUUID();
    jdbc.update("INSERT INTO donaciones.persona (id, tipo_persona) VALUES (?, 'HUMANA')", donante);
    jdbc.update(
        "INSERT INTO donaciones.humana (id, nombre, apellido) VALUES (?, 'Ana', 'Prueba')",
        donante);
    jdbc.update("INSERT INTO donaciones.donante (id, activo) VALUES (?, true)", donante);
    UUID donacion = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.donacion (id, id_donante, descripcion, fecha, estado_donacion)"
            + " VALUES (?, ?, 'Donación de prueba', now(), 'SEGMENTADA')",
        donacion,
        donante);
    UUID normalizado = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO donaciones.item_donacion_normalizado (id, id_donacion, cantidad,"
            + " bien_descripcion, peso_unitario, volumen_unitario, id_subcategoria,"
            + " confianza_normalizacion, estado_normalizacion)"
            + " VALUES (?, ?, ?, 'Fideos', 1, 1, ?, 1.0, 'ACEPTADO')",
        normalizado,
        donacion,
        cantidad,
        necesidad.getSubcategoriaId());
    UUID independiente = UUID.randomUUID();
    String columnas =
        "INSERT INTO donaciones.donacion_independiente (id, id_donacion, estado_actual,"
            + " fecha_registro, id_necesidad, id_beneficiario_asignado";
    if (periodo == null) {
      jdbc.update(
          columnas + ") VALUES (?, ?, 'ASIGNACION_REALIZADA', now(), ?, ?)",
          independiente,
          donacion,
          necesidad.getId(),
          entidadId);
    } else {
      jdbc.update(
          columnas + ", id_periodo) VALUES (?, ?, 'ASIGNACION_REALIZADA', now(), ?, ?, ?)",
          independiente,
          donacion,
          necesidad.getId(),
          entidadId,
          periodo);
    }
    if (noRecibido != null) {
      jdbc.update(
          "UPDATE donaciones.donacion_independiente SET cantidad_no_recibida = ? WHERE id = ?",
          noRecibido,
          independiente);
    }
    jdbc.update(
        "INSERT INTO donaciones.item_donacion_independiente (id, id_donacion_independiente,"
            + " id_item_normalizado, cantidad, bien_descripcion, orden)"
            + " VALUES (?, ?, ?, ?, 'Fideos', 0)",
        UUID.randomUUID(),
        independiente,
        normalizado,
        cantidad);
  }

  private List<UUID> vigentes(CriterioNecesidades criterio) {
    return repository.buscarVigentes(criterio).elementos().stream().map(Necesidad::getId).toList();
  }

  private static CriterioNecesidades sinFiltros(Paginado paginado) {
    return new CriterioNecesidades(null, null, null, null, HOY, paginado);
  }

  @Test
  void buscarVigentes_dejaAfueraLasInactivasLasVencidasYLasSatisfechas() {
    NecesidadExtraordinaria vigente = extraordinaria("Arroz", subcategoriaId, 10);
    NecesidadExtraordinaria inactiva = extraordinaria("Arroz de baja", subcategoriaId, 10);
    inactiva.desactivar();
    repository.save(inactiva);
    NecesidadExtraordinaria vencida = extraordinaria("Arroz vencido", subcategoriaId, 10);
    jdbc.update(
        "UPDATE donaciones.necesidad SET fecha_limite = ? WHERE id = ?",
        HOY.minusDays(1),
        vencida.getId());
    NecesidadExtraordinaria satisfecha = extraordinaria("Arroz cubierto", subcategoriaId, 3);
    asignar(satisfecha, null, 3, null);
    // Asignó 3 pero no llegó 1: cuenta 2 y sigue pidiendo
    NecesidadExtraordinaria incompleta = extraordinaria("Arroz incompleto", subcategoriaId, 3);
    asignar(incompleta, null, 3, 1);

    Tramo<Necesidad> tramo = repository.buscarVigentes(sinFiltros(PRIMERA));

    assertEquals(
        List.of(vigente.getId(), incompleta.getId()),
        tramo.elementos().stream().map(Necesidad::getId).toList());
    assertEquals(2, tramo.total());
  }

  @Test
  void buscarVigentes_unaRecurrenteSeSatisfaceSoloConLoDeSuPeriodoActual() {
    NecesidadRecurrente cubierta = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 4);
    repository.save(cubierta);
    asignar(cubierta, cubierta.periodoActualId(), 4, null);
    NecesidadRecurrente renovada = NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 4);
    UUID periodoAnterior = renovada.periodoActualId();
    renovada.generarNuevoPeriodo();
    repository.save(renovada);
    // Lo del período que ya cerró no cuenta para el actual
    asignar(renovada, periodoAnterior, 4, null);

    assertEquals(List.of(renovada.getId()), vigentes(sinFiltros(PRIMERA)));
  }

  @Test
  void buscarVigentes_filtraPorEntidadSubcategoriaCategoriaYTipo() {
    UUID otraSubcategoria = crearSubcategoria();
    NecesidadExtraordinaria deFideos = extraordinaria("Fideos", subcategoriaId, 10);
    NecesidadExtraordinaria deLaOtra = extraordinaria("Otra", otraSubcategoria, 10);
    NecesidadRecurrente recurrente =
        NecesidadMother.recurrenteSemanal(entidadId, subcategoriaId, 10);
    repository.save(recurrente);
    UUID otraEntidad = crearEntidad();
    NecesidadExtraordinaria ajena =
        new NecesidadExtraordinaria(
            subcategoriaId, 10, "Fideos ajenos", NecesidadMother.FECHA_LIMITE);
    ajena.asociarAEntidad(otraEntidad);
    repository.save(ajena);

    assertEquals(
        List.of(deFideos.getId(), ajena.getId(), recurrente.getId(), deLaOtra.getId()),
        vigentes(sinFiltros(PRIMERA)));
    assertEquals(
        List.of(deFideos.getId(), recurrente.getId(), deLaOtra.getId()),
        vigentes(new CriterioNecesidades(entidadId, null, null, null, HOY, PRIMERA)));
    assertEquals(
        List.of(deLaOtra.getId()),
        vigentes(new CriterioNecesidades(null, null, otraSubcategoria, null, HOY, PRIMERA)));
    assertEquals(
        List.of(deLaOtra.getId()),
        vigentes(
            new CriterioNecesidades(
                null, categoriaDe(otraSubcategoria), null, null, HOY, PRIMERA)));
    assertEquals(
        List.of(recurrente.getId()),
        vigentes(
            new CriterioNecesidades(null, null, null, TipoNecesidad.RECURRENTE, HOY, PRIMERA)));
  }

  @Test
  void buscarVigentes_cortaLaPaginaEnLaBaseYCuentaTodas() {
    extraordinaria("A", subcategoriaId, 10);
    NecesidadExtraordinaria b = extraordinaria("B", subcategoriaId, 10);
    NecesidadExtraordinaria c = extraordinaria("C", subcategoriaId, 10);
    extraordinaria("D", subcategoriaId, 10);

    Tramo<Necesidad> segunda = repository.buscarVigentes(sinFiltros(new Paginado(1, 2)));

    assertEquals(
        List.of(c.getId()), segunda.elementos().stream().map(Necesidad::getId).limit(1).toList());
    assertEquals(2, segunda.elementos().size());
    assertEquals(4, segunda.total());
    assertEquals(List.of(b.getId()), vigentes(sinFiltros(new Paginado(1, 1))));
    assertTrue(repository.buscarVigentes(sinFiltros(new Paginado(9, 2))).elementos().isEmpty());
  }
}
