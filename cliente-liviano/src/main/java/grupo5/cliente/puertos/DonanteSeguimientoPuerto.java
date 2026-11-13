package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteSeguimientoVista;
import java.util.UUID;

/**
 * Seguimiento del donante. Lo implementan la maqueta y el adaptador REST; {@code
 * cliente.datos.donante} elige cuál.
 */
public interface DonanteSeguimientoPuerto {

  DonanteSeguimientoVista seguimiento(UUID donante);
}
