package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DonanteEntidadesVista;
import java.util.UUID;

/**
 * Las entidades que ve el donante, de a seis por página. Lo implementan la maqueta y el adaptador
 * REST; {@code cliente.datos.donante} elige cuál.
 */
public interface DonanteEntidadesPuerto {

  /** {@code nombre}, vacío para todas; {@code pagina}, desde 1. */
  DonanteEntidadesVista entidades(UUID donante, String nombre, int pagina);
}
