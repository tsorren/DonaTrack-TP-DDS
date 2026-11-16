package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Inicio del admin con los datos del puerto: lo que espera una decisión y cómo está todo. */
@Controller
public class AdminInicioController {

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;

  AdminInicioController(AdminPersonasPuerto personas, Pantallas pantallas) {
    this.personas = personas;
    this.pantallas = pantallas;
  }

  @GetMapping("/admin-inicio")
  String inicio(Model model) {
    String vista = pantallas.mostrar("admin-inicio", model);
    model.addAttribute("adminInicio", personas.inicio());
    return vista;
  }
}
