package grupo5.cliente.vistas;

import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Qué página de Organizaciones públicas se pide (RF-22; ola PUB1, PUB1-05): la búsqueda por nombre
 * y los filtros, tal como llegan en la dirección. {@code categoria} y {@code pedido} ya están
 * validados contra las opciones que ofrece la vista ('' es "todas"); {@code emergencias}, solo si
 * la fuente las trae; {@code pagina} empieza en 1. Filtra, ordena por tipo, corta la página y la
 * agrupa por tipo de organización.
 */
public record ConsultaOrganizaciones(
    String buscar, String categoria, String pedido, boolean emergencias, int pagina) {

  /** Los grupos por tipo, en el orden de la pantalla; un tipo que no está va a {@link #OTRAS}. */
  private static final Map<String, List<String>> GRUPOS = grupos();

  static final String OTRAS = "Otras organizaciones";

  public ConsultaOrganizaciones {
    buscar = buscar == null ? "" : buscar.strip();
    categoria = categoria == null ? "" : categoria;
    pedido = pedido == null ? "" : pedido;
    pagina = Math.max(1, pagina);
  }

  private static Map<String, List<String>> grupos() {
    Map<String, List<String>> grupos = new LinkedHashMap<>();
    grupos.put("Comedores y merenderos", List.of("Comedor", "Merendero"));
    grupos.put("Escuelas y bibliotecas", List.of("Escuela", "Biblioteca"));
    grupos.put("Hogares", List.of("Hogar"));
    grupos.put("Clubes", List.of("Club"));
    return grupos;
  }

  /** Si hay algún filtro (la búsqueda no cuenta: tiene su propio texto sin resultados). */
  public boolean filtrada() {
    return !categoria.isEmpty() || !pedido.isEmpty() || emergencias;
  }

  /**
   * La página de las organizaciones que coinciden con la búsqueda y tienen alguna necesidad que
   * cumple los filtros, cada una solo con esas necesidades, ordenadas por tipo.
   */
  public Tramo<Entidad> pagina(PublicoEntidadesVista datos, int tamanio) {
    Predicate<Pedido> cumple =
        p ->
            (categoria.isEmpty() || categoria.equals(p.cat()))
                && (pedido.isEmpty() || pedido.equals(p.tipo()))
                && (!emergencias || p.emergencia());
    String buscado = buscar.toLowerCase(Locale.ROOT);
    List<Entidad> visibles =
        datos.entidades().stream()
            .filter(
                e -> e.nombre() != null && e.nombre().toLowerCase(Locale.ROOT).contains(buscado))
            .map(e -> conPedidos(e, cumple))
            .filter(e -> !e.pedidos().isEmpty())
            .sorted(Comparator.comparingInt(ConsultaOrganizaciones::orden))
            .toList();
    return Tramo.de(visibles, pagina, tamanio);
  }

  /**
   * Clave a la cantidad de necesidades vigentes de cada organización, sin filtrar: «Necesidades
   * satisfechas» cuenta todas las del mes, no solo las que pasan el filtro.
   */
  public static Map<String, Integer> vigentes(PublicoEntidadesVista datos) {
    Map<String, Integer> vigentes = new HashMap<>();
    datos.entidades().forEach(e -> vigentes.putIfAbsent(e.clave(), e.pedidos().size()));
    return vigentes;
  }

  /** La página por tipo, en el orden de la pantalla, sin los grupos vacíos. */
  public static Map<String, List<Entidad>> porGrupo(List<Entidad> pagina) {
    Map<String, List<Entidad>> grupos = new LinkedHashMap<>();
    List<String> titulos = List.copyOf(GRUPOS.keySet());
    for (Entidad e : pagina) {
      int i = orden(e);
      String titulo = i < titulos.size() ? titulos.get(i) : OTRAS;
      grupos.computeIfAbsent(titulo, t -> new ArrayList<>()).add(e);
    }
    return grupos;
  }

  /** La organización con solo las necesidades que cumplen los filtros: primero las emergencias. */
  private static Entidad conPedidos(Entidad e, Predicate<Pedido> cumple) {
    List<Pedido> pedidos =
        e.pedidos().stream()
            .filter(cumple)
            .sorted(Comparator.comparing((Pedido p) -> !p.emergencia()))
            .toList();
    return new Entidad(
        e.clave(),
        e.tipo(),
        e.nombre(),
        e.cat(),
        e.numero(),
        e.cubiertas(),
        e.mision(),
        e.emergencia(),
        pedidos);
  }

  private static int orden(Entidad e) {
    int i = 0;
    for (List<String> tipos : GRUPOS.values()) {
      if (tipos.contains(e.tipo())) {
        return i;
      }
      i++;
    }
    return i;
  }
}
