package grupo5.incentivos.infrastructure.persistencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import grupo5.common.testing.DisabledIfDockerUnavailable;
import grupo5.incentivos.dto.RankingMensualDTO;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.fixtures.MisionMother;
import grupo5.incentivos.fixtures.RankingMensualMother;
import grupo5.incentivos.infrastructure.IN8nClient;
import grupo5.incentivos.infrastructure.clients.NotificacionesFeignClient;
import grupo5.incentivos.infrastructure.persistencia.adapters.DonanteIncentivosRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.adapters.RankingRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.mappers.DonanteIncentivosPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.mappers.MisionPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.mappers.RankingPersistenciaMapper;
import grupo5.incentivos.models.entities.donante.CambioCategoria;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.insignias.InsigniaGanada;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionCompletitud;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionHabilDonador;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.GestorDeRankings;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IRankingRepository;
import grupo5.incentivos.services.RankingService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  MisionPersistenciaMapper.class,
  DonanteIncentivosPersistenciaMapper.class,
  RankingPersistenciaMapper.class,
  DonanteIncentivosRepositoryJpaAdapter.class,
  RankingRepositoryJpaAdapter.class
})
// Sin transacción de test: en producción los adapters no son transaccionales, cada operación del
// repo corre en su propia transacción y el mapeo a dominio ocurre afuera (detecta Lazy* y el
// orden INSERT/DELETE del ranking).
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
@DisabledIfDockerUnavailable
class RepositoriosJpaTest {

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

  @Autowired private IDonanteIncentivosRepository donanteRepository;
  @Autowired private IRankingRepository rankingRepository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void limpiar() {
    rankingRepository.deleteAll();
    donanteRepository.deleteAll();
  }

  @Test
  void deberiaUsarAdaptadoresJpaEnLugarDeMemoria() {
    assertInstanceOf(DonanteIncentivosRepositoryJpaAdapter.class, donanteRepository);
    assertInstanceOf(RankingRepositoryJpaAdapter.class, rankingRepository);
  }

  @Test
  void deberiaPersistirYRecuperarElEstadoCompletoDelDonante() {
    UUID id = UUID.randomUUID();
    DonanteIncentivos original = donanteConEstadoCompleto(id);

    donanteRepository.save(original);
    // Se lee fuera de toda transacción: si alguna colección fuera LAZY fallaría acá.
    DonanteIncentivos leido = donanteRepository.findById(id).orElseThrow();

    verificarDatosBasicos(original, leido);
    verificarInsignias(leido);
    verificarMisiones(leido);
    verificarMetricas(original, leido);
  }

  private static void verificarDatosBasicos(DonanteIncentivos original, DonanteIncentivos leido) {
    assertEquals(original.getIdPersona(), leido.getIdPersona());
    assertEquals("Ana", leido.getNombre());
    assertEquals(CategoriaDonante.COLABORADOR, leido.getCategoria());
    assertEquals(original.getFechaRegistro(), leido.getFechaRegistro());
  }

  private static void verificarInsignias(DonanteIncentivos leido) {
    List<InsigniaGanada> insignias = leido.getInsignias();
    assertEquals(2, insignias.size());
    assertEquals("Gran Aporte Test", insignias.get(0).nombre());
    assertEquals("Extra", insignias.get(1).nombre());
    assertFalse(insignias.get(1).visible());
    assertEquals(LocalDate.of(2026, Month.JUNE, 1), insignias.get(1).fechaObtenida());
  }

  private static void verificarMisiones(DonanteIncentivos leido) {
    List<Mision> misiones = leido.getMisiones();
    assertEquals(4, misiones.size());
    MisionRacha racha = assertInstanceOf(MisionRacha.class, misiones.get(0));
    assertEquals(2, racha.getProgresoActual());
    assertEquals(YearMonth.of(2026, Month.JUNE), racha.getUltimoMesDonado());
    assertEquals("Racha Test", racha.getInsignia().nombre());
    MisionCompletitud completitud = assertInstanceOf(MisionCompletitud.class, misiones.get(1));
    assertEquals(Set.of("alimentos", "ropa"), completitud.getCategoriasDonadas());
    MisionHabilDonador habil = assertInstanceOf(MisionHabilDonador.class, misiones.get(2));
    assertTrue(habil.isCompletada());
    assertEquals(LocalDate.of(2026, Month.JUNE, 11), habil.getFechaCompletada());
    MisionDonacionesExitosas exitosas =
        assertInstanceOf(MisionDonacionesExitosas.class, misiones.get(3));
    assertEquals(1, exitosas.getProgresoActual());
    assertEquals(LocalDate.of(2026, Month.JUNE, 12), exitosas.getFechaUltimoDonacion());
  }

