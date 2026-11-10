package grupo5.cliente.web;

import grupo5.cliente.cuentas.SolicitudesDeEntidad;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Las entidades que esperan su aprobación. La dirección de aprobar es de un solo tramo ({@code
 * /admin-*}) para que la regla de acceso del área de administración la alcance.
 */
@Controller
@Profile("!maqueta")
public class SolicitudesDeEntidadController {

  private final Pantallas pantallas;
  private final SolicitudesDeEntidad solicitudes;

  SolicitudesDeEntidadController(Pantallas pantallas, SolicitudesDeEntidad solicitudes) {
    this.pantallas = pantallas;
    this.solicitudes = solicitudes;
  }

  @GetMapping("/admin-entidades")
  String pendientes(Model model) {
    model.addAttribute("solicitudes", solicitudes.pendientes());
    return pantallas.mostrarReal("admin-entidades", model);
  }

  @PostMapping("/admin-entidades-aprobar")
  String aprobar(@RequestParam String cuenta) {
    solicitudes.aprobar(cuenta);
    return "redirect:/admin-entidades";
  }
}
