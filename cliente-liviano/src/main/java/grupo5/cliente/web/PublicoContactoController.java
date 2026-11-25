package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoContactoPuerto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Contacto (ficha raices-contacto.md). El formulario lleva la clave de envío del puerto. El envío
 * real vuelve a Contacto con el aviso breve del número ({@link PublicoContactoEnvioController});
 * {@code -enviado} es Contacto con ese aviso y el número de ejemplo de la maqueta (DA-06, DA-24).
 * Fuera de la maqueta, sin número no hay nada que mostrar: vuelve al formulario.
 */
@Controller
public class PublicoContactoController {

  /** El atributo con el número del mensaje que lee el aviso de publico-contacto-enviado. */
  static final String NUMERO = "numeroDelMensaje";

  private final Pantallas pantallas;
  private final PublicoContactoPuerto contacto;

  PublicoContactoController(Pantallas pantallas, PublicoContactoPuerto contacto) {
    this.pantallas = pantallas;
    this.contacto = contacto;
  }

  @GetMapping("/publico-contacto")
  String contacto(Model model) {
    String vista = pantallas.mostrar("publico-contacto", model);
    model.addAttribute("claveEnvio", contacto.nuevaClave());
    return vista;
  }

  @GetMapping("/publico-contacto-enviado")
  String enviado(Model model) {
    if (!model.containsAttribute(NUMERO)) {
      Long ejemplo = contacto.numeroDeEjemplo();
      if (ejemplo == null) {
        return "redirect:/publico-contacto";
      }
      model.addAttribute(NUMERO, ejemplo);
    }
    String vista = pantallas.mostrar("publico-contacto-enviado", model);
    model.addAttribute("claveEnvio", contacto.nuevaClave());
    return vista;
  }
}
