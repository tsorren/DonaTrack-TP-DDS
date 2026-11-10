package grupo5.cliente.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * La portada ({@code /}). Vive aparte de {@link PantallasController} para que el frente público de
 * la conexión de pantallas (SPEC-08) le sume sus datos sin tocar lo compartido: este archivo es de
 * ese frente.
 */
@Controller
public class PublicoPortadaController {

  private final Pantallas pantallas;

  PublicoPortadaController(Pantallas pantallas) {
    this.pantallas = pantallas;
  }

  @GetMapping("/")
  String portada(Model model) {
    return pantallas.mostrar("publico-portada", model);
  }
}
