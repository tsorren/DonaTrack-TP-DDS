package grupo5.cliente.puertos;

import grupo5.cliente.vistas.CampanitaVista;
import jakarta.servlet.http.HttpServletRequest;

/**
 * La campanita de la cabecera privada (R11-29, R11-30): las sin leer más nuevas de la cuenta del
 * área y abrir una. {@code area} es el prefijo del área ({@code donante}, {@code entidad} o {@code
 * admin}); la de la administración es la bandeja compartida de la Administración. Lo implementan la
 * maqueta y Notificaciones; {@code cliente.datos.avisos} elige cuál, como el número de la cabecera.
 */
public interface CampanitaPuerto {

  /**
   * Hasta {@link CampanitaVista#MAXIMO} sin leer, las más nuevas primero.
   *
   * @throws RuntimeException si la fuente no contesta (el desplegable muestra su error)
   */
  CampanitaVista recientes(String area, HttpServletRequest pedido);

  /**
   * Marca la notificación como leída y devuelve adónde lleva, calculado desde la notificación
   * (nunca desde el navegador). Si no se pudo marcar, la bandeja del área.
   */
  String abrir(String area, String id, HttpServletRequest pedido);
}
