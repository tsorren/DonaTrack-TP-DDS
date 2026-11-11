package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Nosotros, con las cifras del puerto. */
@Controller
public class PublicoNosotrosController {

  private final Pantallas pantallas;
  private final PublicoPuerto publico;

  PublicoNosotrosController(Pantallas pantallas, PublicoPuerto publico) {
    this.pantallas = pantallas;
    this.publico = publico;
  }

  @GetMapping("/publico-nosotros")
  String nosotros(Model model) {
    String vista = pantallas.mostrar("publico-nosotros", model);
    model.addAttribute("cifrasDeNosotros", publico.cifrasDeNosotros());
    return vista;
  }
}
