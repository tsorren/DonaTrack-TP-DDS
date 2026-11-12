package grupo5.notificaciones.models.repositories;

import java.util.Locale;
import java.util.Set;

/**
 * Qué página de la bandeja de una persona se pide (R11-14, spec R11-p1-notificaciones), ya
 * validada. Los filtros se combinan con "y".
 *
 * @param leida {@code true} o {@code false}, solo esas; {@code null}, todas
 * @param texto el texto buscado en el mensaje, ya pasado por {@link #sinTildes(String)}; {@code
 *     null}, sin búsqueda
 * @param tipos solo las de esos tipos de evento; vacío, sin filtro
 * @param excluirTipos todas menos las de esos tipos; las que no tienen tipo entran
 * @param antiguas {@code true}: de la más vieja a la más nueva; si no, al revés. Desempata por id
 * @param pagina desde 0
 * @param tamanio cuántas por página
 */
public record ConsultaBandeja(
    Boolean leida,
    String texto,
    Set<String> tipos,
    Set<String> excluirTipos,
    boolean antiguas,
    int pagina,
    int tamanio) {

  /**
   * Las letras que la búsqueda iguala a su vocal sin tilde (la base hace lo mismo con translate).
   */
  public static final String CON_TILDE = "áéíóúüñ";

  public static final String SIN_TILDE = "aeiouun";

  public ConsultaBandeja {
    tipos = Set.copyOf(tipos);
    excluirTipos = Set.copyOf(excluirTipos);
  }

  public long desplazamiento() {
    return (long) pagina * tamanio;
  }

  /** En minúsculas y sin tildes: "Camión" queda "camion". */
  public static String sinTildes(String valor) {
    String minusculas = valor.toLowerCase(Locale.ROOT);
    StringBuilder resultado = new StringBuilder(minusculas.length());
    for (char letra : minusculas.toCharArray()) {
      int lugar = CON_TILDE.indexOf(letra);
      resultado.append(lugar < 0 ? letra : SIN_TILDE.charAt(lugar));
    }
    return resultado.toString();
  }
}
