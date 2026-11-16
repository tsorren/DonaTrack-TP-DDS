package grupo5.donaciones.models.repositories.listados;

import java.util.List;

/** Los elementos de una página y cuántos cumplen la búsqueda, sin contar la página. */
public record Tramo<T>(List<T> elementos, long total) {

  /** Corta una lista ya filtrada y ordenada (lo usan los repositorios en memoria). */
  public static <T> Tramo<T> de(List<T> ordenados, Paginado paginado) {
    List<T> pagina =
        ordenados.stream().skip(paginado.desplazamiento()).limit(paginado.tamanio()).toList();
    return new Tramo<>(pagina, ordenados.size());
  }
}
