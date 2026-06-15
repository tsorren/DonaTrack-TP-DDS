package grupo5.incentivos.models.entities.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankingMensualTest {

  @Test
  void rankingMensual_deberiaTenerPodioDeHastaTres() {
    RankingMensual ranking = new RankingMensual(YearMonth.of(2026, 5));
    ranking.agregarEntrada(new EntradaRanking(1, 1L, "Ana", 5));
    ranking.agregarEntrada(new EntradaRanking(2, 2L, "Bob", 3));
    ranking.agregarEntrada(new EntradaRanking(3, 3L, "Carlos", 2));
    ranking.agregarEntrada(new EntradaRanking(4, 4L, "Diana", 1));

    assertEquals(3, ranking.getPodio().size());
    assertEquals("Ana", ranking.getPodio().getFirst().getNombreDonante());
  }

  @Test
  void donante_deberiaMostrarMisionesCompletadasEnMes() {
    DonanteIncentivos donante = new DonanteIncentivos(1L);

    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    donante.getMisiones().add(mision);

    EventoDonacion evento =
            EventoDonacion.builder()
                    .donacionId(1L)
                    .fecha(LocalDate.of(2026, 5, 10))
                    .cantidadBienes(1)
                    .categorias(List.of("x"))
                    .build();

    donante.registrarDonacion(evento);
    donante.registrarDonacionExitosa(100L);

    assertTrue(mision.isCompletada());
    assertEquals(1, donante.misionesCompletadasEnMes(2026, 5));
    assertEquals(0, donante.misionesCompletadasEnMes(2026, 4));
  }

  @Test
  void donante_deberiaAscenderDeCategoriaAlCompletarTodasLasMisionesDeCategoria() {
    DonanteIncentivos donante = new DonanteIncentivos(1L);
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());

    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    donante.getMisiones().add(racha);

    EventoDonacion evento =
            EventoDonacion.builder()
                    .donacionId(1L)
                    .fecha(LocalDate.now())
                    .cantidadBienes(1)
                    .categorias(List.of("x"))
                    .build();

    donante.registrarDonacion(evento);
    boolean ascendio = donante.intentarAscenso();

    assertTrue(ascendio);
    assertEquals(CategoriaDonante.SOSTENEDOR, donante.getCategoria());
  }
}
