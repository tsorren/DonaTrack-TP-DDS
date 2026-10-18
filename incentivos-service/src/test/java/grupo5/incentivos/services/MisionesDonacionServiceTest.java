package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.incentivos.dto.MisionDTO;
import grupo5.incentivos.dto.NuevaDonacionRequest;
import grupo5.incentivos.fixtures.DonanteIncentivosMother;
import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.fixtures.IncentivosFixtures;
import grupo5.incentivos.fixtures.MisionMother;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.eventos.AscensoDonante;
import grupo5.incentivos.models.entities.donante.eventos.MisionCompletada;
import grupo5.incentivos.models.entities.misiones.Mision;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import grupo5.incentivos.models.repositories.IDonanteIncentivosRepository;
import grupo5.incentivos.services.mappers.MisionMapper;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class MisionesDonacionServiceTest {

  private MisionesDonacionService service;
  private DonanteIncentivosRepository repository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new MisionesDonacionService(repository, eventPublisher, new MisionMapper());
  }

  @Test
  void procesarDonacion_deberiaRegistrarElEventoEnLasMetricas() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    DonanteIncentivos guardado = repository.findById(donanteId).orElseThrow();
    assertTrue(guardado.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
  }

  @Test
  void procesarDonacion_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    UUID donanteId = UUID.randomUUID();
    NuevaDonacionRequest request = IncentivosFixtures.nuevaDonacion(donanteId);

    assertThrows(BusinessStateException.class, () -> service.procesarDonacion(request));
  }

  @Test
  void procesarDonacion_cuandoCompletaCategoria_deberiaPublicarAscensoDonante() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha = MisionMother.rachaColaborador(1);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());

    boolean publicoAscenso =
        captor.getAllValues().stream().anyMatch(AscensoDonante.class::isInstance);
    assertTrue(publicoAscenso);
  }

  @Test
  void procesarDonacion_cuandoCompletaMisionConInsignia_deberiaPublicarMisionCompletada() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha de Bronce");
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

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
    MisionRacha racha = MisionMother.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));

    service.procesarDonacion(request);

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void procesarDonacionExitosa_cuandoCompletaMision_deberiaPublicarMisionCompletada() {
    UUID donanteId = UUID.randomUUID();
    MisionDonacionesExitosas exitosas = MisionMother.exitosas(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(exitosas));
    repository.save(donante);

    service.procesarDonacionExitosa(IncentivosFixtures.donacionExitosa(donanteId));

    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());

    boolean publicoMision =
        captor.getAllValues().stream().anyMatch(MisionCompletada.class::isInstance);
    assertTrue(publicoMision);
  }

  @Test
  void procesarDonacionExitosa_cuandoNoTieneMisionesDeExitosas_noDeberiaPublicarEventos() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha = MisionMother.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    service.procesarDonacionExitosa(IncentivosFixtures.donacionExitosa(donanteId));

    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void obtenerMisiones_deberiaRetornarListaDeMisionesDelDonante() {
    UUID donanteId = UUID.randomUUID();
    Mision mision = MisionMother.rachaColaborador(3);
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(mision));
    repository.save(donante);

    List<MisionDTO> list = service.obtenerMisiones(donanteId);

    assertEquals(1, list.size());
    assertEquals(mision.getNombre(), list.get(0).nombre());
  }

  @Test
  void
      obtenerMisiones_cuandoMisionCompletadaTieneInsignia_deberiaIncluirLaInsigniaGanadaDelDonante() {
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha de Bronce");
    DonanteIncentivos donante = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    repository.save(donante);

    NuevaDonacionRequest request =
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10));
    service.procesarDonacion(request);

    List<MisionDTO> misiones = service.obtenerMisiones(donanteId);

    assertEquals(1, misiones.size());
    MisionDTO dto = misiones.get(0);
    assertTrue(dto.completada());
    assertNotNull(dto.insignia());
    assertEquals("Racha de Bronce", dto.insignia().nombre());
    // Si viniera solo del molde estático de la misión (fallback), la fecha sería null:
    // que no sea null confirma que se resolvió la InsigniaGanada real del donante.
    assertNotNull(dto.insignia().fechaObtenida());
  }

  @Test
  void verificarRachasVencidas_deberiaProcesarTodosLosDonantesDelRepositorio() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    MisionRacha r1 = MisionMother.rachaColaborador(3);
    MisionRacha r2 = MisionMother.rachaColaborador(3);

    DonanteIncentivos d1 = DonanteIncentivosMother.conMisiones(id1, List.of(r1));
    d1.registrarDonacion(EventoDonacionMother.enFecha(2026, 1, 15));

    DonanteIncentivos d2 = DonanteIncentivosMother.conMisiones(id2, List.of(r2));
    d2.registrarDonacion(EventoDonacionMother.enFecha(2026, 3, 15));

    repository.save(d1);
    repository.save(d2);

    service.verificarRachasVencidas(YearMonth.of(2026, Month.APRIL));

    assertEquals(0, r1.getProgresoActual()); // Venció por saltarse marzo
    assertEquals(1, r2.getProgresoActual()); // Sigue vigente (donó en marzo)
  }

  @Test
  void
      procesarDonacion_cuandoSaveDevuelveOtraInstanciaSinEventos_deberiaPublicarLosEventosDeLaOriginalDespuesDeGuardar() {
    // Con Postgres save() devuelve un agregado nuevo (toDomain) sin eventos de dominio.
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID donanteId = UUID.randomUUID();
    MisionRacha racha =
        MisionMother.rachaConInsignia(CategoriaDonante.COLABORADOR, 1, "Racha de Bronce");
    DonanteIncentivos original = DonanteIncentivosMother.conMisiones(donanteId, List.of(racha));
    when(repo.findById(donanteId)).thenReturn(Optional.of(original));
    when(repo.save(any())).thenReturn(DonanteIncentivosMother.colaboradorSinMisiones(donanteId));
    MisionesDonacionService servicio =
        new MisionesDonacionService(repo, eventPublisher, new MisionMapper());

    servicio.procesarDonacion(
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10)));

    InOrder orden = inOrder(repo, eventPublisher);
    orden.verify(repo).save(original);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    orden.verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
    assertTrue(captor.getAllValues().stream().anyMatch(MisionCompletada.class::isInstance));
    assertTrue(captor.getAllValues().stream().anyMatch(AscensoDonante.class::isInstance));
  }

  @Test
  void
      procesarDonacionExitosa_cuandoSaveDevuelveOtraInstanciaSinEventos_deberiaPublicarLosEventosDeLaOriginalDespuesDeGuardar() {
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    UUID donanteId = UUID.randomUUID();
    MisionDonacionesExitosas exitosas = MisionMother.exitosas(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos original = DonanteIncentivosMother.conMisiones(donanteId, List.of(exitosas));
    when(repo.findById(donanteId)).thenReturn(Optional.of(original));
    when(repo.save(any())).thenReturn(DonanteIncentivosMother.colaboradorSinMisiones(donanteId));
    MisionesDonacionService servicio =
        new MisionesDonacionService(repo, eventPublisher, new MisionMapper());

    servicio.procesarDonacionExitosa(IncentivosFixtures.donacionExitosa(donanteId));

    InOrder orden = inOrder(repo, eventPublisher);
    orden.verify(repo).save(original);
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    orden.verify(eventPublisher, atLeastOnce()).publishEvent(captor.capture());
    assertTrue(captor.getAllValues().stream().anyMatch(MisionCompletada.class::isInstance));
  }

  @Test
  void procesarDonacion_cuandoOtraEscrituraGana_deberiaReleerYAplicarSobreLaVersionNueva() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos versionVieja = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    DonanteIncentivos versionNueva = DonanteIncentivosMother.colaboradorSinMisiones(donanteId);
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    when(repo.findById(donanteId)).thenReturn(Optional.of(versionVieja), Optional.of(versionNueva));
    when(repo.save(any()))
        .thenThrow(new OptimisticLockingFailureException("versión vieja"))
        .thenAnswer(invocacion -> invocacion.getArgument(0));
    MisionesDonacionService conChoque =
        new MisionesDonacionService(repo, eventPublisher, new MisionMapper());

    conChoque.procesarDonacion(
        IncentivosFixtures.nuevaDonacion(donanteId, LocalDate.of(2026, Month.MAY, 10)));

    verify(repo, times(2)).findById(donanteId);
    verify(repo, times(2)).save(any());
    assertTrue(versionNueva.tuvoActividadEnMes(YearMonth.of(2026, Month.MAY)));
  }

  @Test
  void verificarRachasVencidas_cuandoUnDonanteChocaSiempre_deberiaSeguirConLosDemas() {
    UUID idQueChoca = UUID.randomUUID();
    UUID idSinChoque = UUID.randomUUID();
    DonanteIncentivos queChoca = DonanteIncentivosMother.colaboradorSinMisiones(idQueChoca);
    DonanteIncentivos sinChoque = DonanteIncentivosMother.colaboradorSinMisiones(idSinChoque);
    IDonanteIncentivosRepository repo = mock(IDonanteIncentivosRepository.class);
    when(repo.findAll()).thenReturn(List.of(queChoca, sinChoque));
    when(repo.findById(idQueChoca)).thenReturn(Optional.of(queChoca));
    when(repo.findById(idSinChoque)).thenReturn(Optional.of(sinChoque));
    when(repo.save(any()))
        .thenAnswer(
            invocacion -> {
              DonanteIncentivos donante = invocacion.getArgument(0);
              if (donante.getId().equals(idQueChoca)) {
                throw new OptimisticLockingFailureException("versión vieja");
              }
              return donante;
            });
    MisionesDonacionService conChoque =
        new MisionesDonacionService(repo, eventPublisher, new MisionMapper());

    conChoque.verificarRachasVencidas(YearMonth.of(2026, Month.APRIL));

    verify(repo, times(ReintentoPorConcurrencia.INTENTOS)).findById(idQueChoca);
    verify(repo).save(sinChoque);
  }
}
