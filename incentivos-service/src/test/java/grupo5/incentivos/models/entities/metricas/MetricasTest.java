package grupo5.incentivos.models.entities.metricas;

import static org.junit.jupiter.api.Assertions.*;

import grupo5.incentivos.fixtures.EventoDonacionMother;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricasTest {

  private Metricas metricas;

  @BeforeEach
  void setUp() {
    metricas = new Metricas();
  }

  @Test
  void metricas_inicialmenteDeberianEstarEnCero() {
    assertEquals(0, metricas.getTotalDonacionesHistoricas());
    assertEquals(0, metricas.getTotalDonacionesExitosas());
    assertEquals(0, metricas.getTotalOrganizacionesAyudadas());
    assertNull(metricas.getUltimaDonacion());
    assertTrue(metricas.getHistorialDonaciones().isEmpty());
    assertTrue(metricas.getOrganizacionesAyudadas().isEmpty());
  }

  @Test
  void registrarDonacion_deberiaIncrementarHistoricasYActualizarUltimaFecha() {
    LocalDate fecha = LocalDate.of(2026, 5, 10);
    EventoDonacion evento = EventoDonacionMother.enFecha(fecha);

    metricas.registrarDonacion(evento);

    assertEquals(1, metricas.getTotalDonacionesHistoricas());
    assertEquals(fecha, metricas.getUltimaDonacion());
    assertEquals(1, metricas.getHistorialDonaciones().size());
  }

  @Test
  void registrarDonacionExitosa_deberiaIncrementarExitosasYOrganizacionesUnicas() {
    UUID org1 = UUID.randomUUID();
    UUID org2 = UUID.randomUUID();

    metricas.registrarDonacionExitosa(org1);
    metricas.registrarDonacionExitosa(org1); // duplicada
    metricas.registrarDonacionExitosa(org2);

    assertEquals(3, metricas.getTotalDonacionesExitosas());
    assertEquals(2, metricas.getTotalOrganizacionesAyudadas());
    assertTrue(metricas.yaAyudoA(org1));
    assertTrue(metricas.yaAyudoA(org2));
  }

  @Test
  void donacionesPorPeriodo_y_donacionesEnMes_deberianCalcularCorrectamente() {
    metricas.registrarDonacion(EventoDonacionMother.enFecha(2026, 5, 1));
    metricas.registrarDonacion(EventoDonacionMother.enFecha(2026, 5, 15));
    metricas.registrarDonacion(EventoDonacionMother.enFecha(2026, 6, 1));

    Map<YearMonth, Long> porPeriodo = metricas.donacionesPorPeriodo();
    assertEquals(2L, porPeriodo.get(YearMonth.of(2026, 5)));
    assertEquals(1L, porPeriodo.get(YearMonth.of(2026, 6)));

    assertEquals(2L, metricas.donacionesEnMes(YearMonth.of(2026, 5)));
    assertEquals(0L, metricas.donacionesEnMes(YearMonth.of(2026, 4)));
  }

  @Test
  void getHistorialDonaciones_debeRetornarCopiaInmutable() {
    metricas.registrarDonacion(EventoDonacionMother.enFecha(2026, 5, 1));
    List<EventoDonacion> historial = metricas.getHistorialDonaciones();

    assertThrows(
        UnsupportedOperationException.class,
        () -> historial.add(EventoDonacionMother.enFecha(2026, 5, 2)));
  }

  @Test
  void getOrganizacionesAyudadas_debeRetornarCopiaInmutable() {
    UUID org = UUID.randomUUID();
    metricas.registrarDonacionExitosa(org);
    var organizaciones = metricas.getOrganizacionesAyudadas();

    assertThrows(UnsupportedOperationException.class, () -> organizaciones.add(UUID.randomUUID()));
  }
}
