package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Donaciones del admin (RF-15 y RF-16), de a una página (R11-01), y el detalle de una, con los
 * datos del puerto; la lista se llama {@code adminDonaciones} ({@code viaje-datos.html} ya usa
 * {@code donaciones}) y el detalle, {@code donacion}. La lista y el detalle son la misma hoja de la
 * plantilla. {@code -detalle-vencida} es la pantalla de resultado de la maqueta: fuera de ella no
 * existe.
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

  @GetMapping("/admin-donaciones")
  String donaciones(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) String vence,
      @RequestParam(required = false) String orden,
      Model model,
      HttpServletRequest pedido) {
    String vista = pantallas.mostrar("admin-donaciones", model);
    ConsultaDonaciones consulta =
        new ConsultaDonaciones(estado, "pronto".equals(vence), orden, Paginadores.pedida(pedido));
    AdminDonacionesVista datos = operacion.donaciones(consulta, "");
    model.addAttribute("adminDonaciones", datos);
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de donaciones", datos.donaciones()));
    return vista;
  }

  /** El detalle, pedido por su clave ({@code ?donacion=}): ya no se busca en la lista entera. */
  @GetMapping({"/" + DETALLE, "/" + DETALLE + "-vencida"})
  String detalle(
      @RequestParam(required = false) String donacion, Model model, HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    boolean vencida = pantalla.endsWith("-vencida");
    if (vencida && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    AdminDonacionesVista.Donacion elegida = operacion.donacion(donacion, vencida ? "vencida" : "");
    if (elegida == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No encontramos esa donación");
    }
    model.addAttribute("donacion", elegida);
    return vista;
  }

  /**
   * Registrar donación (RF-15): los donantes que coinciden con {@code ?buscar=} (hasta 10) y el
   * elegido con {@code ?donante=}. {@code -registrar-error} y {@code -registrada} son pantallas de
   * la maqueta.
   */
  @GetMapping({
    "/admin-donaciones-registrar",
    "/admin-donaciones-registrar-error",
    "/admin-donaciones-registrada"
  })
  String registrar(
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String donante,
      Model model,
      HttpServletRequest pedido) {
    String pantalla = pedido.getRequestURI().substring(1);
    if (!"admin-donaciones-registrar".equals(pantalla) && !maqueta) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    String vista = pantallas.mostrar(pantalla, model);
    model.addAttribute("registro", operacion.registro(buscar, donante));
    return vista;
  }
}
