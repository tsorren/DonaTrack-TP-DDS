package grupo5.cliente.sesion;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra lo que no puede quedar legible en la sesión: con Spring Session JDBC la sesión se escribe
 * en PostgreSQL (SesionesJdbcConfig), y una contraseña provisoria no tiene que quedar en claro en
 * la base ni en sus copias. AES-GCM del JDK: cifra y detecta si el texto se alteró.
 *
 * <p>La clave es {@code cliente.sesiones.cifrado-clave} (32 bytes en Base64). Todas las instancias
 * del sitio tienen que tener la misma, porque la redirección puede caer en otra (RNF-CL-04): con la
 * sesión en la base, falta la clave y el sitio no arranca. Con la sesión en memoria (la maqueta y
 * los tests) alcanza una clave al azar por proceso.
 */
@Component
public class CifradoDeLaSesion {

  private static final String ALGORITMO = "AES/GCM/NoPadding";
  private static final int LARGO_DE_LA_CLAVE = 32;
  private static final int LARGO_DEL_IV = 12;
  private static final int BITS_DE_LA_ETIQUETA = 128;

  private final SecureRandom azar = new SecureRandom();
  private final SecretKey clave;

  CifradoDeLaSesion(
      @Value("${cliente.sesiones.cifrado-clave:}") String clave,
      @Value("${cliente.sesiones.jdbc.habilitado:false}") boolean sesionEnLaBase) {
    this.clave = new SecretKeySpec(bytesDeLaClave(clave, sesionEnLaBase), "AES");
  }

  private byte[] bytesDeLaClave(String clave, boolean sesionEnLaBase) {
    if (clave == null || clave.isBlank()) {
      if (sesionEnLaBase) {
        throw new IllegalStateException(
            "Falta CLIENTE_SESIONES_CIFRADO_CLAVE (32 bytes en Base64, ver .env.example): con la"
                + " sesión en la base, todas las instancias necesitan la misma clave.");
      }
      byte[] alAzar = new byte[LARGO_DE_LA_CLAVE];
      azar.nextBytes(alAzar);
      return alAzar;
    }
    byte[] dada;
    try {
      dada = Base64.getDecoder().decode(clave.trim());
    } catch (IllegalArgumentException noEsBase64) {
      dada = new byte[0];
    }
    if (dada.length != LARGO_DE_LA_CLAVE) {
      throw new IllegalStateException(
          "CLIENTE_SESIONES_CIFRADO_CLAVE tiene que ser de 32 bytes en Base64"
              + " (openssl rand -base64 32).");
    }
    return dada;
  }

  /** Los datos cifrados, en Base64: un IV al azar seguido del texto cifrado con su etiqueta. */
  public String cifrar(byte[] datos) {
    byte[] iv = new byte[LARGO_DEL_IV];
    azar.nextBytes(iv);
    try {
      Cipher cifrador = Cipher.getInstance(ALGORITMO);
      cifrador.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(BITS_DE_LA_ETIQUETA, iv));
      byte[] cifrado = cifrador.doFinal(datos);
      return Base64.getEncoder()
          .encodeToString(
              ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array());
    } catch (GeneralSecurityException imposible) {
      throw new IllegalStateException("El JDK no pudo cifrar con " + ALGORITMO, imposible);
    }
  }

  /** Los datos, o vacío si el texto no es de esta clave, se alteró o no es Base64. */
  public Optional<byte[]> descifrar(String cifrado) {
    if (cifrado == null) {
      return Optional.empty();
    }
    try {
      byte[] todo = Base64.getDecoder().decode(cifrado);
      if (todo.length <= LARGO_DEL_IV) {
        return Optional.empty();
      }
      Cipher descifrador = Cipher.getInstance(ALGORITMO);
      descifrador.init(
          Cipher.DECRYPT_MODE,
          clave,
          new GCMParameterSpec(BITS_DE_LA_ETIQUETA, todo, 0, LARGO_DEL_IV));
      return Optional.of(descifrador.doFinal(todo, LARGO_DEL_IV, todo.length - LARGO_DEL_IV));
    } catch (GeneralSecurityException | IllegalArgumentException noSePuede) {
      return Optional.empty();
    }
  }
}
