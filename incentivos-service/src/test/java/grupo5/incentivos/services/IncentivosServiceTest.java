package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.incentivos.dto.*;
import grupo5.incentivos.infrastructure.N8nClient;
import grupo5.incentivos.infrastructure.NotificacionesClient;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.insignias.Insignia;
import grupo5.incentivos.models.entities.donante.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.donante.misiones.MisionRacha;
import grupo5.incentivos.models.entities.inactividad.CriterioInactividad;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
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

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 17);

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    misionFactory = new MisionFactory();
    List<CriterioInactividad> criterios = List.of();
    service =
        new IncentivosService(
            repository, misionFactory, notificacionesClient, rankingService, n8nClient, criterios);
  }

  @Test
  void registrarDonante_deberiaCrearPerfilConMisiones() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));

    DonanteIncentivos donante = repository.findById(id).orElseThrow();

    assertNotNull(donante);
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());
    assertFalse(donante.getMisiones().isEmpty());
  }

  @Test
  void registrarDonante_deberiaSerIdempotente() {
    UUID id = UUID.randomUUID();
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));
    service.registrarDonante(new RegistrarDonanteRequest(id, UUID.randomUUID(), "Test"));

    assertEquals(1, repository.findAll().size());
  }

  @Test
  void procesarDonacion_deberiaRegistrarElEventoEnLasMetricas() {
    UUID id = new UUID(0L, 1L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    DonanteIncentivos donante = repository.findById(id).orElseThrow();
    assertEquals(1, donante.getMetricas().getTotalDonacionesHistoricas());
  }

  @Test
  void procesarDonacion_deberiaLanzarExcepcionSiDonanteNoExiste() {
    UUID id = new UUID(0L, 99L);
    NuevaDonacionRequest request = new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY);
    assertThrows(RecursoNoEncontradoException.class, () -> service.procesarDonacion(request));
  }

  @Test
  void procesarDonacion_noDeberiaNotificarCuandoLaMisionNoSeCompletaAun() {
    UUID id = new UUID(0L, 43L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 5);
    donante.getMisiones().add(mision);
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(notificacionesClient, never()).notificarMisionCumplida(any(), any(), any());
  }

  @Test
  void procesarDonacion_deberiaNotificarAscensoAlSubirCategoria() {
    UUID id = new UUID(0L, 44L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    donante.getMisiones().add(racha);
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(notificacionesClient, atLeastOnce())
        .notificarAscensoCategoria(any(), anyString(), anyString());
  }

  @Test
  void procesarDonacion_deberiaNotificarN8nCuandoSeCompletaMisionConInsignia() {
    UUID id = new UUID(0L, 10L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    mision.setInsignia(new Insignia("Racha", "desc", "/img.png"));
    donante.getMisiones().add(mision);
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(n8nClient, atLeastOnce())
        .publicarInsigniaGanada(any(), anyString(), anyString(), anyString());
  }

  @Test
  void procesarDonacion_noDeberiaNotificarN8nSiLaMisionNoSeCompletaAun() {
    UUID id = new UUID(0L, 11L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 2);
    donante.getMisiones().add(mision);
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(n8nClient, never()).publicarInsigniaGanada(any(), any(), any(), any());
  }

  @Test
  void procesarDonacionExitosa_deberiaNotificarCuandoSeCompletaUnaMision() {
    UUID id = new UUID(0L, 42L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    Insignia insignia = new Insignia("Primera Entrega", "Primera donación exitosa", "/img.png");
    mision.setInsignia(insignia);
    donante.getMisiones().add(mision);
    repository.save(donante);

    service.procesarDonacionExitosa(new DonacionExitosaRequest(id, new UUID(0L, 99L)));

    verify(notificacionesClient, atLeastOnce())
        .notificarMisionCumplida(any(), anyString(), anyString());
  }

  @Test
  void procesarDonacionExitosa_deberiaNotificarN8nCuandoSeCompletaMisionConInsignia() {
    UUID id = new UUID(0L, 20L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    mision.setInsignia(new Insignia("Primera Entrega", "desc", "/img.png"));
    donante.getMisiones().add(mision);
    repository.save(donante);

    service.procesarDonacionExitosa(new DonacionExitosaRequest(id, new UUID(0L, 100L)));

    verify(n8nClient, atLeastOnce())
        .publicarInsigniaGanada(any(), anyString(), anyString(), anyString());
  }

  @Test
  void obtenerDonante_deberiaLanzarExcepcionSiNoExiste() {
    UUID uuid = new UUID(0L, 999L);
    BusinessStateException ex =
        assertThrows(BusinessStateException.class, () -> service.obtenerDonante(uuid));
    assertEquals(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO, ex.getError());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaOcultarInsignia() {
    UUID id = new UUID(0L, 30L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    donante.otorgarInsignia(new Insignia("Explorador", "desc", "/img.png"));
    repository.save(donante);

    service.configurarVisibilidadInsignia(id, "Explorador", false);

    Insignia insignia =
        repository.findById(id).orElseThrow().getInsignias().stream()
            .filter(i -> i.nombre().equals("Explorador"))
            .findFirst()
            .orElseThrow();
    assertFalse(insignia.visible());
  }

  @Test
  void configurarVisibilidadInsignia_deberiaLanzarExcepcionSiInsigniaNoExiste() {
    UUID id = new UUID(0L, 31L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    BusinessStateException ex =
        assertThrows(
            BusinessStateException.class,
            () -> service.configurarVisibilidadInsignia(id, "NoExiste", false));
    assertEquals(ErrorCatalog.INSIGNIA_NO_ENCONTRADA, ex.getError());
  }

  @Test
  void darDeBaja_deberiaEliminarAlDonante() {
    UUID id = new UUID(0L, 40L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    service.darDeBaja(id);

    assertFalse(repository.findById(id).isPresent());
  }

  @Test
  void darDeBaja_deberiaLanzarExcepcionSiDonanteNoExiste() {
    UUID uuid = new UUID(0L, 999L);
    assertThrows(BusinessStateException.class, () -> service.darDeBaja(uuid));
  }

  @Test
  void obtenerMisiones_deberiaRetornarLasMisionesDelDonante() {
    UUID id = new UUID(0L, 50L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    List<MisionDTO> misiones = service.obtenerMisiones(id);

    assertNotNull(misiones);
    assertFalse(misiones.isEmpty());
  }

  @Test
  void obtenerInsignias_deberiaRetornarListaVaciaSinInsignias() {
    UUID id = new UUID(0L, 51L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    List<InsigniaDTO> insignias = service.obtenerInsignias(id);

    assertNotNull(insignias);
    assertTrue(insignias.isEmpty());
  }

  @Test
  void obtenerMetricas_deberiaRetornarMetricasCorrectas() {
    UUID id = new UUID(0L, 60L);
    service.registrarDonante(new RegistrarDonanteRequest(id, id, "Test"));

    MetricasDonanteDTO metricas = service.obtenerMetricas(id);

    assertNotNull(metricas);
    assertEquals(id, metricas.donanteId());
    assertEquals(CategoriaDonante.COLABORADOR, metricas.categoria());
    assertEquals(0, metricas.totalDonacionesHistoricas());
  }
}
