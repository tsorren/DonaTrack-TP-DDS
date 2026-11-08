package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentasDelMenu;
import grupo5.cliente.seguridad.Area;
import grupo5.cliente.vistas.EstadoEntrega;
import grupo5.cliente.vistas.OpcionNavegacion;
import grupo5.cliente.vistas.PasoRecorrido;
import grupo5.cliente.vistas.PasoRecorrido.Avance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

/**
 * Arma la pantalla {@code templates/<pantalla>.html} con la navegación de su área (el prefijo del
 * nombre: publico, donante, entidad o admin) y la cuenta que le da {@link CuentasDelMenu}. La usan
 * la maqueta y el sitio real. Una sección del menú que todavía no tiene plantilla muestra la página
 * de sin dibujar. El menú del donante no tiene Incentivos, que RF-07 oculta a la persona jurídica.
 */
@Component
public class Pantallas {

  /**
   * Los nombres de pantalla que sirve el sitio; el resto (hojas, imágenes, scripts) es estático.
   */
  public static final String NOMBRE = "(?:publico|donante|entidad|admin)(?:-[a-z]+)+";

  private static final String NO_EXISTE = "No existe esa pantalla";

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

  /** Las secciones de cada área, en el orden del menú. Notificaciones va aparte, en la cabecera. */
  private static final Map<Area, List<Seccion>> MENUS =
      Map.of(
          Area.DONANTE,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "donante-inicio"),
              new Seccion("Mis donaciones", "Donaciones", ICONO_DONACIONES, "donante-donaciones"),
              new Seccion("Entidades", "entidades", "donante-entidades"),
              new Seccion("Seguimiento", ICONO_SEGUIMIENTO, "donante-seguimiento")),
          Area.ENTIDAD,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "entidad-inicio"),
              new Seccion("Necesidades", "cat-alimentos", "entidad-necesidades"),
              new Seccion(
                  "Donaciones recibidas", "Recibidas", ICONO_DONACIONES, "entidad-recibidas"),
              new Seccion("Seguir entregas", "Entregas", ICONO_SEGUIMIENTO, "entidad-seguimiento")),
          Area.ADMIN,
          List.of(
              new Seccion(INICIO, ICONO_INICIO, "admin-inicio"),
              new Seccion("Donantes", ICONO_INCENTIVOS, "admin-donantes"),
              new Seccion("Entidades", "entidades", "admin-entidades"),
              new Seccion("Donaciones", ICONO_DONACIONES, "admin-donaciones"),
              new Seccion("Asignar donaciones", "Asignar", ICONO_SEGUIMIENTO, "admin-asignaciones"),
              new Seccion("Camiones", "camiones", "admin-camiones"),
              new Seccion("Ranking", ICONO_INCENTIVOS, "admin-ranking")));

  /** El recorrido de la donación en camino; viaje-datos toma de acá los avances de los pasos. */
  private static final List<PasoRecorrido> PASOS =
      List.of(
          new PasoRecorrido("En depósito", "12 de octubre", Avance.HECHO),
          new PasoRecorrido("Asignada", "13 de octubre", Avance.HECHO),
          new PasoRecorrido("Lista para entregar", "14 de octubre", Avance.HECHO),
          new PasoRecorrido("En camino", "Hoy, 14:30", Avance.ACTUAL),
          new PasoRecorrido("Entregada", "Pendiente", Avance.PENDIENTE));

  private final ResourceLoader recursos;
  private final CuentasDelMenu cuentas;

  Pantallas(ResourceLoader recursos, CuentasDelMenu cuentas) {
    this.recursos = recursos;
    this.cuentas = cuentas;
  }

  /** La vista se arma solo con un nombre que ya pasó por la expresión de la dirección. */
  public boolean existe(String pantalla) {
    return recursos.getResource("classpath:templates/" + pantalla + ".html").exists();
  }

  /** La pantalla con los datos del sistema: su modelo y la plantilla de {@code reales/}. */
  public String mostrarReal(String pantalla, Model model) {
    mostrar(pantalla, model);
    return "reales/" + pantalla;
  }

  /** Completa el modelo de la pantalla y devuelve su vista. */
  public String mostrar(String pantalla, Model model) {
    boolean dibujada = existe(pantalla);
    String prefijo = pantalla.split("-")[0];
    Optional<Area> area = Area.deRuta(prefijo);
    List<Seccion> menu = area.map(MENUS::get).orElse(List.of());
    Seccion actual =
        menu.stream()
            .filter(
                seccion ->
                    pantalla.equals(seccion.pantalla())
                        || pantalla.startsWith(seccion.pantalla() + "-"))
            .findFirst()
            .orElse(null);
    String fueraDelMenu = FUERA_DEL_MENU.get(pantalla.substring(prefijo.length() + 1));
    boolean sinDibujar = area.isPresent() && (actual != null || fueraDelMenu != null);
    if (!dibujada && !sinDibujar) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, NO_EXISTE);
    }
    model.addAttribute("completarPerfil", model.containsAttribute("completarPerfil"));
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
    model.addAttribute("cuenta", cuentas.de(prefijo).orElse(null));
    model.addAttribute("perfil", area.map(a -> "/" + a.prefijo() + "-perfil").orElse(null));
    return dibujada ? pantalla : SIN_DIBUJAR;
  }
}
