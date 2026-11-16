package grupo5.cliente.puertos;

import java.io.Serial;

/** La solicitud ya no espera: el equipo la aprobó (Donaciones responde 409). */
public final class YaAprobada extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public YaAprobada() {
    super("La solicitud ya no está pendiente");
  }
}
