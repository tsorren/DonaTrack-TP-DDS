package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminPersonasPuerto;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Los envíos reales de Donantes, fuera de la maqueta (en la maqueta los atiende SitioController,
 * que vuelve sin guardar). Un archivo que Donaciones no acepta (400) lleva a su pantalla de error.
 */
@Controller
@Profile("!maqueta")
public class AdminDonantesEnvioController {

  private final AdminPersonasPuerto personas;

  AdminDonantesEnvioController(AdminPersonasPuerto personas) {
    this.personas = personas;
  }

  @PostMapping("/admin-donantes-importando")
  String importar(@RequestParam MultipartFile archivo, RedirectAttributes atributos) {
    UUID id;
    try {
      id = personas.importar(archivo);
    } catch (HttpClientErrorException.BadRequest noSeAcepto) {
      return "redirect:/admin-donantes-importar-error";
    }
    String destino =
        UriComponentsBuilder.fromPath("admin-donantes-importando")
            .queryParam("archivo", id)
            .queryParam("nombre", archivo.getOriginalFilename())
            .encode()
            .toUriString();
    return Envios.listo(
        destino,
        "Recibimos el archivo. Podés seguir usando el sitio mientras se importa.",
        atributos);
  }
}
