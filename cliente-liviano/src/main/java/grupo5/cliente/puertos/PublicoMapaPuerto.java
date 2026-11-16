package grupo5.cliente.puertos;

import grupo5.cliente.vistas.PublicoMapaVista;

/**
 * Lo que lee el Mapa de entregadas, sin sesión. Lo implementan la maqueta y el adaptador REST;
 * {@code cliente.datos.publico} elige cuál.
 */
public interface PublicoMapaPuerto {

  /**
   * Las entregas confirmadas del último mes, con tope (R11-19). El filtro por categoría y la página
   * de la lista los arma la vista. {@code escenario} es el de la maqueta ({@code muchas}: más de
   * una página de lugares) o null; el adaptador REST no lo usa.
   */
  PublicoMapaVista mapa(String escenario);
}
