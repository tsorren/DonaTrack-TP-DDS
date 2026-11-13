package grupo5.cliente.cuentas;

import java.security.SecureRandom;

/**
 * La contraseña provisoria de una cuenta de administración (R11-10): 16 caracteres al azar en
 * grupos de 4 separados por guiones, que son parte de la contraseña (se escribe como se ve). Sin
 * los caracteres que se confunden al leerla en voz alta o a mano (0, O, o, 1, l, I, i). Cumple la
 * política del realm (largo entre 10 y 128). No se guarda ni se loguea.
 */
public final class ContrasenaProvisoria {

  private static final String CARACTERES =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
  private static final int GRUPOS = 4;
  private static final int POR_GRUPO = 4;
  private static final SecureRandom AZAR = new SecureRandom();

  private ContrasenaProvisoria() {}

  public static String nueva() {
    StringBuilder contrasena = new StringBuilder();
    for (int grupo = 0; grupo < GRUPOS; grupo++) {
      if (grupo > 0) {
        contrasena.append('-');
      }
      for (int i = 0; i < POR_GRUPO; i++) {
        contrasena.append(CARACTERES.charAt(AZAR.nextInt(CARACTERES.length())));
      }
    }
    return contrasena.toString();
  }
}
