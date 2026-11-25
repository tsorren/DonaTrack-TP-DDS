package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.puertos.AdminPersonasPuerto;
import grupo5.cliente.vistas.ConsultaAsignaciones;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

/**
 * El inicio del admin, «Por decidir» (A03, DA-04 y DA-25): las cuentas y las cifras del puerto de
 * personas ({@code adminInicio}) y la primera página de las propuestas sin problemas del puerto de
 * la operación ({@code asignaciones}, la misma vista que la mesa de {@code /admin-asignaciones}).
 * Si Donaciones no da las propuestas, {@code asignaciones} no llega: solo la pestaña muestra su
 * aviso y el resto del inicio se ve igual.
 */
@Controller
public class AdminInicioController {

  private final AdminPersonasPuerto personas;
  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;

  AdminInicioController(
      AdminPersonasPuerto personas, AdminOperacionPuerto operacion, Pantallas pantallas) {
    this.personas = personas;
    this.operacion = operacion;
    this.pantallas = pantallas;
  }

  @GetMapping("/admin-inicio")
  String inicio(Model model) {
    String vista = pantallas.mostrar("admin-inicio", model);
    model.addAttribute("adminInicio", personas.inicio());
    try {
      model.addAttribute(
          "asignaciones", operacion.asignaciones(ConsultaAsignaciones.primera(), ""));
    } catch (RestClientException noRespondio) {
      // La pestaña Propuestas muestra su aviso; las cuentas y las cifras siguen.
    }
    return vista;
  }
}
