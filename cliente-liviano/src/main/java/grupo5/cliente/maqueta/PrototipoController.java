package grupo5.cliente.maqueta;

import grupo5.cliente.vistas.CuentaVista;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.web.EstilosController;
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
import org.springframework.web.server.ResponseStatusException;

/**
 * Prototipo navegable del flujo completo de la Alternativa Cozy / Medieval (bitácora, iteración 3):
 * {@code /prototipo/<pantalla>} muestra {@code templates/prototipo/cozy-medieval/<pantalla>.html},
 * con los datos de ejemplo del catálogo ({@link EstilosController}) y los que escribe cada
 * plantilla, y la navegación de su rol (el prefijo del nombre: publico, donante, entidad o admin).
 * No son pantallas del sistema: no tienen gemela en Glassmorphism; cada una se construye en las dos
 * alternativas cuando se apruebe su flujo. Una sección del menú que todavía no tiene plantilla
 * muestra la tierra sin explorar.
 */
@Controller
@Profile("maqueta")
class PrototipoController {

  private static final String CARPETA = "prototipo/cozy-medieval/";
  private static final String NOMBRE = "(publico|donante|entidad|admin)(-[a-z]+)+";
  private static final String NO_EXISTE = "No existe esa pantalla";

  /** Pantallas que el layout privado enlaza fuera del menú y todavía no están dibujadas. */
  private static final Map<String, String> FUERA_DEL_MENU = Map.of("perfil", "Mi perfil");

  private static final String SIN_DIBUJAR = "sin-dibujar";
  private static final String ALTERNATIVA = "cozy-medieval";
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

  /** Las secciones de cada rol, en el orden del menú. Notificaciones va en la carta. */
  private static final Map<String, List<Seccion>> MENUS =
      Map.of(
          "donante",
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "donante-inicio"),
              new Seccion("Mis donaciones", "Donaciones", ICONO_DONACIONES, "donante-donaciones"),
              new Seccion("Entidades", "entidades", "donante-entidades"),
              new Seccion("Incentivos", ICONO_INCENTIVOS, "donante-incentivos"),
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
          "donante", new CuentaVista("Ana", "Donante"),
          "entidad", new CuentaVista("Marta", "Comedor Barrio Ejemplo"),
          "admin", new CuentaVista("Leo", "Administración"));

  private final ResourceLoader recursos;

  PrototipoController(ResourceLoader recursos) {
    this.recursos = recursos;
  }

  /**
   * Una pantalla de nivel 3 lleva el nombre de su sección adelante ({@code
   * donante-donaciones-detalle}): el menú marca esa sección.
   */
  @GetMapping("/prototipo/{pantalla}")
  String pantalla(@PathVariable String pantalla, Model model) {
    boolean dibujada = dibujada(pantalla);
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
    boolean enElMenu = actual != null && actual.pantalla().equals(pantalla);
    String fueraDelMenu =
        pantalla.matches(NOMBRE) ? FUERA_DEL_MENU.get(pantalla.substring(rol.length() + 1)) : null;
    if (!dibujada && !enElMenu && fueraDelMenu == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    EstilosController.datosDeEjemplo(model);
    model.addAttribute("alternativa", ALTERNATIVA);
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
                        "/prototipo/" + seccion.pantalla(),
                        seccion.equals(actual)))
            .toList());
    model.addAttribute("cuenta", CUENTAS.get(rol));
    return CARPETA + (dibujada ? pantalla : SIN_DIBUJAR);
  }

  /**
   * Los formularios del prototipo se envían con POST, para que lo escrito (una contraseña, por
   * ejemplo) no quede en la dirección; vuelven a la pantalla de destino sin sus datos.
   */
  @PostMapping("/prototipo/{pantalla}")
  String enviar(@PathVariable String pantalla) {
    if (!dibujada(pantalla)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    return "redirect:/prototipo/" + pantalla;
  }

  /** La vista se arma solo con un nombre de rol, letras y guiones que existe como plantilla. */
  private boolean dibujada(String pantalla) {
    return pantalla.matches(NOMBRE)
        && recursos.getResource("classpath:templates/" + CARPETA + pantalla + ".html").exists();
  }
}
