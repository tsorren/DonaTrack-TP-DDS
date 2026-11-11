package grupo5.cliente.puertos;

import grupo5.cliente.vistas.PublicoMapaVista;

/**
 * Lo que lee el Mapa de entregadas, sin sesión. Lo implementan la maqueta y el adaptador REST;
 * {@code cliente.datos.publico} elige cuál.
 */
public interface PublicoMapaPuerto {

  /** Las entregas confirmadas del último mes. El filtro por categoría lo aplica la pantalla. */
  PublicoMapaVista mapa();
}
