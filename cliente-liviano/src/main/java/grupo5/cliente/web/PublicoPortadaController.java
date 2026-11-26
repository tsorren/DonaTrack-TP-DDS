package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La portada ({@code /} y {@code /publico-portada}, la misma plantilla), con las cifras y las
 * entregas confirmadas del mes del puerto (A01: la última, los frutos y las del mes). La ruta
 * literal le gana a la {@code /{pantalla}} de {@link PantallasController}.
 */
@Controller
public class PublicoPortadaController {

  private final Pantallas pantallas;
  private final PublicoPuerto publico;

  PublicoPortadaController(Pantallas pantallas, PublicoPuerto publico) {
    this.pantallas = pantallas;
    this.publico = publico;
  }

  @GetMapping({"/", "/publico-portada"})
  String portada(Model model) {
    String vista = pantallas.mostrar("publico-portada", model);
    model.addAttribute("cifrasDePortada", publico.cifrasDePortada());
    model.addAttribute("entregasConfirmadas", publico.entregasConfirmadas());
    return vista;
  }
}
