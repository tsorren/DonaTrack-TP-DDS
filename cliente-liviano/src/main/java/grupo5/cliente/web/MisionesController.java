package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.MisionesPuerto;
import grupo5.cliente.vistas.MisionesVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClientException;

/**
 * Misiones e insignias del donante (RF-07), con los datos del backend de Incentivos. Exclusivo para
 * donantes humanos (personas). Las organizaciones donan sin misiones.
 */
@Controller
public class MisionesController {

  private static final Logger LOG = LoggerFactory.getLogger(MisionesController.class);
  private static final String INGRESAR = "redirect:/publico-ingresar";

  private final MisionesPuerto misiones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  MisionesController(MisionesPuerto misiones, CuentaActual cuentas, Pantallas pantallas) {
    this.misiones = misiones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-misiones")
  String misiones(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-misiones", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    try {
      MisionesVista datos = misiones.misiones(donante.get());
      if (datos.persona()) {
        model.addAttribute("persona", true);
        model.addAttribute("misiones", datos.misiones());
        model.addAttribute("categorias", datos.categorias());
        model.addAttribute("obtenidas", datos.obtenidas());
        model.addAttribute("bajada", datos.bajada());
        model.addAttribute("nueva", datos.nueva());
      } else {
        model.addAttribute("persona", false);
      }
    } catch (RestClientException falla) {
      LOG.error("No se pudieron cargar las misiones del donante {}", donante.get(), falla);
      model.addAttribute("persona", true);
      model.addAttribute("escenario", "error");
    }
    return vista;
  }
}
