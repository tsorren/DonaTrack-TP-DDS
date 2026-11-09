package grupo5.cliente.web;

import grupo5.cliente.puertos.RankingPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Ranking de donantes del admin (RF-19), con los datos del puerto. La ruta literal le gana a la
 * {@code /{pantalla}} de {@link PantallasController}, que sigue armando el menú y la cuenta.
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
  String ranking(Model model) {
    String vista = pantallas.mostrar("admin-ranking", model);
    model.addAttribute("ranking", rankings.ranking());
    return vista;
  }
}
