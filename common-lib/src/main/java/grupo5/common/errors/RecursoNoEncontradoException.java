package grupo5.common.errors;

public class RecursoNoEncontradoException extends RuntimeException {
  public RecursoNoEncontradoException(String mensaje) {
    super(mensaje);
  }

  public RecursoNoEncontradoException(Object id) {
    super("No se encontró el recurso con id: " + id);
  }
}
