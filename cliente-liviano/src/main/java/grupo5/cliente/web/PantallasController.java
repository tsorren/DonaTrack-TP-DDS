package grupo5.cliente.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * {@code /} es la portada y {@code /<pantalla>} muestra {@code templates/<pantalla>.html}. Una
 * pantalla de nivel 3 lleva el nombre de su sección adelante ({@code donante-donaciones-detalle}):
 * el menú marca esa sección.
 */
@Controller
public class PantallasController {

  private final Pantallas pantallas;

  PantallasController(Pantallas pantallas) {
    this.pantallas = pantallas;
  }

  @GetMapping("/")
  String portada(Model model) {
    return pantallas.mostrar("publico-portada", model);
  }

  @GetMapping("/{pantalla:" + Pantallas.NOMBRE + "}")
  String pantalla(@PathVariable String pantalla, Model model) {
    return pantallas.mostrar(pantalla, model);
  }
}
