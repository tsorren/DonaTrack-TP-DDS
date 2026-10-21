package grupo5.donaciones.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Comparación de claves cuyo tiempo no depende de cuánto coincidan. */
public final class ClaveSegura {

  private ClaveSegura() {}

  /** Compara los hashes SHA-256 de las dos claves, que tienen siempre el mismo largo. */
  public static boolean coinciden(String recibida, String esperada) {
    if (recibida == null || esperada == null) {
      return false;
    }
    return MessageDigest.isEqual(sha256(recibida), sha256(esperada));
  }

  private static byte[] sha256(String valor) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 no disponible en la JVM", e);
    }
  }
}
