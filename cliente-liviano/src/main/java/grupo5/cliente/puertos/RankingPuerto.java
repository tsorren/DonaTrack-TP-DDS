package grupo5.cliente.puertos;

import grupo5.cliente.vistas.RankingVista;

/**
 * Los rankings mensuales de donantes. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.ranking} elige cuál.
 */
public interface RankingPuerto {

  RankingVista ranking();
}
