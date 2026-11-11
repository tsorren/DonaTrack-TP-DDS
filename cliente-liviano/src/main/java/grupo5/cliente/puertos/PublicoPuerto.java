package grupo5.cliente.puertos;

import grupo5.cliente.vistas.PublicoCifra;
import java.util.List;

/**
 * Lo que leen las páginas públicas, sin sesión. Lo implementan la maqueta y el adaptador REST;
 * {@code cliente.datos.publico} elige cuál.
 */
public interface PublicoPuerto {

  /** Las tres cifras de la portada. */
  List<PublicoCifra> cifrasDePortada();

  /** Las tres cifras de Nosotros. */
  List<PublicoCifra> cifrasDeNosotros();
}
