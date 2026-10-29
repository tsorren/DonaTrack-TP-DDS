package grupo5.auth.models.entities;

/** RNF-AUTH-01: mínimo 10, máximo 128 caracteres, distinta del email. */
public final class PoliticaContrasenia {

  public static final int MIN = 10;
  public static final int MAX = 128;

  private PoliticaContrasenia() {}

  public static void validar(String contrasenia, Email email) {
    if (contrasenia == null || contrasenia.length() < MIN || contrasenia.length() > MAX) {
      throw new DominioAuthException("Contraseña fuera de política");
    }
    if (email != null && contrasenia.equalsIgnoreCase(email.valor())) {
      throw new DominioAuthException("Contraseña fuera de política");
    }
  }
}
