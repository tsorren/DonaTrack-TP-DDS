package grupo5.cliente.vistas;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * La barra de búsqueda y filtros de una lista (R11-B04, componente {@code componentes/listado}): lo
 * que dibuja y lo que quedó elegido. Los campos van siempre en el mismo orden y con los mismos
 * nombres de parámetro en todas las pantallas: {@code buscar}, la fecha ({@code desde} y {@code
 * hasta}, días inclusivos en ISO), {@code estado}, {@code categoria}, {@code quien} (el donante o
 * la organización del ítem), los propios de la pantalla ({@code otras} y {@code casillas}) y {@code
 * orden}. Un campo null no se dibuja: la pantalla no lo tiene o su fuente no lo resuelve.
 *
 * <p>{@code id} distingue los ids de los controles cuando hay más de una lista; {@code ocultos}, lo
 * que la lista conserva sin mostrarlo (la vista, el donante elegido); {@code sacar}, a dónde lleva
 * "Sacar los filtros" (null, sin el enlace; solo se ve si hay algo buscado o elegido); {@code
 * indicador}, el aviso mientras HTMX trae la lista, para las fuentes que pueden tardar más de 300
 * ms (RNF-UI-06; null, sin aviso).
 */
public record Listado(
    String id,
    String ruta,
    String rotulo,
    Texto buscar,
    Fechas fecha,
    Eleccion estado,
    Eleccion categoria,
    Texto quien,
    List<Eleccion> otras,
    List<Casilla> casillas,
    Eleccion orden,
    List<Oculto> ocultos,
    String sacar,
    Indicador indicador) {

  /** El valor de {@code orden} para "Más antiguas primero"; '' es "Más recientes primero". */
  public static final String ANTIGUAS = "antiguas";

  /** La primera opción de Estado, la misma en todas las listas. */
  public static final String TODOS_LOS_ESTADOS = "Todos los estados";

  /** La primera opción de Categoría, la misma en todas las listas. */
  public static final String TODAS_LAS_CATEGORIAS = "Todas las categorías";

  /** El orden por fecha, el mismo en todas las listas que lo tienen. */
  public static final List<Opcion> POR_FECHA =
      List.of(
          new Opcion("", "Más recientes primero"), new Opcion(ANTIGUAS, "Más antiguas primero"));

  public Listado {
    otras = List.copyOf(otras);
    casillas = List.copyOf(casillas);
    ocultos = List.copyOf(ocultos);
  }

  /** Una barra nueva para la lista de {@code ruta}, sin campos. */
  public static Armado en(String id, String ruta, String rotulo) {
    return new Armado(id, ruta, rotulo);
  }

  /**
   * Cuántos filtros quedaron elegidos (la fecha cuenta una vez; la búsqueda no, porque se ve
   * siempre; el orden, si no es el de partida): lo dice el botón que abre los filtros en el
   * celular.
   */
  public int elegidos() {
    int n = fecha != null && fecha.elegida() ? 1 : 0;
    for (Eleccion e : elecciones()) {
      n += e.elegida() && e.filtra() ? 1 : 0;
    }
    n += quien != null && !quien.valor().isEmpty() ? 1 : 0;
    n += (int) casillas.stream().filter(Casilla::prendida).count();
    return n;
  }

  /**
   * La misma barra con otras categorías, escritas en la plantilla como {@code {valor, texto}} (la
   * bandeja de la persona de la maqueta, que tiene las suyas). Una categoría elegida que no está
   * entre ellas queda en la primera.
   */
  public Listado conCategorias(List<? extends Map<String, ?>> opciones) {
    List<Opcion> nuevas = deLaPlantilla(opciones);
    String elegido =
        categoria != null && nuevas.stream().anyMatch(o -> o.valor().equals(categoria.elegido()))
            ? categoria.elegido()
            : "";
    return new Listado(
        id,
        ruta,
        rotulo,
        buscar,
        fecha,
        estado,
        new Eleccion("categoria", "Categoría", elegido, conTodas(nuevas, TODAS_LAS_CATEGORIAS)),
        quien,
        otras,
        casillas,
        orden,
        ocultos,
        sacar,
        indicador);
  }

  /**
   * La misma barra con otras opciones de Estado, escritas en la plantilla como {@code {valor,
   * texto}} (Organizaciones del admin, que cuenta en la plantilla cuántas hay de cada estado), y la
   * primera con su cuenta ({@code todas}: "Todos los estados · 55"). Un estado elegido que no está
   * entre ellas queda en el primero.
   */
  public Listado conEstados(List<? extends Map<String, ?>> opciones, String todas) {
    List<Opcion> nuevas = deLaPlantilla(opciones);
    String elegido =
        estado != null && nuevas.stream().anyMatch(o -> o.valor().equals(estado.elegido()))
            ? estado.elegido()
            : "";
    return new Listado(
        id,
        ruta,
        rotulo,
        buscar,
        fecha,
        new Eleccion(
            "estado", "Estado", elegido, conTodas(nuevas, TODOS_LOS_ESTADOS + " · " + todas)),
        categoria,
        quien,
        otras,
        casillas,
        orden,
        ocultos,
        sacar,
        indicador);
  }

  private static List<Opcion> deLaPlantilla(List<? extends Map<String, ?>> opciones) {
    return opciones.stream()
        .map(o -> new Opcion(String.valueOf(o.get("valor")), String.valueOf(o.get("texto"))))
        .toList();
  }

  /** Cuántos campos tiene la barra además de la búsqueda. */
  private int campos() {
    return (fecha != null ? 1 : 0)
        + (quien != null ? 1 : 0)
        + elecciones().size()
        + casillas.size();
  }

  /**
   * Si en el celular los campos se pliegan debajo de un botón: solo con dos o más campos además de
   * la búsqueda. Con uno solo (Período, Categoría) o ninguno, quedan a la vista.
   */
  public boolean conPliegue() {
    return campos() >= 2;
  }

  /**
   * El texto del botón que pliega los campos: «Filtros», «Orden» o «Filtros y orden», según lo que
   * la barra tenga (Agrupar cuenta como orden: cambia cómo se ve la lista, no qué entra).
   */
  public String rotuloDelPliegue() {
    boolean filtros =
        fecha != null
            || quien != null
            || !casillas.isEmpty()
            || elecciones().stream().anyMatch(e -> e != orden && e.filtra());
    boolean ordena = orden != null || otras.stream().anyMatch(e -> !e.filtra());
    if (filtros && ordena) {
      return "Filtros y orden";
    }
    return ordena ? "Orden" : "Filtros";
  }

  /** Si la barra es solo una búsqueda: el botón dice «Buscar» y el enlace, «Borrar la búsqueda». */
  public boolean soloBusqueda() {
    return buscar != null && campos() == 0;
  }

  /** La elección propia de la pantalla con ese parámetro, o null si la barra no la tiene. */
  public Eleccion otra(String nombre) {
    return otras.stream().filter(e -> e.nombre().equals(nombre)).findFirst().orElse(null);
  }

  /** Si va "Sacar los filtros": hay a dónde llevar y algo buscado o elegido. */
  public boolean conSacar() {
    return sacar != null && (elegidos() > 0 || buscar != null && !buscar.valor().isEmpty());
  }

  /** Si hay algo buscado o algún filtro elegido (el orden no cuenta): la lista está filtrada. */
  public boolean filtrada() {
    int porOrden = orden != null && orden.elegida() ? 1 : 0;
    return elegidos() > porOrden || buscar != null && !buscar.valor().isEmpty();
  }

  /**
   * {@code opciones} con la de "todos" primero y con el texto común ({@code todos}): la que tiene
   * valor '' cambia su texto; si no hay, se agrega.
   */
  private static List<Opcion> conTodas(List<Opcion> opciones, String todos) {
    List<Opcion> lista = new ArrayList<>();
    lista.add(new Opcion("", todos));
    opciones.stream().filter(o -> !o.valor().isEmpty()).forEach(lista::add);
    return List.copyOf(lista);
  }

  /** Las elecciones en el orden en que se dibujan: estado, categoría, las propias y el orden. */
  private List<Eleccion> elecciones() {
    List<Eleccion> todas = new ArrayList<>();
    if (estado != null) {
      todas.add(estado);
    }
    if (categoria != null) {
      todas.add(categoria);
    }
    todas.addAll(otras);
    if (orden != null) {
      todas.add(orden);
    }
    return todas;
  }

  /**
   * Un día escrito en ISO ({@code 2026-10-15}), como lo manda {@code input type="date"}; null si no
   * hay o no se entiende.
   */
  public static LocalDate dia(String valor) {
    if (valor == null || valor.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(valor.strip());
    } catch (DateTimeParseException malEscrito) {
      return null;
    }
  }

  /**
   * Desde y hasta tal como llegan, ya entendidos: si vienen al revés se dan vuelta (la persona
   * quiso ese tramo). Cualquiera de los dos puede ser null.
   */
  public static LocalDate[] rango(String desde, String hasta) {
    LocalDate d = dia(desde);
    LocalDate h = dia(hasta);
    if (d != null && h != null && d.isAfter(h)) {
      return new LocalDate[] {h, d};
    }
    return new LocalDate[] {d, h};
  }

  /**
   * Si {@code dia} entra entre {@code desde} y {@code hasta} (inclusivos; null no acota). Un día
   * null no entra si hay algún límite.
   */
  public static boolean entre(LocalDate dia, LocalDate desde, LocalDate hasta) {
    if (desde == null && hasta == null) {
      return true;
    }
    return dia != null
        && (desde == null || !dia.isBefore(desde))
        && (hasta == null || !dia.isAfter(hasta));
  }

  /** Si {@code texto} contiene {@code buscado}, sin mayúsculas ni tildes; vacío coincide. */
  public static boolean contiene(String texto, String buscado) {
    if (buscado == null || buscado.isBlank()) {
      return true;
    }
    return texto != null && plano(texto).contains(plano(buscado.strip()));
  }

  private static String plano(String texto) {
    return Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  /** Un campo para escribir: búsqueda o quién. {@code ejemplo} es el texto atenuado. */
  public record Texto(String nombre, String rotulo, String valor, String ejemplo) {
    public Texto {
      valor = valor == null ? "" : valor;
    }
  }

  /**
   * El rango de fechas: {@code rotulo} dice qué fecha es ("Registrada", "Decidida"); {@code desde}
   * y {@code hasta}, en ISO o null.
   */
  public record Fechas(String rotulo, LocalDate desde, LocalDate hasta) {

    /** Si se eligió alguno de los dos días. */
    public boolean elegida() {
      return desde != null || hasta != null;
    }
  }

  /**
   * Un desplegable: su parámetro, su rótulo, el valor elegido ('' es el primero, "todos" o el orden
   * de partida), sus opciones y, si tiene, grupos de opciones ({@code optgroup}) después de ellas.
   * {@code filtra} es false en lo que solo cambia cómo se ve la lista (Agrupar): no cuenta como
   * filtro elegido.
   */
  public record Eleccion(
      String nombre,
      String rotulo,
      String elegido,
      List<Opcion> opciones,
      List<Grupo> grupos,
      boolean filtra) {

    public Eleccion {
      elegido = elegido == null ? "" : elegido;
      opciones = List.copyOf(opciones);
      grupos = grupos == null ? List.of() : List.copyOf(grupos);
    }

    public Eleccion(
        String nombre, String rotulo, String elegido, List<Opcion> opciones, List<Grupo> grupos) {
      this(nombre, rotulo, elegido, opciones, grupos, true);
    }

    public Eleccion(String nombre, String rotulo, String elegido, List<Opcion> opciones) {
      this(nombre, rotulo, elegido, opciones, List.of(), true);
    }

    /** Un desplegable que no filtra: solo cambia cómo se ve la lista (Agrupar). */
    public static Eleccion deVista(
        String nombre, String rotulo, String elegido, List<Opcion> opciones) {
      return new Eleccion(nombre, rotulo, elegido, opciones, List.of(), false);
    }

    /** Si hay algo elegido distinto del primero y es una de las opciones. */
    public boolean elegida() {
      return !elegido.isEmpty()
          && Stream.concat(opciones.stream(), grupos.stream().flatMap(g -> g.opciones().stream()))
              .anyMatch(o -> o.valor().equals(elegido));
    }
  }

  /** Una opción de un desplegable: su valor en la dirección y su texto. */
  public record Opcion(String valor, String texto) {}

  /** Un grupo de opciones de un desplegable. */
  public record Grupo(String texto, List<Opcion> opciones) {
    public Grupo {
      opciones = List.copyOf(opciones);
    }
  }

  /** Un filtro de sí o no ("Ya doné"): su parámetro, su valor, su rótulo y si está prendido. */
  public record Casilla(String nombre, String valor, String rotulo, boolean prendida) {}

  /** Un parámetro que la lista conserva sin mostrarlo. */
  public record Oculto(String nombre, String valor) {}

  /** El aviso mientras HTMX trae la lista: el id del elemento y su texto ("Buscando…"). */
  public record Indicador(String id, String texto) {}

  /** Arma la barra campo por campo, en el orden de siempre. */
  public static final class Armado {
    private final String id;
    private final String ruta;
    private final String rotulo;
    private Texto buscar;
    private Fechas fecha;
    private Eleccion estado;
    private Eleccion categoria;
    private Texto quien;
    private final List<Eleccion> otras = new ArrayList<>();
    private final List<Casilla> casillas = new ArrayList<>();
    private Eleccion orden;
    private final List<Oculto> ocultos = new ArrayList<>();
    private String sacar;
    private Indicador indicador;

    private Armado(String id, String ruta, String rotulo) {
      this.id = Objects.requireNonNull(id);
      this.ruta = Objects.requireNonNull(ruta);
      this.rotulo = Objects.requireNonNull(rotulo);
    }

    public Armado buscar(String rotulo, String valor, String ejemplo) {
      this.buscar = new Texto("buscar", rotulo, valor, ejemplo);
      return this;
    }

    public Armado fecha(String rotulo, LocalDate desde, LocalDate hasta) {
      this.fecha = new Fechas(rotulo, desde, hasta);
      return this;
    }

    public Armado estado(String elegido, List<Opcion> opciones) {
      this.estado =
          new Eleccion("estado", "Estado", elegido, conTodas(opciones, TODOS_LOS_ESTADOS));
      return this;
    }

    public Armado categoria(String elegido, List<Opcion> opciones) {
      this.categoria =
          new Eleccion("categoria", "Categoría", elegido, conTodas(opciones, TODAS_LAS_CATEGORIAS));
      return this;
    }

    public Armado quien(String rotulo, String valor, String ejemplo) {
      this.quien = new Texto("quien", rotulo, valor, ejemplo);
      return this;
    }

    public Armado otra(Eleccion eleccion) {
      otras.add(eleccion);
      return this;
    }

    public Armado casilla(String nombre, String valor, String rotulo, boolean prendida) {
      casillas.add(new Casilla(nombre, valor, rotulo, prendida));
      return this;
    }

    public Armado orden(String elegido, List<Opcion> opciones) {
      this.orden = new Eleccion("orden", "Ordenar", elegido, opciones);
      return this;
    }

    /** Un parámetro que se conserva; uno vacío o null no se escribe. */
    public Armado oculto(String nombre, String valor) {
      if (valor != null && !valor.isEmpty()) {
        ocultos.add(new Oculto(nombre, valor));
      }
      return this;
    }

    /** A dónde lleva "Sacar los filtros" (se ve solo con algo buscado o elegido). */
    public Armado sacar(String direccion) {
      this.sacar = direccion;
      return this;
    }

    /** El aviso mientras HTMX trae la lista ({@code texto}, por ejemplo "Buscando actividad…"). */
    public Armado indicador(String texto) {
      this.indicador = new Indicador(id + "-buscando", texto);
      return this;
    }

    public Listado listo() {
      return new Listado(
          id, ruta, rotulo, buscar, fecha, estado, categoria, quien, otras, casillas, orden,
          ocultos, sacar, indicador);
    }
  }
}
