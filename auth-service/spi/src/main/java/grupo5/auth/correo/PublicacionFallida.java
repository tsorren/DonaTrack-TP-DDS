package grupo5.auth.correo;

/**
 * No se pudo publicar el evento: el broker no está, rechazó el mensaje o ninguna cola lo recibió.
 */
public class PublicacionFallida extends Exception {

  private static final long serialVersionUID = 1L;

  public PublicacionFallida(String mensaje) {
    super(mensaje);
  }

  public PublicacionFallida(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
