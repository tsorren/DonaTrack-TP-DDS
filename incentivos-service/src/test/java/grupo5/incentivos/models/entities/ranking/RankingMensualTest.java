package grupo5.incentivos.models.entities.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.incentivos.models.entities.donante.CategoriaDonante;
import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import grupo5.incentivos.models.entities.donante.EventoDonacion;
import grupo5.incentivos.models.entities.misiones.MisionDonacionesExitosas;
import grupo5.incentivos.models.entities.misiones.MisionRacha;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RankingMensualTest {

  @Test
  void rankingMensual_deberiaTenerPodioDeHastaTres() {
    RankingMensual ranking = new RankingMensual(YearMonth.of(2026, Month.MAY));
    ranking.agregarEntrada(new EntradaRanking(1, UUID.randomUUID(), "Ana", 5));
    ranking.agregarEntrada(new EntradaRanking(2, UUID.randomUUID(), "Bob", 3));
    ranking.agregarEntrada(new EntradaRanking(3, UUID.randomUUID(), "Carlos", 2));
    ranking.agregarEntrada(new EntradaRanking(4, UUID.randomUUID(), "Diana", 1));

    assertEquals(3, ranking.getPodio().size());
    assertEquals("Ana", ranking.getPodio().getFirst().getNombreDonante());
  }

  @Test
  void donante_deberiaMostrarMisionesCompletadasEnMes() {
    MisionDonacionesExitosas mision = new MisionDonacionesExitosas(CategoriaDonante.COLABORADOR, 1);
    // Se usa el constructor con lista explícita de misiones: el constructor por defecto
    // ya trae misiones estándar de MisionFactory, y getMisionActiva() habría evaluado
    // esa misión estándar en vez de la que arma este test.
    DonanteIncentivos donante =
        new DonanteIncentivos(UUID.randomUUID(), UUID.randomUUID(), "Test", List.of(mision));

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 1L))
            .fecha(LocalDate.of(2026, Month.MAY, 10))
            .cantidadBienes(1)
            .categorias(List.of("x"))
            .build();

    donante.registrarDonacion(evento);
    donante.registrarDonacionExitosa(new UUID(0L, 100L));

    assertTrue(mision.isCompletada());
    assertEquals(1, donante.misionesCompletadasEnMes(2026, 5));
    assertEquals(0, donante.misionesCompletadasEnMes(2026, 4));
  }

  @Test
  void donante_deberiaAscenderDeCategoriaAlCompletarTodasLasMisionesDeCategoria() {
    MisionRacha racha = new MisionRacha(CategoriaDonante.COLABORADOR, 1);
    // Idem: se pasa la única misión explícitamente para que sea la misión activa.
    DonanteIncentivos donante =
        new DonanteIncentivos(UUID.randomUUID(), UUID.randomUUID(), "Test", List.of(racha));
    assertEquals(CategoriaDonante.COLABORADOR, donante.getCategoria());

    EventoDonacion evento =
        EventoDonacion.builder()
            .donacionId(new UUID(0L, 1L))
            .fecha(LocalDate.of(2026, Month.MAY, 15))
            .cantidadBienes(1)
            .categorias(List.of("x"))
            .build();

    donante.registrarDonacion(evento);

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
    RankingMensual ranking = new RankingMensual(YearMonth.of(2026, Month.MAY));
    ValidationException ex =
        assertThrows(ValidationException.class, () -> ranking.agregarEntrada(null));
    assertEquals(ErrorCatalog.RANKING_ENTRADA_NULA, ex.getError());
  }
}
