package grupo5.cliente.web;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.RankingPuerto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Ranking de donantes del admin (RF-19), con los datos del puerto. La ruta literal le gana a la
 * {@code /{pantalla}} de {@link SitioController}, que sigue armando el menú y la cuenta.
 */
@Controller
public class RankingController {

  private final RankingPuerto rankings;
  private final SitioController sitio;

  RankingController(RankingPuerto rankings, SitioController sitio) {
    this.rankings = rankings;
    this.sitio = sitio;
  }

  @GetMapping("/admin-ranking")
  String ranking(
      @RequestParam(required = false) String respuesta, Model model, HttpSession sesion) {
    // Primero la página: ?respuesta= responde sin llamar a la API.
    String vista = sitio.pagina("admin-ranking", respuesta, null, model, sesion);
    model.addAttribute("ranking", rankings.ranking());
    return vista;
  }
}
