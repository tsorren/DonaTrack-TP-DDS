package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.fixtures.MisionMotherTest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricasIncentivosServiceTest {

  private MetricasIncentivosService service;
  private DonanteIncentivosRepository repository;

  @Mock private IRankingService rankingService;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new MetricasIncentivosService(repository, rankingService);
  }

  @Test
  void obtenerMetricas_cuandoDonanteTienePosicionEnRanking_deberiaIncluirlaEnDTO() {
    UUID donanteId = UUID.randomUUID();
    MisionDonacionesExitosas mision = MisionMotherTest.exitosas(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(donanteId, List.of(mision));
    donante.registrarDonacion(EventoDonacionMotherTest.enFecha(LocalDate.now()));
    donante.registrarDonacionExitosa(new UUID(0L, 1L));
    repository.save(donante);

    when(rankingService.obtenerPosicionDonante(any())).thenReturn(Optional.of(1));

    MetricasDonanteDTO metricas = service.obtenerMetricas(donanteId);

    assertNotNull(metricas);
    assertEquals(1, metricas.posicionEnRanking());
    assertEquals(1, metricas.misionesCompletadasTotal());
  }

  @Test
  void obtenerMetricas_cuandoDonanteNoTieneRanking_deberiaTenerPosicionNula() {
    UUID donanteId = UUID.randomUUID();
    DonanteIncentivos donante = DonanteIncentivosMotherTest.colaboradorSinMisiones(donanteId);
    repository.save(donante);

    when(rankingService.obtenerPosicionDonante(any())).thenReturn(Optional.empty());

    MetricasDonanteDTO metricas = service.obtenerMetricas(donanteId);

    assertNotNull(metricas);
    assertNull(metricas.posicionEnRanking());
  }

  @Test
  void obtenerMetricas_cuandoDonanteNoExiste_deberiaLanzarExcepcion() {
    UUID donanteId = UUID.randomUUID();
    assertThrows(BusinessStateException.class, () -> service.obtenerMetricas(donanteId));
  }

  @Test
  void obtenerResumenSistema_deberiaCalcularTotalesGlobales() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    DonanteIncentivos d1 = DonanteIncentivosMotherTest.colaboradorSinMisiones(id1);
    DonanteIncentivos d2 = DonanteIncentivosMotherTest.colaboradorSinMisiones(id2);
    d1.registrarDonacion(EventoDonacionMotherTest.enFecha(LocalDate.now()));
    d2.registrarDonacion(EventoDonacionMotherTest.enFecha(LocalDate.now()));
    repository.save(d1);
    repository.save(d2);

    ResumenSistemaDTO resumen = service.obtenerResumenSistema();

    assertNotNull(resumen);
    assertEquals(2, resumen.totalDonantes());
    assertEquals(2, resumen.donantesMesActual());
  }

  @Test
  void obtenerResumenSistema_conRepositorioVacio_deberiaRetornarMetricasEnCero() {
    ResumenSistemaDTO resumen = service.obtenerResumenSistema();

    assertNotNull(resumen);
    assertEquals(0, resumen.totalDonantes());
    assertEquals(0, resumen.donantesMesActual());
    assertEquals(0, resumen.totalMisionesCompletadas());
  }
}
