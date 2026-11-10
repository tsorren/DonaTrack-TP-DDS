package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.MisDonacionesVista;
import java.util.Optional;
import java.util.UUID;

/** Las donaciones de un donante, para Mis donaciones y su detalle. */
public interface DonacionesPuerto {

  MisDonacionesVista misDonaciones(UUID donante);

  /**
   * Una donación del donante por su clave; vacío si no existe o no es suya. Sin clave, la maqueta
   * muestra la que va en camino.
   */
  Optional<DetalleDonacionVista> detalle(UUID donante, String clave);
}
