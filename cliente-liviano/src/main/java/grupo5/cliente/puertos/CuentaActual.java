package grupo5.cliente.puertos;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

/**
 * Quién usa el área privada. Los controllers le piden la cuenta y no saben de dónde sale: hoy, de
 * la maqueta o de un parámetro; con el login (F4), del usuario de la sesión.
 */
public interface CuentaActual {

  /** El id del donante, o vacío si todavía no se sabe quién es. */
  Optional<UUID> donante(HttpServletRequest pedido);

  /** El id de la entidad beneficiaria, o vacío si todavía no se sabe cuál es. */
  Optional<UUID> entidad(HttpServletRequest pedido);
}