  private static void verificarMetricas(DonanteIncentivos original, DonanteIncentivos leido) {
    assertEquals(2, leido.getMetricas().getTotalDonacionesHistoricas());
    assertEquals(1, leido.getMetricas().getTotalDonacionesExitosas());
    assertEquals(LocalDate.of(2026, Month.JUNE, 11), leido.getMetricas().getUltimaDonacion());
    assertEquals(
        Map.of(YearMonth.of(2026, Month.MAY), 1L, YearMonth.of(2026, Month.JUNE), 1L),
        leido.getMetricas().donacionesPorPeriodo());
    assertEquals(
        Set.copyOf(original.getMetricas().getOrganizacionesAyudadas()),
        leido.getMetricas().getOrganizacionesAyudadas());
  }

  @Test
  void deberiaMantenerLosIdsDeMisionesAlGuardarDeNuevoSinBorrarFilas() {
    DonanteIncentivos nuevo =
        new DonanteIncentivos(
            UUID.randomUUID(), UUID.randomUUID(), "Ana", LocalDate.of(2026, Month.JANUARY, 1));
    donanteRepository.save(nuevo);

    DonanteIncentivos guardado = donanteRepository.findById(nuevo.getId()).orElseThrow();
    Set<UUID> idsAntes = idsDeMisiones(guardado);
    assertEquals(6, idsAntes.size());

    guardado.registrarDonacion(EventoDonacionMother.enFecha(2026, 6, 10));
    Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    estadisticas.clear();
    donanteRepository.save(guardado);

    DonanteIncentivos recargado = donanteRepository.findById(nuevo.getId()).orElseThrow();
    assertEquals(idsAntes, idsDeMisiones(recargado));
    assertEquals(
        1,
        recargado.getMisiones().stream()
            .filter(
                m -> m instanceof MisionRacha && m.getCategoria() == CategoriaDonante.COLABORADOR)
            .findFirst()
            .orElseThrow()
            .getProgresoActual());
    assertEquals(0, estadisticas.getEntityDeleteCount());
    assertEquals(
        6,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM incentivos.mision WHERE donante_id = ?",
            Integer.class,
            nuevo.getId()));
    // El conteo mensual se actualiza en su fila: no se duplica ni se borra.
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM incentivos.donante_donaciones_por_periodo WHERE donante_id = ?",
            Integer.class,
            nuevo.getId()));
  }

  @Test
  void noDeberiaPersistirEventosDeDominioYElOriginalLosConserva() {
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorConMisionRacha(1);
    donante.registrarDonacion(EventoDonacionMother.valido());
    assertFalse(donante.getDomainEvents().isEmpty());

    DonanteIncentivos devuelto = donanteRepository.save(donante);

    assertTrue(devuelto.getDomainEvents().isEmpty());
    assertFalse(donante.getDomainEvents().isEmpty());

    DonanteIncentivos recargado = donanteRepository.findById(donante.getId()).orElseThrow();
    assertEquals(CategoriaDonante.SOSTENEDOR, recargado.getCategoria());
    assertEquals(1, recargado.getHistorialCategorias().size());
    CambioCategoria cambio = recargado.getHistorialCategorias().get(0);
    assertEquals(CategoriaDonante.COLABORADOR, cambio.getAnterior());
    assertEquals(CategoriaDonante.SOSTENEDOR, cambio.getNueva());
    assertEquals(LocalDate.now(ZoneId.systemDefault()), cambio.getFecha());
  }

  @Test
  void deberiaBuscarDonantePorIdPersona() {
    UUID persona = UUID.randomUUID();
    DonanteIncentivos donante =
        DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID(), persona);
    donanteRepository.save(donante);

    assertEquals(donante.getId(), donanteRepository.findByIdPersona(persona).orElseThrow().getId());
    assertTrue(donanteRepository.findByIdPersona(UUID.randomUUID()).isEmpty());
    assertTrue(donanteRepository.findByIdPersona(null).isEmpty());
  }

  @Test
  void deberiaActualizarElNombreSinCargarElAgregado() {
    DonanteIncentivos donante = donanteConEstadoCompleto(UUID.randomUUID());
    donanteRepository.save(donante);

    assertTrue(donanteRepository.actualizarNombre(donante.getId(), "Nuevo Nombre"));

    DonanteIncentivos leido = donanteRepository.findById(donante.getId()).orElseThrow();
    assertEquals("Nuevo Nombre", leido.getNombre());
    assertEquals(4, leido.getMisiones().size());
    assertFalse(donanteRepository.actualizarNombre(UUID.randomUUID(), "X"));
  }

  @Test
  void deberiaActualizarLaVisibilidadDeUnaInsignia() {
    DonanteIncentivos donante = donanteConEstadoCompleto(UUID.randomUUID());
    donanteRepository.save(donante);

    assertTrue(
        donanteRepository.actualizarVisibilidadInsignia(
            donante.getId(), "Gran Aporte Test", false));
    assertTrue(donanteRepository.actualizarVisibilidadInsignia(donante.getId(), "Extra", true));

    List<InsigniaGanada> insignias =
        donanteRepository.findById(donante.getId()).orElseThrow().getInsignias();
    assertFalse(insignias.get(0).visible());
    assertTrue(insignias.get(1).visible());
    assertFalse(
        donanteRepository.actualizarVisibilidadInsignia(donante.getId(), "Inexistente", true));
    assertFalse(donanteRepository.actualizarVisibilidadInsignia(UUID.randomUUID(), "Extra", true));
  }

  @Test
  void deberiaEliminarElDonanteYSusHijosPorId() {
    DonanteIncentivos donante = donanteConEstadoCompleto(UUID.randomUUID());
    donanteRepository.save(donante);

    assertTrue(donanteRepository.eliminarPorId(donante.getId()));

    assertTrue(donanteRepository.findById(donante.getId()).isEmpty());
    for (String tabla :
        List.of(
            "mision",
            "donante_insignia_ganada",
            "donante_historial_categoria",
            "donante_organizacion_ayudada",
            "donante_donaciones_por_periodo")) {
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM incentivos." + tabla + " WHERE donante_id = ?",
              Integer.class,
              donante.getId()),
          tabla);
    }
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM incentivos.mision_categorias_donadas", Integer.class));
    assertFalse(donanteRepository.eliminarPorId(donante.getId()));
  }

  @Test
  void deberiaRechazarDosDonantesConLaMismaPersona() {
    UUID persona = UUID.randomUUID();
    donanteRepository.save(
        DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID(), persona));
    DonanteIncentivos otro =
        DonanteIncentivosMother.colaboradorSinMisiones(UUID.randomUUID(), persona);

    assertThrows(DataIntegrityViolationException.class, () -> donanteRepository.save(otro));
  }

  @Test
  void deberiaBorrarLosHijosDelDonanteYConservarElRankingHistorico() {
    YearMonth periodo = YearMonth.of(2026, Month.MAY);
    DonanteIncentivos donante =
        DonanteIncentivosMother.conMisionesCompletadasEnMes(UUID.randomUUID(), "Ana", periodo, 2);
    donanteRepository.save(donante);
    RankingMensual ranking = new RankingMensual(periodo);
    ranking.agregarEntrada(new EntradaRanking(1, donante.getId(), "Ana", 2));
    rankingRepository.save(ranking);

    donanteRepository.delete(donante);

    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM incentivos.mision WHERE donante_id = ?",
            Integer.class,
            donante.getId()));
    RankingMensual conservado = rankingRepository.findByPeriodo(periodo).orElseThrow();
    assertEquals(donante.getId(), conservado.getEntradas().get(0).getDonanteId());
  }

  @Test
  void deberiaGuardarElPeriodoComoTextoYBuscarPorYearMonth() {
    YearMonth periodo = YearMonth.of(2026, Month.AUGUST);
    RankingMensual ranking = RankingMensualMother.conNEntradas(periodo, 3);

    rankingRepository.save(ranking);

    assertEquals(
        "2026-08",
        jdbc.queryForObject("SELECT periodo FROM incentivos.ranking_mensual", String.class));
    RankingMensual encontrado = rankingRepository.findByPeriodo(periodo).orElseThrow();
    assertEquals(ranking.getId(), encontrado.getId());
    assertEquals(
        List.of(1, 2, 3),
        encontrado.getEntradas().stream().map(EntradaRanking::getPosicion).toList());
    assertTrue(rankingRepository.findByPeriodo(YearMonth.of(2026, Month.SEPTEMBER)).isEmpty());
  }

  @Test
  void deberiaGuardarElUltimoMesDeLaRachaComoTexto() {
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorConMisionRacha(3);
    donante.registrarDonacion(EventoDonacionMother.enFecha(2026, 5, 10));

    donanteRepository.save(donante);

    assertEquals(
        "2026-05",
        jdbc.queryForObject(
            "SELECT ultimo_mes_donado FROM incentivos.mision WHERE tipo_mision = 'RACHA'",
            String.class));
  }

  @Test
  void deberiaRecalcularElMismoPeriodoDosVecesSinViolarElUnicoDePeriodo() {
    YearMonth periodo = YearMonth.of(2026, Month.MAY);
    donanteRepository.save(
        DonanteIncentivosMother.conMisionesCompletadasEnMes(new UUID(0L, 10L), "Ana", periodo, 2));
    donanteRepository.save(
        DonanteIncentivosMother.conMisionesCompletadasEnMes(new UUID(0L, 11L), "Beto", periodo, 1));
    RankingService servicio =
        new RankingService(
            donanteRepository, rankingRepository, mock(IN8nClient.class), new GestorDeRankings());

    servicio.calcularYPersistir(periodo);
    RankingMensualDTO segundo = servicio.calcularYPersistir(periodo);

    assertEquals(1, rankingRepository.count());
    assertEquals(2, segundo.entradas().size());
    assertEquals("Ana", segundo.entradas().get(0).nombreDonante());
    RankingMensual guardado = rankingRepository.findByPeriodo(periodo).orElseThrow();
    assertEquals(
        List.of("Ana", "Beto"),
        guardado.getEntradas().stream().map(EntradaRanking::getNombreDonante).toList());
  }

  private static Set<UUID> idsDeMisiones(DonanteIncentivos donante) {
    return donante.getMisiones().stream().map(Mision::getId).collect(Collectors.toSet());
  }

  private static DonanteIncentivos donanteConEstadoCompleto(UUID id) {
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 3, "Racha Test");
    racha.setNumeroMision(1);
    MisionCompletitud completitud =
        MisionMother.completitudConInsignia(CategoriaDonante.COLABORADOR, 3, "Explorador Test");
    completitud.setNumeroMision(2);
    MisionHabilDonador habil =
        MisionMother.habilDonadorConInsignia(CategoriaDonante.SOSTENEDOR, 5, "Gran Aporte Test");
    habil.setNumeroMision(3);
    MisionDonacionesExitosas exitosas =
        MisionMother.exitosasConInsignia(CategoriaDonante.TRANSFORMADOR, 3, "Impacto Test");
    exitosas.setNumeroMision(4);

    DonanteIncentivos donante =
        new DonanteIncentivos(
            id, UUID.randomUUID(), "Ana", List.of(racha, completitud, habil, exitosas));

    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 5, 10));
    racha.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 6, 10));
    completitud.evaluarProgreso(
        donante,
        EventoDonacionMother.conCategorias(
            LocalDate.of(2026, Month.JUNE, 10), List.of("Alimentos", "Ropa")));
    habil.evaluarProgreso(
        donante, EventoDonacionMother.conCantidadBienes(LocalDate.of(2026, Month.JUNE, 11), 5));
    exitosas.evaluarProgreso(donante, EventoDonacionMother.enFecha(2026, 6, 12));
    exitosas.evaluarProgresoExitoso(donante);

    donante
        .getMetricas()
        .registrarDonacion(
            EventoDonacion.builder()
                .donacionId(UUID.randomUUID())
                .fecha(LocalDate.of(2026, Month.MAY, 10))
                .cantidadBienes(3)
                .categorias(List.of("alimentos", "ropa"))
                .build());
    donante.getMetricas().registrarDonacion(EventoDonacionMother.enFecha(2026, 6, 11));
    donante.getMetricas().registrarDonacionExitosa(UUID.randomUUID());

    donante.otorgarInsignia(
        new Insignia("Extra", "Descripcion", "/extra.png"), LocalDate.of(2026, Month.JUNE, 1));
    donante.configurarVisibilidadInsignia("Extra", false);
    return donante;
  }
}
