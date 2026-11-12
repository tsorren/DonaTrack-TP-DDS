package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.MisDonacionesVista;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Las donaciones de un donante, para Mis donaciones y su detalle. */
public interface DonacionesPuerto {

  /**
   * Qué página de Mis donaciones se pide y con qué filtros; un campo null no filtra. {@code mes}:
   * las registradas ese mes ("Ver todas" de un mes del inicio de la persona). {@code categoria}: el
   * valor de una de las categorías del filtro; una que no está se ignora. {@code subcategoria}
   * (RF-05, R11-13): el valor de una subcategoría de alguna de ellas; si está, manda sobre la
   * categoría. {@code pagina}: desde 1.
   */
  record Consulta(
      YearMonth mes, EstadoEntrega estado, String categoria, String subcategoria, int pagina) {

    /** Las donaciones por página de Mis donaciones (R11-01: nunca la lista entera). */
    public static final int POR_PAGINA = 12;

    public Consulta {
      pagina = Math.max(1, pagina);
    }

    /** Sin subcategoría. */
    public Consulta(YearMonth mes, EstadoEntrega estado, String categoria, int pagina) {
      this(mes, estado, categoria, null, pagina);
    }

    /** La primera página, sin filtros. */
    public static Consulta primera() {
      return new Consulta(null, null, null, 1);
    }

    /**
     * La categoría que filtra entre {@code categorias}: la de la subcategoría pedida si es de una
     * de ellas; si no, la pedida si está; si no, null.
     */
    public String categoriaEn(List<MisDonacionesVista.Categoria> categorias) {
      return categorias.stream()
          .filter(c -> c.subcategorias().stream().anyMatch(s -> s.valor().equals(subcategoria)))
          .map(MisDonacionesVista.Categoria::valor)
          .findFirst()
          .orElseGet(
              () ->
                  categorias.stream().anyMatch(c -> c.valor().equals(categoria))
                      ? categoria
                      : null);
    }

    /** La subcategoría pedida si es de una de {@code categorias}; si no, null. */
    public String subcategoriaEn(List<MisDonacionesVista.Categoria> categorias) {
      return categorias.stream()
              .flatMap(c -> c.subcategorias().stream())
              .anyMatch(s -> s.valor().equals(subcategoria))
          ? subcategoria
          : null;
    }
  }

  MisDonacionesVista misDonaciones(UUID donante);

  /**
   * Una página de las del donante con los filtros de {@code consulta}. Por defecto filtra y pagina
   * las de {@link #misDonaciones(UUID)} (la maqueta, que tiene pocas de ejemplo; sus donaciones no
   * dicen su subcategoría, así que una subcategoría filtra por su categoría); el adaptador REST se
   * lo pide a Donaciones, que filtra y pagina en la base. Una página más allá de la última muestra
   * la última.
   */
  default MisDonacionesVista misDonaciones(UUID donante, Consulta consulta) {
    MisDonacionesVista todas = misDonaciones(donante);
    String sufijo =
        consulta.mes() == null
            ? null
            : consulta
                .mes()
                .format(
                    DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-AR")));
    String categoria = consulta.categoriaEn(todas.categorias());
    List<MisDonacionesVista.Donacion> filtradas =
        todas.donaciones().stream()
            .filter(d -> sufijo == null || d.donada().endsWith(sufijo))
            .filter(d -> consulta.estado() == null || d.estado() == consulta.estado())
            .filter(d -> categoria == null || categoria.equals(d.categoria()))
            .toList();
    int tamanio = porPagina();
    int paginas = Math.max(1, (filtradas.size() + tamanio - 1) / tamanio);
    int pagina = Math.min(consulta.pagina(), paginas);
    int desde = (pagina - 1) * tamanio;
    return new MisDonacionesVista(
        filtradas.subList(desde, Math.min(filtradas.size(), desde + tamanio)),
        todas.categorias(),
        todas.actualizado(),
        filtradas.size(),
        pagina,
        paginas);
  }

  /** Cuántas van por página; la maqueta usa menos para que sus pocas de ejemplo tengan páginas. */
  default int porPagina() {
    return Consulta.POR_PAGINA;
  }

  /**
   * Una donación del donante por su clave; vacío si no existe o no es suya. Sin clave, la maqueta
   * muestra la que va en camino.
   */
  Optional<DetalleDonacionVista> detalle(UUID donante, String clave);
}
