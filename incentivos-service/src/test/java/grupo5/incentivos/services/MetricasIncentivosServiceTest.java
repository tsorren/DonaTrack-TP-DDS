package grupo5.incentivos.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import grupo5.incentivos.dto.MetricasDonanteDTO;
import grupo5.incentivos.dto.ResumenSistemaDTO;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.repositories.DonanteIncentivosRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricasIncentivosServiceTest {

  @Mock private IRankingService rankingService;

  private MetricasIncentivosService service;
  private DonanteIncentivosRepository repository;

  @BeforeEach
  void setUp() {
    repository = new DonanteIncentivosRepository();
    service = new MetricasIncentivosService(repository, rankingService);
  }

  @Test
  void obtenerMetricas_deberiaRetornarMetricasCorrectas() {
    UUID id = new UUID(0L, 60L);
    DonanteIncentivos donante = new DonanteIncentivos(id, id, "Test");
    repository.save(donante);
    when(rankingService.obtenerPosicionDonante(id)).thenReturn(Optional.of(1));

    MetricasDonanteDTO metricas = service.obtenerMetricas(id);

    assertNotNull(metricas);
    assertEquals(id, metricas.donanteId());
    assertEquals(CategoriaDonante.COLABORADOR, metricas.categoria());
    assertEquals(0, metricas.totalDonacionesHistoricas());
    assertEquals(1, metricas.posicionEnRanking());
  }

  @Test
  void obtenerResumenSistema_deberiaRetornarTotalesCorrectos() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    repository.save(new DonanteIncentivos(id1, id1, "D1"));
    repository.save(new DonanteIncentivos(id2, id2, "D2"));

    ResumenSistemaDTO resumen = service.obtenerResumenSistema();

    assertNotNull(resumen);
    assertEquals(2, resumen.totalDonantes());
  }
}
