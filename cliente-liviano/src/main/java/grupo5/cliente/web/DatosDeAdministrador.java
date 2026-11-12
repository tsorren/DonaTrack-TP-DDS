package grupo5.cliente.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Revisa lo escrito en Mi perfil y en el alta de un administrador antes de mandarlo al proveedor de
 * ingreso, con los textos de la ficha (docs/interfaz/flujos/r11-p1-admins.md, §5). Los signos que
 * no acepta son los del validador de nombres de Keycloak ({@code
 * person-name-prohibited-characters}). Devuelve los errores por campo, en el orden del formulario;
 * vacío si está todo bien.
 */
final class DatosDeAdministrador {

  static final String NOMBRE = "nombre";
  static final String APELLIDO = "apellido";
  static final String CORREO = "correo";

  static final String CORREO_EN_USO =
      "Ya hay una cuenta con este correo en DonaTrack. Usá otro correo para esta cuenta de"
          + " administración.";

  private static final int LARGO = 255;
  private static final Pattern SIGNOS =
      Pattern.compile("[<>&\"$%!#?§;*~/\\\\|^=\\[\\]{}()\\p{Cntrl}]");
  private static final Pattern FORMA_DE_CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
  private static final String SIGNOS_NO_ACEPTADOS =
      "El nombre no puede llevar signos como < > & o comillas.";
  private static final String MUY_LARGO = "Usá hasta 255 letras.";

  private DatosDeAdministrador() {}

  /** Mi perfil: el nombre propio ("Escribí tu nombre."). */
  static Map<String, String> perfil(String nombre, String apellido) {
    Map<String, String> errores = new LinkedHashMap<>();
    nombre(errores, NOMBRE, nombre, "Escribí tu nombre.");
    nombre(errores, APELLIDO, apellido, "Escribí tu apellido.");
    return errores;
  }

  /** El alta: los datos de otra persona ("Escribí el nombre."). */
  static Map<String, String> alta(String nombre, String apellido, String correo) {
    Map<String, String> errores = new LinkedHashMap<>();
    nombre(errores, NOMBRE, nombre, "Escribí el nombre.");
    nombre(errores, APELLIDO, apellido, "Escribí el apellido.");
    String limpio = limpio(correo);
    if (limpio.isEmpty()) {
      errores.put(CORREO, "Escribí el correo.");
    } else if (limpio.length() > LARGO || !FORMA_DE_CORREO.matcher(limpio).matches()) {
      errores.put(CORREO, "Revisá el correo: tiene que tener la forma nombre@organizacion.org.");
    }
    return errores;
  }

  static String limpio(String valor) {
    return valor == null ? "" : valor.strip();
  }

  private static void nombre(
      Map<String, String> errores, String campo, String valor, String siFalta) {
    String limpio = limpio(valor);
    if (limpio.isEmpty()) {
      errores.put(campo, siFalta);
    } else if (SIGNOS.matcher(limpio).find()) {
      errores.put(campo, SIGNOS_NO_ACEPTADOS);
    } else if (limpio.length() > LARGO) {
      errores.put(campo, MUY_LARGO);
    }
  }
}
