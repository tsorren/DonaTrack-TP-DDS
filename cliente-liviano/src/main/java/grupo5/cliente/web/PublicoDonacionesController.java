package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entregas confirmadas (RF-01), con las del puerto. El período ({@code ?periodo=semana}) lo elige
 * la plantilla sobre la lista del mes.
 */
@Controller
public class PublicoDonacionesController {

  private final Pantallas pantallas;
  private final PublicoPuerto publico;

  PublicoDonacionesController(Pantallas pantallas, PublicoPuerto publico) {
    this.pantallas = pantallas;
    this.publico = publico;
  }

  @GetMapping("/publico-donaciones")
  String donaciones(Model model) {
    String vista = pantallas.mostrar("publico-donaciones", model);
    model.addAttribute("entregasConfirmadas", publico.entregasConfirmadas());
    return vista;
  }
}
