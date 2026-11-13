package grupo5.cliente.web;

import grupo5.cliente.puertos.ContactoRechazado;
import grupo5.cliente.puertos.PublicoContactoPuerto;
import grupo5.cliente.puertos.PublicoContactoPuerto.Mensaje;
import grupo5.cliente.puertos.PublicoContactoPuerto.Motivo;
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
          "Una necesidad de mi organización", "NECESIDAD",
          "Mi cuenta", "CUENTA",
          "Algo del sitio no anduvo", "SITIO",
          "Otra cosa", OTRO);

  /** A dónde puede volver el enviado: una pantalla del sitio, como en SitioController. */
  private static final String VOLVER =
      "/" + Pantallas.NOMBRE + "(?:\\?[a-z]+=[a-z0-9-]+(?:&[a-z]+=[a-z0-9-]+)*)?";

  /** El formato de los códigos de los avisos, el mismo que valida Notificaciones (HB-21). */
  private static final String CODIGO = "[0-9a-fA-F]{12,32}";

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

  /**
   * {@code sitio} es el campo trampa del ADR de HB-21: oculto para las personas; si llega con
   * texto, muestra el aviso de recibido sin llamar a Notificaciones. Un código que no tiene el
   * formato de los avisos no se manda: el campo es opcional y el mensaje llega igual.
   */
  // ponytail: la IP es getRemoteAddr(), la del navegador porque ningún despliegue pone un proxy
  // delante del cliente; con un proxy, server.forward-headers-strategy=native y probarlo.
  @PostMapping("/publico-contacto-enviado")
  String enviar(
      @RequestParam(required = false) String tema,
      @RequestParam(required = false) String mensaje,
      @RequestParam(required = false) String codigo,
      @RequestParam(required = false) String correo,
      @RequestParam(required = false) UUID claveEnvio,
      @RequestParam(required = false) String volver,
      @RequestParam(required = false) String sitio,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    if (sitio != null && !sitio.isBlank()) {
      return Envios.listo(FORMULARIO, RECIBIDO, atributos);
    }
    Mensaje enviado =
        new Mensaje(
            tema == null ? OTRO : TEMAS.getOrDefault(tema, OTRO),
            mensaje,
            codigo == null || !codigo.strip().matches(CODIGO) ? null : codigo.strip(),
            correo,
            claveEnvio == null ? UUID.randomUUID() : claveEnvio);
    try {
      atributos.addFlashAttribute(
          PublicoContactoController.NUMERO, contacto.enviar(enviado, pedido.getRemoteAddr()));
    } catch (ContactoRechazado rechazo) {
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
