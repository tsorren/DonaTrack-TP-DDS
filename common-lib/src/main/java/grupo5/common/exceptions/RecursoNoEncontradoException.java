package grupo5.common.exceptions;

import lombok.Getter;

import java.util.UUID;

@Getter
public class RecursoNoEncontradoException extends DonaTrackException {
  private final UUID id;

  public RecursoNoEncontradoException(UUID id) {
    super(ErrorCatalog.RECURSO_NO_ENCONTRADO);
    this.id = id;
  }
}
