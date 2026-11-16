package grupo5.cliente.puertos;

import grupo5.cliente.vistas.RankingVista;

/**
 * Los rankings mensuales de donantes. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.ranking} elige cuál.
 */
public interface RankingPuerto {

  RankingVista ranking();

  /**
   * La página {@code pagina} (desde 1) de los meses calculados y el mes {@code mes} ({@code
   * 2026-09}; null o uno que no existe: el más reciente de la página), aunque esté en otra página
   * (R11-43). La maqueta manda todo en una página.
   */
  default RankingVista ranking(String mes, int pagina) {
    return ranking();
  }
}
