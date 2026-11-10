package grupo5.cliente.puertos;

import grupo5.cliente.vistas.CuentaVista;
import java.util.Optional;

/**
 * De dónde sale la cuenta que muestra el menú del área privada (la de ejemplo o la de la sesión).
 */
@FunctionalInterface
public interface CuentasDelMenu {

  /** La cuenta del área que empieza con ese prefijo; vacía si no es un área privada. */
  Optional<CuentaVista> de(String prefijoDeArea);
}
