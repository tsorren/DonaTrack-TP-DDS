package grupo5.cliente.puertos;

/** Ya hay una cuenta con ese correo. */
public final class CorreoEnUso extends RuntimeException {
  public CorreoEnUso() {
    super("Ya hay una cuenta con ese correo");
  }
}
