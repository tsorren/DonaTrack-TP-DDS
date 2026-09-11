package grupo5.notificaciones.exceptions;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import java.util.UUID;

/**
 * Excepción lanzada cuando se intenta sincronizar o actualizar una persona que ya ha sido
 * anonimizada, en protección del derecho al olvido y la inmutabilidad del estado.
 */
public class PersonaYaAnonimizadaException extends BusinessStateException {

  private final String detalle;

  public PersonaYaAnonimizadaException(UUID personaId) {
    super(ErrorCatalog.ARGUMENTO_INVALIDO);
    this.detalle =
        String.format(
            "La persona con ID %s se encuentra anonimizada y no admite nuevas actualizaciones.",
            personaId);
  }

  @Override
  public String getMessage() {
    return this.detalle;
  }
}
