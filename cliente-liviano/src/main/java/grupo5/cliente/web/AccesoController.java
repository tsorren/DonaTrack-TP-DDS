package grupo5.cliente.web;

import grupo5.cliente.seguridad.Area;
import grupo5.cliente.seguridad.UsuarioActual;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Las pantallas de ingreso y registro las muestra Keycloak: los enlaces del sitio llevan a él. Al
 * volver, {@code /inicio} manda a cada persona al inicio de su área.
 */
@Controller
@Profile("!maqueta")
public class AccesoController {

  private static final String INGRESAR = "redirect:/oauth2/authorization/keycloak";

  @GetMapping("/publico-ingresar")
  String ingresar() {
    return INGRESAR;
  }

  @GetMapping({"/publico-registro", "/publico-registro-donante", "/publico-registro-entidad"})
  String crearCuenta() {
    return INGRESAR + "?prompt=create";
  }

  @GetMapping("/inicio")
  String inicio(@AuthenticationPrincipal OidcUser usuario) {
    return "redirect:"
        + UsuarioActual.desde(usuario.getClaims()).area().map(Area::inicio).orElse("/");
  }
}
