package grupo5.cliente.puertos;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Seguir entregas de la entidad (RF-14): lo asignado llega por {@link RecibidasPuerto}; acá, la
 * última noticia del camión de cada entrega en viaje, por la clave de su donación.
 */
public interface EntidadSeguimientoPuerto {

  /**
   * Las noticias de las entregas de las donaciones {@code claves} (las de la página que se muestra,
   * R11-43): no se leen todas las entregas de la entidad.
   */
  Map<String, String> noticias(UUID entidad, Collection<String> claves);

  /** Si va el dibujo de las raíces del ejemplo: fuera de la maqueta, no (el mapa es D21). */
  boolean dibujoDeEjemplo();
}
