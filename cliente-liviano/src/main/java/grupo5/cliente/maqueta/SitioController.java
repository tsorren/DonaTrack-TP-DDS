package grupo5.cliente.maqueta;

import grupo5.cliente.web.Pantallas;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lo que el sitio navegable tiene de más con datos de ejemplo: el catálogo de la interfaz y los
 * formularios, que vuelven a su pantalla sin guardar nada. Las pantallas las sirve {@link
 * Pantallas}.
 */
@Controller
@Profile("maqueta")
class SitioController {

  /**
   * A dónde puede volver Contacto: una pantalla del sitio, con parámetros de letras, números y
   * guiones; nada más, para que no lleve fuera del sitio. Las plantillas de Contacto usan la misma.
   */
  private static final String VOLVER =
      "/" + Pantallas.NOMBRE + "(?:\\?[a-z]+=[a-z0-9-]+(?:&[a-z]+=[a-z0-9-]+)*)?";

  /** Los motivos de Llegó con un problema, los del reporte del contrato de entregas. */
  private static final String MOTIVOS = "incompleta|danada|otro";

  /** IDs de notificaciones que pueden viajar en el formulario de la bandeja. */
  private static final String NOTIFICACION_ID = "[A-Za-z0-9-]+";

  private final Pantallas pantallas;

  SitioController(Pantallas pantallas) {
    this.pantallas = pantallas;
  }

  /**
   * El catálogo de la interfaz (RF-CL-05, D33): página interna, sin rol y fuera del menú. Muestra
   * cada pieza de la capa CUBE con su ejemplo; el índice para los agentes es docs/interfaz/
   * componentes.md.
   */
  @GetMapping("/estilos")
  String estilos() {
    return "estilos";
  }

  /**
   * Los formularios se envían con POST, para que lo escrito (una contraseña, por ejemplo) no quede
   * en la dirección; vuelven a la pantalla de destino sin sus datos. Se conservan dos datos que no
   * son lo escrito: a qué pantalla volver después ({@code ?volver=}, en Contacto, solo si es una
   * pantalla del sitio), la elección de Mi perfil ({@code mostrar=si}) y la sección de Mi perfil
   * que se guardó ({@code seccion=datos} o {@code avisos}), para que el aviso diga lo que se
   * eligió. La pestaña ({@code #datos}) la conserva el navegador al seguir la redirección. Llegó
   * con un problema manda el motivo ({@code incompleta}, {@code danada} u {@code otro}), que vuelve
   * como {@code ?problema=} para que Recepción confirmada muestre el problema asentado.
   */
  @PostMapping("/{pantalla:" + Pantallas.NOMBRE + "}")
  String enviar(
      @PathVariable String pantalla,
      @RequestParam(required = false) String volver,
      @RequestParam(required = false) String mostrar,
      @RequestParam(required = false) String seccion,
      @RequestParam(required = false) String motivo,
      @RequestParam(required = false) List<String> elegidas,
      @RequestParam(required = false) String marcar,
      @RequestParam(name = "no-leer", required = false) String noLeer) {
    if (!pantallas.existe(pantalla)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe esa pantalla");
    }
    List<String> consulta = new ArrayList<>();
    if (volver != null && volver.matches(VOLVER)) {
      consulta.add("volver=" + URLEncoder.encode(volver, StandardCharsets.UTF_8));
    }
    if ("si".equals(mostrar)) {
      consulta.add("mostrar=si");
    }
    if ("datos".equals(seccion) || "avisos".equals(seccion)) {
      consulta.add("seccion=" + seccion);
    }
    if (motivo != null && motivo.matches(MOTIVOS)) {
      consulta.add("problema=" + motivo);
    }
    if (pantalla.endsWith("-notificaciones-leidas")) {
      agregarMarcas(consulta, elegidas, marcar, noLeer);
    }
    String destino = "redirect:/" + pantalla;
    return consulta.isEmpty() ? destino : destino + "?" + String.join("&", consulta);
  }

  /**
   * La bandeja de notificaciones vuelve con las que se marcaron como leídas ({@code ?marcadas=}) y
   * la que se dejó sin leer ({@code ?noLeidas=}); solo pasan los IDs bien escritos.
   */
  private static void agregarMarcas(
      List<String> consulta, List<String> elegidas, String marcar, String noLeer) {
    List<String> ids = new ArrayList<>();
    if (elegidas != null) {
      ids.addAll(elegidas.stream().filter(id -> id.matches(NOTIFICACION_ID)).toList());
    }
    if (marcar != null && marcar.matches(NOTIFICACION_ID)) {
      ids.add(marcar);
    }
    if (elegidas != null || marcar != null || noLeer != null) {
      consulta.add("marcadas=" + String.join(",", ids));
    }
    if (noLeer != null && noLeer.matches(NOTIFICACION_ID)) {
      consulta.add("noLeidas=" + noLeer);
    }
  }
}
