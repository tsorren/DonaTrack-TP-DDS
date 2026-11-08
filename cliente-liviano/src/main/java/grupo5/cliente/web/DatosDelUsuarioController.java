package grupo5.cliente.web;

import grupo5.cliente.puertos.DonacionesComoUsuario;
import grupo5.cliente.seguridad.UsuarioActual;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Las pantallas del donante y de la entidad con los datos reales de Donaciones, de la persona en
 * cuyo nombre opera la cuenta. Al ser más específicas que {@code /{pantalla}}, reemplazan a las de
 * ejemplo.
 */
@Controller
@Profile("!maqueta")
public class DatosDelUsuarioController {

  private final Pantallas pantallas;
  private final DonacionesComoUsuario donaciones;

  DatosDelUsuarioController(Pantallas pantallas, DonacionesComoUsuario donaciones) {
    this.pantallas = pantallas;
    this.donaciones = donaciones;
  }

  @GetMapping("/donante-donaciones")
  String donaciones(@AuthenticationPrincipal OidcUser usuario, Model model) {
    model.addAttribute("donaciones", donaciones.donacionesDe(personaDe(usuario)));
    return pantallas.mostrarReal("donante-donaciones", model);
  }

  @GetMapping("/entidad-necesidades")
  String necesidades(@AuthenticationPrincipal OidcUser usuario, Model model) {
    model.addAttribute("necesidades", donaciones.necesidadesDe(personaDe(usuario)));
    return pantallas.mostrarReal("entidad-necesidades", model);
  }

  private static UUID personaDe(OidcUser usuario) {
    return UsuarioActual.desde(usuario.getClaims()).personaId();
  }
}
