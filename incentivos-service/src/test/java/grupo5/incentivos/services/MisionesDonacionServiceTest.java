package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.incentivos.dto.DonacionExitosaRequest;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.insignias.Insignia;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MisionesDonacionServiceTest {

  @Mock private ApplicationEventPublisher eventPublisher;

  private MisionesDonacionService service;
  private DonanteIncentivosRepository repository;

  private static final LocalDate HOY = LocalDate.of(2026, Month.JUNE, 17);

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new MisionesDonacionService(repository, eventPublisher);
  }

  @Test
  void procesarDonacion_deberiaRegistrarElEventoEnLasMetricas() {
    UUID id = new UUID(0L, 1L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    DonanteIncentivos actualizado = repository.findById(id).orElseThrow();
    assertEquals(1, actualizado.getMetricas().getTotalDonacionesHistoricas());
  }

  @Test
  void procesarDonacion_deberiaLanzarExcepcionSiDonanteNoExiste() {
    UUID id = new UUID(0L, 99L);
    NuevaDonacionRequest request = new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY);
    assertThrows(RecursoNoEncontradoException.class, () -> service.procesarDonacion(request));
  }

  @Test
  void procesarDonacion_deberiaNotificarAscensoAlSubirCategoria() {
    UUID id = new UUID(0L, 44L);
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test", List.of(racha));
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(eventPublisher, atLeastOnce()).publishEvent(any(AscensoDonante.class));
  }

  @Test
  void procesarDonacion_deberiaNotificarCuandoSeCompletaMisionConInsignia() {
    UUID id = new UUID(0L, 10L);
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    mision.setInsignia(new Insignia("Racha", "desc", "/img.png"));
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test", List.of(mision));
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    ArgumentCaptor<MisionCompletada> captor = ArgumentCaptor.forClass(MisionCompletada.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
    assertTrue(captor.getAllValues().stream().anyMatch(e -> e.insignia() != null));
  }

  @Test
  void procesarDonacion_noDeberiaNotificarSiLaMisionNoSeCompletaAun() {
    UUID id = new UUID(0L, 11L);
    MisionRacha mision = new MisionRacha(CategoriaDonante.COLABORADOR, 2);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test", List.of(mision));
    repository.save(donante);

    service.procesarDonacion(new NuevaDonacionRequest(id, List.of("arroz"), 1, HOY));

    verify(eventPublisher, never()).publishEvent(any(MisionCompletada.class));
  }

  @Test
  void procesarDonacionExitosa_deberiaNotificarCuandoSeCompletaUnaMision() {
    UUID id = new UUID(0L, 42L);
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    Insignia insignia = new Insignia("Primera Entrega", "Primera donación exitosa", "/img.png");
    mision.setInsignia(insignia);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test", List.of(mision));
    repository.save(donante);

    service.procesarDonacionExitosa(new DonacionExitosaRequest(id, new UUID(0L, 99L)));

    verify(eventPublisher, atLeastOnce()).publishEvent(any(MisionCompletada.class));
  }

  @Test
  void obtenerMisiones_deberiaRetornarLasMisionesDelDonante() {
    UUID id = new UUID(0L, 50L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);

    List<MisionDTO> misiones = service.obtenerMisiones(id);

    assertNotNull(misiones);
    assertFalse(misiones.isEmpty());
  }

  @Test
  void verificarRachasVencidas_deberiaProcesarTodosLosDonantes() {
    UUID id = new UUID(0L, 51L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);

    assertDoesNotThrow(() -> service.verificarRachasVencidas(YearMonth.of(2026, Month.JULY)));
  }
}
