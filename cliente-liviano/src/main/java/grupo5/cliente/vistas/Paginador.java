package grupo5.cliente.vistas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

/**
 * El paginador de una lista (R11-11, componente {@code componentes/paginas}; ficha
 * docs/interfaz/flujos/r11-p1-paginador.md): "« ‹ 1 2 3 … 9 › »" y, en el celular, "‹ 3 de 9 ›".
 * {@code nombre} es el de la navegación para los lectores ("Páginas de donantes"); {@code primera},
 * {@code anterior}, {@code siguiente} y {@code ultima}, las direcciones de esas páginas con la
 * búsqueda y los filtros que había, o null cuando ya se está ahí. {@code tramos}: lo que va entre
 * las flechas, en orden (la plantilla no calcula). {@code cambio}: si se llegó a esta página desde
 * el paginador ({@code ?pagina=} en la dirección): el foco va al título de la lista.
 */
public record Paginador(
    String nombre,
    int pagina,
    int paginas,
    String primera,
    String anterior,
    String siguiente,
    String ultima,
    List<Casilla> tramos,
    boolean cambio) {

  public Paginador {
    tramos = List.copyOf(tramos);
  }

  /**
   * Una casilla entre las flechas: un número con su dirección, la actual (sin dirección) o un hueco
   * "…" ({@code numero} 0).
   */
  public record Casilla(int numero, String direccion) {

    static final int HUECO = 0;

    public boolean hueco() {
      return numero == HUECO;
    }

    public boolean actual() {
      return numero != HUECO && direccion == null;
    }
  }

  /**
   * El paginador de la página {@code pagina} (corregida si se sale del rango) de {@code paginas},
   * con {@code direccion} armando la dirección de cada número; null con una sola página (no se
   * dibuja).
   */
  public static Paginador de(
      String nombre, int pagina, int paginas, IntFunction<String> direccion, boolean cambio) {
    if (paginas <= 1) {
      return null;
    }
    int actual = Math.clamp(pagina, 1, paginas);
    List<Casilla> tramos =
        ventana(actual, paginas).stream()
            .map(n -> new Casilla(n, n == Casilla.HUECO || n == actual ? null : direccion.apply(n)))
            .toList();
    boolean hayAntes = actual > 1;
    boolean hayDespues = actual < paginas;
    return new Paginador(
        nombre,
        actual,
        paginas,
        hayAntes ? direccion.apply(1) : null,
        hayAntes ? direccion.apply(actual - 1) : null,
        hayDespues ? direccion.apply(actual + 1) : null,
        hayDespues ? direccion.apply(paginas) : null,
        tramos,
        cambio);
  }

  /**
   * Los números entre las flechas: la primera, la última y tres seguidas alrededor de la actual (en
   * un extremo, las tres primeras o las tres últimas, para que el ancho no salte). Entre dos
   * números va un hueco (0) solo si esconde dos páginas o más; si escondería una, va ese número.
   */
  static List<Integer> ventana(int actual, int paginas) {
    int desde = Math.max(1, Math.min(actual - 1, paginas - 2));
    int hasta = Math.min(paginas, desde + 2);
    List<Integer> numeros = new ArrayList<>();
    numeros.add(1);
    for (int n = Math.max(2, desde); n <= hasta; n++) {
      numeros.add(n);
    }
    if (hasta < paginas) {
      numeros.add(paginas);
    }
    List<Integer> ventana = new ArrayList<>();
    for (int i = 0; i < numeros.size(); i++) {
      if (i > 0) {
        int salto = numeros.get(i) - numeros.get(i - 1);
        if (salto == 2) {
          ventana.add(numeros.get(i) - 1);
        } else if (salto > 2) {
          ventana.add(Casilla.HUECO);
        }
      }
      ventana.add(numeros.get(i));
    }
    return ventana;
  }
}
