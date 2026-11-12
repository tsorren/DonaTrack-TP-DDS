package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminOperacionPuerto;
import grupo5.cliente.vistas.AdminDonacionesVista;
import grupo5.cliente.vistas.ConsultaDonaciones;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
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

  /** El aviso en el lugar de la ficha (R11-34): {@code no-existe} o {@code no-cargo}. */
  static final String FICHA_AVISO = "fichaAviso";

  private static final Logger LOG = LoggerFactory.getLogger(AdminDonacionesController.class);

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
      @RequestParam(required = false) String buscar,
      @RequestParam(required = false) String categoria,
      @RequestParam(required = false) String registrada,
      @RequestParam(required = false) String donante,
      @RequestParam(required = false) String donacion,
      Model model,
      HttpServletRequest pedido) {
    String vista = pantallas.mostrar("admin-donaciones", model);
    ConsultaDonaciones consulta =
        new ConsultaDonaciones(
            estado,
            "pronto".equals(vence),
            orden,
            Paginadores.pedida(pedido),
            buscar,
            categoria,
            registrada,
            donante,
            donacion);
    AdminDonacionesVista datos = elegida(operacion.donaciones(consulta, ""), model);
    model.addAttribute("adminDonaciones", datos);
    model.addAttribute(
        "paginador", Paginadores.de(pedido, "Páginas de donaciones", datos.donaciones()));
    return vista;
  }

  /**
   * La bandeja (R11-34, ficha r11-p2-admin-bandeja §4 y §5): la ficha de la pedida con {@code
   * ?donacion=} (aunque no esté en la página) o, si no se pidió ninguna, la de la primera de la
   * página, de partida. Una lectura por clave. Si la pedida no existe, {@code fichaAviso} es {@code
   * no-existe}; si no se pudo leer, {@code no-cargo} (con {@code fichaAvisoPartida} si era la de
   * partida, que en el celular no se abre sola): la pantalla lo avisa en el lugar de la ficha y la
   * lista se ve igual.
   */
  private AdminDonacionesVista elegida(AdminDonacionesVista datos, Model model) {
    String pedida = datos.consulta().donacion();
    boolean dePartida = pedida.isEmpty();
    if (dePartida) {
      if (datos.donaciones() == null || datos.donaciones().filas().isEmpty()) {
        return datos;
      }
      pedida = datos.donaciones().filas().getFirst().clave();
    }
    AdminDonacionesVista.Donacion ficha;
    try {
      ficha = operacion.donacion(pedida, "");
    } catch (RestClientException sinFicha) {
      LOG.warn("No se pudo leer la donación {}", pedida, sinFicha);
      model.addAttribute(FICHA_AVISO, "no-cargo");
      model.addAttribute("fichaAvisoPartida", dePartida);
      return datos;
    }
    if (ficha == null) {
      model.addAttribute(FICHA_AVISO, "no-existe");
      model.addAttribute("fichaAvisoPartida", dePartida);
      return datos;
    }
    return datos.conElegida(ficha, dePartida);
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
