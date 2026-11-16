package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadPerfilPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mi perfil de la entidad con los datos del puerto, también después de guardar ({@code -guardado},
 * que en la maqueta muestra el aviso). Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadPerfilController {

  private final EntidadPerfilPuerto perfiles;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadPerfilController(EntidadPerfilPuerto perfiles, CuentaActual cuentas, Pantallas pantallas) {
    this.perfiles = perfiles;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping({"/entidad-perfil", "/entidad-perfil-guardado"})
  String perfil(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(Entidad.pantalla(pedido), model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("perfilEntidad", perfiles.perfil(entidad.get()));
    return vista;
  }
}
