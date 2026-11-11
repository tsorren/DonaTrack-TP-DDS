package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoEntidadesPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Entidades y necesidades (RF-22), con las del puerto. El atributo es {@code entidadesPublicas}; la
 * búsqueda ({@code ?q=}) y los filtros los aplica la plantilla sobre la lista.
 */
@Controller
public class PublicoEntidadesController {

  private final Pantallas pantallas;
  private final PublicoEntidadesPuerto entidades;

  PublicoEntidadesController(Pantallas pantallas, PublicoEntidadesPuerto entidades) {
    this.pantallas = pantallas;
    this.entidades = entidades;
  }

  @GetMapping("/publico-entidades")
  String entidades(Model model) {
    String vista = pantallas.mostrar("publico-entidades", model);
    model.addAttribute("entidadesPublicas", entidades.entidades());
    return vista;
  }
}
