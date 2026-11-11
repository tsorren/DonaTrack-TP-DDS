package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminDonacionesVista;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * Donaciones del admin (RF-15 y RF-16) y el detalle de una, con los datos del puerto; el atributo
 * se llama {@code adminDonaciones} ({@code viaje-datos.html} ya usa {@code donaciones}). La lista y
 * el detalle son la misma hoja de la plantilla. {@code -detalle-vencida} es la pantalla de
 * resultado de la maqueta: fuera de ella no existe.
 */
@Controller
public class AdminDonacionesController {

  private static final String DETALLE = "admin-donaciones-detalle";

  private final AdminOperacionPuerto operacion;
  private final Pantallas pantallas;
  private final boolean maqueta;

  AdminDonacionesController(
      AdminOperacionPuerto operacion, Pantallas pantallas, Environment entorno) {
    this.operacion = operacion;
    this.pantallas = pantallas;
    this.maqueta = entorno.acceptsProfiles(Profiles.of("maqueta"));
  }

  @GetMapping({"/admin-donaciones", "/" + DETALLE, "/" + DETALLE + "-vencida"})
  String donaciones(Model model, HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    boolean vencida = pantalla.endsWith("-vencida");
    if (vencida && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    AdminDonacionesVista datos = operacion.donaciones(vencida ? "vencida" : "");
    String pedida = pedido.getParameter("donacion");
    if (!maqueta
        && DETALLE.equals(pantalla)
        && datos.todas().stream().noneMatch(d -> d.clave().equals(pedida))) {
      // La maqueta muestra la primera; con datos reales, una que no está es un 404.
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa donación");
    }
    model.addAttribute("adminDonaciones", datos);
    return vista;
  }
}
