package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.UUID;

public interface ItemTransportable {
  Unidad getUnidad();

  UUID getSubcategoriaId();

  Integer getCantidad();
}
