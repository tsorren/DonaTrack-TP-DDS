package grupo5.cliente.puertos;

/** El alta falló a mitad de camino; la cuenta creada se borró. */
public final class AltaIncompleta extends RuntimeException {
  public AltaIncompleta(Throwable causa) {
    super("No se creó la cuenta de administración", causa);
  }
}
