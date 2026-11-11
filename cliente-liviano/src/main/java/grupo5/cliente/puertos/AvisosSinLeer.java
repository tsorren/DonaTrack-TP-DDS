package grupo5.cliente.puertos;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cuántas notificaciones sin leer tiene la cuenta: el número de la cabecera de las pantallas
 * privadas. Lo pide {@code web.Pantallas} en cada pantalla; {@code cliente.datos.avisos} elige la
 * maqueta o Notificaciones. Es compartido por los tres roles: lo declara el esqueleto de la
 * conexión de pantallas (SPEC-08).
 */
@FunctionalInterface
public interface AvisosSinLeer {

  /** Las sin leer de la cuenta del área que empieza con ese prefijo; 0 si no se sabe. */
  long sinLeer(String prefijoDeArea, HttpServletRequest pedido);
}
