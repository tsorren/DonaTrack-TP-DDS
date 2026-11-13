package grupo5.cliente.vistas;

import java.util.List;

/**
 * Una página de una lista que se pide de a una (R11-01): sus filas, cuántas cumplen la búsqueda y
 * los filtros ({@code total}, como lo da el servicio), la página que se muestra (desde 1, ya
 * corregida si la pedida no existía) y cuántas hay.
 */
public record Tramo<T>(List<T> filas, long total, int pagina, int paginas) {

  public Tramo {
    filas = List.copyOf(filas);
  }

  /** Cuántas páginas hacen falta para {@code total} filas de a {@code tamanio}: al menos una. */
  public static int paginas(long total, int tamanio) {
    return total <= 0 ? 1 : (int) ((total + tamanio - 1) / tamanio);
  }

  /** Corta una lista ya filtrada y ordenada (la maqueta), con la página corregida. */
  public static <T> Tramo<T> de(List<T> todas, int pedida, int tamanio) {
    int paginas = paginas(todas.size(), tamanio);
    int pagina = Math.clamp(pedida, 1, paginas);
    List<T> filas = todas.stream().skip((long) (pagina - 1) * tamanio).limit(tamanio).toList();
    return new Tramo<>(filas, todas.size(), pagina, paginas);
  }

  /** La página pedida en la dirección ({@code ?pagina=}): 1 si falta o no es un número. */
  public static int pedida(String valor) {
    if (valor == null || !valor.strip().matches("\\d{1,7}")) {
      return 1;
    }
    return Math.max(1, Integer.parseInt(valor.strip()));
  }
}
