package grupo5.cliente.puertos;

import grupo5.cliente.vistas.CuentaVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * De dónde sale la cuenta que muestra el menú del área privada (la de ejemplo o la de la sesión).
 */
@FunctionalInterface
public interface CuentasDelMenu {

  /** La cuenta del área que empieza con ese prefijo; vacía si no es un área privada. */
  Optional<CuentaVista> de(String prefijoDeArea);

  /**
   * La cuenta sin pedirle nada a ningún servicio, para las páginas de error y Contacto: lo que la
   * sesión ya sabe. Por defecto, la misma que {@link #de(String)}.
   */
  default Optional<CuentaVista> deLaSesion(String prefijoDeArea) {
    return de(prefijoDeArea);
  }

  /** Olvida lo que la sesión sabe de la cuenta (después de que cambia su nombre). */
  default void olvidar(HttpServletRequest pedido) {
    // La cuenta de ejemplo no se guarda en la sesión: no hay nada que olvidar.
  }
}
