package grupo5.cliente.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * El final común de un envío real, fuera de la maqueta (conexión de pantallas, SPEC-08): después de
 * guardar en la API, redirige a la pantalla (POST, redirect, GET) y deja el aviso del servidor, que
 * los layouts muestran en la región {@code #avisos} (templates/envio.html). El formulario lleva
 * {@code hx-boost}, {@code hx-indicator} y {@code hx-select-oob="#avisos"}: así lo pide la pieza
 * {@code envio}. Un error de la API no pasa por acá: lo resuelve {@link ErroresHttp}.
 */
public final class Envios {

  /** El atributo que leen los layouts para el aviso. */
  public static final String AVISO = "avisoDelServidor";

  private Envios() {}

  /**
   * Redirige a {@code destino} (una pantalla, con su consulta si hace falta: {@code
   * donante-perfil?seccion=datos}) con el aviso {@code texto}, que sigue la ficha del flujo.
   */
  public static String listo(String destino, String texto, RedirectAttributes atributos) {
    atributos.addFlashAttribute(AVISO, texto);
    return "redirect:/" + destino;
  }
}
