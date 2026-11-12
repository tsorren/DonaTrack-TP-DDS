package grupo5.donaciones.models.repositories.listados;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comparación de la búsqueda {@code q}: "contiene", sin distinguir mayúsculas ni tildes ("Gomez"
 * encuentra "Gómez"). La base hace lo mismo con {@code lower(unaccent(...))}.
 */
public final class TextoBuscado {

  private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

  private TextoBuscado() {}

  /** En minúsculas y sin marcas diacríticas; {@code null} queda {@code null}. */
  public static String normalizar(String texto) {
    if (texto == null) {
      return null;
    }
    String descompuesto = Normalizer.normalize(texto, Normalizer.Form.NFD);
    return MARCAS.matcher(descompuesto).replaceAll("").toLowerCase(Locale.ROOT);
  }

  /** Si {@code campo} contiene {@code buscado}; {@code buscado} nulo no filtra. */
  public static boolean contiene(String campo, String buscado) {
    if (buscado == null) {
      return true;
    }
    return campo != null && normalizar(campo).contains(normalizar(buscado));
  }
}
