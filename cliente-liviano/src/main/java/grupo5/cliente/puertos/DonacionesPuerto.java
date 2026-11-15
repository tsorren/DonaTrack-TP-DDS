package grupo5.cliente.puertos;

import grupo5.cliente.vistas.DetalleDonacionVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.MisDonacionesVista;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Las donaciones de un donante, para Mis donaciones y su detalle. */
public interface DonacionesPuerto {

  /** Cómo escribe la lista el día de registro. */
  DateTimeFormatter DIA_REGISTRADA =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"));

  /**
   * Qué página de Mis donaciones se pide y con qué filtros; un campo null no filtra. {@code mes}:
   * las registradas ese mes ("Ver todas" de un mes del inicio de la persona). {@code categoria}: el
   * valor de una de las categorías del filtro; una que no está se ignora. {@code subcategoria}
   * (RF-05, R11-13): el valor de una subcategoría de alguna de ellas; si está, manda sobre la
   * categoría. {@code pagina}: desde 1.
   *
   * <p>Desde R11-B04 (componente listado): {@code desde} y {@code hasta}, días inclusivos sobre la
   * fecha de registro (null no acota); {@code destino}, parte del nombre de la organización a la
   * que fue (sin mayúsculas ni tildes; null no filtra); {@code antiguas}, de la primera a la más
   * reciente (si no, de la más reciente a la primera).
   */
  record Consulta(
      YearMonth mes,
      EstadoEntrega estado,
      String categoria,
      String subcategoria,
      int pagina,
      LocalDate desde,
      LocalDate hasta,
      String destino,
      boolean antiguas) {

    /** Las donaciones por página de Mis donaciones (R11-01: nunca la lista entera). */
    public static final int POR_PAGINA = 12;

    public Consulta {
      pagina = Math.max(1, pagina);
      destino = destino == null || destino.isBlank() ? null : destino.strip();
    }

    /** Sin fechas, organización ni orden: la consulta de antes de R11-B04. */
    public Consulta(
        YearMonth mes, EstadoEntrega estado, String categoria, String subcategoria, int pagina) {
      this(mes, estado, categoria, subcategoria, pagina, null, null, null, false);
    }

    /** Sin subcategoría. */
    public Consulta(YearMonth mes, EstadoEntrega estado, String categoria, int pagina) {
      this(mes, estado, categoria, null, pagina);
    }

    /** El primer día que entra: el de {@code desde} o, si no, el del mes; null si ninguno. */
    public LocalDate primerDia() {
      LocalDate delMes = mes == null ? null : mes.atDay(1);
      if (desde == null || delMes == null) {
        return desde == null ? delMes : desde;
      }
      return desde.isAfter(delMes) ? desde : delMes;
    }

    /** El último día que entra: el de {@code hasta} o, si no, el del mes; null si ninguno. */
    public LocalDate ultimoDia() {
      LocalDate delMes = mes == null ? null : mes.atEndOfMonth();
      if (hasta == null || delMes == null) {
        return hasta == null ? delMes : hasta;
      }
      return hasta.isBefore(delMes) ? hasta : delMes;
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
    String categoria = consulta.categoriaEn(todas.categorias());
    List<MisDonacionesVista.Donacion> filtradas =
        new ArrayList<>(
            todas.donaciones().stream()
                .filter(
                    d -> Listado.entre(dia(d.donada()), consulta.primerDia(), consulta.ultimoDia()))
                .filter(d -> consulta.estado() == null || d.estado() == consulta.estado())
                .filter(d -> categoria == null || categoria.equals(d.categoria()))
                .filter(
                    d ->
                        consulta.destino() == null
                            || Listado.contiene(d.destino(), consulta.destino()))
                .toList());
    if (consulta.antiguas()) {
      Collections.reverse(filtradas);
    }
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

  /**
   * Si la fuente filtra por la organización a la que fue ({@link Consulta#destino()}, por su
   * nombre). Donaciones solo filtra por el id de la organización: con ella, el campo no se ofrece.
   */
  default boolean filtraPorDestino() {
    return true;
  }

  /** El día de registro escrito como lo muestra la lista ("14 de octubre de 2026"); null si no. */
  private static LocalDate dia(String donada) {
    try {
      return donada == null ? null : LocalDate.parse(donada.strip(), DIA_REGISTRADA);
    } catch (DateTimeParseException otroFormato) {
      return null;
    }
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
