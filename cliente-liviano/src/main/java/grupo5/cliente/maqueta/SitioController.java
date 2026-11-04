package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * El sitio navegable con datos de ejemplo: {@code /<pantalla>} muestra {@code
 * templates/<pantalla>.html}, con los datos que escribe cada plantilla y la navegación de su rol
 * (el prefijo del nombre: publico, donante, entidad o admin). Una sección del menú que todavía no
 * tiene plantilla muestra la página de sin dibujar. La cuenta de ejemplo del donante es una
 * empresa, y su menú no tiene Incentivos, que RF-07 oculta a la persona jurídica.
 */
@Controller
@Profile("maqueta")
class SitioController {

  private static final String NOMBRE = "(?:publico|donante|entidad|admin)(?:-[a-z]+)+";
  private static final String NO_EXISTE = "No existe esa pantalla";

  /**
   * A dónde puede volver Contacto: una pantalla del sitio, con parámetros de letras, números y
   * guiones; nada más, para que no lleve fuera del sitio. Las plantillas de Contacto usan la misma.
   */
  private static final String VOLVER =
      "/" + NOMBRE + "(?:\\?[a-z]+=[a-z0-9-]+(?:&[a-z]+=[a-z0-9-]+)*)?";

  /**
   * Pantallas que el layout privado enlaza fuera del menú; si no están dibujadas, muestran la
   * página de sin dibujar.
   */
  private static final Map<String, String> FUERA_DEL_MENU = Map.of("perfil", "Mi perfil");

  private static final String SIN_DIBUJAR = "sin-dibujar";
  private static final String INICIO = "Inicio";
  private static final String ICONO_INICIO = "inicio";
  private static final String ICONO_DONACIONES = "donaciones";
  private static final String ICONO_SEGUIMIENTO = "seguimiento";
  private static final String ICONO_INCENTIVOS = "incentivos";

  /** Una sección del menú: su nombre, el corto de la barra del celular, su ícono y su pantalla. */
  private record Seccion(String texto, String textoCorto, String icono, String pantalla) {

    Seccion(String texto, String icono, String pantalla) {
      this(texto, texto, icono, pantalla);
    }
  }

