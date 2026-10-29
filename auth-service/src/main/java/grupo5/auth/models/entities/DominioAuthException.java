package grupo5.auth.models.entities;

/**
 * Violación de una invariante del dominio de auth. Es pura (sin common-lib): los Application
 * Services la traducen a las excepciones de frontera.
 */

// a cambiar
public class DominioAuthException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DominioAuthException(String mensaje) {
    super(mensaje);
  }
}
