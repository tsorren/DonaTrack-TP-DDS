package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Entidades del admin (RF-21 y RF-15) con los datos del puerto, y las pantallas que comparten su
 * hoja después de aprobar, pedir corrección o rechazar (las muestra la maqueta). Reemplaza a la
 * página de solicitudes de antes: las que piden sumarse están en la misma lista.
 */
@Controller
public class AdminEntidadesController {

  private static final String ENTIDADES = "admin-entidades";

  private final AdminPersonasPuerto personas;
  private final Pantallas pantallas;

  AdminEntidadesController(AdminPersonasPuerto personas, Pantallas pantallas) {
    this.personas = personas;
    this.pantallas = pantallas;
  }

  @GetMapping("/" + ENTIDADES)
  String entidades(Model model) {
    return mostrar(ENTIDADES, model);
  }

  @GetMapping("/" + ENTIDADES + "-{accion:aprobada|correccion|rechazada}")
  String despues(@PathVariable String accion, Model model) {
    return mostrar(ENTIDADES + "-" + accion, model);
  }

  private String mostrar(String pantalla, Model model) {
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute("adminEntidades", personas.entidades());
    return vista;
  }
}