  /** Las secciones de cada rol, en el orden del menú. Notificaciones va aparte, en la cabecera. */
  private static final Map<String, List<Seccion>> MENUS =
      Map.of(
          "donante",
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "donante-inicio"),
              new Seccion("Mis donaciones", "Donaciones", ICONO_DONACIONES, "donante-donaciones"),
              new Seccion("Entidades", "entidades", "donante-entidades"),
              new Seccion("Seguimiento", ICONO_SEGUIMIENTO, "donante-seguimiento")),
          "entidad",
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "entidad-inicio"),
              new Seccion("Necesidades", "cat-alimentos", "entidad-necesidades"),
              new Seccion(
                  "Donaciones recibidas", "Recibidas", ICONO_DONACIONES, "entidad-recibidas"),
              new Seccion("Seguir entregas", "Entregas", ICONO_SEGUIMIENTO, "entidad-seguimiento")),
          "admin",
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "admin-inicio"),
              new Seccion("Donantes", ICONO_INCENTIVOS, "admin-donantes"),
              new Seccion("Entidades", "entidades", "admin-entidades"),
              new Seccion("Donaciones", ICONO_DONACIONES, "admin-donaciones"),
              new Seccion("Asignar donaciones", "Asignar", ICONO_SEGUIMIENTO, "admin-asignaciones"),
              new Seccion("Camiones", "camiones", "admin-camiones"),
              new Seccion("Ranking", ICONO_INCENTIVOS, "admin-ranking")));

  private static final Map<String, CuentaVista> CUENTAS =
      Map.of(
          "donante", new CuentaVista("Distribuidora Ejemplo SA", "Empresa donante"),
          "entidad", new CuentaVista("Marta", "Comedor Barrio Ejemplo"),
          "admin", new CuentaVista("Leo", "Administración"));

  /** El recorrido de la donación en camino; viaje-datos toma de acá los avances de los pasos. */
  private static final List<PasoRecorrido> PASOS =
      List.of(
          new PasoRecorrido("En depósito", "12 de octubre", Avance.HECHO),
          new PasoRecorrido("Asignada", "13 de octubre", Avance.HECHO),
          new PasoRecorrido("Lista para entregar", "14 de octubre", Avance.HECHO),
          new PasoRecorrido("En camino", "Hoy, 14:30", Avance.ACTUAL),
          new PasoRecorrido("Entregada", "Pendiente", Avance.PENDIENTE));

  private static final String PUBLICO = "publico";

  private final ResourceLoader recursos;

  SitioController(ResourceLoader recursos) {
    this.recursos = recursos;
  }

  @GetMapping("/")
  String portada(Model model) {
    return mostrar("publico-portada", model);
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
   * Una pantalla de nivel 3 lleva el nombre de su sección adelante ({@code
   * donante-donaciones-detalle}): el menú marca esa sección. La dirección acepta solo nombres de
   * pantalla, para que el resto (hojas, imágenes, scripts) siga siendo estático.
   */
  @GetMapping("/{pantalla:" + NOMBRE + "}")
  String pantalla(@PathVariable String pantalla, Model model) {
    return mostrar(pantalla, model);
  }

  private String mostrar(String pantalla, Model model) {
    boolean dibujada = existe(pantalla);
    String rol = pantalla.split("-")[0];
    List<Seccion> menu = MENUS.getOrDefault(rol, List.of());
    Seccion actual =
        menu.stream()
            .filter(
                seccion ->
                    pantalla.equals(seccion.pantalla())
                        || pantalla.startsWith(seccion.pantalla() + "-"))
            .findFirst()
            .orElse(null);
    String fueraDelMenu = FUERA_DEL_MENU.get(pantalla.substring(rol.length() + 1));
    boolean sinDibujar = !PUBLICO.equals(rol) && (actual != null || fueraDelMenu != null);
    if (!dibujada && !sinDibujar) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    model.addAttribute("estados", EstadoEntrega.values());
    model.addAttribute("pasos", PASOS);
    model.addAttribute("seccion", actual == null ? fueraDelMenu : actual.texto());
    model.addAttribute(
        "navegacion",
        menu.stream()
            .map(
                seccion ->
                    new OpcionNavegacion(
                        seccion.texto(),
                        seccion.textoCorto(),
                        seccion.icono(),
                        "/" + seccion.pantalla(),
                        seccion.equals(actual)))
            .toList());
    model.addAttribute("cuenta", CUENTAS.get(rol));
    model.addAttribute(
        "perfil", menu.isEmpty() || PUBLICO.equals(rol) ? null : "/" + rol + "-perfil");
    return dibujada ? pantalla : SIN_DIBUJAR;
  }

  /**
   * Los formularios se envían con POST, para que lo escrito (una contraseña, por ejemplo) no quede
   * en la dirección; vuelven a la pantalla de destino sin sus datos. Se conservan dos datos que no
   * son lo escrito: a qué pantalla volver después ({@code ?volver=}, en Contacto, solo si es una
   * pantalla del sitio), la elección de Mi perfil ({@code mostrar=si}) y la sección de Mi perfil
   * que se guardó ({@code seccion=datos} o {@code avisos}), para que el aviso diga lo que se
   * eligió. La pestaña ({@code #datos}) la conserva el navegador al seguir la redirección.
   */
  @PostMapping("/{pantalla:" + NOMBRE + "}")
  String enviar(
      @PathVariable String pantalla,
      @RequestParam(required = false) String volver,
      @RequestParam(required = false) String mostrar,
      @RequestParam(required = false) String seccion) {
    if (!existe(pantalla)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
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
    String destino = "redirect:/" + pantalla;
    return consulta.isEmpty() ? destino : destino + "?" + String.join("&", consulta);
  }

  /** La vista se arma solo con un nombre que ya pasó por la expresión de la dirección. */
  private boolean existe(String pantalla) {
    return recursos.getResource("classpath:templates/" + pantalla + ".html").exists();
  }
}
