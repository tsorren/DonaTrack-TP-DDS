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
 * Camiones del admin (RF-18) con los datos del puerto; el atributo se llama {@code flota}. Las
 * pantallas de resultado ({@code -agregado}, {@code -deshabilitado}, {@code -habilitado} y {@code
 * -agregar-error}) son de la maqueta: fuera de ella el envío vuelve a la lista con el aviso ({@link
 * AdminCamionesEnvios}) y esas direcciones no existen.
 */
@Controller
public class AdminCamionesController {

  private static final String PANTALLA = "admin-camiones";

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminCamionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping({
    "/admin-camiones",
    "/admin-camiones-agregado",
    "/admin-camiones-deshabilitado",
    "/admin-camiones-habilitado",
    "/admin-camiones-agregar-error"
  })
  String camiones(Model model, HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    String accion = pantalla.substring(PANTALLA.length()).replaceFirst("^-", "");
    if (!accion.isEmpty() && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute(
        "flota", operacion.camiones("agregar-error".equals(accion) ? "error" : accion));
    return vista;
  }
}
