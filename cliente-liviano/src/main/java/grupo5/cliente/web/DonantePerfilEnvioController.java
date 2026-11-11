package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonantePerfilPuerto;
import grupo5.cliente.vistas.DonantePerfilVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Los formularios de Mi perfil del donante, fuera de la maqueta (en la maqueta los atiende
 * SitioController). "Cómo te ven" guarda {@code mostrarNombre} en Donaciones y vuelve con el aviso
 * de la ficha. Los datos de la cuenta y los avisos todavía no se guardan: no hay operación que no
 * pise los representantes y los medios de la persona, ni modelo de preferencias de avisos; el aviso
 * lo dice.
 */
@Controller
@Profile("!maqueta")
class DonantePerfilEnvioController {

  static final String SIN_GUARDAR =
      "Todavía no podemos guardar estos datos desde acá. Si algo está mal, escribinos.";

  private final DonantePerfilPuerto perfiles;
  private final CuentaActual cuentas;

  DonantePerfilEnvioController(DonantePerfilPuerto perfiles, CuentaActual cuentas) {
    this.perfiles = perfiles;
    this.cuentas = cuentas;
  }

  @PostMapping("/donante-perfil-guardado")
  String guardar(
      @RequestParam(required = false) String mostrar,
      @RequestParam(required = false) String seccion,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    if (seccion != null) {
      return Envios.listo("donante-perfil", SIN_GUARDAR, atributos);
    }
    boolean visible = "si".equals(mostrar);
    perfiles.mostrarNombre(donante.get(), visible);
    DonantePerfilVista perfil = perfiles.perfil(donante.get());
    String aviso =
        visible
            ? "Listo. Las entidades ven tu nombre: «" + perfil.nombre() + "»."
            : "Listo. Las entidades ya no ven tu nombre: ven «" + perfil.anonimo() + "».";
    return Envios.listo("donante-perfil", aviso, atributos);
  }
}
