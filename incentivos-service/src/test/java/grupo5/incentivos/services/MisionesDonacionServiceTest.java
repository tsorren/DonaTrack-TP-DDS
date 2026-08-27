package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.fixtures.IncentivosFixturesTest;
import grupo5.incentivos.fixtures.MisionMotherTest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.misiones.Mision;
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

  private MisionesDonacionService service;
  private DonanteIncentivosRepository repository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new MisionesDonacionService(repository, eventPublisher);
  }

  @Test
  void procesarDonacion_deberiaRegistrarElEventoEnLasMetricas() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMotherTest.colaboradorSinMisiones(donanteId);
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixturesTest.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    DonanteIncentivos guardado = repository.findById(donanteId).orElseThrow();
    assertTrue(guardado.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
  }

  @Test
  void procesarDonacion_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    UUID donanteId = UUID.randomUUID();
    NuevaDonacionRequest request = IncentivosFixturesTest.nuevaDonacion(donanteId);

    assertThrows(BusinessStateException.class, () -> service.procesarDonacion(request));
  }

  @Test
  void procesarDonacion_cuandoCompletaCategoria_deberiaPublicarAscensoDonante() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha = MisionMotherTest.rachaColaborador(1);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixturesTest.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());

    boolean publicoAscenso =
        captor.getAllValues().stream().anyMatch(e -> e instanceof AscensoDonante);
    assertTrue(publicoAscenso);
  }

  @Test
  void procesarDonacion_cuandoCompletaMisionConInsignia_deberiaPublicarMisionCompletada() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha =
        MisionMotherTest.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha de Bronce");
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixturesTest.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());

    boolean publicoMision =
        captor.getAllValues().stream()
            .anyMatch(
                e ->
                    e instanceof MisionCompletada mc
                        && mc.insignia() != null
                        && "Racha de Bronce".equals(mc.insignia().nombre()));
    assertTrue(publicoMision);
  }

  @Test
  void procesarDonacion_cuandoNoCompletaMision_noDeberiaPublicarEventos() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixturesTest.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void procesarDonacionExitosa_cuandoCompletaMision_deberiaPublicarMisionCompletada() {
    UUID donanteId = UUID.randomUUID();
    MisionDonacionesExitosas exitosas = MisionMotherTest.exitosas(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos donante =
        DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(exitosas));
    repository.save(donante);

    service.procesarDonacionExitosa(IncentivosFixturesTest.donacionExitosa(donanteId));

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());

    boolean publicoMision =
        captor.getAllValues().stream().anyMatch(e -> e instanceof MisionCompletada);
    assertTrue(publicoMision);
  }

  @Test
  void procesarDonacionExitosa_cuandoNoTieneMisionesDeExitosas_noDeberiaPublicarEventos() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha = MisionMotherTest.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    service.procesarDonacionExitosa(IncentivosFixturesTest.donacionExitosa(donanteId));

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void obtenerMisiones_deberiaRetornarListaDeMisionesDelDonante() {
    UUID donanteId = UUID.randomUUID();
    Mision mision = MisionMotherTest.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(mision));
    repository.save(donante);

    List<MisionDTO> list = service.obtenerMisiones(donanteId);

    assertEquals(1, list.size());
    assertEquals(mision.getNombre(), list.get(0).nombre());
  }

  @Test
  void verificarRachasVencidas_deberiaProcesarTodosLosDonantesDelRepositorio() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    MisionRacha r1 = MisionMotherTest.rachaColaborador(3);
    MisionRacha r2 = MisionMotherTest.rachaColaborador(3);

    DonanteIncentivos d1 = DonanteIncentivosMotherTest.conMisiones(id1, List.of(r1));
    d1.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 1, 15));

    DonanteIncentivos d2 = DonanteIncentivosMotherTest.conMisiones(id2, List.of(r2));
    d2.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 3, 15));

    repository.save(d1);
    repository.save(d2);

    service.verificarRachasVencidas(YearMonth.of(2026, Month.APRIL));

    assertEquals(0, r1.getProgresoActual()); // Venció por saltarse marzo
    assertEquals(1, r2.getProgresoActual()); // Sigue vigente (donó en marzo)
  }
}
