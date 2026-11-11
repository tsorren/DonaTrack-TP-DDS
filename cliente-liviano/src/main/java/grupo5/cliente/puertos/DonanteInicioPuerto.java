package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteInicioVista;
import java.util.UUID;

/**
 * El inicio del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteInicioPuerto {

  DonanteInicioVista inicio(UUID donante);
}
