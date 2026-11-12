package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoMapaPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * El mapa de entregadas (RF-02), con el del puerto. El atributo es {@code mapaPublico}; el filtro
 * por categoría ({@code ?categoria=}) lo aplica la plantilla.
 */
@Controller
public class PublicoMapaController {

  private final Pantallas pantallas;
  private final PublicoMapaPuerto mapa;

  PublicoMapaController(Pantallas pantallas, PublicoMapaPuerto mapa) {
    this.pantallas = pantallas;
    this.mapa = mapa;
  }

  @GetMapping("/publico-mapa")
  String mapa(Model model) {
    String vista = pantallas.mostrar("publico-mapa", model);
    model.addAttribute("mapaPublico", mapa.mapa());
    return vista;
  }
}
