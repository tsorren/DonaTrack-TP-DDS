package grupo5.cliente.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DatosDeAdministradorTest {

  // La forma del correo antes de volverla lineal (S8786): la nueva debe aceptar lo mismo.
  private static final Pattern VIEJA = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
  private static final char[] ALFABETO = {'a', '.', '@', ' '};

  @Test
  void laFormaDeCorreoAceptaLoMismoQueLaAnterior() {
    probar("", 7);
  }

  private static void probar(String s, int resto) {
    assertEquals(
        VIEJA.matcher(s).matches(),
        DatosDeAdministrador.FORMA_DE_CORREO.matcher(s).matches(),
        "cadena: [" + s + "]");
    if (resto > 0) {
      for (char c : ALFABETO) {
        probar(s + c, resto - 1);
      }
    }
  }
}
