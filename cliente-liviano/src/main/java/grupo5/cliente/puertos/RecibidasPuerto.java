package grupo5.cliente.puertos;

import grupo5.cliente.vistas.RecibidasVista;
import java.util.Set;
import java.util.UUID;

/** Las donaciones asignadas a una entidad, para Donaciones recibidas (RF-11). */
public interface RecibidasPuerto {

  /**
   * Para Inicio: lo asignado, con las recibidas del mes en curso. Los datos reales leen una sola
   * página de las más urgentes primero (R11-43).
   */
  RecibidasVista recibidas(UUID entidad);

  /**
   * Una página de lo asignado (R11-43), solo de los {@code grupos} pedidos (vacío: todos), con las
   * cifras de cada grupo; {@code pagina} desde 1. La maqueta manda todo y filtra la plantilla.
   */
  default RecibidasVista recibidas(UUID entidad, Set<String> grupos, int pagina) {
    return recibidas(entidad);
  }
}
