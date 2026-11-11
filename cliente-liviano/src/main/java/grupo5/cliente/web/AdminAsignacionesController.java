package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Asignar donaciones del admin (RF-17) con los datos del puerto; el atributo se llama {@code
 * asignaciones}. Las pantallas de resultado ({@code -aprobada}, {@code -descartada}, {@code -error}
 * y {@code -corrida}) son de la maqueta: fuera de ella el envío vuelve a la lista con el aviso
 * ({@link AdminAsignacionesEnvios}) y esas direcciones no existen. Las variantes de estructura
 * ({@code ?variante=asignaciones-a|b|c}) siguen con sus datos de ejemplo.
 */
@Controller
public class AdminAsignacionesController {

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminAsignacionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping({
    "/admin-asignaciones",
    "/admin-asignaciones-aprobada",
    "/admin-asignaciones-descartada",
    "/admin-asignaciones-error",
    "/admin-asignaciones-corrida"
  })
  String asignaciones(Model model, HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    if (!"admin-asignaciones".equals(pantalla) && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute("asignaciones", operacion.asignaciones());
    return vista;
  }
}
