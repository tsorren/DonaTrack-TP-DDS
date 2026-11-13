package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoMapaPuerto;
import grupo5.cliente.vistas.Paginador;
import grupo5.cliente.vistas.PublicoMapaVista;
import grupo5.cliente.vistas.PublicoMapaVista.Punto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * El mapa de entregadas (RF-02), con el del puerto. La lista va de a una página (R11-19): el
 * atributo {@code mapaPublico} es lo del puerto; {@code listaDelMapa}, todos los puntos del mapa y
 * la página de la lista ({@code ?pagina=}, la última que hay si se pasa) con las entregas de la
 * categoría ({@code ?categoria=}, solo si la API las trae); {@code paginador}, el del sitio; {@code
 * globos}, el enlace de cada globo a la página de su fila, abierta ({@code ?abierta=}).
 */
@Controller
public class PublicoMapaController {

  static final String ABIERTA = "abierta";
  private static final String NOMBRE_DEL_PAGINADOR = "Páginas de la lista del mapa";
  private static final Set<String> CATEGORIAS = Set.of("alimentos", "ropa", "muebles", "escolares");

  private final Pantallas pantallas;
  private final PublicoMapaPuerto mapa;

  PublicoMapaController(Pantallas pantallas, PublicoMapaPuerto mapa) {
    this.pantallas = pantallas;
    this.mapa = mapa;
  }

  @GetMapping("/publico-mapa")
  String mapa(
      @RequestParam(name = "escenario", required = false) String escenario,
      @RequestParam(name = "categoria", required = false) String categoria,
      @RequestParam(name = ABIERTA, required = false) String abierta,
      HttpServletRequest pedido,
      Model model) {
    String vista = pantallas.mostrar("publico-mapa", model);
    PublicoMapaVista datos = mapa.mapa(escenario);
    String elegida =
        datos.conCategorias() && categoria != null && CATEGORIAS.contains(categoria)
            ? categoria
            : "";
    PublicoMapaVista.Lista lista = datos.lista(elegida, Paginadores.pedida(pedido));
    Map<String, String> globos = new LinkedHashMap<>();
    for (Punto punto : lista.puntos()) {
      globos.put(punto.estacion().clave(), aLaFila(pedido, punto));
    }
    model.addAttribute("mapaPublico", datos);
    model.addAttribute("categoriaElegida", elegida);
    model.addAttribute("listaDelMapa", lista);
    model.addAttribute(
        "paginador",
        Paginador.de(
            NOMBRE_DEL_PAGINADOR,
            lista.pagina().pagina(),
            lista.pagina().paginas(),
            numero -> direccion(pedido, numero).replaceQueryParam(ABIERTA).toUriString(),
            pedido.getParameter(Paginadores.PAGINA) != null));
    model.addAttribute("globos", globos);
    model.addAttribute(ABIERTA, abierta);
    return vista;
  }

  /** La página de la fila del punto, con la fila abierta y el ancla para llegar a ella. */
  private static String aLaFila(HttpServletRequest pedido, Punto punto) {
    String clave = punto.estacion().clave();
    return direccion(pedido, punto.pagina())
        .replaceQueryParam(ABIERTA, clave)
        .fragment("entidad-" + clave)
        .toUriString();
  }

  /** La misma dirección con otra página: conserva la categoría y el escenario. */
  private static UriComponentsBuilder direccion(HttpServletRequest pedido, int pagina) {
    return UriComponentsBuilder.fromPath(pedido.getRequestURI())
        .query(pedido.getQueryString())
        .replaceQueryParam(Paginadores.PAGINA, pagina);
  }
}
