package grupo5.cliente.sesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class CifradoDeLaSesionTest {

  // Claves sintéticas de 32 bytes, solo para el test.
  private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);
  private static final String OTRA_CLAVE =
      Base64.getEncoder()
          .encodeToString("otra-clave-de-prueba-de-32-bytes".getBytes(StandardCharsets.UTF_8));
  private static final byte[] DATOS = "K7pQ-m4Rt-x9Wa-2Lse".getBytes(StandardCharsets.UTF_8);

  @Test
  void loCifradoNoDejaLosDatosEnClaroYVuelveIgual() {
    CifradoDeLaSesion cifrado = new CifradoDeLaSesion(CLAVE, true);

    String texto = cifrado.cifrar(DATOS);

    assertThat(texto).doesNotContain("K7pQ");
    assertThat(new String(Base64.getDecoder().decode(texto), StandardCharsets.ISO_8859_1))
        .doesNotContain("K7pQ");
    assertThat(cifrado.descifrar(texto)).hasValueSatisfying(d -> assertThat(d).isEqualTo(DATOS));
    // IV al azar: cifrar lo mismo dos veces da textos distintos.
    assertThat(cifrado.cifrar(DATOS)).isNotEqualTo(texto);
  }

  @Test
  void otraInstanciaConLaMismaClaveLoDescifra() {
    String texto = new CifradoDeLaSesion(CLAVE, true).cifrar(DATOS);

    assertThat(new CifradoDeLaSesion(" " + CLAVE + "\n", true).descifrar(texto))
        .hasValueSatisfying(d -> assertThat(d).isEqualTo(DATOS));
  }

  @Test
  void conOtraClaveAlteradoONoBase64NoDescifra() {
    String texto = new CifradoDeLaSesion(CLAVE, true).cifrar(DATOS);
    byte[] bytes = Base64.getDecoder().decode(texto);
    bytes[bytes.length - 1] ^= 1;
    String alterado = Base64.getEncoder().encodeToString(bytes);
    CifradoDeLaSesion cifrado = new CifradoDeLaSesion(CLAVE, true);

    assertThat(new CifradoDeLaSesion(OTRA_CLAVE, true).descifrar(texto)).isEmpty();
    assertThat(cifrado.descifrar(alterado)).isEmpty();
    assertThat(cifrado.descifrar("no es base64 %")).isEmpty();
    assertThat(cifrado.descifrar("AAAA")).isEmpty();
    assertThat(cifrado.descifrar(null)).isEmpty();
  }

  @Test
  void conLaSesionEnLaBaseSinClaveOConUnaMalaNoArranca() {
    assertThatThrownBy(() -> new CifradoDeLaSesion("", true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CLIENTE_SESIONES_CIFRADO_CLAVE");
    assertThatThrownBy(() -> new CifradoDeLaSesion("corta", false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("32 bytes");
    assertThatThrownBy(() -> new CifradoDeLaSesion("no es base64 %", false))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void conLaSesionEnMemoriaSinClaveUsaUnaAlAzar() {
    CifradoDeLaSesion uno = new CifradoDeLaSesion("", false);
    CifradoDeLaSesion otro = new CifradoDeLaSesion(null, false);

    assertThat(uno.descifrar(uno.cifrar(DATOS))).isPresent();
    assertThat(otro.descifrar(uno.cifrar(DATOS))).isEmpty();
  }
}
