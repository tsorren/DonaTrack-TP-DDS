package grupo5.cliente.web;

import grupo5.cliente.puertos.PublicoContactoPuerto;
import grupo5.cliente.puertos.PublicoContactoPuerto.Mensaje;
import grupo5.cliente.puertos.PublicoContactoPuerto.Motivo;
import grupo5.cliente.puertos.PublicoContactoPuerto.Rechazado;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * El envío real de Contacto, fuera de la maqueta: manda el mensaje a Notificaciones (HB-21) y lleva
 * al enviado con su número. Los textos de los avisos que la ficha no tenía (límite y datos que no
 * validan) están propuestos en raices-contacto.md, pendientes de OK.
 */
@Controller
@Profile("!maqueta")
class PublicoContactoEnvioController {

  private static final String OTRO = "OTRO";

  /** El tema del contrato de cada opción de la plantilla (las opciones no llevan value). */
  private static final Map<String, String> TEMAS =
      Map.of(
          "Una entrega", "ENTREGA",
          "Una donación", "DONACION",
          "Una necesidad de mi entidad", "NECESIDAD",
          "Mi cuenta", "CUENTA",
          "Algo del sitio no anduvo", "SITIO",
          "Otra cosa", OTRO);

  /** A dónde puede volver el enviado: una pantalla del sitio, como en SitioController. */
  private static final String VOLVER =
      "/" + Pantallas.NOMBRE + "(?:\\?[a-z]+=[a-z0-9-]+(?:&[a-z]+=[a-z0-9-]+)*)?";

  private static final String FORMULARIO = "publico-contacto";
  private static final String RECIBIDO = "Recibimos tu mensaje.";
  private static final String LIMITE =
      "No pudimos enviar este mensaje: ya mandaste varios seguidos. Los anteriores nos llegaron;"
          + " probá de nuevo más tarde.";
  private static final String INVALIDO =
      "No pudimos enviar tu mensaje: revisá que esté escrito y que el correo tenga la @.";

  private final PublicoContactoPuerto contacto;

  PublicoContactoEnvioController(PublicoContactoPuerto contacto) {
    this.contacto = contacto;
  }

  // ponytail: la IP es la de quien le habla al cliente; detrás de un proxy, leer X-Forwarded-For.
  @PostMapping("/publico-contacto-enviado")
  String enviar(
      @RequestParam(required = false) String tema,
      @RequestParam(required = false) String mensaje,
      @RequestParam(required = false) String codigo,
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) UUID claveEnvio,
      @RequestParam(required = false) String volver,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    Mensaje enviado =
        new Mensaje(
            tema == null ? OTRO : TEMAS.getOrDefault(tema, OTRO),
            mensaje,
            codigo == null || codigo.isBlank() ? null : codigo.strip(),
            correo,
            claveEnvio == null ? UUID.randomUUID() : claveEnvio);
    try {
      atributos.addFlashAttribute(
          PublicoContactoController.NUMERO, contacto.enviar(enviado, pedido.getRemoteAddr()));
    } catch (Rechazado rechazo) {
      return Envios.listo(
          FORMULARIO, rechazo.motivo() == Motivo.LIMITE ? LIMITE : INVALIDO, atributos);
    }
    String destino =
        volver != null && volver.matches(VOLVER)
            ? "publico-contacto-enviado?volver=" + URLEncoder.encode(volver, StandardCharsets.UTF_8)
            : "publico-contacto-enviado";
    return Envios.listo(destino, RECIBIDO, atributos);
  }
}
