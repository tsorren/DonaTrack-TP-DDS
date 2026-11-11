package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadNecesidadesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Necesidades de la entidad (RF-10) con los datos del puerto, también las pantallas de después de
 * un envío en la maqueta ({@code -publicada}, {@code -cambiada}, {@code -baja} y {@code
 * -publicar-error}), que usan su mismo fragmento. Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadNecesidadesController {

  private static final String PANTALLA = "entidad-necesidades";

  private final EntidadNecesidadesPuerto necesidades;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadNecesidadesController(
      EntidadNecesidadesPuerto necesidades, CuentaActual cuentas, Pantallas pantallas) {
    this.necesidades = necesidades;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping({
    "/entidad-necesidades",
    "/entidad-necesidades-publicada",
    "/entidad-necesidades-cambiada",
    "/entidad-necesidades-baja",
    "/entidad-necesidades-publicar-error"
  })
  String necesidades(Model model, HttpServletRequest pedido) {
    String pantalla = Entidad.pantalla(pedido);
    String vista = pantallas.mostrar(pantalla, model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    String accion = pantalla.equals(PANTALLA) ? "" : pantalla.substring(PANTALLA.length() + 1);
    model.addAttribute("necesidadesEntidad", necesidades.necesidades(entidad.get(), accion));
    return vista;
  }
}
