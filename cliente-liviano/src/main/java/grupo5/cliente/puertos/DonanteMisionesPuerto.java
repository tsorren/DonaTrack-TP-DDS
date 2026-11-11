package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteMisionesVista;
import java.util.UUID;

/**
 * Misiones e insignias del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.misiones} elige cuál.
 */
public interface DonanteMisionesPuerto {

  /** Las misiones del donante; {@code donante} es el id de su persona. */
  DonanteMisionesVista misiones(UUID donante);
}
