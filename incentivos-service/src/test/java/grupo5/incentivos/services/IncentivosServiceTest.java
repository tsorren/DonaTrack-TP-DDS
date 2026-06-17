package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.incentivos.dto.InsigniaDTO;
import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncentivosServiceTest {

  @Mock private NotificacionesClient notificacionesClient;
  @Mock private RankingService rankingService;
  @Mock private N8nClient n8nClient;

  private IncentivosService service;
  private DonanteIncentivosRepository repository;
  private MisionFactory misionFactory;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    misionFactory = new MisionFactory();
    service =
        new IncentivosService(
            repository, misionFactory, notificacionesClient, rankingService, n8nClient);
  }

  @Test
  void registrarDonante_deberiaCrearPerfilConMisiones() {
    service.registrarDonante(1L, "Test");

    DonanteIncentivos donante = repository.buscarPorId(1L).orElseThrow();

    assertNotNull(donante);
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());
    assertFalse(donante.getMisiones().isEmpty());
  }

  @Test
  void registrarDonante_deberiaSerIdempotente() {
    service.registrarDonante(1L, "Test");
    service.registrarDonante(1L, "Test");

    assertEquals(1, repository.listarTodos().size());
  }

  @Test
  void procesarDonacion_deberiaRegistrarElEventoEnLasMetricas() {
    service.registrarDonante(1L, "Test");

    service.procesarDonacion(1L, "Test", eventoHoy(1L));

    DonanteIncentivos donante = repository.buscarPorId(1L).orElseThrow();
    assertEquals(1, donante.getMetricas().getTotalDonacionesHistoricas());
  }

  @Test
  void procesarDonacion_deberiaRegistrarDonanteAutomaticamenteSiNoExiste() {
    service.procesarDonacion(99L, "Nuevo", eventoHoy(1L));

    assertTrue(repository.buscarPorId(99L).isPresent());
  }

  @Test
  void procesarDonacion_noDeberiaNotificarCuandoLaMisionNoSeCompletaAun() {
    DonanteIncentivos donante = new DonanteIncentivos(43L, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 5);
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    service.procesarDonacion(43L, "Test", eventoHoy(1L));

    verify(notificacionesClient, never()).notificarMisionCumplida(any(), any(), any());
  }

  @Test
  void procesarDonacion_deberiaNotificarAscensoAlSubirCategoria() {
    DonanteIncentivos donante = new DonanteIncentivos(44L, "Test");
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    donante.getMisiones().add(racha);
    repository.guardar(donante);

    service.procesarDonacion(44L, "Test", eventoHoy(1L));

    verify(notificacionesClient, atLeastOnce()).notificarAscensoCategoria(anyLong(), anyString());
  }

  @Test
  void procesarDonacion_deberiaNotificarN8nCuandoSeCompletaMisionConInsignia() {
    DonanteIncentivos donante = new DonanteIncentivos(10L, "Test");
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    mision.setInsignia(new Insignia("Racha", "desc", "/img.png"));
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    service.procesarDonacion(10L, "Test", eventoHoy(1L));

    verify(n8nClient, atLeastOnce())
        .publicarInsigniaGanada(anyLong(), anyString(), anyString(), anyString());
  }

  @Test
  void procesarDonacion_noDeberiaNotificarN8nSiLaMisionNoSeCompletaAun() {
    DonanteIncentivos donante = new DonanteIncentivos(11L, "Test");
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 2);
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    service.procesarDonacion(11L, "Test", eventoHoy(1L));

    verify(n8nClient, never()).publicarInsigniaGanada(any(), any(), any(), any());
  }

  @Test
  void procesarDonacionExitosa_deberiaNotificarCuandoSeCompletaUnaMision() {
    DonanteIncentivos donante = new DonanteIncentivos(42L, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    Insignia insignia = new Insignia("Primera Entrega", "Primera donación exitosa", "/img.png");
    mision.setInsignia(insignia);
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    service.procesarDonacionExitosa(42L, 99L);

    verify(notificacionesClient, atLeastOnce())
        .notificarMisionCumplida(anyLong(), anyString(), anyString());
  }

  @Test
  void procesarDonacionExitosa_deberiaNotificarN8nCuandoSeCompletaMisionConInsignia() {
    DonanteIncentivos donante = new DonanteIncentivos(20L, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    mision.setInsignia(new Insignia("Primera Entrega", "desc", "/img.png"));
    donante.getMisiones().add(mision);
    repository.guardar(donante);

    service.procesarDonacionExitosa(20L, 100L);

    verify(n8nClient, atLeastOnce())
        .publicarInsigniaGanada(anyLong(), anyString(), anyString(), anyString());
  }

  @Test
  void obtenerDonante_deberiaLanzarExcepcionSiNoExiste() {
    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.obtenerDonante(999L));
    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaOcultarInsignia() {
    DonanteIncentivos donante = new DonanteIncentivos(30L, "Test");
    donante.otorgarInsignia(new Insignia("Explorador", "desc", "/img.png"));
    repository.guardar(donante);

    service.configurarVisibilidadInsignia(30L, "Explorador", false);

    Insignia insignia =
        repository.buscarPorId(30L).orElseThrow().getInsignias().stream()
            .filter(i -> i.getNombre().equals("Explorador"))
            .findFirst()
            .orElseThrow();
    assertFalse(insignia.isVisible());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaLanzarExcepcionSiInsigniaNoExiste() {
    service.registrarDonante(31L, "Test");

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(31L, "NoExiste", false));
    assertEquals(ErrorCatalog.INSIGNIA_NO_ENCONTRADA, ex.getError());
  }

  @Test
  void darDeBaja_deberiaEliminarAlDonante() {
    service.registrarDonante(40L, "Test");

    service.darDeBaja(40L);

    assertFalse(repository.buscarPorId(40L).isPresent());
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcionSiDonanteNoExiste() {
    assertThrows(BusinessStateException.class, () -> service.darDeBaja(999L));
  }

  @Test
  void obtenerMisiones_deberiaRetornarLasMisionesDelDonante() {
    service.registrarDonante(50L, "Test");

    List<MisionDTO> misiones = service.obtenerMisiones(50L);

    assertNotNull(misiones);
    assertFalse(misiones.isEmpty());
  }

  @Test
  void obtenerInsignias_deberiaRetornarListaVaciaSinInsignias() {
    service.registrarDonante(51L, "Test");

    List<InsigniaDTO> insignias = service.obtenerInsignias(51L);

    assertNotNull(insignias);
    assertTrue(insignias.isEmpty());
  }

  @Test
  void obtenerMetricas_deberiaRetornarMetricasCorrectas() {
    service.registrarDonante(60L, "Test");

    MetricasDonanteDTO metricas = service.obtenerMetricas(60L);

    assertNotNull(metricas);
    assertEquals(60L, metricas.donanteId());
    assertEquals(CategoriaDonante.COLABORADOR, metricas.categoria());
    assertEquals(0, metricas.totalDonacionesHistoricas());
  }

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 17);

  private EventoDonacion eventoHoy(long donacionId) {
    return EventoDonacion.builder()
        .donacionId(donacionId)
        .fecha(HOY)
        .cantidadBienes(1)
        .categorias(List.of("arroz"))
        .build();
  }
}
