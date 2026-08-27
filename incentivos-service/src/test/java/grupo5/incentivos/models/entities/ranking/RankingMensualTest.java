package grupo5.incentivos.models.entities.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.fixtures.DonanteIncentivosMotherTest;
import grupo5.incentivos.fixtures.EventoDonacionMotherTest;
import grupo5.incentivos.fixtures.MisionMotherTest;
import grupo5.incentivos.fixtures.RankingMensualMotherTest;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankingMensualTest {

  @Test
  void rankingMensual_conCuatroDonantes_deberiaTenerPodioDeTres() {
    RankingMensual ranking =
        RankingMensualMotherTest.conNEntradas(YearMonth.of(2026, Month.MAY), 4);

    assertEquals(3, ranking.getPodio().size());
    assertEquals("Donante 1", ranking.getPodio().getFirst().getNombreDonante());
  }

  @Test
  void rankingMensual_conUnSoloDonante_deberiaTenerPodioDeUno() {
    RankingMensual ranking =
        RankingMensualMotherTest.conNEntradas(YearMonth.of(2026, Month.MAY), 1);

    assertEquals(1, ranking.getPodio().size());
    assertEquals("Donante 1", ranking.getPodio().getFirst().getNombreDonante());
  }

  @Test
  void rankingMensual_vacio_deberiaTenerPodioVacio() {
    RankingMensual ranking = RankingMensualMotherTest.vacioDeMayo2026();

    assertTrue(ranking.getPodio().isEmpty());
  }

  @Test
  void donante_deberiaMostrarMisionesCompletadasEnMes() {
    MisionDonacionesExitosas mision = MisionMotherTest.exitosas(CategoriaDonante.COLABORADOR, 1);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(List.of(mision));

    donante.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 5, 10));
    donante.registrarDonacionExitosa(new UUID(0L, 100L));

    assertTrue(mision.isCompletada());
    assertEquals(1, donante.misionesCompletadasEnMes(2026, 5));
    assertEquals(0, donante.misionesCompletadasEnMes(2026, 4));
  }

  @Test
  void donante_deberiaAscenderDeCategoriaAlCompletarTodasLasMisionesDeCategoria() {
    MisionRacha racha = MisionMotherTest.rachaColaborador(1);
    DonanteIncentivos donante = DonanteIncentivosMotherTest.conMisiones(List.of(racha));
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());

    donante.registrarDonacion(EventoDonacionMotherTest.enFecha(2026, 5, 15));

    // El ascenso ocurre automáticamente al completarse la última misión de la categoría.
    assertEquals(CategoriaDonante.SOSTENEDOR, donante.getCategoria());
  }

  @Test
  void constructor_deberiaLanzarExcepcionConPeriodoNulo() {
    ValidationException ex =
        assertThrows(ValidationException.class, () -> new RankingMensual(null));
    assertEquals(ErrorCatalog.RANKING_PERIODO_NULO, ex.getError());
  }

  @Test
  void agregarEntrada_deberiaLanzarExcepcionConEntradaNula() {
    RankingMensual ranking = RankingMensualMotherTest.vacioDeMayo2026();
    ValidationException ex =
        assertThrows(ValidationException.class, () -> ranking.agregarEntrada(null));
    assertEquals(ErrorCatalog.RANKING_ENTRADA_NULA, ex.getError());
  }

  @Test
  void gestorDeRankings_conEmpateDeMisiones_deberiaDesempatarDeterminísticamente() {
    GestorDeRankings gestor = new GestorDeRankings();
    YearMonth mayo = YearMonth.of(2026, Month.MAY);
    UUID idA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID idB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    DonanteIncentivos dA =
        DonanteIncentivosMotherTest.conMisionesCompletadasEnMes(idA, "Donante A", mayo, 2);
    DonanteIncentivos dB =
        DonanteIncentivosMotherTest.conMisionesCompletadasEnMes(idB, "Donante B", mayo, 2);

    RankingMensual ranking = gestor.calcular(List.of(dB, dA), mayo);

    assertEquals(2, ranking.getEntradas().size());
    assertEquals(idA, ranking.getEntradas().get(0).getDonanteId());
    assertEquals(idB, ranking.getEntradas().get(1).getDonanteId());
  }

  @Test
  void gestorDeRankings_conListaNulaOVacia_deberiaRetornarRankingVacio() {
    GestorDeRankings gestor = new GestorDeRankings();
    YearMonth mayo = YearMonth.of(2026, Month.MAY);

    assertTrue(gestor.calcular(null, mayo).getEntradas().isEmpty());
    assertTrue(gestor.calcular(List.of(), mayo).getEntradas().isEmpty());
  }
}
