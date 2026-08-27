package grupo5.incentivos.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import grupo5.incentivos.models.entities.inactividad.DonanteInactivo;
import grupo5.incentivos.models.entities.inactividad.GestorDeInactivos;
import grupo5.incentivos.models.entities.ranking.GestorDeRankings;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainServicesConfigTest {

  @Test
  void domainServicesConfig_deberiaProveerBeansNoNulos() {
    DomainServicesConfig config = new DomainServicesConfig();

    assertNotNull(config.gestorDeInactivos());
    assertNotNull(config.gestorDeRankings());
  }

  @Test
  void gestorDeRankings_conInputsNulosOVacios_debeRetornarRankingVacioDeFormaSegura() {
    GestorDeRankings gestor = new GestorDeRankings();
    YearMonth periodo = YearMonth.of(2026, 5);

    RankingMensual rankingConNull = gestor.calcular(null, periodo);
    assertNotNull(rankingConNull);
    assertTrue(rankingConNull.getEntradas().isEmpty());

    RankingMensual rankingConVacio = gestor.calcular(List.of(), periodo);
    assertNotNull(rankingConVacio);
    assertTrue(rankingConVacio.getEntradas().isEmpty());
  }

  @Test
  void gestorDeInactivos_conInputsNulosOVacios_debeRetornarListaVaciaDeFormaSegura() {
    GestorDeInactivos gestor = new GestorDeInactivos();

    List<DonanteInactivo> resultado1 = gestor.procesarInactividad(null, null);
    assertNotNull(resultado1);
    assertTrue(resultado1.isEmpty());

    List<DonanteInactivo> resultado2 = gestor.procesarInactividad(List.of(), null);
    assertNotNull(resultado2);
    assertTrue(resultado2.isEmpty());

    List<DonanteInactivo> resultado3 = gestor.procesarInactividad(null, List.of());
    assertNotNull(resultado3);
    assertTrue(resultado3.isEmpty());
  }
}
