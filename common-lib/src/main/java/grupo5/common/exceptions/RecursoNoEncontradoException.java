package grupo5.common.exceptions;

import java.util.UUID;
import lombok.Getter;

@Getter
public class RecursoNoEncontradoException extends DonaTrackException {
  private final UUID id;

  public RecursoNoEncontradoException(UUID id) {
    super(ErrorCatalog.RECURSO_NO_ENCONTRADO);
    this.id = id;
  }
}
