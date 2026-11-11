package grupo5.cliente.puertos;

import java.util.Map;
import java.util.UUID;

/**
 * Seguir entregas de la entidad (RF-14): lo asignado llega por {@link RecibidasPuerto}; acá, la
 * última noticia del camión de cada entrega en viaje, por la clave de su donación.
 */
public interface EntidadSeguimientoPuerto {

  Map<String, String> noticias(UUID entidad);

  /** Si va el dibujo de las raíces del ejemplo: fuera de la maqueta, no (el mapa es D21). */
  boolean dibujoDeEjemplo();
}
