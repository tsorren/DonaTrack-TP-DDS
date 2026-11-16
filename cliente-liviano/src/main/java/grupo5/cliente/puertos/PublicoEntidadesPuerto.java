package grupo5.cliente.puertos;

import grupo5.cliente.vistas.PublicoEntidadesVista;

/**
 * Lo que lee Entidades y necesidades, sin sesión. Lo implementan la maqueta y el adaptador REST;
 * {@code cliente.datos.publico} elige cuál.
 */
public interface PublicoEntidadesPuerto {

  /** Las entidades con pedidos vigentes. La búsqueda y los filtros los aplica la pantalla. */
  PublicoEntidadesVista entidades();
}
