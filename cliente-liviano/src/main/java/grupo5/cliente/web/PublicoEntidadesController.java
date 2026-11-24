package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import grupo5.cliente.vistas.Listado;
import grupo5.cliente.vistas.PublicoEntidadesVista;
import grupo5.cliente.vistas.PublicoEntidadesVista.Entidad;
import grupo5.cliente.vistas.PublicoEntidadesVista.Pedido;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Organizaciones y sus necesidades (RF-22; ola PUB1, PUB1-05), con las del puerto. Busca ({@code
 * ?buscar=}; {@code ?q=} sigue valiendo) y filtra ({@code ?categoria=}, {@code ?pedido=} y {@code
 * ?emergencia=si}) sobre todas las organizaciones, las ordena por tipo y las corta en páginas de
 * {@value #POR_PAGINA} ({@code ?pagina=}). Atributos: {@code entidadesPublicas} (lo que da el
 * puerto), {@code organizaciones} (la página, cada una solo con sus necesidades que cumplen los
 * filtros), {@code grupos} (la página por tipo, en orden), {@code listado} (la barra del componente
 * listado, R11-B04: la categoría y "Emergencias" solo si la fuente los trae), {@code paginador} y,
 * para los textos sin resultados, {@code busqueda}, {@code categoriaElegida} y {@code filtrado}.
 */
@Controller
public class PublicoEntidadesController {

  static final int POR_PAGINA = 6;

  private static final String RECURRENTE = "recurrente";

  private static final List<Listado.Opcion> PEDIDOS =
      List.of(
          new Listado.Opcion("", "Todas las necesidades"),
          new Listado.Opcion(RECURRENTE, "Recurrentes, se repiten cada mes"),
          new Listado.Opcion("extraordinaria", "Extraordinarios, con fecha límite"));

  /** Los grupos por tipo, en el orden de la pantalla; un tipo que no está va a "Otras". */
  private static final Map<String, List<String>> GRUPOS = grupos();

  private static final String OTRAS = "Otras organizaciones";

  private final Pantallas pantallas;
  private final PublicoEntidadesPuerto entidades;

  PublicoEntidadesController(Pantallas pantallas, PublicoEntidadesPuerto entidades) {
    this.pantallas = pantallas;
    this.entidades = entidades;
  }

  private static Map<String, List<String>> grupos() {
    Map<String, List<String>> grupos = new LinkedHashMap<>();
    grupos.put("Comedores y merenderos", List.of("Comedor", "Merendero"));
    grupos.put("Escuelas y bibliotecas", List.of("Escuela", "Biblioteca"));
    grupos.put("Hogares", List.of("Hogar"));
    grupos.put("Clubes", List.of("Club"));
    return grupos;
  }

  @GetMapping("/publico-entidades")
  String entidades(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String pedido,
      @RequestParam(required = false) String emergencia,
      Model model,
      HttpServletRequest peticion) {
    String vista = pantallas.mostrar("publico-entidades", model);
    PublicoEntidadesVista datos = entidades.entidades();
    model.addAttribute("entidadesPublicas", datos);
    // R11-B04: la búsqueda se llama buscar en todas las listas; q sigue valiendo
    String texto = Objects.requireNonNullElse(buscar != null ? buscar : q, "").strip();
    String cat =
        datos.conCategorias() && PublicoMapaController.esCategoria(categoria) ? categoria : "";
    String tipo = PEDIDOS.stream().anyMatch(o -> o.valor().equals(pedido)) ? pedido : "";
    boolean urgentes = datos.conEmergencias() && "si".equals(emergencia);

    Predicate<Pedido> cumple =
        p ->
            (cat.isEmpty() || cat.equals(p.cat()))
                && (tipo.isEmpty() || tipo.equals(p.tipo()))
                && (!urgentes || p.emergencia());
    String buscado = texto.toLowerCase(Locale.ROOT);
    List<Entidad> visibles =
        datos.entidades().stream()
            .filter(
                e -> e.nombre() != null && e.nombre().toLowerCase(Locale.ROOT).contains(buscado))
            .map(e -> conPedidos(e, cumple))
            .filter(e -> !e.pedidos().isEmpty())
            .sorted(Comparator.comparingInt(PublicoEntidadesController::orden))
            .toList();
    Tramo<Entidad> pagina = Tramo.de(visibles, Paginadores.pedida(peticion), POR_PAGINA);
    model.addAttribute("organizaciones", pagina);
    model.addAttribute("grupos", porGrupo(pagina.filas()));
    model.addAttribute("busqueda", texto);
    model.addAttribute(
        "categoriaElegida",
        PublicoMapaController.CATEGORIAS.stream()
            .filter(c -> !cat.isEmpty() && c.valor().equals(cat))
            .findFirst()
            .orElse(null));
    model.addAttribute("filtrado", !cat.isEmpty() || !tipo.isEmpty() || urgentes);
    model.addAttribute("paginador", Paginadores.de(peticion, "Páginas de organizaciones", pagina));

    Listado.Armado barra =
        Listado.en("entidades", "/publico-entidades", "Buscar organizaciones")
            .buscar("Buscar por nombre", texto, "Por ejemplo: comedor.");
    if (datos.conCategorias()) {
      barra.categoria(cat, PublicoMapaController.CATEGORIAS);
    }
    barra.otra(new Listado.Eleccion("pedido", "Tipo de necesidad", tipo, PEDIDOS));
    if (datos.conEmergencias()) {
      barra.casilla("emergencia", "si", "Emergencias", urgentes);
    }
    model.addAttribute("listado", barra.sacar("/publico-entidades").listo());
    return vista;
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

  /** La página por tipo, en el orden de la pantalla, sin los grupos vacíos. */
  private static Map<String, List<Entidad>> porGrupo(List<Entidad> pagina) {
    Map<String, List<Entidad>> grupos = new LinkedHashMap<>();
    List<String> titulos = List.copyOf(GRUPOS.keySet());
    for (Entidad e : pagina) {
      int i = orden(e);
      String titulo = i < titulos.size() ? titulos.get(i) : OTRAS;
      grupos.computeIfAbsent(titulo, t -> new ArrayList<>()).add(e);
    }
    return grupos;
  }
}
