package grupo5.incentivos.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.incentivos.dto.DonanteRegistradoDTO;
import grupo5.incentivos.dto.RegistrarDonanteRequest;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.IncentivosFixtures;
import grupo5.incentivos.fixtures.MisionMother;
import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import grupo5.incentivos.infrastructure.persistencia.adapters.DonanteIncentivosRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.mappers.DonanteIncentivosPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.mappers.MisionPersistenciaMapper;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.EventoDonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.services.GestionDonanteService;
import grupo5.incentivos.services.InsigniasService;
import grupo5.incentivos.services.MisionesDonacionService;
import grupo5.incentivos.services.mappers.MisionMapper;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * Los services de incentivos sobre los adapters JPA y una base Postgres real. Los services son los
 * beans de producción (sin {@code new}), pero ninguno es {@code @Transactional}: cada llamada al
 * repositorio corre en su propia transacción, igual que en producción.
 */
@DataJpaTest
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  MisionPersistenciaMapper.class,
  DonanteIncentivosPersistenciaMapper.class,
  DonanteIncentivosRepositoryJpaAdapter.class,
  GestionDonanteService.class,
  InsigniasService.class,
  MisionesDonacionService.class,
  MisionMapper.class,
  ServicesSobrePostgresTest.EventosObservados.class
})
// Sin transacción de test: si no, el commit de cada operación del repo no ocurriría hasta el final
// del test y no se podría comprobar qué hay en la base cuando se publica un evento.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class ServicesSobrePostgresTest {

  private static final LocalDate FECHA_DONACION = LocalDate.of(2026, Month.MAY, 10);
  private static final List<String> TABLAS_HIJAS_CON_DATOS =
      List.of(
          "mision",
          "donante_insignia_ganada",
          "donante_organizacion_ayudada",
          "donante_donaciones_por_periodo");

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("donatrack")
          .withUsername("incentivos_user")
          .withPassword("inc_pass_2026")
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("init-db/01-init-schemas-roles.sql"),
              "/docker-entrypoint-initdb.d/01-init-schemas-roles.sql");

  // @EnableFeignClients de la clase principal registra este cliente también en el slice JPA.
  @MockitoBean private NotificacionesFeignClient notificacionesFeignClient;

  @Autowired private GestionDonanteService gestionDonanteService;
  @Autowired private InsigniasService insigniasService;
  @Autowired private MisionesDonacionService misionesDonacionService;
  @Autowired private IDonanteIncentivosRepository donanteRepository;
  @Autowired private EventosObservados eventos;
  @Autowired private JdbcTemplate jdbc;

  /**
   * Hace de {@code NotificacionesIncentivosListener}: al recibir cada evento de dominio mira qué
   * hay en la base en ese instante, para comprobar que el estado ya estaba persistido.
   */
  public static class EventosObservados {

    record Observado(
        EventoDonanteIncentivos evento,
        CategoriaDonante categoriaPersistida,
        int misionesCompletadasPersistidas) {}

    private final IDonanteIncentivosRepository repository;
    private final List<Observado> observados = new CopyOnWriteArrayList<>();
    private volatile boolean fallar;

    public EventosObservados(IDonanteIncentivosRepository repository) {
      this.repository = repository;
    }

    @EventListener
    public void alRecibir(EventoDonanteIncentivos evento) {
      DonanteIncentivos persistido = repository.findById(evento.donanteId()).orElseThrow();
      observados.add(
          new Observado(evento, persistido.getCategoria(), persistido.misionesCompletadas()));
      if (fallar) {
        throw new IllegalStateException("fallo simulado de un listener de notificaciones");
      }
    }

    void reiniciar() {
      observados.clear();
      fallar = false;
    }

    List<Observado> observados() {
      return List.copyOf(observados);
    }

    void fallarAlRecibir() {
      fallar = true;
    }
  }

  @BeforeEach
  void limpiar() {
    donanteRepository.deleteAll();
    eventos.reiniciar();
  }

  // ---------------------------------------------------------------- modificarDonante

  @Test
  void modificarDonante_cuandoExiste_deberiaCambiarSoloElNombreYConservarElEstado() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.conEstadoCompleto(id));

    gestionDonanteService.modificarDonante(id, IncentivosFixtures.modificarDonante("Nuevo Nombre"));

    DonanteIncentivos leido = donanteRepository.findById(id).orElseThrow();
    assertEquals("Nuevo Nombre", leido.getNombre());
    assertEquals(4, leido.getMisiones().size());
    assertEquals(2, leido.getInsignias().size());
    assertEquals(2, leido.getMetricas().getTotalDonacionesHistoricas());
  }

  @Test
  void modificarDonante_cuandoNoExiste_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();
    var request = IncentivosFixtures.modificarDonante("X");

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> gestionDonanteService.modificarDonante(id, request));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  // ---------------------------------------------------------------- darDeBaja

  @Test
  void darDeBaja_cuandoTieneHijos_deberiaEliminarDonanteYTodasSusFilas() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.conEstadoCompleto(id));
    // Si algún hijo estuviera vacío, el 0 posterior no probaría nada.
    for (String tabla : TABLAS_HIJAS_CON_DATOS) {
      assertTrue(contarPorDonante(tabla, id) > 0, "debería haber filas en " + tabla);
    }
    assertTrue(
        jdbc.queryForObject(
                "SELECT COUNT(*) FROM incentivos.mision_categorias_donadas", Integer.class)
            > 0);

    gestionDonanteService.darDeBaja(id);

    assertTrue(donanteRepository.findById(id).isEmpty());
    assertEquals(0, contarPorDonante("donante_incentivos", id, "id"));
    for (String tabla : TABLAS_HIJAS_CON_DATOS) {
      assertEquals(0, contarPorDonante(tabla, id), tabla);
    }
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM incentivos.mision_categorias_donadas", Integer.class));
  }

  @Test
  void darDeBaja_cuandoYaSeDioDeBaja_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.colaboradorSinMisiones(id));
    gestionDonanteService.darDeBaja(id);

    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> gestionDonanteService.darDeBaja(id));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  // ---------------------------------------------------------------- visibilidad de insignias

  @Test
  void configurarVisibilidadInsignia_cuandoExiste_deberiaPersistirElCambio() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.conEstadoCompleto(id));

    insigniasService.configurarVisibilidadInsignia(id, "Gran Aporte Test", false);
    insigniasService.configurarVisibilidadInsignia(id, "Extra", true);

    List<InsigniaGanada> insignias = donanteRepository.findById(id).orElseThrow().getInsignias();
    assertFalse(visibilidadDe(insignias, "Gran Aporte Test"));
    assertTrue(visibilidadDe(insignias, "Extra"));
  }

  @Test
  void configurarVisibilidadInsignia_cuandoDonanteNoExiste_deberiaLanzarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> insigniasService.configurarVisibilidadInsignia(id, "Extra", true));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void
      configurarVisibilidadInsignia_cuandoDonanteNoExisteYNombreEsBlank_deberiaPriorizarDonanteNoEncontrado() {
    UUID id = UUID.randomUUID();

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> insigniasService.configurarVisibilidadInsignia(id, "  ", true));

    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void
      configurarVisibilidadInsignia_cuandoNombreEsBlank_deberiaLanzarInsigniaSinNombreSinTocarLaBase() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.conEstadoCompleto(id));

    ValidationException blank =
        assertThrows(
            ValidationException.class,
            () -> insigniasService.configurarVisibilidadInsignia(id, "   ", true));
    ValidationException nulo =
        assertThrows(
            ValidationException.class,
            () -> insigniasService.configurarVisibilidadInsignia(id, null, true));

    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, blank.getError());
    assertEquals(ErrorCatalog.INSIGNIA_SIN_NOMBRE, nulo.getError());
    List<InsigniaGanada> insignias = donanteRepository.findById(id).orElseThrow().getInsignias();
    assertTrue(visibilidadDe(insignias, "Gran Aporte Test"));
    assertFalse(visibilidadDe(insignias, "Extra"));
  }

  @Test
  void configurarVisibilidadInsignia_cuandoInsigniaNoExiste_deberiaLanzarInsigniaNoEncontrada() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(DonanteIncentivosMother.conEstadoCompleto(id));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> insigniasService.configurarVisibilidadInsignia(id, "Inexistente", true));

    assertEquals(ErrorCatalog.INSIGNIA_NO_ENCONTRADA, ex.getError());
  }

  // ---------------------------------------------------------------- misiones y eventos

  @Test
  void procesarDonacion_cuandoCompletaMision_deberiaPublicarEventosDespuesDeQuedarPersistido() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(donanteConRachaDeUnMes(id));

    misionesDonacionService.procesarDonacion(IncentivosFixtures.nuevaDonacion(id, FECHA_DONACION));

    List<EventosObservados.Observado> observados = eventos.observados();
    assertTrue(
        observados.stream().anyMatch(o -> o.evento() instanceof MisionCompletada),
        "debería publicarse MisionCompletada");
    assertTrue(
        observados.stream().anyMatch(o -> o.evento() instanceof AscensoDonante),
        "debería publicarse AscensoDonante");
    // Cuando el listener corrió, la base ya tenía la misión completada y la categoría nueva.
    observados.forEach(
        o -> {
          assertEquals(CategoriaDonante.SOSTENEDOR, o.categoriaPersistida());
          assertEquals(1, o.misionesCompletadasPersistidas());
        });
  }

  @Test
  void
      procesarDonacionExitosa_cuandoCompletaMision_deberiaPublicarEventosDespuesDeQuedarPersistido() {
    UUID id = UUID.randomUUID();
    MisionDonacionesExitosas exitosas = MisionMother.exitosas(CategoriaDonante.COLABORADOR, 1);
    donanteRepository.save(DonanteIncentivosMother.conMisiones(id, List.of(exitosas)));

    misionesDonacionService.procesarDonacionExitosa(IncentivosFixtures.donacionExitosa(id));

    List<EventosObservados.Observado> observados = eventos.observados();
    assertTrue(observados.stream().anyMatch(o -> o.evento() instanceof MisionCompletada));
    observados.forEach(o -> assertEquals(1, o.misionesCompletadasPersistidas()));
  }

  @Test
  void procesarDonacion_cuandoUnListenerDeNotificacionFalla_deberiaConservarElAvanceYaPersistido() {
    UUID id = UUID.randomUUID();
    donanteRepository.save(donanteConRachaDeUnMes(id));
    eventos.fallarAlRecibir();
    var donacion = IncentivosFixtures.nuevaDonacion(id, FECHA_DONACION);

    // Hoy el fallo de un listener sale hacia el llamador; lo que fija este test es que el avance
    // ya estaba confirmado en la base y no se revierte.
    assertThrows(
        IllegalStateException.class, () -> misionesDonacionService.procesarDonacion(donacion));

    DonanteIncentivos leido = donanteRepository.findById(id).orElseThrow();
    assertEquals(CategoriaDonante.SOSTENEDOR, leido.getCategoria());
    assertEquals(1, leido.misionesCompletadas());
    assertTrue(leido.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
  }

  // ---------------------------------------------------------------- registrarDonante

  @Test
  void registrarDonante_cuandoYaExiste_deberiaDevolverloSinModificarlo() {
    UUID id = UUID.randomUUID();
    UUID persona = UUID.randomUUID();
    gestionDonanteService.registrarDonante(IncentivosFixtures.registrarDonante(id, persona, "Ana"));

    DonanteRegistradoDTO repetido =
        gestionDonanteService.registrarDonante(
            IncentivosFixtures.registrarDonante(id, persona, "Otro Nombre"));

    assertEquals(id, repetido.donanteId());
    assertEquals(1, donanteRepository.count());
    assertEquals("Ana", donanteRepository.findById(id).orElseThrow().getNombre());
  }

  @Test
  void registrarDonante_cuandoOtroDonanteTieneLaMismaPersona_deberiaRelanzarLaViolacion() {
    UUID persona = UUID.randomUUID();
    gestionDonanteService.registrarDonante(
        IncentivosFixtures.registrarDonante(UUID.randomUUID(), persona, "Ana"));
    RegistrarDonanteRequest conflicto =
        IncentivosFixtures.registrarDonante(UUID.randomUUID(), persona, "Beto");

    assertThrows(
        DataIntegrityViolationException.class,
        () -> gestionDonanteService.registrarDonante(conflicto));

    assertEquals(1, donanteRepository.count());
  }

  @Test
  void registrarDonante_cuandoSeInvocaConcurrentemente_deberiaSerIdempotente() throws Exception {
    int hilos = 6;
    UUID id = UUID.randomUUID();
    RegistrarDonanteRequest request =
        IncentivosFixtures.registrarDonante(id, UUID.randomUUID(), "Ana");
    CyclicBarrier largada = new CyclicBarrier(hilos);
    ExecutorService pool = Executors.newFixedThreadPool(hilos);
    try {
      List<Future<DonanteRegistradoDTO>> resultados = new ArrayList<>();
      for (int i = 0; i < hilos; i++) {
        resultados.add(
            pool.submit(
                () -> {
                  largada.await(30, TimeUnit.SECONDS);
                  return gestionDonanteService.registrarDonante(request);
                }));
      }
      for (Future<DonanteRegistradoDTO> resultado : resultados) {
        // get() relanza cualquier excepción que haya escapado del service.
        assertEquals(id, resultado.get(60, TimeUnit.SECONDS).donanteId());
      }
    } finally {
      pool.shutdownNow();
    }

    assertEquals(1, donanteRepository.count());
    assertEquals(1, contarPorDonante("donante_incentivos", id, "id"));
    assertEquals(6, contarPorDonante("mision", id));
  }

  // ---------------------------------------------------------------- helpers

  private int contarPorDonante(String tabla, UUID donanteId) {
    return contarPorDonante(tabla, donanteId, "donante_id");
  }

  private int contarPorDonante(String tabla, UUID donanteId, String columna) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM incentivos." + tabla + " WHERE " + columna + " = ?",
        Integer.class,
        donanteId);
  }

  private static boolean visibilidadDe(List<InsigniaGanada> insignias, String nombre) {
    return insignias.stream()
        .filter(i -> i.nombre().equals(nombre))
        .findFirst()
        .orElseThrow()
        .visible();
  }

  /**
   * Una sola misión de racha de un mes: la primera donación la completa y asciende de categoría.
   */
  private static DonanteIncentivos donanteConRachaDeUnMes(UUID id) {
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha de Bronce");
    return DonanteIncentivosMother.conMisiones(id, List.of(racha));
  }
}
