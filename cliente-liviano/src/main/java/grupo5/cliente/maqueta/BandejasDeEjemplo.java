package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.ConsultaBandeja;
import grupo5.cliente.vistas.Tramo;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Lo que en el sistema resuelve Notificaciones (R11-14), sobre los avisos de ejemplo de la maqueta:
 * "Sin leer", la búsqueda sin mayúsculas ni tildes, la categoría, el orden por fecha y la página de
 * a 20.
 */
final class BandejasDeEjemplo {

  private BandejasDeEjemplo() {}

  /**
   * La página de {@code todas} que pide {@code consulta}. {@code leida}, {@code tema}, {@code
   * texto} (donde se busca) y {@code fecha} (ISO, se ordena como texto) leen cada aviso.
   */
  static <T> Tramo<T> pagina(
      List<T> todas,
      ConsultaBandeja consulta,
      Predicate<T> leida,
      Function<T, String> tema,
      Function<T, String> texto,
      Function<T, String> fecha) {
    String buscado = sinTildes(consulta.buscar());
    Comparator<T> orden = Comparator.comparing(fecha);
    List<T> cumplen =
        todas.stream()
            .filter(n -> !consulta.sinLeer() || !leida.test(n))
            .filter(n -> consulta.tema().isEmpty() || consulta.tema().equals(tema.apply(n)))
            .filter(n -> buscado.isEmpty() || sinTildes(texto.apply(n)).contains(buscado))
            .sorted(consulta.viejas() ? orden : orden.reversed())
            .toList();
    return Tramo.de(cumplen, consulta.pagina(), ConsultaBandeja.TAMANIO);
  }

  static String sinTildes(String valor) {
    return Normalizer.normalize(valor, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
