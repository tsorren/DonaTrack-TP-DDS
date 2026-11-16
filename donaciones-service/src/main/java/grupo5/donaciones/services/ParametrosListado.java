package grupo5.donaciones.services;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.repositories.listados.Paginado;
import java.util.Locale;

/**
 * Los parámetros comunes de los listados paginados (R11-01), tal como llegan por la URL. Sin {@code
 * page} ni {@code size} se pide la página 0 de 20: nadie recibe una lista completa. Un valor fuera
 * de rango da 400 con {@code ERR-CSR-003}.
 *
 * @param page desde 0
 * @param size de 1 a 100
 * @param q texto buscado; con espacios recortados, vacío es lo mismo que no mandarlo; hasta 100
 * @param orden el nombre de un orden del listado, sin distinguir mayúsculas; nulo es el de defecto
 */
public record ParametrosListado(Integer page, Integer size, String q, String orden) {

  public static final int TAMANIO_POR_DEFECTO = 20;
  public static final int TAMANIO_MAXIMO = 100;
  public static final int LARGO_MAXIMO_BUSQUEDA = 100;

  /** Sin parámetros: la primera página de 20, sin búsqueda y en el orden por defecto. */
  public static ParametrosListado primeraPagina() {
    return new ParametrosListado(null, null, null, null);
  }

  public Paginado paginado() {
    int pagina = page == null ? 0 : page;
    int tamanio = size == null ? TAMANIO_POR_DEFECTO : size;
    if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return new Paginado(pagina, tamanio);
  }

  /** El texto buscado, recortado; {@code null} si no hay. */
  public String texto() {
    return texto(q);
  }

  /** Recorta y valida un texto de búsqueda cualquiera (por ejemplo, el {@code nombre}). */
  public static String texto(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    String recortado = valor.trim();
    if (recortado.length() > LARGO_MAXIMO_BUSQUEDA) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return recortado;
  }

  /** El orden pedido entre los valores de {@code tipo}; uno desconocido da 400. */
  public <E extends Enum<E>> E orden(Class<E> tipo, E porDefecto) {
    if (orden == null || orden.isBlank()) {
      return porDefecto;
    }
    try {
      return Enum.valueOf(tipo, orden.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
  }
}
