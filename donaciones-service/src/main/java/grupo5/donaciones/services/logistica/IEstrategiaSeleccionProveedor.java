package grupo5.donaciones.services.logistica;

import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import java.util.List;

/** Decide en qué orden se prueban los proveedores de logística para un pedido de entrega. */
public interface IEstrategiaSeleccionProveedor {

  /** Ids de proveedores en orden de preferencia. */
  List<String> ordenar(DatosEntregaLogistica datos);
}
