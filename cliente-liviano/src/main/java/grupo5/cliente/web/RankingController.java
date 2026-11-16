package grupo5.cliente.web;

import grupo5.cliente.puertos.RankingPuerto;
import grupo5.cliente.vistas.Paginador;
import grupo5.cliente.vistas.RankingVista;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Ranking de donantes del admin (RF-19), con los datos del puerto. La ruta literal le gana a la
 * {@code /{pantalla}} de {@link PantallasController}, que sigue armando el menú y la cuenta. Desde
 * R11-43 los meses llegan de a una página ({@code ?pagina=}) y el mes pedido ({@code ?mes=}) se
 * muestra aunque esté en otra; el paginador recorre las páginas del menú "Mes".
 */
@Controller
public class RankingController {

  private final RankingPuerto rankings;
  private final Pantallas pantallas;

  RankingController(RankingPuerto rankings, Pantallas pantallas) {
    this.rankings = rankings;
    this.pantallas = pantallas;
  }

  @GetMapping("/admin-ranking")
  String ranking(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("admin-ranking", model);
    RankingVista ranking = rankings.ranking(pedido.getParameter("mes"), Paginadores.pedida(pedido));
    model.addAttribute("ranking", ranking);
    model.addAttribute(
        "paginador",
        Paginador.de(
            "Páginas de meses",
            ranking.pagina(),
            ranking.paginas(),
            pagina -> direccion(pedido, pagina),
            pedido.getParameter(Paginadores.PAGINA) != null));
    return vista;
  }

  /**
   * La misma pantalla en la página {@code pagina}, sin el mes elegido: así muestra el más reciente
   * de esa página.
   */
  private static String direccion(HttpServletRequest pedido, int pagina) {
    return UriComponentsBuilder.fromPath(pedido.getRequestURI())
        .query(pedido.getQueryString())
        .replaceQueryParam(Paginadores.PAGINA, pagina)
        .replaceQueryParam("mes")
        .build()
        .toUriString();
  }
}
