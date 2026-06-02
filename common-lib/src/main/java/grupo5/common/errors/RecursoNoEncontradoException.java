package grupo5.common.errors;

public class RecursoNoEncontradoException extends RuntimeException {
  public RecursoNoEncontradoException(Object id) {
    super("No se encontró el recurso con id: " + id);
  }

  public RecursoNoEncontradoException(String message) {
    super(message);
  }
}
